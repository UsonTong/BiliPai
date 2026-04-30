package com.android.purebilibili.feature.plugin

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.R
import com.android.purebilibili.core.coroutines.AppScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.plugin.Plugin
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.util.Logger
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ServerRack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val CDN_REGION_PLUGIN_ID = "cdn_region"
private const val TAG = "CdnRegionPlugin"

data class PlaybackCdnRewriteResult(
    val videoUrls: List<String>,
    val audioUrls: List<String>,
    val regionLabel: String?
)

interface PlaybackCdnPlugin : Plugin {
    fun rewritePlaybackCandidates(
        videoUrls: List<String>,
        audioUrls: List<String>
    ): PlaybackCdnRewriteResult
}

class CdnRegionPlugin : PlaybackCdnPlugin {
    override val id: String = CDN_REGION_PLUGIN_ID
    override val name: String = "CDN 属地优选"
    override val description: String = "按当前 IP 属地把同地区 B 站视频 CDN 排到线路候选前面"
    override val version: String = "1.0.0"
    override val author: String = "BiliPai项目组"
    override val icon: ImageVector = CupertinoIcons.Outlined.ServerRack
    override val capabilityManifest: PluginCapabilityManifest = PluginCapabilityManifest(
        pluginId = id,
        displayName = name,
        version = version,
        apiVersion = 1,
        entryClassName = "com.android.purebilibili.feature.plugin.CdnRegionPlugin",
        capabilities = setOf(
            PluginCapability.PLAYBACK_CDN,
            PluginCapability.NETWORK,
            PluginCapability.PLUGIN_STORAGE
        )
    )

    @Volatile
    private var cache: CdnRegionPluginCache = CdnRegionPluginCache()

    @Volatile
    private var catalog: Map<String, List<String>> = emptyMap()

    override suspend fun onEnable() {
        val context = PluginManager.getContext()
        catalog = loadCdnRegionCatalog(context)
        cache = CdnRegionPluginStore.read(context)
        AppScope.ioScope.launch {
            delay(1_500L)
            refreshIpLocationIfNeeded()
        }
        Logger.d(TAG, "CDN 属地优选已启用，缓存地区=${cache.selectedRegion.ifBlank { "未命中" }}")
    }

    override suspend fun onDisable() {
        Logger.d(TAG, "CDN 属地优选已禁用")
    }

    override fun rewritePlaybackCandidates(
        videoUrls: List<String>,
        audioUrls: List<String>
    ): PlaybackCdnRewriteResult {
        val snapshot = cache
        val hosts = resolveCdnRegionHosts(
            region = snapshot.selectedRegion,
            cachedHosts = snapshot.selectedHosts,
            catalog = catalog
        )

        if (hosts.isEmpty()) {
            return PlaybackCdnRewriteResult(
                videoUrls = videoUrls.distinct(),
                audioUrls = audioUrls.distinct(),
                regionLabel = null
            )
        }

        return PlaybackCdnRewriteResult(
            videoUrls = rewriteCdnUrlCandidates(videoUrls, hosts).urls,
            audioUrls = rewriteCdnUrlCandidates(audioUrls, hosts).urls,
            regionLabel = snapshot.selectedRegion.takeIf { it.isNotBlank() }
        )
    }

    private suspend fun refreshIpLocationIfNeeded() {
        val context = PluginManager.getContext()
        val enabled = PluginStore.isEnabled(context, id)
        val loadedCatalog = catalog.ifEmpty {
            loadCdnRegionCatalog(context).also { catalog = it }
        }
        if (loadedCatalog.isEmpty()) return
        val current = CdnRegionPluginStore.read(context).also { cache = it }
        val hasSelection = hasUsableCdnRegionSelection(
            region = current.selectedRegion,
            cachedHosts = current.selectedHosts,
            catalog = loadedCatalog
        )

        if (!shouldRefreshCdnIpLocation(
                enabled = enabled,
                nowMs = System.currentTimeMillis(),
                lastRefreshMs = current.refreshedAtMs,
                hasSelection = hasSelection
            )
        ) {
            if (current.selectedHosts != resolveCdnRegionHosts(current.selectedRegion, current.selectedHosts, loadedCatalog)) {
                val corrected = current.copy(
                    selectedHosts = resolveCdnRegionHosts(current.selectedRegion, current.selectedHosts, loadedCatalog)
                )
                cache = corrected
                CdnRegionPluginStore.write(context, corrected)
            }
            return
        }

        try {
            val response = NetworkModule.api.getIpZone()
            val data = response.data
            if (response.code != 0 || data == null) {
                error(response.message.ifBlank { "IP 属地接口返回 code=${response.code}" })
            }

            val location = IpLocationSnapshot(
                country = data.country,
                province = data.province,
                city = data.city
            )
            val selection = selectCdnRegionForLocation(
                location = location,
                catalog = loadedCatalog,
                fallbackRegion = {
                    current.fallbackRegion.takeIf { it in loadedCatalog }
                        ?: loadedCatalog.keys.shuffled().first()
                }
            )
            val next = CdnRegionPluginCache(
                location = location,
                selectedRegion = selection.region,
                selectedHosts = selection.hosts,
                fallbackRegion = if (selection.fallbackUsed) selection.region else current.fallbackRegion,
                fallbackUsed = selection.fallbackUsed,
                refreshedAtMs = System.currentTimeMillis(),
                lastError = null
            )
            cache = next
            CdnRegionPluginStore.write(context, next)
            Logger.d(TAG, "CDN 属地刷新成功: ${location.country}/${location.province}/${location.city} -> ${selection.region}")
        } catch (e: Exception) {
            val preserved = current.copy(lastError = e.message ?: e.javaClass.simpleName)
            cache = preserved
            CdnRegionPluginStore.write(context, preserved)
            Logger.w(TAG, "CDN 属地刷新失败，保留旧缓存: ${e.message}")
        }
    }
}

@Serializable
internal data class CdnRegionPluginCache(
    val location: IpLocationSnapshot = IpLocationSnapshot(),
    val selectedRegion: String = "",
    val selectedHosts: List<String> = emptyList(),
    val fallbackRegion: String = "",
    val fallbackUsed: Boolean = false,
    val refreshedAtMs: Long = 0L,
    val lastError: String? = null
)

internal object CdnRegionPluginStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun read(context: Context): CdnRegionPluginCache {
        val raw = PluginStore.getConfigJson(context, CDN_REGION_PLUGIN_ID) ?: return CdnRegionPluginCache()
        return runCatching { json.decodeFromString<CdnRegionPluginCache>(raw) }
            .getOrDefault(CdnRegionPluginCache())
    }

    suspend fun write(context: Context, cache: CdnRegionPluginCache) {
        PluginStore.setConfigJson(
            context = context,
            pluginId = CDN_REGION_PLUGIN_ID,
            configJson = json.encodeToString(cache)
        )
    }
}

internal fun loadCdnRegionCatalog(context: Context): Map<String, List<String>> {
    return runCatching {
        context.resources.openRawResource(R.raw.cdn_region_catalog).bufferedReader().use { reader ->
            Json.decodeFromString<Map<String, List<String>>>(reader.readText())
        }.filterValues { hosts -> hosts.any { it.isNotBlank() } }
            .mapValues { (_, hosts) -> hosts.filter { it.isNotBlank() }.distinct() }
    }.getOrElse { error ->
        Logger.w(TAG, "读取 CDN catalog 失败: ${error.message}")
        emptyMap()
    }
}
