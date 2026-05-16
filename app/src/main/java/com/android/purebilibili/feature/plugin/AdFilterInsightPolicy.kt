package com.android.purebilibili.feature.plugin

import android.content.Context
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val AD_FILTER_HISTORY_STORE_NAME = "filter_history"
private const val AD_FILTER_HISTORY_LIMIT = 120

@Serializable
internal enum class AdFilterReasonType {
    BLOCKED_UP,
    SPONSORED,
    CLICKBAIT,
    CUSTOM_KEYWORD,
    LOW_VIEW
}

@Serializable
internal data class AdFilterRecord(
    val reasonType: AdFilterReasonType,
    val reasonLabel: String,
    val matchedText: String,
    val videoTitle: String,
    val bvid: String,
    val cid: Long,
    val videoCoverUrl: String,
    val upName: String,
    val upFaceUrl: String,
    val upMid: Long,
    val viewCount: Int,
    val timestampMs: Long
)

internal data class AdFilterInsightSummary(
    val totalFilteredCount: Int,
    val blockedUpCount: Int,
    val sponsoredRecords: List<AdFilterRecord>,
    val clickbaitRecords: List<AdFilterRecord>,
    val lowViewRecords: List<AdFilterRecord>,
    val customKeywordRecords: List<AdFilterRecord>,
    val blockedUpProfiles: List<AdFilterBlockedUpProfile>
)

internal data class AdFilterBlockedUpProfile(
    val name: String,
    val faceUrl: String,
    val mid: Long,
    val filteredCount: Int
)

internal fun buildAdFilterRecord(
    item: VideoItem,
    reasonType: AdFilterReasonType,
    matchedText: String,
    timestampMs: Long = System.currentTimeMillis()
): AdFilterRecord {
    return AdFilterRecord(
        reasonType = reasonType,
        reasonLabel = resolveAdFilterReasonLabel(reasonType),
        matchedText = matchedText,
        videoTitle = item.title,
        bvid = item.bvid,
        cid = item.cid,
        videoCoverUrl = item.pic,
        upName = item.owner.name,
        upFaceUrl = item.owner.face,
        upMid = item.owner.mid,
        viewCount = item.stat.view,
        timestampMs = timestampMs
    )
}

internal fun resolveAdFilterInsightSummary(
    records: List<AdFilterRecord>,
    blockedUpNames: List<String>,
    recentLimit: Int = 6
): AdFilterInsightSummary {
    val sortedRecords = records.sortedByDescending { it.timestampMs }
    val safeLimit = recentLimit.coerceAtLeast(1)
    return AdFilterInsightSummary(
        totalFilteredCount = records.size,
        blockedUpCount = blockedUpNames.size,
        sponsoredRecords = sortedRecords
            .filter { it.reasonType == AdFilterReasonType.SPONSORED }
            .take(safeLimit),
        clickbaitRecords = sortedRecords
            .filter { it.reasonType == AdFilterReasonType.CLICKBAIT }
            .take(safeLimit),
        lowViewRecords = sortedRecords
            .filter { it.reasonType == AdFilterReasonType.LOW_VIEW }
            .take(safeLimit),
        customKeywordRecords = sortedRecords
            .filter { it.reasonType == AdFilterReasonType.CUSTOM_KEYWORD }
            .take(safeLimit),
        blockedUpProfiles = resolveAdFilterBlockedUpProfiles(
            records = records,
            blockedUpNames = blockedUpNames
        )
    )
}

internal fun resolveAdFilterBlockedUpProfiles(
    records: List<AdFilterRecord>,
    blockedUpNames: List<String>
): List<AdFilterBlockedUpProfile> {
    return blockedUpNames.map { name ->
        val matchedRecords = records
            .filter { record ->
                record.reasonType == AdFilterReasonType.BLOCKED_UP &&
                    record.upName.equals(name, ignoreCase = true)
            }
            .sortedByDescending { it.timestampMs }
        val latest = matchedRecords.firstOrNull()
        AdFilterBlockedUpProfile(
            name = name,
            faceUrl = latest?.upFaceUrl.orEmpty(),
            mid = latest?.upMid ?: 0L,
            filteredCount = matchedRecords.size
        )
    }
}

internal fun resolveAdFilterReasonLabel(reasonType: AdFilterReasonType): String {
    return when (reasonType) {
        AdFilterReasonType.BLOCKED_UP -> "UP 拉黑"
        AdFilterReasonType.SPONSORED -> "广告推广"
        AdFilterReasonType.CLICKBAIT -> "标题党"
        AdFilterReasonType.CUSTOM_KEYWORD -> "关键词"
        AdFilterReasonType.LOW_VIEW -> "低播放量"
    }
}

internal object AdFilterInsightStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val writeMutex = Mutex()

    suspend fun readRecords(context: Context): List<AdFilterRecord> {
        val value = PluginStore.getDataJson(
            context = context,
            pluginId = ADFILTER_PLUGIN_ID,
            name = AD_FILTER_HISTORY_STORE_NAME
        ) ?: return emptyList()
        return runCatching {
            json.decodeFromString(
                ListSerializer(AdFilterRecord.serializer()),
                value
            )
        }.getOrDefault(emptyList())
    }

    suspend fun appendRecord(
        context: Context,
        record: AdFilterRecord
    ) {
        writeMutex.withLock {
            val nextRecords = (listOf(record) + readRecords(context))
                .distinctBy { "${it.reasonType}:${it.bvid}:${it.cid}:${it.videoTitle}:${it.upMid}" }
                .sortedByDescending { it.timestampMs }
                .take(AD_FILTER_HISTORY_LIMIT)
            PluginStore.setDataJson(
                context = context,
                pluginId = ADFILTER_PLUGIN_ID,
                name = AD_FILTER_HISTORY_STORE_NAME,
                dataJson = json.encodeToString(
                    ListSerializer(AdFilterRecord.serializer()),
                    nextRecords
                )
            )
        }
    }
}
