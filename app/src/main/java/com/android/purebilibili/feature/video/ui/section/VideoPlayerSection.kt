// 文件路径: feature/video/VideoPlayerSection.kt
package com.android.purebilibili.feature.video.ui.section

import com.android.purebilibili.feature.video.danmaku.DanmakuManager
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlay
import com.android.purebilibili.feature.video.ui.components.SponsorSkipButton
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.data.model.response.ViewPoint

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
// 🌈 Material Icons Extended - 亮度图标
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.util.FormatUtils
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class VideoGestureMode { None, Brightness, Volume, Seek }

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerSection(
    playerState: VideoPlayerState,
    uiState: PlayerUiState,
    isFullscreen: Boolean,
    isInPipMode: Boolean,
    onToggleFullscreen: () -> Unit,
    onQualityChange: (Int, Long) -> Unit,
    onBack: () -> Unit,
    // 🔗 [新增] 分享功能
    bvid: String = "",
    coverUrl: String = "",
    //  实验性功能：双击点赞
    onDoubleTapLike: () -> Unit = {},
    //  空降助手
    sponsorSegment: com.android.purebilibili.data.model.response.SponsorSegment? = null,
    showSponsorSkipButton: Boolean = false,
    onSponsorSkip: () -> Unit = {},
    onSponsorDismiss: () -> Unit = {},
    //  [新增] 重载视频回调
    onReloadVideo: () -> Unit = {},
    //  [新增] CDN 线路切换
    currentCdnIndex: Int = 0,
    cdnCount: Int = 1,
    onSwitchCdn: () -> Unit = {},
    onSwitchCdnTo: (Int) -> Unit = {},
    
    //  [新增] 音频模式
    isAudioOnly: Boolean = false,
    onAudioOnlyToggle: () -> Unit = {},
    
    //  [新增] 定时关闭
    sleepTimerMinutes: Int? = null,
    onSleepTimerChange: (Int?) -> Unit = {},
    
    // 🖼️ [新增] 视频预览图数据
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    
    // 📖 [新增] 视频章节数据
    viewPoints: List<ViewPoint> = emptyList(),
    
    // 📱 [新增] 竖屏全屏模式
    isVerticalVideo: Boolean = false,
    onPortraitFullscreen: () -> Unit = {},
    isPortraitFullscreen: Boolean = false,
    // 📲 [新增] 小窗模式
    // 📲 [新增] 小窗模式
    onPipClick: () -> Unit = {},
    // [New] Codec & Audio Params
    currentCodec: String = "hev1", 
    onCodecChange: (String) -> Unit = {},
    currentAudioQuality: Int = -1,
    onAudioQualityChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    // --- 新增：读取设置中的"详细统计信息"开关 ---
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    // 使用 rememberUpdatedState 确保重组时获取最新值（虽然在单一 Activity 生命周期内可能需要重启生效，但简单场景够用）
    val showStats by remember { mutableStateOf(prefs.getBoolean("show_stats", false)) }
    
    //  [新增] 读取手势灵敏度设置
    val gestureSensitivity by com.android.purebilibili.core.store.SettingsManager
        .getGestureSensitivity(context)
        .collectAsState(initial = 1.0f)

    // 📱 [优化] realResolution 现在从 playerState.videoSize 计算（见下方）
    
    //  读取双击点赞设置 (从 DataStore 读取)
    val doubleTapLikeEnabled by com.android.purebilibili.core.store.SettingsManager
        .getDoubleTapLike(context)
        .collectAsState(initial = true)
    
    //  [新增] 读取双击跳转秒数设置
    val doubleTapSeekEnabled by com.android.purebilibili.core.store.SettingsManager
        .getDoubleTapSeekEnabled(context)
        .collectAsState(initial = true)

    val seekForwardSeconds by com.android.purebilibili.core.store.SettingsManager
        .getSeekForwardSeconds(context)
        .collectAsState(initial = 10)
    val seekBackwardSeconds by com.android.purebilibili.core.store.SettingsManager
        .getSeekBackwardSeconds(context)
        .collectAsState(initial = 10)
    
    //  [新增] 双击跳转视觉反馈状态
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var seekFeedbackVisible by remember { mutableStateOf(false) }
    
    //  [新增] 长按倍速设置和状态
    val longPressSpeed by com.android.purebilibili.core.store.SettingsManager
        .getLongPressSpeed(context)
        .collectAsState(initial = 2.0f)
    var isLongPressing by remember { mutableStateOf(false) }
    var originalSpeed by remember { mutableFloatStateOf(1.0f) }
    var longPressSpeedFeedbackVisible by remember { mutableStateOf(false) }
    
    //  [新增] 缓冲状态监听
    var isBuffering by remember { mutableStateOf(false) }
    DisposableEffect(playerState.player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        playerState.player.addListener(listener)
        // 初始化状态
        isBuffering = playerState.player.playbackState == Player.STATE_BUFFERING
        onDispose {
            playerState.player.removeListener(listener)
        }
    }

    // 📱 [优化] 复用 VideoPlayerState 中的视频尺寸状态，避免重复监听
    val videoSizeState by playerState.videoSize.collectAsState()
    val realResolution = if (videoSizeState.first > 0 && videoSizeState.second > 0) {
        "${videoSizeState.first} x ${videoSizeState.second}"
    } else {
        ""
    }

    // 控制器显示状态
        var showControls by remember { mutableStateOf(true) }
    
    // 🔒 [新增] 屏幕锁定状态（全屏时防误触）
    var isScreenLocked by remember { mutableStateOf(false) }

    var gestureMode by remember { mutableStateOf<VideoGestureMode>(VideoGestureMode.None) }
    var gestureIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gesturePercent by remember { mutableFloatStateOf(0f) }

    // 进度手势相关状态
    var seekTargetTime by remember { mutableLongStateOf(0L) }
    var startPosition by remember { mutableLongStateOf(0L) }
    var isGestureVisible by remember { mutableStateOf(false) }
    
    //  视频比例状态
    var currentAspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }
    
    //  [新增] 视频翻转状态
    var isFlippedHorizontal by remember { mutableStateOf(false) }
    var isFlippedVertical by remember { mutableStateOf(false) }

    // 记录手势开始时的初始值
    var startVolume by remember { mutableIntStateOf(0) }
    var startBrightness by remember { mutableFloatStateOf(0f) }

    // 记录累计拖动距离
    var totalDragDistanceY by remember { mutableFloatStateOf(0f) }
    var totalDragDistanceX by remember { mutableFloatStateOf(0f) }

    fun getActivity(): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> context.baseContext as? Activity
        else -> null
    }

    //  [新增] 缩放和平移状态
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // [新增] 共享元素过渡支持
    val sharedTransitionScope = com.android.purebilibili.core.ui.LocalSharedTransitionScope.current
    val animatedVisibilityScope = com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope.current
    
    var rootModifier = Modifier
        .fillMaxSize()
        .clipToBounds()
        .background(Color.Black)

    // 应用共享元素
    if (bvid.isNotEmpty() && sharedTransitionScope != null && animatedVisibilityScope != null) {
         with(sharedTransitionScope) {
             rootModifier = rootModifier.sharedElement(
                 sharedContentState = rememberSharedContentState(key = "video-$bvid"),
                 animatedVisibilityScope = animatedVisibilityScope
             )
         }
    }

    Box(
        modifier = rootModifier
            //  [新增] 处理双指缩放和平移
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    
                    if (scale > 1f) {
                        // 缩放状态下，允许平移
                        val maxPanX = (size.width * scale - size.width) / 2
                        val maxPanY = (size.height * scale - size.height) / 2
                        panX = (panX + pan.x * scale).coerceIn(-maxPanX, maxPanX)
                        panY = (panY + pan.y * scale).coerceIn(-maxPanY, maxPanY)
                        
                        // 如果正在缩放/平移，隐藏手势图标和控制栏
                        isGestureVisible = false
                        showControls = false
                    } else {
                        // 恢复原始比例时，重置平移
                        panX = 0f
                        panY = 0f
                    }
                }
            }
            //  先处理拖拽手势 (音量/亮度/进度)
            .pointerInput(isInPipMode, isScreenLocked) {
                if (!isInPipMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // [新增] 如果处于缩放状态，禁用常规拖拽手势，优先处理平移
                            if (scale > 1.01f) {  // 留一点浮点数buffer
                                return@detectDragGestures
                            }
                            
                            // 🔒 锁定时禁用拖拽手势
                            if (isScreenLocked) {
                                return@detectDragGestures
                            }
                            
                            //  [新增] 边缘防误触检测
                            //  如果在屏幕顶部或底部区域开始滑动，则视为系统手势（如下拉通知栏），不触发播放器手势
                            val density = context.resources.displayMetrics.density
                            val safeZonePx = 48 * density  //  48dp 安全区域
                            val screenHeight = size.height

                            // 检查是否在安全区域内 (顶部或底部)
                            val isEdgeGesture = offset.y < safeZonePx || offset.y > (screenHeight - safeZonePx)
                            
                            if (isEdgeGesture) {
                                isGestureVisible = false
                                gestureMode = VideoGestureMode.None
                                // 不需要 return，直接不执行下面的初始化逻辑即可
                            } else {
                                isGestureVisible = true
                                gestureMode = VideoGestureMode.None
                                totalDragDistanceY = 0f
                                totalDragDistanceX = 0f

                                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                startPosition = playerState.player.currentPosition

                                val attributes = getActivity()?.window?.attributes
                                val currentWindowBrightness = attributes?.screenBrightness ?: -1f

                                if (currentWindowBrightness < 0) {
                                    try {
                                        val sysBrightness = Settings.System.getInt(
                                            context.contentResolver,
                                            Settings.System.SCREEN_BRIGHTNESS
                                        )
                                        startBrightness = sysBrightness / 255f
                                    } catch (e: Exception) {
                                        startBrightness = 0.5f
                                    }
                                } else {
                                    startBrightness = currentWindowBrightness
                                }
                            }
                        },
                        onDragEnd = {
                            if (gestureMode == VideoGestureMode.Seek) {
                                playerState.player.seekTo(seekTargetTime)
                                playerState.player.play()
                            }
                            isGestureVisible = false
                            gestureMode = VideoGestureMode.None
                        },
                        onDragCancel = {
                            isGestureVisible = false
                            gestureMode = VideoGestureMode.None
                        },
                        //  [修复点] 使用 dragAmount 而不是 change.positionChange()
                        onDrag = { change, dragAmount ->
                            // 如果手势不可见（即在 safe zone 中启动被忽略），则停止处理
                            if (!isGestureVisible && gestureMode == VideoGestureMode.None) {
                                // do nothing
                            } else {

                            if (gestureMode == VideoGestureMode.None) {
                                if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    gestureMode = VideoGestureMode.Seek
                                } else {
                                    // 根据起始 X 坐标判断左右屏
                                    val screenWidth = context.resources.displayMetrics.widthPixels
                                    gestureMode = if (change.position.x < screenWidth / 2) {
                                        VideoGestureMode.Brightness
                                    } else {
                                        VideoGestureMode.Volume
                                    }
                                }
                            }

                            when (gestureMode) {
                                VideoGestureMode.Seek -> {
                                    totalDragDistanceX += dragAmount.x
                                    val duration = playerState.player.duration.coerceAtLeast(0L)
                                    //  应用灵敏度
                                    val seekDelta = (totalDragDistanceX * 200 * gestureSensitivity).toLong()
                                    seekTargetTime = (startPosition + seekDelta).coerceIn(0L, duration)
                                }
                                VideoGestureMode.Brightness -> {
                                    totalDragDistanceY -= dragAmount.y
                                    val screenHeight = context.resources.displayMetrics.heightPixels
                                    //  应用灵敏度
                                    val deltaPercent = totalDragDistanceY / screenHeight * gestureSensitivity
                                    val newBrightness = (startBrightness + deltaPercent).coerceIn(0f, 1f)
                                    
                                    //  优化：仅在变化超过阈值时更新（减少 WindowManager 调用）
                                    if (kotlin.math.abs(newBrightness - gesturePercent) > 0.02f) {
                                        getActivity()?.window?.attributes = getActivity()?.window?.attributes?.apply {
                                            screenBrightness = newBrightness
                                        }
                                        gesturePercent = newBrightness
                                    }
                                    //  亮度图标：CupertinoIcons SunMax (iOS SF Symbols 风格)
                                    gestureIcon = CupertinoIcons.Default.SunMax
                                }
                                VideoGestureMode.Volume -> {
                                    totalDragDistanceY -= dragAmount.y
                                    val screenHeight = context.resources.displayMetrics.heightPixels
                                    //  应用灵敏度
                                    val deltaPercent = totalDragDistanceY / screenHeight * gestureSensitivity
                                    val newVolPercent = ((startVolume.toFloat() / maxVolume) + deltaPercent).coerceIn(0f, 1f)
                                    val targetVol = (newVolPercent * maxVolume).toInt()
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                    gesturePercent = newVolPercent
                                    //  动态音量图标：3 级
                                    gestureIcon = when {
                                        gesturePercent < 0.01f -> CupertinoIcons.Default.SpeakerSlash
                                        gesturePercent < 0.5f -> CupertinoIcons.Default.Speaker
                                        else -> CupertinoIcons.Default.SpeakerWave2
                                    }
                                }
                                else -> {}
                            }
                            }
                        }
                    )
                }
            }
            //  点击/双击/长按手势在拖拽之后处理
            .pointerInput(seekForwardSeconds, seekBackwardSeconds, longPressSpeed, isScreenLocked) {
                detectTapGestures(
                    onTap = { 
                        // 🔒 锁定时点击只显示解锁按钮
                        if (isScreenLocked) {
                            showControls = !showControls  // 显示/隐藏解锁按钮
                        } else {
                            showControls = !showControls
                        }
                    },
                    onLongPress = {
                        // 🔒 锁定时禁用长按倍速
                        if (isScreenLocked) return@detectTapGestures
                        //  长按开始：保存原速度并应用长按倍速
                        val player = playerState.player
                        originalSpeed = player.playbackParameters.speed
                        player.setPlaybackSpeed(longPressSpeed)
                        isLongPressing = true
                        longPressSpeedFeedbackVisible = true
                        com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏩ LongPress: speed ${longPressSpeed}x")
                    },
                    onDoubleTap = { offset ->
                        // 🔒 锁定时禁用双击
                        if (isScreenLocked) return@detectTapGestures
                        
                        val screenWidth = size.width
                        val player = playerState.player
                        
                        //  [新增] 读取双击跳转开关
                        // 注意：这里 directly accessing the state value captured in the closure
                        // We need to ensure we have access to the latest value. 
                        // Since `doubleTapSeekEnabled` is a state, we can read it here.
                        
                        // 逻辑：如果开启跳转 -> 以前的逻辑 (两侧跳转，中间暂停)
                        //      如果关闭跳转 -> 全屏双击均为暂停/播放 (解决长屏按不到暂停的问题)
                        
                        if (doubleTapSeekEnabled) {
                            when {
                                // 右侧 1/3：快进
                                offset.x > screenWidth * 2 / 3 -> {
                                    val seekMs = seekForwardSeconds * 1000L
                                    val newPos = (player.currentPosition + seekMs).coerceAtMost(player.duration.coerceAtLeast(0L))
                                    player.seekTo(newPos)
                                    seekFeedbackText = "+${seekForwardSeconds}s"
                                    seekFeedbackVisible = true
                                    com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏩ DoubleTap right: +${seekForwardSeconds}s")
                                }
                                // 左侧 1/3：后退
                                offset.x < screenWidth / 3 -> {
                                    val seekMs = seekBackwardSeconds * 1000L
                                    val newPos = (player.currentPosition - seekMs).coerceAtLeast(0L)
                                    player.seekTo(newPos)
                                    seekFeedbackText = "-${seekBackwardSeconds}s"
                                    seekFeedbackVisible = true
                                    com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏪ DoubleTap left: -${seekBackwardSeconds}s")
                                }
                                // 中间：暂停/播放
                                else -> {
                                    player.playWhenReady = !player.playWhenReady
                                    com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏯️ DoubleTap center: toggle play/pause")
                                }
                            }
                        } else {
                            // 关闭跳转时，全屏双击暂停/播放
                            player.playWhenReady = !player.playWhenReady
                            com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏯️ DoubleTap (Seek Disabled): toggle play/pause")
                        }
                    },
                    onPress = { offset ->
                        //  等待手指抬起
                        tryAwaitRelease()
                        //  如果之前是长按状态，松开时恢复原速度
                        if (isLongPressing) {
                            playerState.player.setPlaybackSpeed(originalSpeed)
                            isLongPressing = false
                            longPressSpeedFeedbackVisible = false
                            com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏹️ LongPress released: speed ${originalSpeed}x")
                        }
                    }
                )
            }
    ) {
        //  弹幕管理器 (使用单例模式，确保横竖屏切换时保持状态)
        val scope = rememberCoroutineScope()  //  用于设置弹幕开关
        val danmakuManager = rememberDanmakuManager()
        
        //  弹幕开关设置
        val danmakuEnabled by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuEnabled(context)
            .collectAsState(initial = true)
        
        //  弹幕设置（全局持久化）
        val danmakuOpacity by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuOpacity(context)
            .collectAsState(initial = 0.85f)
        val danmakuFontScale by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuFontScale(context)
            .collectAsState(initial = 1.0f)
        val danmakuSpeed by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuSpeed(context)
            .collectAsState(initial = 1.0f)
        val danmakuDisplayArea by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuArea(context)
            .collectAsState(initial = 0.5f)
        
        //  当视频加载成功时加载弹幕（不再依赖 isFullscreen，单例会保持弹幕）
        val cid = (uiState as? PlayerUiState.Success)?.info?.cid ?: 0L
        val aid = (uiState as? PlayerUiState.Success)?.info?.aid ?: 0L
        //  监听 player 状态，等待 duration 可用后加载弹幕
        LaunchedEffect(cid) {
            if (cid > 0) {
                danmakuManager.isEnabled = danmakuEnabled
                
                //  [修复] 等待播放器准备好并获取 duration (最多等待 5 秒)
                var durationMs = 0L
                var retries = 0
                while (durationMs <= 0 && retries < 50) {
                    durationMs = playerState.player.duration.takeIf { it > 0 } ?: 0L
                    if (durationMs <= 0) {
                        kotlinx.coroutines.delay(100)
                        retries++
                    }
                }
                
                android.util.Log.d("VideoPlayerSection", "🎯 Loading danmaku for cid=$cid, aid=$aid, duration=${durationMs}ms (after $retries retries)")
                danmakuManager.loadDanmaku(cid, aid, durationMs)  //  传入时长启用 Protobuf API
            }
        }
        
        //  弹幕开关变化时更新
        LaunchedEffect(danmakuEnabled) {
            danmakuManager.isEnabled = danmakuEnabled
        }

        //  横竖屏/小窗切换后，若应当播放但未播放，主动恢复
        LaunchedEffect(isFullscreen, isInPipMode) {
            val player = playerState.player
            if (player.playWhenReady && !player.isPlaying && player.playbackState == Player.STATE_READY) {
                player.play()
            }
        }
        
        //  弹幕设置变化时实时应用
        LaunchedEffect(danmakuOpacity, danmakuFontScale, danmakuSpeed, danmakuDisplayArea) {
            danmakuManager.updateSettings(
                opacity = danmakuOpacity,
                fontScale = danmakuFontScale,
                speed = danmakuSpeed,
                displayArea = danmakuDisplayArea
            )
        }
        
        //  绑定 Player（不在 onDispose 中释放，单例保持状态）
        DisposableEffect(playerState.player) {
            android.util.Log.d("VideoPlayerSection", " attachPlayer, isFullscreen=$isFullscreen")
            danmakuManager.attachPlayer(playerState.player)
            onDispose {
                // 单例模式不需要释放
            }
        }
        
        //  [修复] 使用 LifecycleOwner 监听真正的 Activity 生命周期
        // DisposableEffect(Unit) 会在横竖屏切换时触发，导致 player 引用被清除
        //  [关键修复] 添加 ON_RESUME 事件，确保从其他视频返回后重新绑定弹幕播放器
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, playerState.player) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                        //  [关键修复] 返回页面时重新绑定弹幕播放器
                        // 解决导航到其他视频后返回，弹幕暂停失效的问题
                        android.util.Log.d("VideoPlayerSection", " ON_RESUME: Re-attaching danmaku player")
                        danmakuManager.attachPlayer(playerState.player)
                    }
                    androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                        android.util.Log.d("VideoPlayerSection", " ON_DESTROY: Clearing danmaku references")
                        danmakuManager.clearViewReference()
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        
        // 1. PlayerView (底层) - key 触发 graphicsLayer 强制更新
        //  [修复] 添加 isPortraitFullscreen 到 key，确保从全屏返回时重建 PlayerView 并重新绑定 Surface (解决黑屏问题)
        key(isFlippedHorizontal, isFlippedVertical, isPortraitFullscreen) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerState.player
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)  // 禁用系统缓冲指示器，使用自定义iOS风格加载动画
                        useController = false
                        keepScreenOn = true
                        resizeMode = currentAspectRatio.resizeMode
                    }
                },
                update = { playerView ->
                    playerView.player = playerState.player
                    playerView.resizeMode = currentAspectRatio.resizeMode
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        //  [新增] 应用缩放和平移
                        scaleX = if (isFlippedHorizontal) -scale else scale
                        scaleY = if (isFlippedVertical) -scale else scale
                        translationX = panX
                        translationY = panY
                    }
            )
        }
        

        
    // --- [优化] 视频封面逻辑 ---
    // 使用 isFirstFrameRendered 确保只有在第一帧真正渲染后才隐藏封面，防止黑屏
    var isFirstFrameRendered by remember { mutableStateOf(false) }

    DisposableEffect(playerState.player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                android.util.Log.d("VideoPlayerCover", "🎬 onRenderedFirstFrame triggered")
                isFirstFrameRendered = true
            }
            
            // 兼容性：同时也监听 Events
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_RENDERED_FIRST_FRAME)) {
                    android.util.Log.d("VideoPlayerCover", "🎬 EVENT_RENDERED_FIRST_FRAME triggered")
                    isFirstFrameRendered = true
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // 播放结束显示重播状态（通常由上层逻辑处理，这里不复位封面以免闪烁）
                    // isFirstFrameRendered = false 
                }
            }
        }
        
        playerState.player.addListener(listener)
        
        // 初始化检查：如果播放器已经开始播放且有进度，可能错过了事件
        // [Debug] Log initial check
        if (playerState.player.isPlaying && playerState.player.currentPosition > 0) {
             android.util.Log.d("VideoPlayerCover", "⚠️ Initial check: Already playing at ${playerState.player.currentPosition}, hiding cover. (Might be previous video?)")
             isFirstFrameRendered = true
        } else {
             android.util.Log.d("VideoPlayerCover", "✅ Initial check: Not playing or at start. Keeping cover.")
        }

        onDispose {
            playerState.player.removeListener(listener)
        }
    }
    
    // 如果 bvid 改变，重置状态
    LaunchedEffect(bvid) {
        isFirstFrameRendered = false
    }

    // 4. 封面图 (Cover Image) - 始终在第一帧渲染前显示
    // 优先使用 PlayerUiState.Success 中的高清封面 (pic)，否则使用传入的 coverUrl
    var rawCoverUrl = if (uiState is PlayerUiState.Success) uiState.info.pic else coverUrl
    
    // [Fix] 使用 FormatUtils 统一处理 URL (支持无协议头 URL)
    val currentCoverUrl = FormatUtils.fixImageUrl(rawCoverUrl)
    
    // [修改] 只要第一帧未渲染，就显示封面
    // 增加额外检查：如果 buffering 且位置 > 1000ms，说明是中途缓冲，不需要显示封面(保持最后一帧)
    val isInitialBuffering = isBuffering && playerState.player.currentPosition < 1000
    val showCover = !isFirstFrameRendered || (isInitialBuffering && !isFirstFrameRendered)
    
    // [Debug] Logging
    LaunchedEffect(showCover, currentCoverUrl, isFirstFrameRendered, uiState) {
        android.util.Log.d("VideoPlayerCover", "🔍 Check: bvid=$bvid, showCover=$showCover, isFirstFrame=$isFirstFrameRendered, coverUrl=$coverUrl, finalUrl=$currentCoverUrl")
    }

    AnimatedVisibility(
        visible = showCover && currentCoverUrl.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(300)), // 稍微慢一点消失，平滑过渡
        modifier = Modifier.zIndex(10f) // [Fix] 强制提升层级，确保在 VideoSurface 之上
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(LocalContext.current)
                .data(currentCoverUrl)
                // [关键] 尝试使用首页卡片的缓存 Key 作为占位，实现无缝过渡
                // 假设首页卡片使用的是普通模式 ("n")
                .placeholderMemoryCacheKey("cover_${bvid}_n")
                .listener(
                    onStart = { android.util.Log.d("VideoPlayerCover", "🖼️ Image loading started: $currentCoverUrl") },
                    onSuccess = { _, _ -> android.util.Log.d("VideoPlayerCover", "🖼️ Image loaded successfully") },
                    onError = { _, result -> android.util.Log.e("VideoPlayerCover", "❌ Image load failed: ${result.throwable.message}", result.throwable) }
                )
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop, // [修改] 使用 Crop 填满屏幕
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }

    // 2. DanmakuView (使用 ByteDance DanmakuRenderEngine - 覆盖在 PlayerView 上方)
    android.util.Log.d("VideoPlayerSection", "🔍 DanmakuView check: isInPipMode=$isInPipMode, danmakuEnabled=$danmakuEnabled")
        if (!isInPipMode && danmakuEnabled && !isPortraitFullscreen) {
            android.util.Log.d("VideoPlayerSection", " Conditions met, creating DanmakuView...")
            //  计算状态栏高度
            val statusBarHeightPx = remember(context) {
                val resourceId = context.resources.getIdentifier(
                    "status_bar_height", "dimen", "android"
                )
                if (resourceId > 0) {
                    context.resources.getDimensionPixelSize(resourceId)
                } else {
                    (24 * context.resources.displayMetrics.density).toInt()
                }
            }
            
            //  非全屏时的顶部偏移量
            val topOffset = if (isFullscreen) 0 else statusBarHeightPx + 20
            
            //  [修复] 移除 key(isFullscreen)，避免横竖屏切换时重建 DanmakuView 导致弹幕消失
            // 使用 remember 保存 DanmakuView 引用，在 update 回调中处理尺寸变化
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!isFullscreen) {
                            Modifier.padding(top = with(LocalContext.current.resources.displayMetrics) {
                                (topOffset / density).dp
                            })
                        } else Modifier
                    )
                    .clipToBounds()
            ) {
                AndroidView(
                    factory = { ctx ->
                        com.bytedance.danmaku.render.engine.DanmakuView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            danmakuManager.attachView(this)
                            android.util.Log.d("VideoPlayerSection", " DanmakuView (RenderEngine) created, isFullscreen=$isFullscreen")
                        }
                    },
                    update = { view ->
                        //  [关键] 横竖屏切换后视图尺寸变化时，重新 attachView 确保弹幕正确显示
                        android.util.Log.d("VideoPlayerSection", " DanmakuView update: size=${view.width}x${view.height}, isFullscreen=$isFullscreen")
                        // 只有当视图有有效尺寸时才 re-attach
                        if (view.width > 0 && view.height > 0) {
                            danmakuManager.attachView(view)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // 3. 高级弹幕层 (Mode 7) - 覆盖在标准弹幕上方
        val advancedDanmakuList by danmakuManager.advancedDanmakuFlow.collectAsState()
        
        if (!isInPipMode && danmakuEnabled && advancedDanmakuList.isNotEmpty()) {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                com.android.purebilibili.feature.video.ui.overlay.AdvancedDanmakuOverlay(
                    danmakuList = advancedDanmakuList,
                    player = playerState.player,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 🖼️ [修复] 手势指示器 - Seek 模式使用缩略图预览
        if (isGestureVisible && !isInPipMode) {
            if (gestureMode == VideoGestureMode.Seek) {
                // 🖼️ Seek 模式：显示带缩略图的预览气泡
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoshotData != null && videoshotData.isValid) {
                        // 🖼️ 有缩略图：显示完整预览
                        com.android.purebilibili.feature.video.ui.components.SeekPreviewBubble(
                            videoshotData = videoshotData,
                            targetPositionMs = seekTargetTime,
                            currentPositionMs = startPosition,
                            durationMs = playerState.player.duration,
                            offsetX = 80f,  // 居中偏移（气泡宽度的一半）
                            containerWidth = 160f  // 与气泡宽度匹配
                        )
                    } else {
                        // 无缩略图：使用原有样式
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(Color.Black.copy(0.7f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val durationSeconds = (playerState.player.duration / 1000).coerceAtLeast(1)
                                val targetSeconds = (seekTargetTime / 1000).toInt()

                                Text(
                                    text = "${FormatUtils.formatDuration(targetSeconds)} / ${FormatUtils.formatDuration(durationSeconds.toInt())}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                val deltaSeconds = (seekTargetTime - startPosition) / 1000
                                val sign = if (deltaSeconds > 0) "+" else ""
                                if (deltaSeconds != 0L) {
                                    Text(
                                        text = "($sign${deltaSeconds}s)",
                                        color = if (deltaSeconds > 0) com.android.purebilibili.core.theme.iOSGreen else com.android.purebilibili.core.theme.iOSRed,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 亮度/音量模式：保持原有样式
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(120.dp)
                        .background(Color.Black.copy(0.7f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = gestureIcon ?: CupertinoIcons.Default.SunMax,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(gesturePercent * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                    }
                }
            }
        }
        
        //  [新增] 双击跳转视觉反馈 (±Ns 提示)
        LaunchedEffect(seekFeedbackVisible) {
            if (seekFeedbackVisible) {
                kotlinx.coroutines.delay(800)
                seekFeedbackVisible = false
            }
        }
        
        AnimatedVisibility(
            visible = seekFeedbackVisible && !isInPipMode,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Black.copy(0.75f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = seekFeedbackText ?: "",
                    color = if (seekFeedbackText?.startsWith("+") == true) com.android.purebilibili.core.theme.iOSGreen else com.android.purebilibili.core.theme.iOSRed,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        //  [新增] 缩放还原按钮 (仅在放大时显示)
        AnimatedVisibility(
            visible = scale > 1.05f && !isInPipMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp), // 避开底部进度条位置
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Button(
                onClick = {
                    scale = 1f
                    panX = 0f
                    panY = 0f
                    // showControls = true // 可选：还原后显示控制栏
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "还原画面",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "还原画面",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        
        //  长按倍速提示（简洁版，1秒后消失）
        LaunchedEffect(longPressSpeedFeedbackVisible) {
            if (longPressSpeedFeedbackVisible) {
                kotlinx.coroutines.delay(1000)
                longPressSpeedFeedbackVisible = false
            }
        }
        
        AnimatedVisibility(
            visible = longPressSpeedFeedbackVisible && !isInPipMode,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(0.75f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "${longPressSpeed}x",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (uiState is PlayerUiState.Success && !isInPipMode) {
            VideoPlayerOverlay(
                player = playerState.player,
                title = uiState.info.title,
                // [修复] 竖屏全屏模式下隐藏底部 Overlay，避免进度状态冲突
                isVisible = showControls && !isPortraitFullscreen,
                onToggleVisible = { showControls = !showControls },
                isFullscreen = isFullscreen,
                currentQualityLabel = uiState.qualityLabels.getOrNull(uiState.qualityIds.indexOf(uiState.currentQuality)) ?: "自动",
                qualityLabels = uiState.qualityLabels,
                qualityIds = uiState.qualityIds,
                onQualitySelected = { index ->
                    val id = uiState.qualityIds.getOrNull(index) ?: 0
                    onQualityChange(id, playerState.player.currentPosition)
                },
                onBack = onBack,
                onToggleFullscreen = onToggleFullscreen,
                
                // 🔒 [新增] 屏幕锁定
                isScreenLocked = isScreenLocked,
                onLockToggle = { isScreenLocked = !isScreenLocked },
                //  [关键] 传入设置状态和真实分辨率字符串
                showStats = showStats,
                realResolution = realResolution,
                //  [新增] 传入清晰度切换状态和会员状态
                isQualitySwitching = uiState.isQualitySwitching,
                isBuffering = isBuffering,  // 缓冲状态
                isLoggedIn = uiState.isLoggedIn,
                isVip = uiState.isVip,
                //  [新增] 弹幕开关和设置
                danmakuEnabled = danmakuEnabled,
                onDanmakuToggle = {
                    val newState = !danmakuEnabled
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuEnabled(context, newState)
                    }
                    //  记录弹幕开关事件
                    com.android.purebilibili.core.util.AnalyticsHelper.logDanmakuToggle(newState)
                },
                danmakuOpacity = danmakuOpacity,
                danmakuFontScale = danmakuFontScale,
                danmakuSpeed = danmakuSpeed,
                danmakuDisplayArea = danmakuDisplayArea,
                onDanmakuOpacityChange = { value ->
                    danmakuManager.opacity = value
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuOpacity(context, value)
                    }
                },
                onDanmakuFontScaleChange = { value ->
                    danmakuManager.fontScale = value
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuFontScale(context, value)
                    }
                },
                onDanmakuSpeedChange = { value ->
                    danmakuManager.speedFactor = value
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuSpeed(context, value)
                    }
                },
                onDanmakuDisplayAreaChange = { value ->
                    danmakuManager.displayArea = value
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuArea(context, value)
                    }
                },
                //  视频比例调节
                currentAspectRatio = currentAspectRatio,
                onAspectRatioChange = { currentAspectRatio = it },
                // 🕺 [新增] 分享功能
                bvid = bvid,
                //  [新增] 视频设置面板回调
                onReloadVideo = onReloadVideo,
                isFlippedHorizontal = isFlippedHorizontal,
                isFlippedVertical = isFlippedVertical,
                onFlipHorizontal = { isFlippedHorizontal = !isFlippedHorizontal },
                onFlipVertical = { isFlippedVertical = !isFlippedVertical },
                //  [新增] 画质切换（用于设置面板）
                onQualityChange = { qid, pos ->
                    onQualityChange(qid, playerState.player.currentPosition)
                },
                //  [新增] CDN 线路切换
                currentCdnIndex = currentCdnIndex,
                cdnCount = cdnCount,
                onSwitchCdn = onSwitchCdn,
                onSwitchCdnTo = onSwitchCdnTo,
                
                //  [新增] 音频模式
                isAudioOnly = isAudioOnly,
                onAudioOnlyToggle = onAudioOnlyToggle,
                
                //  [新增] 定时关闭
                sleepTimerMinutes = sleepTimerMinutes,
                onSleepTimerChange = onSleepTimerChange,
                
                // 🖼️ [新增] 视频预览图数据
                videoshotData = videoshotData,
                
                // 📖 [新增] 视频章节数据
                viewPoints = viewPoints,
                
                // 📱 [新增] 竖屏全屏模式
                isVerticalVideo = isVerticalVideo,
                onPortraitFullscreen = onPortraitFullscreen,
                // 📲 [新增] 小窗模式
                // 📲 [新增] 小窗模式
                onPipClick = onPipClick,
                //  [新增] 拖动进度条开始时清除弹幕
                onSeekStart = { danmakuManager.clear() },
                // [New] Codec & Audio
                currentCodec = currentCodec,
                onCodecChange = onCodecChange,
                currentAudioQuality = currentAudioQuality,
                onAudioQualityChange = onAudioQualityChange,
                // 👀 [新增] 在线观看人数
                onlineCount = uiState.onlineCount
            )
        }
        
        //  空降助手跳过按钮
        if (!isInPipMode) {
            SponsorSkipButton(
                segment = sponsorSegment,
                visible = showSponsorSkipButton,
                onSkip = onSponsorSkip,
                onDismiss = onSponsorDismiss,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
