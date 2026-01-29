// 文件路径: feature/video/VideoPlayerState.kt
package com.android.purebilibili.feature.video.state

import com.android.purebilibili.feature.video.player.MiniPlayerManager
import com.android.purebilibili.feature.video.VideoActivity
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState

import android.app.NotificationChannel
import android.support.v4.media.session.PlaybackStateCompat
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import com.android.purebilibili.R
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "media_playback_channel"
private const val THEME_COLOR = 0xFFFB7299.toInt()

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VideoPlayerState(
    val context: Context,
    val player: ExoPlayer,
    val mediaSession: MediaSession,
    //  性能优化：传入受管理的 CoroutineScope，避免内存泄漏
    private val scope: CoroutineScope
) {
    // 📱 竖屏视频状态 - 双重验证机制
    // 来源1: API dimension 字段（预判断，快速可用）
    // 来源2: 播放器 onVideoSizeChanged（精确验证，需要等待加载）
    
    private val _isVerticalVideo = MutableStateFlow(false)
    val isVerticalVideo: StateFlow<Boolean> = _isVerticalVideo.asStateFlow()
    
    // 📐 视频尺寸（来自播放器回调，精确值）
    private val _videoSize = MutableStateFlow(Pair(0, 0))
    val videoSize: StateFlow<Pair<Int, Int>> = _videoSize.asStateFlow()
    
    // 🎯 API 预判断值（用于视频加载前的 UI 显示）
    private val _apiDimension = MutableStateFlow<Pair<Int, Int>?>(null)
    val apiDimension: StateFlow<Pair<Int, Int>?> = _apiDimension.asStateFlow()
    
    // 📱 竖屏全屏模式状态
    private val _isPortraitFullscreen = MutableStateFlow(false)
    val isPortraitFullscreen: StateFlow<Boolean> = _isPortraitFullscreen.asStateFlow()
    
    // 🔍 验证来源标记
    enum class VerticalVideoSource {
        UNKNOWN,  // 未知
        API,      // 来自 API dimension 字段
        PLAYER    // 来自播放器回调（精确）
    }
    private val _verticalVideoSource = MutableStateFlow(VerticalVideoSource.UNKNOWN)
    val verticalVideoSource: StateFlow<VerticalVideoSource> = _verticalVideoSource.asStateFlow()
    
    // 🎵 缓存元数据用于状态更新
    private var currentTitle: String = ""
    private var currentArtist: String = ""
    private var currentBitmap: Bitmap? = null

    private val playerListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                _videoSize.value = Pair(videoSize.width, videoSize.height)
                val isVertical = videoSize.height > videoSize.width
                _isVerticalVideo.value = isVertical
                _verticalVideoSource.value = VerticalVideoSource.PLAYER
                
                // 🔍 双重验证：检查是否与 API 预判断一致
                val apiSize = _apiDimension.value
                if (apiSize != null) {
                    val apiVertical = apiSize.second > apiSize.first
                    if (apiVertical != isVertical) {
                        com.android.purebilibili.core.util.Logger.w(
                            "VideoPlayerState",
                            "⚠️ 竖屏判断不一致! API: ${apiSize.first}x${apiSize.second}=$apiVertical, 播放器: ${videoSize.width}x${videoSize.height}=$isVertical"
                        )
                    }
                }
                
                com.android.purebilibili.core.util.Logger.d(
                    "VideoPlayerState",
                    "📱 VideoSize(PLAYER): ${videoSize.width}x${videoSize.height}, isVertical=$isVertical"
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // 当播放状态改变时，更新通知栏（主要是播放/暂停按钮）
            if (currentTitle.isNotEmpty()) {
                scope.launch(Dispatchers.Main) {
                    pushMediaNotification()
                }
            }
        }
    }
    
    init {
        player.addListener(playerListener) // 使用统一的 listener
        // 初始检查
        val size = player.videoSize
        if (size.width > 0 && size.height > 0) {
            _videoSize.value = Pair(size.width, size.height)
            _isVerticalVideo.value = size.height > size.width
            _verticalVideoSource.value = VerticalVideoSource.PLAYER
        }
    }
    
    /**
     * 📱 从 API dimension 字段设置预判断值
     * 在视频加载完成但播放器还未解析时调用
     */
    fun setApiDimension(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            _apiDimension.value = Pair(width, height)
            // 只有在播放器还没提供精确值时才使用 API 值
            if (_verticalVideoSource.value != VerticalVideoSource.PLAYER) {
                _isVerticalVideo.value = height > width
                _verticalVideoSource.value = VerticalVideoSource.API
                com.android.purebilibili.core.util.Logger.d(
                    "VideoPlayerState",
                    "📱 VideoSize(API): ${width}x${height}, isVertical=${height > width}"
                )
            }
        }
    }
    
    /**
     * 📱 进入/退出竖屏全屏模式
     */
    fun setPortraitFullscreen(enabled: Boolean) {
        _isPortraitFullscreen.value = enabled
        com.android.purebilibili.core.util.Logger.d(
            "VideoPlayerState",
            "📱 PortraitFullscreen: $enabled"
        )
    }
    
    /**
     * 🔄 重置视频尺寸状态（切换视频时调用）
     * 注意：不重置 isPortraitFullscreen，允许连续切换视频时保持竖屏全屏模式
     */
    fun resetVideoSize() {
        _videoSize.value = Pair(0, 0)
        _apiDimension.value = null
        _isVerticalVideo.value = false
        _verticalVideoSource.value = VerticalVideoSource.UNKNOWN
        // 不重置 _isPortraitFullscreen，保持竖屏全屏状态
    }
    
    fun release() {
        player.removeListener(playerListener)
    }

    fun updateMediaMetadata(title: String, artist: String, coverUrl: String) {
        // 缓存元数据
        currentTitle = title
        currentArtist = artist
        
        val currentItem = player.currentMediaItem ?: return

        // 1. 更新 Player 内部元数据
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(FormatUtils.fixImageUrl(coverUrl)))
            .setDisplayTitle(title)
            .setIsPlayable(true)
            .build()

        val newItem = currentItem.buildUpon()
            .setMediaMetadata(metadata)
            .build()
        
        // 避免在此处不必要地重置 mediaItem，这可能导致播放中断
        if (currentItem.mediaMetadata.title != title) {
            player.replaceMediaItem(player.currentMediaItemIndex, newItem)
        }

        // 2.  性能优化：使用传入的 scope 而非裸创建的 CoroutineScope
        scope.launch(Dispatchers.IO) {
            val bitmap = loadBitmap(context, coverUrl)
            currentBitmap = bitmap // 更新缓存

            // 切回主线程操作 Player 和发送通知
            launch(Dispatchers.Main) {
                pushMediaNotification()
            }
        }
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
        return try {
            //  性能优化：使用 Coil 单例，避免重复创建 ImageLoader
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(FormatUtils.fixImageUrl(url))
                .allowHardware(false)
                .scale(Scale.FILL)
                .transformations(RoundedCornersTransformation(16f))
                .size(512, 512)
                .build()
            val result = loader.execute(request)
            (result as? SuccessResult)?.drawable?.let { (it as BitmapDrawable).bitmap }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun pushMediaNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 确保渠道存在
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, "媒体播放", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "显示播放控制"
                    setShowBadge(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
        
        val isPlaying = player.isPlaying
        
        // 创建播放/暂停 Intent
        // 注意：MediaSession 通常会自动处理这些，但为了通知栏按钮生效，我们显式添加 Action
        // 更好的方式是直接利用 MediaStyle 的自动行为，但 Media3 下有时需要手动添加 Action 到 NotificationCompat
        
        // 我们使用 MediaSession 的 PendingIntent 或 BroadcastReceiver 来处理
        // 简单起见，这里复用 `mediaSession.sessionActivity` 点击跳转，
        // 按钮操作由 System UI 通过 sessionToken 直接控制 Session，
        // 但我们需要在 Notification UI 上画出这些按钮。
        
        // 对于 Android MediaStyle，必须 addAction 才能显示按钮
        val pauseAction = NotificationCompat.Action(
            android.R.drawable.ic_media_pause, "Pause",
            androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_PAUSE
            )
        )
        
        val playAction = NotificationCompat.Action(
            android.R.drawable.ic_media_play, "Play",
            androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_PLAY
            )
        )

        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionCompatToken)
            .setShowActionsInCompactView(0) // 显示第1个 Action (索引0)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setLargeIcon(currentBitmap)
            .setStyle(style)
            .setColor(THEME_COLOR)
            .setColorized(true)
            .setOngoing(isPlaying) // 播放时常驻，暂停时可清除
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentIntent(mediaSession.sessionActivity)

        // 添加 Play/Pause 按钮
        if (isPlaying) {
            builder.addAction(pauseAction)
        } else {
            builder.addAction(playAction)
        }

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun rememberVideoPlayerState(
    context: Context,
    viewModel: PlayerViewModel,
    bvid: String
): VideoPlayerState {

    //  尝试复用 MiniPlayerManager 中已加载的 player
    val miniPlayerManager = MiniPlayerManager.getInstance(context)
    val reuseFromMiniPlayer = miniPlayerManager.isActive && miniPlayerManager.currentBvid == bvid
    
    //  [修复] 添加唯一 key 强制在每次进入时重新创建 player
    // 解决重复打开同一视频时 player 已被释放导致无声音的问题
    val playerCreationKey = remember { System.currentTimeMillis() }
    
    val player = remember(context, bvid, reuseFromMiniPlayer, playerCreationKey) {
        // 如果小窗有这个视频的 player，直接复用
        if (reuseFromMiniPlayer) {
            miniPlayerManager.player?.also {
                com.android.purebilibili.core.util.Logger.d("VideoPlayerState", " 复用小窗 player: bvid=$bvid")
            }
        } else {
            null
        } ?: run {
            // 创建新的 player
            com.android.purebilibili.core.util.Logger.d("VideoPlayerState", " 创建新 player: bvid=$bvid")
            val headers = mapOf(
                "Referer" to "https://www.bilibili.com",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            )
            val dataSourceFactory = OkHttpDataSource.Factory(NetworkModule.okHttpClient)
                .setDefaultRequestProperties(headers)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            //  [性能优化] 使用 PlayerSettingsCache 直接从内存读取，避免 I/O
            val hwDecodeEnabled = com.android.purebilibili.core.store.PlayerSettingsCache.isHwDecodeEnabled(context)
            val seekFastEnabled = com.android.purebilibili.core.store.PlayerSettingsCache.isSeekFastEnabled(context)

            //  根据设置选择 RenderersFactory
            val renderersFactory = if (hwDecodeEnabled) {
                // 默认 Factory，优先使用硬件解码
                androidx.media3.exoplayer.DefaultRenderersFactory(context)
                    .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            } else {
                // 强制使用软件解码
                androidx.media3.exoplayer.DefaultRenderersFactory(context)
                    .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                    .setEnableDecoderFallback(true)
            }

            ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                //  性能优化：自定义缓冲策略，改善播放流畅度
                .setLoadControl(
                    androidx.media3.exoplayer.DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            15000,  // 最小缓冲 15s
                            50000,  // 最大缓冲 50s
                            2500,   // 播放开始前缓冲 2.5s
                            5000    // 重新缓冲后缓冲 5s
                        )
                        .setPrioritizeTimeOverSizeThresholds(true)  // 优先保证播放时长
                        .build()
                )
                //  [性能优化] 快速 Seek：跳转到最近的关键帧而非精确位置
                .setSeekParameters(
                    if (seekFastEnabled) {
                        androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC
                    } else {
                        androidx.media3.exoplayer.SeekParameters.DEFAULT
                    }
                )
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    //  [修复] 确保音量正常，解决第二次播放静音问题
                    volume = 1.0f
                    //  [重构] 不在此处调用 prepare()，因为还没有媒体源
                    // prepare() 和 playWhenReady 将在 attachPlayer/loadVideo 设置媒体源后调用
                    playWhenReady = true
                }
        }
    }

    val sessionActivityPendingIntent = remember(context, bvid) {
        //  [修复] 点击通知跳转到 MainActivity 而不是新建 VideoActivity
        // 这样可以复用应用内的导航栈，保持"单 Activity"架构
        val intent = Intent(context, com.android.purebilibili.MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("https://www.bilibili.com/video/$bvid")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // 🚀 [修复] MediaSession 与 player 生命周期同步
    val mediaSession = remember(player) {
        val sessionId = "bilipai_${java.util.UUID.randomUUID()}"
        com.android.purebilibili.core.util.Logger.d("VideoPlayerState", "📺 Creating MediaSession with ID: $sessionId")
        MediaSession.Builder(context, player)
            .setId(sessionId)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    //  性能优化：使用 rememberCoroutineScope 创建受管理的协程作用域
    val scope = rememberCoroutineScope()

    val holder = remember(player, mediaSession, scope) {
        VideoPlayerState(context, player, mediaSession, scope)
    }

    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState) {
        if (uiState is PlayerUiState.Success) {
            val info = (uiState as PlayerUiState.Success).info
            holder.updateMediaMetadata(info.title, info.owner.name, info.pic)
        }
    }

    DisposableEffect(player, mediaSession) {
        onDispose {
            //  [新增] 保存播放进度到 ViewModel 缓存
            viewModel.saveCurrentPosition()
            
            //  检查是否有小窗在使用这个 player
            val miniPlayerManager = MiniPlayerManager.getInstance(context)
            //  [修复] 使用 isActive 和 hasExternalPlayer 来判断是否保留 player
            // isMiniMode 可能还没有被设置（AppNavigation.onDispose 可能在之后执行）
            // 但如果 isActive 为 true 且当前 player 是被引用的外部 player，则不释放
            val shouldKeepPlayer = miniPlayerManager.isActive && miniPlayerManager.hasExternalPlayer
            
            if (shouldKeepPlayer) {
                // 小窗模式下不释放 player，只释放其他资源
                com.android.purebilibili.core.util.Logger.d("VideoPlayerState", " 小窗正在使用此 player，不释放")
            } else {
                // 正常释放所有资源
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)
                
                com.android.purebilibili.core.util.Logger.d("VideoPlayerState", " 释放所有资源")
                //  [修复2] 清除外部播放器引用，防止状态混乱
                miniPlayerManager.resetExternalPlayer()
                holder.release()  // 📱 释放视频尺寸监听器
                mediaSession.release()
                player.release()
            }
            
            (context as? ComponentActivity)?.window?.attributes?.screenBrightness =
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    //  [后台恢复优化] 监听生命周期，保存/恢复播放状态
    var savedPosition by remember { mutableStateOf(-1L) }
    var wasPlaying by remember { mutableStateOf(false) }
    //  [修复] 记录是否从后台音频模式恢复（后台音频时不应 seek 回旧位置）
    var wasBackgroundAudio by remember { mutableStateOf(false) }
    
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            val miniPlayerManager = MiniPlayerManager.getInstance(context)
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    //  [修复] 保存进度到 ViewModel 缓存（用于跨导航恢复）
                    viewModel.saveCurrentPosition()
                    
                    //  保存播放状态（用于本地恢复）
                    savedPosition = player.currentPosition
                    wasPlaying = player.isPlaying
                    
                    //  [新增] 判断是否应该继续播放
                    // 1. 应用内小窗模式 - 继续播放
                    // 2. 系统 PiP 模式 - 用户按 Home 键返回桌面时继续播放
                    // 3. 后台音频模式 - 继续播放音频
                    val isMiniMode = miniPlayerManager.isMiniMode
                    val isPip = miniPlayerManager.shouldEnterPip()
                    val isBackgroundAudio = miniPlayerManager.shouldContinueBackgroundAudio()
                    val shouldContinuePlayback = isMiniMode || isPip || isBackgroundAudio
                    
                    //  [修复] 记录后台音频状态，恢复时不要 seek 回旧位置
                    wasBackgroundAudio = isBackgroundAudio
                    
                    if (!shouldContinuePlayback) {
                        // 非小窗/PiP/后台模式下暂停
                        player.pause()
                        com.android.purebilibili.core.util.Logger.d("VideoPlayerState", " ON_PAUSE: 暂停播放")
                    } else {
                        com.android.purebilibili.core.util.Logger.d("VideoPlayerState", "🎵 ON_PAUSE: 保持播放 (miniMode=$isMiniMode, pip=$isPip, bg=$isBackgroundAudio)")
                    }
                    com.android.purebilibili.core.util.Logger.d("VideoPlayerState", " ON_PAUSE: pos=$savedPosition, wasPlaying=$wasPlaying, bgAudio=$wasBackgroundAudio")
                }
                // 🔋 注意: ON_STOP/ON_START 的视频轨道禁用/恢复由 MiniPlayerManager 通过 BackgroundManager 统一处理
                // 避免重复处理导致 savedTrackParams 被覆盖
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    //  [修复] 恢复前台时，只在确实暂停了的情况下恢复播放
                    //  如果是在 PiP 或后台音频模式下，播放器一直在运行，不需要干预
                    val isRunning = player.isPlaying
                    val shouldResume = wasPlaying && !isRunning
                    
                    if (shouldResume) {
                        // 只有当完全暂停时才检查是否需要恢复
                        // 移除 seekTo(savedPosition)，因为 player.currentPosition 才是最新的（即使暂停了也还在该位置）
                        // 且 seekTo 会导致 PiP 返回时回退到进入 PiP 前的旧位置
                        if (!miniPlayerManager.isMiniMode && !miniPlayerManager.shouldEnterPip()) {
                             player.play()
                             com.android.purebilibili.core.util.Logger.d("VideoPlayerState", " ON_RESUME: Resuming playback")
                        }
                    } else {
                        com.android.purebilibili.core.util.Logger.d("VideoPlayerState", " ON_RESUME: Player already running or was not playing, skipping resume")
                    }
                    
                    // 重置标志
                    wasBackgroundAudio = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    //  [修复3] 监听播放器错误，智能重试（网络错误 → CDN 切换 → 重试）
    val retryCountRef = remember { object { 
        var count = 0 
        var cdnSwitchCount = 0  // 📡 [新增] CDN 切换计数
    } }
    val maxRetries = 3
    val maxCdnSwitches = 2  // 📡 [新增] 最多尝试切换 2 次 CDN
    
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("VideoPlayerState", " Player error: ${error.message}, code=${error.errorCode}")
                
                //  判断是否为网络/IO 相关错误（可能是 CDN 问题）
                val isNetworkError = error.errorCode in listOf(
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,  // 📡 [新增] HTTP 错误也尝试切换
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND   // 📡 [新增] 404 也可能是 CDN 问题
                )
                
                // 📡 [新增] 检查是否有多个 CDN 可用
                val currentState = viewModel.uiState.value
                val hasCdnAlternatives = currentState is com.android.purebilibili.feature.video.viewmodel.PlayerUiState.Success 
                    && currentState.cdnCount > 1
                    && retryCountRef.cdnSwitchCount < maxCdnSwitches
                
                if (isNetworkError && hasCdnAlternatives) {
                    // 📡 [策略1] 网络错误 + 有备用 CDN → 先切换 CDN
                    retryCountRef.cdnSwitchCount++
                    com.android.purebilibili.core.util.Logger.d("VideoPlayerState", "📡 Network error, switching CDN (${retryCountRef.cdnSwitchCount}/$maxCdnSwitches)")
                    
                    scope.launch {
                        kotlinx.coroutines.delay(500) // 短暂延迟避免请求过快
                        viewModel.switchCdn()
                    }
                } else if (isNetworkError && retryCountRef.count < maxRetries) {
                    // 🔄 [策略2] 网络错误 + 无备用 CDN / 已切换完 → 重试
                    retryCountRef.count++
                    // 🔧 [优化] 指数退避：1s, 2s, 4s（更快首次重试）
                    val delayMs = (1000L * (1 shl (retryCountRef.count - 1))).coerceAtMost(8000L)
                    com.android.purebilibili.core.util.Logger.d("VideoPlayerState", "🔄 Network error, retry ${retryCountRef.count}/$maxRetries in ${delayMs}ms")
                    
                    // 🚀 [修复] 使用受管理的 scope 避免内存泄漏
                    scope.launch {
                        kotlinx.coroutines.delay(delayMs)
                        viewModel.retry()
                    }
                } else if (retryCountRef.count < 1) {
                    // 非网络错误，只重试一次
                    retryCountRef.count++
                    com.android.purebilibili.core.util.Logger.d("VideoPlayerState", " Auto-retrying video load (non-network error)...")
                    viewModel.retry()
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    // 播放成功，重置所有计数
                    retryCountRef.count = 0
                    retryCountRef.cdnSwitchCount = 0
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    //  [重构] 合并为单个 LaunchedEffect 确保执行顺序
    // 必须先 attachPlayer，再 loadVideo，否则 ViewModel 中的 exoPlayer 引用无效
    LaunchedEffect(player, bvid, reuseFromMiniPlayer) {
        // 1️⃣ 首先绑定 player
        viewModel.attachPlayer(player)
        
        // 2️⃣ 尝试从缓存恢复 UI 状态 (仅当复用播放器时)
        // 解决从小窗/后台返回时的网络请求错误问题
        var restored = false
        if (reuseFromMiniPlayer) {
            val cachedState = miniPlayerManager.consumeCachedUiState()
            if (cachedState != null && cachedState.info.bvid == bvid) {
                com.android.purebilibili.core.util.Logger.d("VideoPlayerState", "♻️ Restoring cached UI state for $bvid")
                viewModel.restoreUiState(cachedState)
                restored = true
            }
        }
        
        // 3️⃣ 如果没有恢复成功，则调用 loadVideo
        if (!restored) {
            com.android.purebilibili.core.util.Logger.d("VideoPlayerState", " Calling loadVideo: $bvid")
            viewModel.loadVideo(bvid)
        }
    }

    return holder
}