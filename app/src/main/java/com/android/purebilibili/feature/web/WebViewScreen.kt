package com.android.purebilibili.feature.web

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import kotlinx.coroutines.launch

/**
 * WebViewScreen - 应用内浏览器
 * 
 * 支持拦截 Bilibili 链接并跳转到原生界面：
 * - 视频: bilibili.com/video/BV... 或 av...
 * - UP主空间: space.bilibili.com/{mid}
 * - 直播: live.bilibili.com/{roomId}
 * - 番剧: bilibili.com/bangumi/play/ss{id} 或 ep{id}
 * - 音乐: music.bilibili.com/h5/music-detail?music_id=...
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    title: String? = null,
    onBack: () -> Unit,
    // [新增] 链接拦截回调
    onVideoClick: ((bvid: String) -> Unit)? = null,
    onSpaceClick: ((mid: Long) -> Unit)? = null,
    onLiveClick: ((roomId: Long) -> Unit)? = null,
    onBangumiClick: ((seasonId: Long, epId: Long) -> Unit)? = null,
    onMusicClick: ((musicId: String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = title ?: "浏览器",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Outlined.ChevronBackward, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        
                        // [核心] 自定义 WebViewClient 拦截 Bilibili 链接
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val requestUrl = request?.url?.toString() ?: return false
                                return handleBilibiliUrl(view, requestUrl)
                            }
                            
                            // 兼容旧版 API
                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                return url?.let { handleBilibiliUrl(view, it) } ?: false
                            }
                            
                            /**
                             * 处理 Bilibili URL 拦截
                             * @param webView WebView 实例，用于加载转换后的 URL
                             * @return true 表示已拦截处理，false 表示继续加载网页
                             */
                            private fun handleBilibiliUrl(webView: WebView?, urlString: String): Boolean {
                                android.util.Log.d("WebViewScreen", "🔗 Intercepting URL: $urlString")
                                try {
                                    val uri = Uri.parse(urlString)
                                    val scheme = uri.scheme ?: ""
                                    val host = uri.host ?: ""
                                    
                                    android.util.Log.d("WebViewScreen", "🔍 Scheme: $scheme, Host: $host")

                                    fun dispatchTarget(target: BilibiliNavigationTarget): Boolean {
                                        return when (target) {
                                            is BilibiliNavigationTarget.Video -> {
                                                onVideoClick?.invoke(target.videoId)
                                                onVideoClick != null
                                            }

                                            is BilibiliNavigationTarget.Space -> {
                                                onSpaceClick?.invoke(target.mid)
                                                onSpaceClick != null
                                            }

                                            is BilibiliNavigationTarget.Live -> {
                                                onLiveClick?.invoke(target.roomId)
                                                onLiveClick != null
                                            }

                                            is BilibiliNavigationTarget.BangumiSeason -> {
                                                onBangumiClick?.invoke(target.seasonId, 0)
                                                onBangumiClick != null
                                            }

                                            is BilibiliNavigationTarget.BangumiEpisode -> {
                                                onBangumiClick?.invoke(0, target.epId)
                                                onBangumiClick != null
                                            }

                                            is BilibiliNavigationTarget.Music -> {
                                                onMusicClick?.invoke(target.musicId)
                                                onMusicClick != null
                                            }

                                            is BilibiliNavigationTarget.Dynamic -> false
                                            is BilibiliNavigationTarget.Search -> false
                                        }
                                    }

                                    BilibiliNavigationTargetParser.parse(urlString)?.let { target ->
                                        if (dispatchTarget(target)) {
                                            android.util.Log.d("WebViewScreen", "✅ Routed target: $target")
                                            return true
                                        }
                                    }
                                    
                                    // ===== 0. 处理 bilibili:// Deep Link =====
                                    // 将自定义协议转换为 HTTPS URL 并在 WebView 中加载
                                    if (scheme == "bilibili" || scheme == "bili") {
                                        val convertedUrl = convertDeepLinkToWebUrl(uri)
                                        if (convertedUrl != null) {
                                            android.util.Log.d("WebViewScreen", "🔄 Deep link -> $convertedUrl")
                                            // 在 WebView 中加载转换后的 URL
                                            webView?.loadUrl(convertedUrl)
                                            return true // 拦截原始 deep link
                                        }
                                        android.util.Log.w("WebViewScreen", "⚠️ Unknown deep link: $urlString")
                                        return true // 拦截，防止 ERR_UNKNOWN_URL_SCHEME
                                    }

                                    if (host.contains("b23.tv")) {
                                        scope.launch {
                                            val resolvedTarget = BilibiliNavigationTargetParser.resolve(urlString)
                                            if (resolvedTarget != null && dispatchTarget(resolvedTarget)) {
                                                android.util.Log.d("WebViewScreen", "✅ Routed resolved short link: $resolvedTarget")
                                            } else {
                                                webView?.post { webView.loadUrl(urlString) }
                                            }
                                        }
                                        return true
                                    }
                                    
                                } catch (e: Exception) {
                                    android.util.Log.e("WebViewScreen", "URL parsing error: ${e.message}")
                                }
                                
                                return false // 不拦截，继续加载
                            }
                        }
                        
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    // Avoid reloading on recomposition if URL hasn't changed
                    if (webView.url != url) {
                        webView.loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 将 bilibili:// 深链接转换为 HTTPS 网页 URL
 * 
 * 支持的格式:
 * - bilibili://video/{id} -> https://m.bilibili.com/video/av{id}
 * - bilibili://space/{mid} -> https://space.bilibili.com/{mid}
 * - bilibili://live/{roomId} -> https://live.bilibili.com/{roomId}
 * - bilibili://bangumi/season/{ssid} -> https://m.bilibili.com/bangumi/play/ss{ssid}
 */
private fun convertDeepLinkToWebUrl(uri: android.net.Uri): String? {
    val host = uri.host ?: uri.pathSegments?.getOrNull(0) ?: return null
    val pathSegments = uri.pathSegments ?: return null
    
    android.util.Log.d("WebViewScreen", "🔗 Converting deep link: host=$host, segments=$pathSegments")
    
    return when {
        // bilibili://video/123456 -> https://m.bilibili.com/video/av123456
        host == "video" || (pathSegments.isNotEmpty() && pathSegments[0] == "video") -> {
            val videoId = if (host == "video") {
                pathSegments.getOrNull(0) ?: uri.path?.removePrefix("/")
            } else {
                pathSegments.getOrNull(1)
            }
            if (videoId != null) {
                // 检查是否已经是 BV 格式
                if (videoId.startsWith("BV")) {
                    "https://m.bilibili.com/video/$videoId"
                } else {
                    // [重要] 检查 AV ID 是否有效
                    // 超大 ID (> 10B) 是音乐页面的内部 ID，不是真实视频
                    // 返回 null 阻止转换，防止无限循环
                    val numericId = videoId.toLongOrNull()
                    if (numericId != null && numericId > 10_000_000_000L) {
                        android.util.Log.w("WebViewScreen", "⚠️ Blocking invalid video ID: $numericId")
                        null // 不转换，直接阻止
                    } else {
                        "https://m.bilibili.com/video/av$videoId"
                    }
                }
            } else null
        }
        // bilibili://space/123456
        host == "space" -> {
            val mid = pathSegments.getOrNull(0)
            if (mid != null) "https://space.bilibili.com/$mid" else null
        }
        // bilibili://live/123456
        host == "live" -> {
            val roomId = pathSegments.getOrNull(0)
            if (roomId != null) "https://live.bilibili.com/$roomId" else null
        }
        // bilibili://bangumi/season/123456
        host == "bangumi" -> {
            if (pathSegments.getOrNull(0) == "season") {
                val ssid = pathSegments.getOrNull(1)
                if (ssid != null) "https://m.bilibili.com/bangumi/play/ss$ssid" else null
            } else null
        }
        else -> null
    }
}
