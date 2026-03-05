// 文件路径: core/util/BilibiliUrlParser.kt
package com.android.purebilibili.core.util

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL

/**
 * Bilibili URL 解析工具类
 * 
 * 支持解析以下格式：
 * - https://www.bilibili.com/video/BVxxxxxx
 * - https://www.bilibili.com/video/avxxxxxx
 * - https://m.bilibili.com/video/BVxxxxxx
 * - https://b23.tv/xxxxxx (短链接，需要重定向)
 * - BV1xx411c7mD / av12345 (纯 ID)
 */
object BilibiliUrlParser {
    
    private const val TAG = "BilibiliUrlParser"
    
    // BV 号正则表达式
    private val BV_REGEX = Regex("BV([a-zA-Z0-9]{10})", RegexOption.IGNORE_CASE)
    
    // AV 号正则表达式
    private val AV_REGEX = Regex("av(\\d+)", RegexOption.IGNORE_CASE)

    // /video/170001 这类纯数字 aid 路径（兼容 bilibili://video/170001 与 https://.../video/170001）
    private val VIDEO_AID_PATH_REGEX = Regex("/video/(\\d+)(?:[/?#]|$)", RegexOption.IGNORE_CASE)
    
    /**
     * 解析结果数据类
     */
    data class ParseResult(
        val bvid: String? = null,
        val aid: Long? = null,
        val isValid: Boolean = false
    ) {
        /**
         * 获取用于 API 调用的视频 ID (优先 BV)
         */
        fun getVideoId(): String? = bvid ?: aid?.let { "av$it" }
    }
    
    /**
     * 从任意文本中提取 Bilibili 视频信息
     * 
     * @param input URL 或包含 URL 的文本
     * @return 解析结果
     */
    fun parse(input: String): ParseResult {
        if (input.isBlank()) return ParseResult()
        
        // 首先尝试直接匹配 BV 号
        BV_REGEX.find(input)?.let { match ->
            val normalizedBvid = "BV${match.groupValues[1]}"
            Logger.d(TAG, "Found BV: $normalizedBvid")
            return ParseResult(bvid = normalizedBvid, isValid = true)
        }
        
        // 尝试匹配 AV 号
        AV_REGEX.find(input)?.let { match ->
            val aid = match.groupValues[1].toLongOrNull()
            if (aid != null) {
                Logger.d(TAG, "Found AV: $aid")
                return ParseResult(aid = aid, isValid = true)
            }
        }

        // 尝试匹配 /video/{aid} 结构
        VIDEO_AID_PATH_REGEX.find(input)?.let { match ->
            val aid = match.groupValues[1].toLongOrNull()
            if (aid != null) {
                Logger.d(TAG, "Found numeric video path aid: $aid")
                return ParseResult(aid = aid, isValid = true)
            }
        }
        
        // 没有找到视频 ID
        Logger.d(TAG, "No video ID found in: $input")
        return ParseResult()
    }
    
    /**
     * 从 Uri 中提取视频信息
     */
    fun parseUri(uri: Uri?): ParseResult {
        if (uri == null) return ParseResult()
        
        val host = uri.host ?: ""
        val path = uri.path ?: ""
        
        Logger.d(TAG, "Parsing URI: host=$host, path=$path")
        
        // b23.tv 短链接需要特殊处理
        if (host.contains("b23.tv")) {
            // 短链接优先尝试路径；失败时回退全量 URI，避免 query/fragment 携带视频 ID 被漏掉。
            val pathResult = parse(path)
            return if (pathResult.isValid) pathResult else parse(uri.toString())
        }
        
        // bilibili.com 链接
        if (host.contains("bilibili.com")) {
            val pathResult = parse(path)
            if (pathResult.isValid) return pathResult
            resolveAidFromVideoPath(
                pathSegments = uri.pathSegments,
                allowBareNumericLeading = false
            )?.let { aid ->
                Logger.d(TAG, "Found structured aid from bilibili.com path: $aid")
                return ParseResult(aid = aid, isValid = true)
            }
            return parse(uri.toString())
        }

        // bilibili:// / bili:// 链接或其他 scheme，先尝试 query 参数再尝试结构化 path。
        val queryCandidates = listOfNotNull(
            uri.getQueryParameter("bvid"),
            uri.getQueryParameter("BVID"),
            uri.getQueryParameter("aid"),
            uri.getQueryParameter("AID"),
            uri.getQueryParameter("avid"),
            uri.getQueryParameter("AVID")
        )
        queryCandidates.forEach { candidate ->
            val parsed = parse(candidate)
            if (parsed.isValid) return parsed
        }
        resolveAidFromVideoPath(
            pathSegments = uri.pathSegments,
            allowBareNumericLeading = true
        )?.let { aid ->
            Logger.d(TAG, "Found structured aid from custom scheme path: $aid")
            return ParseResult(aid = aid, isValid = true)
        }
        
        // 尝试从整个 URI 中提取
        return parse(uri.toString())
    }

    private fun resolveAidFromVideoPath(
        pathSegments: List<String>,
        allowBareNumericLeading: Boolean
    ): Long? {
        if (pathSegments.isEmpty()) return null
        // https://www.bilibili.com/video/170001
        if (pathSegments[0].equals("video", ignoreCase = true)) {
            return pathSegments.getOrNull(1)?.toLongOrNull()
        }
        // bilibili://video/170001 -> host=video, pathSegments[0]=170001
        return if (allowBareNumericLeading) {
            pathSegments[0].toLongOrNull()
        } else {
            null
        }
    }
    
    /**
     * 解析 b23.tv 短链接 (需要在 IO 线程调用)
     * 
     * @param shortUrl b23.tv 短链接
     * @return 完整 URL 或 null
     */
    suspend fun resolveShortUrl(shortUrl: String): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = URL(shortUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "HEAD"
                
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val redirectUrl = connection.getHeaderField("Location")
                    Logger.d(TAG, "Short URL redirected to: $redirectUrl")
                    connection.disconnect()
                    redirectUrl
                } else {
                    connection.disconnect()
                    null
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to resolve short URL: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 从任意文本中提取所有可能的 URL
     */
    fun extractUrls(text: String): List<String> {
        val urlRegex = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
        return urlRegex.findAll(text).map { it.value }.toList()
    }
}
