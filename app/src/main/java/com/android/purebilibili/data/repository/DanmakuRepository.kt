// 文件路径: data/repository/DanmakuRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * 弹幕相关数据仓库
 * 从 VideoRepository 拆分出来，专注于弹幕功能
 */
object DanmakuRepository {
    private val api = NetworkModule.api

    // 弹幕数据缓存 - 避免横竖屏切换时重复下载
    private val danmakuCache = LinkedHashMap<Long, ByteArray>(5, 0.75f, true)
    private const val MAX_DANMAKU_CACHE_COUNT = 3  // 最多缓存3个视频的弹幕
    private const val MAX_DANMAKU_CACHE_BYTES = 4L * 1024 * 1024
    private var danmakuCacheBytes = 0L
    
    // Protobuf 弹幕分段缓存
    private val danmakuSegmentCache = LinkedHashMap<Long, List<ByteArray>>(5, 0.75f, true)
    private const val MAX_SEGMENT_CACHE_COUNT = 3
    private const val MAX_SEGMENT_CACHE_BYTES = 12L * 1024 * 1024
    private const val MAX_SEGMENT_PARALLELISM = 3
    private var danmakuSegmentCacheBytes = 0L

    /**
     * 清除弹幕缓存
     */
    fun clearDanmakuCache() {
        synchronized(danmakuCache) {
            danmakuCache.clear()
            danmakuCacheBytes = 0L
        }
        synchronized(danmakuSegmentCache) {
            danmakuSegmentCache.clear()
            danmakuSegmentCacheBytes = 0L
        }
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku cache cleared")
    }

    /**
     * 获取 XML 格式弹幕原始数据
     */
    suspend fun getDanmakuRawData(cid: Long): ByteArray? = withContext(Dispatchers.IO) {
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🎯 getDanmakuRawData: cid=$cid")
        
        // 先检查缓存
        synchronized(danmakuCache) {
            danmakuCache[cid]?.let {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku cache hit for cid=$cid, size=${it.size}")
                return@withContext it
            }
        }
        
        try {
            val responseBody = api.getDanmakuXml(cid)
            val bytes = responseBody.bytes()
            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🎯 Danmaku raw bytes: ${bytes.size}, first byte: ${if (bytes.isNotEmpty()) String.format("0x%02X", bytes[0]) else "empty"}")

            if (bytes.isEmpty()) {
                android.util.Log.w("DanmakuRepo", " Danmaku response is empty!")
                return@withContext null
            }

            val result: ByteArray?
            
            // 检查首字节判断是否压缩
            // XML 以 '<' 开头 (0x3C)
            if (bytes[0] == 0x3C.toByte()) {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku is plain XML, size=${bytes.size}")
                result = bytes
            } else {
                // 尝试 Deflate 解压
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku appears compressed, attempting deflate...")
                result = try {
                    val inflater = java.util.zip.Inflater(true) // nowrap=true
                    inflater.setInput(bytes)
                    val outputStream = java.io.ByteArrayOutputStream(bytes.size * 3)
                    val tempBuffer = ByteArray(1024)
                    while (!inflater.finished()) {
                        val count = inflater.inflate(tempBuffer)
                        if (count == 0) {
                             if (inflater.needsInput()) break
                             if (inflater.needsDictionary()) break
                        }
                        outputStream.write(tempBuffer, 0, count)
                    }
                    inflater.end()
                    val decompressed = outputStream.toByteArray()
                    com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku decompressed: ${bytes.size} → ${decompressed.size} bytes")
                    decompressed
                } catch (e: Exception) {
                    android.util.Log.e("DanmakuRepo", " Deflate failed: ${e.message}")
                    e.printStackTrace()
                    // 解压失败，返回原始数据
                    bytes
                }
            }
            
            // 存入缓存（限制条目数与字节数）
            if (result != null && result.isNotEmpty()) {
                val entrySize = result.size.toLong()
                if (entrySize <= MAX_DANMAKU_CACHE_BYTES) {
                    synchronized(danmakuCache) {
                        danmakuCache.remove(cid)?.let { danmakuCacheBytes -= it.size.toLong() }
                        
                        val iterator = danmakuCache.entries.iterator()
                        while (iterator.hasNext() &&
                            (danmakuCache.size >= MAX_DANMAKU_CACHE_COUNT ||
                                danmakuCacheBytes + entrySize > MAX_DANMAKU_CACHE_BYTES)
                        ) {
                            val eldest = iterator.next()
                            danmakuCacheBytes -= eldest.value.size.toLong()
                            iterator.remove()
                            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku cache evicted: cid=${eldest.key}")
                        }
                        danmakuCache[cid] = result
                        danmakuCacheBytes += entrySize
                        com.android.purebilibili.core.util.Logger.d(
                            "DanmakuRepo",
                            " Danmaku cached: cid=$cid, size=${result.size}, cacheSize=${danmakuCache.size}, bytes=$danmakuCacheBytes"
                        )
                    }
                } else {
                    com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku too large to cache: size=$entrySize")
                }
            }
            
            result
        } catch (e: Exception) {
            android.util.Log.e("DanmakuRepo", " getDanmakuRawData failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 获取弹幕元数据 (High-Energy, Command Dms, etc.)
     */
    suspend fun getDanmakuView(cid: Long, aid: Long): com.android.purebilibili.feature.video.danmaku.DanmakuProto.DmWebViewReply? = withContext(Dispatchers.IO) {
        try {
             com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🎯 getDanmakuView: cid=$cid, aid=$aid")
             val responseBody = api.getDanmakuView(oid = cid, pid = aid)
             val bytes = responseBody.bytes()
             if (bytes.isNotEmpty()) {
                 val result = com.android.purebilibili.feature.video.danmaku.DanmakuParser.parseWebViewReply(bytes)
                 com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Metadata parsed: count=${result.count}, special=${result.specialDms.size}, command=${result.commandDms.size}")
                 result
             } else {
                 null
             }
        } catch (e: Exception) {
             android.util.Log.e("DanmakuRepo", " getDanmakuView failed: ${e.message}")
             null
        }
    }
    
    /**
     * 获取 Protobuf 格式弹幕 (分段加载)
     * 
     * @param cid 视频 cid
     * @param durationMs 视频时长 (毫秒)，用于计算所需分段数
     * @return 所有分段的 Protobuf 数据列表
     */
    suspend fun getDanmakuSegments(cid: Long, durationMs: Long): List<ByteArray> = withContext(Dispatchers.IO) {
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🎯 getDanmakuSegments: cid=$cid, duration=${durationMs}ms")
        
        // 检查缓存
        synchronized(danmakuSegmentCache) {
            danmakuSegmentCache[cid]?.let {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Protobuf danmaku cache hit: cid=$cid, segments=${it.size}")
                return@withContext it
            }
        }
        
        // 计算所需分段数 (每段 6 分钟 = 360000ms)
        val segmentDurationMs = 360000L
        val segmentCount = ((durationMs + segmentDurationMs - 1) / segmentDurationMs).toInt().coerceAtLeast(1)
        
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Fetching $segmentCount segments for ${durationMs}ms video")
        
        data class SegmentResult(val index: Int, val bytes: ByteArray)
        
        // 并发获取分段，限制并发度避免过载
        val segmentResults = coroutineScope {
            val semaphore = Semaphore(MAX_SEGMENT_PARALLELISM)
            (1..segmentCount).map { index ->
                async {
                    semaphore.withPermit {
                        try {
                            val response = api.getDanmakuSeg(oid = cid, segmentIndex = index)
                            val bytes = response.bytes()
                            if (bytes.isNotEmpty()) {
                                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Segment $index: ${bytes.size} bytes")
                                SegmentResult(index, bytes)
                            } else {
                                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Segment $index is empty")
                                null
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("DanmakuRepo", " Segment $index failed: ${e.message}")
                            null
                        }
                    }
                }
            }.awaitAll()
        }
        
        val results = segmentResults
            .filterNotNull()
            .sortedBy { it.index }
            .map { it.bytes }
        
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Got ${results.size}/$segmentCount segments for cid=$cid")
        
        // 缓存结果（限制条目数与字节数）
        if (results.isNotEmpty()) {
            val entrySize = results.sumOf { it.size.toLong() }
            if (entrySize <= MAX_SEGMENT_CACHE_BYTES) {
                synchronized(danmakuSegmentCache) {
                    danmakuSegmentCache.remove(cid)?.let { removed ->
                        danmakuSegmentCacheBytes -= removed.sumOf { it.size.toLong() }
                    }
                    
                    val iterator = danmakuSegmentCache.entries.iterator()
                    while (iterator.hasNext() &&
                        (danmakuSegmentCache.size >= MAX_SEGMENT_CACHE_COUNT ||
                            danmakuSegmentCacheBytes + entrySize > MAX_SEGMENT_CACHE_BYTES)
                    ) {
                        val eldest = iterator.next()
                        danmakuSegmentCacheBytes -= eldest.value.sumOf { it.size.toLong() }
                        iterator.remove()
                    }
                    
                    danmakuSegmentCache[cid] = results.toList()
                    danmakuSegmentCacheBytes += entrySize
                }
            } else {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Segments too large to cache: size=$entrySize")
            }
        }
        
        results.toList()
    }
    
    /**
     * 发送弹幕
     * 
     * @param aid 视频 aid (必需)
     * @param cid 视频 cid (必需)
     * @param message 弹幕内容 (最多 100 字)
     * @param progress 弹幕出现时间 (毫秒)
     * @param color 弹幕颜色 (十进制 RGB，默认白色 16777215)
     * @param fontSize 字号: 18=小, 25=中(默认), 36=大
     * @param mode 模式: 1=滚动(默认), 4=底部, 5=顶部
     * @return 发送结果，包含弹幕 ID
     */
    suspend fun sendDanmaku(
        aid: Long,
        cid: Long,
        message: String,
        progress: Long,
        color: Int = 16777215,
        fontSize: Int = 25,
        mode: Int = 1
    ): Result<com.android.purebilibili.data.model.response.SendDanmakuData> = withContext(Dispatchers.IO) {
        try {
            // 验证登录状态
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            
            // 验证弹幕内容
            if (message.isBlank()) {
                return@withContext Result.failure(Exception("弹幕内容不能为空"))
            }
            if (message.length > 100) {
                return@withContext Result.failure(Exception("弹幕内容过长，最多 100 字"))
            }
            
            com.android.purebilibili.core.util.Logger.d(
                "DanmakuRepo",
                "📤 sendDanmaku: aid=$aid, cid=$cid, msg=$message, progress=${progress}ms, color=$color, mode=$mode"
            )
            
            val response = api.sendDanmaku(
                oid = cid,
                aid = aid,
                msg = message,
                progress = progress,
                color = color,
                fontsize = fontSize,
                mode = mode,
                csrf = csrf
            )
            
            if (response.code == 0 && response.data != null) {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "✅ Danmaku sent: dmid=${response.data.dmid_str}")
                Result.success(response.data)
            } else {
                val errorMsg = when (response.code) {
                    -101 -> "请先登录"
                    -102 -> "账号被封禁"
                    -111 -> "鉴权失败，请重新登录"
                    -400 -> "请求参数错误"
                    -509 -> "请求过于频繁，请稍后再试"
                    36700 -> "弹幕内容包含敏感词"
                    36701 -> "弹幕发送冷却中"
                    36702 -> "弹幕字数过多"
                    36703 -> "弹幕被禁用"
                    36704 -> "禁止向此视频发送弹幕"
                    36705 -> "弹幕包含被禁止的内容"
                    else -> response.message.ifEmpty { "发送弹幕失败 (${response.code})" }
                }
                android.util.Log.e("DanmakuRepo", "❌ sendDanmaku failed: ${response.code} - ${response.message}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("DanmakuRepo", "❌ sendDanmaku exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 撤回弹幕
     * 
     * 仅能撤回自己 2 分钟内的弹幕，每天 3 次机会
     * 
     * @param cid 视频 cid
     * @param dmid 弹幕 ID
     * @return 撤回结果 (message 包含剩余次数)
     */
    suspend fun recallDanmaku(
        cid: Long,
        dmid: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }

            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "📤 recallDanmaku: cid=$cid, dmid=$dmid")
            
            val response = api.recallDanmaku(cid = cid, dmid = dmid, csrf = csrf)
            
            if (response.code == 0) {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "✅ Danmaku recalled: ${response.message}")
                Result.success(response.message)
            } else {
                val errorMsg = when (response.code) {
                    -101 -> "请先登录"
                    -111 -> "鉴权失败，请重新登录"
                    -400 -> "请求参数错误"
                    36301 -> "撤回次数已用完" 
                    36302 -> "弹幕发送超过2分钟，无法撤回"
                    36303 -> "该弹幕无法撤回"
                    else -> response.message.ifEmpty { "撤回失败 (${response.code})" }
                }
                android.util.Log.e("DanmakuRepo", "❌ recallDanmaku failed: ${response.code} - ${response.message}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("DanmakuRepo", "❌ recallDanmaku exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 点赞弹幕
     * 
     * @param cid 视频 cid
     * @param dmid 弹幕 ID
     * @param like true=点赞, false=取消点赞
     */
    suspend fun likeDanmaku(
        cid: Long,
        dmid: Long,
        like: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }

            val op = if (like) 1 else 2
            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "📤 likeDanmaku: cid=$cid, dmid=$dmid, op=$op")
            
            val response = api.likeDanmaku(oid = cid, dmid = dmid, op = op, csrf = csrf)
            
            if (response.code == 0) {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "✅ Danmaku ${if (like) "liked" else "unliked"}")
                Result.success(Unit)
            } else {
                val errorMsg = when (response.code) {
                    -101 -> "请先登录"
                    -111 -> "鉴权失败，请重新登录"
                    -400 -> "请求参数错误"
                    65004 -> "已经点过赞了"
                    65005 -> "已经取消点赞了"
                    else -> response.message.ifEmpty { "操作失败 (${response.code})" }
                }
                android.util.Log.e("DanmakuRepo", "❌ likeDanmaku failed: ${response.code} - ${response.message}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("DanmakuRepo", "❌ likeDanmaku exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 举报弹幕
     * 
     * @param cid 视频 cid
     * @param dmid 弹幕 ID
     * @param reason 举报原因: 1=违法/2=色情/3=广告/4=引战/5=辱骂/6=剧透/7=刷屏/8=其他
     * @param content 举报描述 (可选)
     */
    suspend fun reportDanmaku(
        cid: Long,
        dmid: Long,
        reason: Int,
        content: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }

            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "📤 reportDanmaku: cid=$cid, dmid=$dmid, reason=$reason")
            
            val response = api.reportDanmaku(cid = cid, dmid = dmid, reason = reason, content = content, csrf = csrf)
            
            if (response.code == 0) {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "✅ Danmaku reported")
                Result.success(Unit)
            } else {
                val errorMsg = when (response.code) {
                    -101 -> "请先登录"
                    -111 -> "鉴权失败，请重新登录"
                    -400 -> "请求参数错误"
                    else -> response.message.ifEmpty { "举报失败 (${response.code})" }
                }
                android.util.Log.e("DanmakuRepo", "❌ reportDanmaku failed: ${response.code} - ${response.message}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("DanmakuRepo", "❌ reportDanmaku exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 启动直播弹幕连接
     * 
     * @param scope 用于管理 WebSocket 生命周期的协程作用域 (通常是 ViewModelScope)
     * @param roomId 直播间 ID
     * @return 连接成功的 Client 实例
     */
    suspend fun startLiveDanmaku(
        scope: kotlinx.coroutines.CoroutineScope,
        roomId: Long
    ): Result<com.android.purebilibili.core.network.socket.LiveDanmakuClient> = withContext(Dispatchers.IO) {
        try {
            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "📡 Getting live danmaku info for room=$roomId...")
            
            // 1. 获取 Wbi 密钥并签名参数 (解决 -352 风控)
            val wbiKeys = com.android.purebilibili.core.network.WbiKeyManager.getWbiKeys().getOrNull()
            val response = if (wbiKeys != null) {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Using Wbi signature for danmaku info")
                val params = mapOf(
                    "id" to roomId.toString(),
                    "type" to "0"
                )
                val signedParams = com.android.purebilibili.core.network.WbiUtils.sign(params, wbiKeys.first, wbiKeys.second)
                api.getDanmuInfoWbi(signedParams)
            } else {
                com.android.purebilibili.core.util.Logger.w("DanmakuRepo", " Wbi keys missing, falling back to unsigned request")
                api.getDanmuInfo(roomId)
            }

            if (response.code != 0 || response.data == null) {
                return@withContext Result.failure(Exception("获取弹幕服务信息失败: ${response.code} (msg=${response.message})"))
            }
            
            val info = response.data
            val token = info.token
            val hosts = info.host_list
            
            if (hosts.isEmpty()) {
                return@withContext Result.failure(Exception("无可用弹幕服务器"))
            }
            
            // 2. 选择最佳服务器 (优先 wss, 默认 443 端口)
            val bestHost = hosts.find { it.wss_port == 443 } 
                ?: hosts.find { it.wss_port != 0 }
                ?: hosts.first()
                
            val port = if (bestHost.wss_port != 0) bestHost.wss_port else bestHost.ws_port
            val schema = if (bestHost.wss_port != 0) "wss" else "ws"
            val webSocketUrl = "$schema://${bestHost.host}:$port/sub"
            
            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🔗 Connecting to Live Danmaku: $webSocketUrl")
            
            if (webSocketUrl.isNotEmpty()) {
            val client = com.android.purebilibili.core.network.socket.LiveDanmakuClient(scope) // Removed onMessage and onPopularity as they are not defined in the original context
            
            // 获取当前用户 UID (如果已登录)
            val uid = com.android.purebilibili.core.store.TokenManager.midCache ?: 0L
            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🔌 Connecting with UID: $uid")
            
            client.connect(webSocketUrl, token, roomId, uid)
            // liveDanmakuClient = client // liveDanmakuClient is not defined in the original context
            Result.success(client)
        } else {
            Result.failure(Exception("未找到有效的 WebSocket 地址"))
        }
    } catch (e: Exception) {
            android.util.Log.e("DanmakuRepo", "❌ Start live danmaku failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
