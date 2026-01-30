// File: feature/video/PlayerViewModel.kt
//  [重构] 简化版 PlayerViewModel - 使用 UseCase 层
package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.feature.video.usecase.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.core.cache.PlayUrlCache
import com.android.purebilibili.core.cooldown.PlaybackCooldownManager
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.SkipAction
import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.core.util.NetworkUtils
import com.android.purebilibili.data.model.VideoLoadError
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.feature.video.controller.QualityManager
import com.android.purebilibili.feature.video.controller.QualityPermissionResult
import com.android.purebilibili.feature.video.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.android.purebilibili.feature.video.player.PlaylistManager
import com.android.purebilibili.feature.video.player.PlaylistItem
import com.android.purebilibili.feature.video.player.PlayMode

// ========== UI State ==========
sealed class PlayerUiState {
    data class Loading(
        val retryAttempt: Int = 0,
        val maxAttempts: Int = 4,
        val message: String = "\u52a0\u8f7d\u4e2d..."
    ) : PlayerUiState() {
        companion object { val Initial = Loading() }
    }
    
    data class Success(
        val info: ViewInfo,
        val playUrl: String,
        val audioUrl: String? = null,
        val related: List<RelatedVideo> = emptyList(),
        val currentQuality: Int = 64,
        val qualityLabels: List<String> = emptyList(),
        val qualityIds: List<Int> = emptyList(),
        val startPosition: Long = 0L,
        val cachedDashVideos: List<DashVideo> = emptyList(),
        val cachedDashAudios: List<DashAudio> = emptyList(),
        val isQualitySwitching: Boolean = false,
        val requestedQuality: Int? = null,
        val isLoggedIn: Boolean = false,
        val isVip: Boolean = false,
        val isFollowing: Boolean = false,
        val isFavorited: Boolean = false,
        val isLiked: Boolean = false,
        val coinCount: Int = 0,
        val emoteMap: Map<String, String> = emptyMap(),
        val isInWatchLater: Boolean = false,  //  稍后再看状态
        val followingMids: Set<Long> = emptySet(),  //  已关注用户 ID 列表
        val videoTags: List<VideoTag> = emptyList(),  //  视频标签列表
        //  CDN 线路切换
        val currentCdnIndex: Int = 0,  // 当前使用的 CDN 索引 (0=主线路)
        val allVideoUrls: List<String> = emptyList(),  // 所有可用视频 URL (主+备用)
        val allAudioUrls: List<String> = emptyList(),   // 所有可用音频 URL (主+备用)
        // 🖼️ [新增] 视频预览图数据（用于进度条拖动预览）
        val videoshotData: VideoshotData? = null,
        // 🎞️ [New] Codec & Audio Info
        val videoCodecId: Int = 0,
        val audioCodecId: Int = 0,
        // 👀 [新增] 在线观看人数
        val onlineCount: String = ""
    ) : PlayerUiState() {
        val cdnCount: Int get() = allVideoUrls.size.coerceAtLeast(1)
        val currentCdnLabel: String get() = "线路${currentCdnIndex + 1}"
    }
    
    data class Error(
        val error: VideoLoadError,
        val canRetry: Boolean = true
    ) : PlayerUiState() {
        val msg: String get() = error.toUserMessage()
    }
}

// ========== ViewModel ==========
class PlayerViewModel : ViewModel() {
    // UseCases
    private val playbackUseCase = VideoPlaybackUseCase()
    private val interactionUseCase = VideoInteractionUseCase()
    private val qualityManager = QualityManager()
    
    //  插件系统（替代旧的SponsorBlockUseCase）
    private var pluginCheckJob: Job? = null
    
    // State
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading.Initial)
    val uiState = _uiState.asStateFlow()
    
    private val _toastEvent = Channel<String>()
    val toastEvent = _toastEvent.receiveAsFlow()
    
    // Celebration animations
    private val _likeBurstVisible = MutableStateFlow(false)
    val likeBurstVisible = _likeBurstVisible.asStateFlow()
    
    private val _tripleCelebrationVisible = MutableStateFlow(false)
    val tripleCelebrationVisible = _tripleCelebrationVisible.asStateFlow()
    
    // Coin dialog
    private val _coinDialogVisible = MutableStateFlow(false)
    val coinDialogVisible = _coinDialogVisible.asStateFlow()
    
    //  SponsorBlock (via Plugin)
    private val _showSkipButton = MutableStateFlow(false)
    val showSkipButton = _showSkipButton.asStateFlow()
    private val _currentSkipReason = MutableStateFlow<String?>( null)
    val currentSkipReason = _currentSkipReason.asStateFlow()
    
    //  Download state
    private val _downloadProgress = MutableStateFlow(-1f)
    val downloadProgress = _downloadProgress.asStateFlow()
    
    //  [新增] 视频章节/看点数据
    private val _viewPoints = MutableStateFlow<List<ViewPoint>>(emptyList())
    val viewPoints = _viewPoints.asStateFlow()
    
    // [新增] 播放完成选择对话框状态
    private val _showPlaybackEndedDialog = MutableStateFlow(false)
    val showPlaybackEndedDialog = _showPlaybackEndedDialog.asStateFlow()
    
    fun dismissPlaybackEndedDialog() {
        _showPlaybackEndedDialog.value = false
    }
    
    fun showPlaybackEndedDialogIfNeeded() {
        _showPlaybackEndedDialog.value = true
    }
    
    // Internal state
    private var currentBvid = ""
    private var currentCid = 0L
    private var exoPlayer: ExoPlayer? = null
    private var heartbeatJob: Job? = null
    private var appContext: android.content.Context? = null  //  [新增] 保存 Context 用于网络检测
    private var hasUserStartedPlayback = false  // 🛡️ [修复] 用户是否主动开始播放（用于区分“加载已看完视频”和“自然播放结束”）
    
    //  Public Player Accessor
    val currentPlayer: Player?
        get() = exoPlayer
        
    /**
     *  UI 仅音频模式状态
     * 
     * 注意：这与 SettingsManager.MiniPlayerMode.BACKGROUND 是两个不同的概念：
     * - isInAudioMode: UI 层的仅音频显示模式，用户主动切换，显示音频播放界面
     * - MiniPlayerMode.BACKGROUND: 设置层的后台音频模式，应用退到后台时的行为
     * 
     * isInAudioMode 控制 UI 显示，MiniPlayerMode.BACKGROUND 控制后台行为
     */
    private val _isInAudioMode = MutableStateFlow(false)
    val isInAudioMode = _isInAudioMode.asStateFlow()
    
    fun setAudioMode(enabled: Boolean) {
        _isInAudioMode.value = enabled
    }

    //  Sleep Timer State
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes = _sleepTimerMinutes.asStateFlow()
    private var sleepTimerJob: Job? = null

    /**
     * 设置定时关闭
     * @param minutes 分钟数，null 表示关闭定时
     */
    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        
        if (minutes != null) {
            sleepTimerJob = viewModelScope.launch {
                Logger.d("PlayerVM", "⏰ 定时关闭已启动: ${minutes}分钟")
                toast("将在 ${minutes} 分钟后停止播放")
                delay(minutes * 60 * 1000L)
                
                // 定时结束
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    exoPlayer?.pause()
                    toast("⏰ 定时结束，已暂停播放")
                    _sleepTimerMinutes.value = null
                    // 如果需要关闭应用或退出页面，可以在这里添加逻辑
                }
            }
        } else {
            Logger.d("PlayerVM", "⏰ 定时关闭已取消")
            toast("定时关闭已取消")
        }
    }
    
    // ========== Public API ==========
    
    /**
     * 初始化持久化存储（需要在使用前调用一次）
     */
    fun initWithContext(context: android.content.Context) {
        appContext = context.applicationContext  //  [新增] 保存应用 Context
        playbackUseCase.initWithContext(context)
        
        // 🎧 Start observing settings preferences
        viewModelScope.launch {
            // Observe Video Codec
            com.android.purebilibili.core.store.SettingsManager.getVideoCodec(context)
                .collect { _videoCodecPreference.value = it }
        }
        
        viewModelScope.launch {
            com.android.purebilibili.core.store.SettingsManager.getAudioQuality(context)
                .collect { 
                    com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "🎵 Audio preference updated from Settings to: $it")
                    _audioQualityPreference.value = it 
                }
        }
    }
    
    fun attachPlayer(player: ExoPlayer) {
        val changed = exoPlayer !== player
        if (changed && exoPlayer != null) saveCurrentPosition()
        exoPlayer = player
        playbackUseCase.attachPlayer(player)
        player.volume = 1.0f
        
        //  [新增] 添加播放完成监听器
        player.addListener(playbackEndListener)
    }
    
    //  [新增] 播放完成监听器
    private val playbackEndListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // �️ [修复] 仅当用户主动开始播放后才触发自动连播
                // 防止从历史记录加载已看完视频时立即跳转
                if (!hasUserStartedPlayback) {
                    Logger.d("PlayerVM", "🛡️ STATE_ENDED but user hasn't started playback, skip auto-play")
                    return
                }
                
                // �🔧 [修复] 检查自动播放设置 - 使用 SettingsManager 同步读取
                val context = appContext ?: return
                val autoPlayEnabled = com.android.purebilibili.core.store.SettingsManager
                    .getAutoPlaySync(context)
                
                if (autoPlayEnabled) {
                    // 🎵 [修复] 优先播放下一个分P，没有分P时再播放推荐视频
                    playNextPageOrRecommended()
                } else {
                    // 自动播放关闭，显示选择对话框
                    _showPlaybackEndedDialog.value = true
                }
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                // 🛡️ [修复] 用户开始播放时设置标志
                hasUserStartedPlayback = true
            }
        }
    }
    
    /**
     * 获取下一个视频的 BVID (用于导航)
     * Side effect: Updates PlaylistManager index
     */
    fun getNextVideoId(): String? {
        val nextItem = PlaylistManager.playNext()
        return nextItem?.bvid
    }

    /**
     * 获取上一个视频的 BVID (用于导航)
     * Side effect: Updates PlaylistManager index
     */
    fun getPreviousVideoId(): String? {
        val prevItem = PlaylistManager.playPrevious()
        return prevItem?.bvid
    }

    /**
     *  [新增] 自动播放推荐视频（使用 PlaylistManager）
     */
    fun playNextRecommended() {
        // 使用 PlaylistManager 获取下一曲
        val nextItem = PlaylistManager.playNext()
        
        if (nextItem != null) {
            viewModelScope.launch {
                toast("正在播放: ${nextItem.title}")
            }
            // 加载新视频 (Auto-play next always forces true)
            loadVideo(nextItem.bvid, autoPlay = true)
        } else {
            // 根据播放模式显示不同提示
            val mode = PlaylistManager.playMode.value
            when (mode) {
                PlayMode.SEQUENTIAL -> toast(" 播放列表结束")
                PlayMode.REPEAT_ONE -> {
                    // 单曲循环：重新播放当前视频
                    exoPlayer?.seekTo(0)
                    exoPlayer?.play()
                }
                else -> toast("没有更多视频")
            }
        }
    }
    
    /**
     * 🎵 [新增] 优先播放下一个分P，如果没有分P则检查合集，最后播放推荐视频
     * 用于分集视频（如音乐合集）的连续播放
     * 优先级: 分P > 合集下一集 > 推荐视频
     */
    fun playNextPageOrRecommended() {
        val current = _uiState.value as? PlayerUiState.Success ?: run {
            // 如果当前没有成功状态，直接播放推荐
            playNextRecommended()
            return
        }
        
        // 1. 优先检查分P
        val pages = current.info.pages
        if (pages.size > 1) {
            val currentPageIndex = pages.indexOfFirst { it.cid == currentCid }
            val nextPageIndex = currentPageIndex + 1
            
            if (nextPageIndex < pages.size) {
                // 播放下一个分P
                val nextPage = pages[nextPageIndex]
                Logger.d("PlayerVM", "🎵 播放下一个分P: P${nextPageIndex + 1} - ${nextPage.part}")
                switchPage(nextPageIndex)
                return
            }
            // 所有分P播放完成，继续检查合集
        }
        
        // 2. 检查合集 (UGC Season)
        current.info.ugc_season?.let { season ->
            val allEpisodes = season.sections.flatMap { it.episodes }
            val currentEpIndex = allEpisodes.indexOfFirst { it.bvid == current.info.bvid }
            val nextEpIndex = currentEpIndex + 1
            
            if (nextEpIndex < allEpisodes.size) {
                // 播放合集下一集
                val nextEpisode = allEpisodes[nextEpIndex]
                Logger.d("PlayerVM", "📂 播放合集下一集: ${nextEpisode.title}")
                viewModelScope.launch {
                    toast("播放合集下一集: ${nextEpisode.title}")
                }
                loadVideo(nextEpisode.bvid, autoPlay = true)
                return
            }
            // 合集已播放完成
            Logger.d("PlayerVM", "📂 合集全部播放完成")
        }
        
        // 3. 最后播放推荐视频
        Logger.d("PlayerVM", "🎵 播放推荐视频")
        playNextRecommended()
    }
    
    /**
     *  [新增] 播放上一个推荐视频（使用 PlaylistManager）
     */
    fun playPreviousRecommended() {
        // 使用 PlaylistManager 获取上一曲
        val prevItem = PlaylistManager.playPrevious()
        
        if (prevItem != null) {
            viewModelScope.launch {
                toast("正在播放: ${prevItem.title}")
            }
            // 加载新视频
            loadVideo(prevItem.bvid, autoPlay = true)
        } else {
            toast("没有上一个视频")
        }
    }
    
    fun reloadVideo() {
        val bvid = currentBvid.takeIf { it.isNotBlank() } ?: return
        val currentPos = exoPlayer?.currentPosition ?: 0L

        // 💾 [修复] 在清除状态前明确保存进度，防止 loadVideo 读取到 0
        if (currentPos > 0) {
            playbackUseCase.savePosition(bvid)
            Logger.d("PlayerVM", "💾 reloadVideo: Saved position $currentPos ms")
        }

        Logger.d("PlayerVM", "🔄 Reloading video (forced)...")
        // 设置标志位，确保 loadVideo 不会跳过
        loadVideo(bvid, force = true, autoPlay = true)
        
        // 如果之前有进度，尝试恢复
        // 注意：loadVideo 是异步的，这里只是一个兜底，主要还是靠 loadVideo 内部读取 cachedPosition
        if (currentPos > 1000) {
             viewModelScope.launch {
                 delay(500)
                 if (exoPlayer?.currentPosition ?: 0L < 1000) {
                     seekTo(currentPos)
                 }
             }
        }
    }
    
    // [修复] 添加 aid 参数支持，用于移动端推荐流（可能只返回 aid）
    // [Added] autoPlay override: null = use settings, true/false = force
    fun loadVideo(bvid: String, aid: Long = 0, force: Boolean = false, autoPlay: Boolean? = null) {
        if (bvid.isBlank()) return
        
        //  防止重复加载：只有在正在加载同一视频时才跳过
        if (!force && currentBvid == bvid && _uiState.value is PlayerUiState.Loading) {
            Logger.d("PlayerVM", " Already loading $bvid, skip")
            return
        }
        
        //  [修复] 更智能的重复检测：只有播放器真正在播放同一视频时才跳过
        // 如果播放器已停止、出错或处于空闲状态，应该重新加载
        val player = exoPlayer
        val isPlayerHealthy = player != null && 
            player.playbackState in listOf(Player.STATE_READY, Player.STATE_BUFFERING) &&
            player.playerError == null // 没有播放错误
        
        val currentSuccess = _uiState.value as? PlayerUiState.Success
        
        // 🎯 [关键修复] 即使 currentBvid 为空（新 ViewModel），如果播放器已经在播放这个视频，也不要重新加载
        // 这种情况发生在 Notification -> MainActivity (New Activity/VM) -> VideoDetailScreen -> reuse attached player
        val isPlayerPlayingSameVideo = isPlayerHealthy && (currentBvid == bvid || (currentBvid.isEmpty() && player?.isPlaying == true))
        val isUiLoaded = currentSuccess != null && currentSuccess.info.bvid == bvid

        if (!force && isPlayerPlayingSameVideo && isUiLoaded) {
            Logger.d("PlayerVM", "🎯 $bvid already playing healthy + UI loaded, skip reload")
            // 补全 ViewModel 状态：currentBvid 可能为空，需要同步
            if (currentBvid.isEmpty()) {
                currentBvid = bvid
            }
            
            //  确保音量正常
            player?.volume = 1.0f
            if (player?.isPlaying == false) {
                player.play()
            }
            return
        }

        // 如果播放器正在播放目标视频，但 UI 未加载（新 ViewModel），我们需要获取信息但跳过播放器重置
        val shouldSkipPlayerPrepare = !force && isPlayerPlayingSameVideo
        if (shouldSkipPlayerPrepare) {
            Logger.d("PlayerVM", "🎯 $bvid already playing but UI missing (New VM). Fetching info, skipping player prepare.")
        }
        
        if (currentBvid.isNotEmpty() && currentBvid != bvid) saveCurrentPosition()
        
        // 🛡️ [修复] 加载新视频时重置标志
        hasUserStartedPlayback = false
        
        val cachedPosition = playbackUseCase.getCachedPosition(bvid)
        currentBvid = bvid
        
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading.Initial
            
                val defaultQuality = appContext?.let { NetworkUtils.getDefaultQualityId(it) } ?: 64
                //  [新增] 获取音频/视频偏好
                val audioQualityPreference = appContext?.let { 
                    com.android.purebilibili.core.store.SettingsManager.getAudioQualitySync(it) 
                } ?: -1
                val videoCodecPreference = appContext?.let { 
                    com.android.purebilibili.core.store.SettingsManager.getVideoCodecSync(it) 
                } ?: "hev1"
                
                // [Added] Determine auto-play behavior
                // If autoPlay arg is present, use it. Otherwise reset to "Click to Play" setting
                val shouldAutoPlay = autoPlay ?: appContext?.let {
                    com.android.purebilibili.core.store.SettingsManager.getClickToPlaySync(it)
                } ?: true
                
                Logger.d("PlayerViewModel", "⏯️ AutoPlay Decision: arg=$autoPlay, setting=${shouldAutoPlay}, Final=$shouldAutoPlay")
            
            // 📉 [省流量] 省流量模式逻辑：
            // - ALWAYS: 任何网络都限制 480P
            // - MOBILE_ONLY: 仅移动数据时限制 480P（WiFi不受限）
            val isOnMobileNetwork = appContext?.let { NetworkUtils.isMobileData(it) } ?: false
            val dataSaverMode = appContext?.let { 
                com.android.purebilibili.core.store.SettingsManager.getDataSaverModeSync(it) 
            } ?: com.android.purebilibili.core.store.SettingsManager.DataSaverMode.MOBILE_ONLY
            
            //  判断是否应该限制画质
            val shouldLimitQuality = when (dataSaverMode) {
                com.android.purebilibili.core.store.SettingsManager.DataSaverMode.OFF -> false
                com.android.purebilibili.core.store.SettingsManager.DataSaverMode.ALWAYS -> true  // 任何网络都限制
                com.android.purebilibili.core.store.SettingsManager.DataSaverMode.MOBILE_ONLY -> isOnMobileNetwork  // 仅移动数据
            }
            
            var finalQuality = defaultQuality
            if (shouldLimitQuality && finalQuality > 32) {
                finalQuality = 32
                com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "📉 省流量模式(${dataSaverMode.label}): 限制画质为480P")
            }
            
            try {
                // 🛡️ [修复] 增加超时保护，防止加载无限挂起
                val result = kotlinx.coroutines.withTimeout(15000L) {
                    playbackUseCase.loadVideo(
                        bvid, 
                        aid, 
                        finalQuality, 
                        audioQualityPreference,
                        videoCodecPreference,
                        shouldAutoPlay  // Pass to UseCase (even if unused there)
                    )
                }

                when (result) {
                    is VideoLoadResult.Success -> {
                        currentCid = result.info.cid
                        
                        // Play video
                        if (!shouldSkipPlayerPrepare) {
                            if (result.audioUrl != null) {
                                playbackUseCase.playDashVideo(result.playUrl, result.audioUrl, cachedPosition, playWhenReady = shouldAutoPlay)
                            } else {
                                playbackUseCase.playVideo(result.playUrl, cachedPosition, playWhenReady = shouldAutoPlay)
                            }
                        } else {
                             // 🎯 Skip preparing player, but ensure it's playing if needed
                             Logger.d("PlayerVM", "🎯 Skipping player preparation (already playing)")
                             exoPlayer?.let { p ->
                                 p.volume = 1.0f
                                 if (!p.isPlaying) p.play()
                             }
                        }
                        
                        //  收集所有 CDN URL (主+备用)
                        val allVideoUrls = buildList {
                            add(result.playUrl)
                            result.cachedDashVideos
                                .find { it.id == result.quality }
                                ?.backupUrl
                                ?.filterNotNull()
                                ?.filter { it.isNotEmpty() }
                                ?.let { addAll(it) }
                        }.distinct()
                        
                        val allAudioUrls = buildList {
                            result.audioUrl?.let { add(it) }
                            result.cachedDashAudios.firstOrNull()
                                ?.backupUrl
                                ?.filterNotNull()
                                ?.filter { it.isNotEmpty() }
                                ?.let { addAll(it) }
                        }.distinct()
                        
                        Logger.d("PlayerVM", "📡 CDN 线路: 视频${allVideoUrls.size}个, 音频${allAudioUrls.size}个")
                        
                        _uiState.value = PlayerUiState.Success(
                            info = result.info,
                            playUrl = result.playUrl,
                            audioUrl = result.audioUrl,
                            related = result.related,
                            currentQuality = result.quality,
                            qualityIds = result.qualityIds,
                            qualityLabels = result.qualityLabels,
                            cachedDashVideos = result.cachedDashVideos,
                            cachedDashAudios = result.cachedDashAudios,
                            emoteMap = result.emoteMap,
                            isLoggedIn = result.isLoggedIn,
                            isVip = result.isVip,
                            isFollowing = result.isFollowing,
                            isFavorited = result.isFavorited,
                            isLiked = result.isLiked,
                            coinCount = result.coinCount,
                            //  CDN 线路
                            currentCdnIndex = 0,
                            allVideoUrls = allVideoUrls,

                            allAudioUrls = allAudioUrls,
                            // [New] Codec/Audio info
                            videoCodecId = result.videoCodecId,
                            audioCodecId = result.audioCodecId
                        )
                        
                        //  [新增] 异步加载关注列表（用于推荐视频的已关注标签）
                        if (result.isLoggedIn) {
                            loadFollowingMids()
                        }
                        
                        //  异步加载视频标签
                        loadVideoTags(bvid)
                        
                        // 🖼️ 异步加载视频预览图（用于进度条拖动预览）
                        loadVideoshot(bvid, result.info.cid)
                        
                        // 📖 异步加载视频章节信息（用于进度条章节标记）
                        loadChapterInfo(bvid, result.info.cid)
                        
                        // 👀 [新增] 开始轮询在线观看人数
                        startOnlineCountPolling(bvid, result.info.cid)
                        
                        //  [新增] 更新播放列表
                        updatePlaylist(result.info, result.related)
                        
                        startHeartbeat()
                        
                        //  通知插件系统：视频已加载
                        PluginManager.getEnabledPlayerPlugins().forEach { plugin ->
                            try {
                                plugin.onVideoLoad(bvid, currentCid)
                            } catch (e: Exception) {
                                Logger.e("PlayerVM", "Plugin ${plugin.name} onVideoLoad failed", e)
                            }
                        }
                        
                        //  启动插件检查定时器
                        startPluginCheck()
                        
                        AnalyticsHelper.logVideoPlay(bvid, result.info.title, result.info.owner.name)
                    }
                    is VideoLoadResult.Error -> {
                        CrashReporter.reportVideoError(bvid, "load_failed", result.error.toUserMessage())
                        _uiState.value = PlayerUiState.Error(result.error, result.canRetry)
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Logger.e("PlayerVM", "⚠️ Video load timed out for $bvid")
                PlaybackCooldownManager.recordFailure(bvid, "timeout")
                _uiState.value = PlayerUiState.Error(VideoLoadError.Timeout)
            } catch (e: Exception) {
                Logger.e("PlayerVM", "⚠️ Unexpected load exception", e)
                _uiState.value = PlayerUiState.Error(VideoLoadError.UnknownError(e))
            }
        }
    }
    
    /**
     *  [新增] 更新播放列表
     */
    private fun updatePlaylist(currentInfo: com.android.purebilibili.data.model.response.ViewInfo, related: List<com.android.purebilibili.data.model.response.RelatedVideo>) {
        val currentPlaylist = PlaylistManager.playlist.value
        val currentIndex = PlaylistManager.currentIndex.value
        val currentItemInList = currentPlaylist.getOrNull(currentIndex)

        // 转换推荐视频为播放项
        val relatedItems = related.map { video ->
            PlaylistItem(
                bvid = video.bvid,
                title = video.title,
                cover = video.pic,
                owner = video.owner.name,
                duration = video.duration.toLong()
            )
        }
        
        // 创建当前视频的播放项 (updated with full info)
        val currentFullItem = PlaylistItem(
            bvid = currentInfo.bvid,
            title = currentInfo.title,
            cover = currentInfo.pic,
            owner = currentInfo.owner.name,
            duration = 0L // ViewInfo 暂无 duration 字段，暂置为 0
        )

        if (currentItemInList != null && currentItemInList.bvid == currentInfo.bvid) {
             // 命中当前播放列表逻辑：保留历史，更新未来
             // 1. 获取当前索引及之前的列表 (历史 + 当前)
             val history = currentPlaylist.take(currentIndex) // 0 .. currentIndex-1
             
             // 2. 组合新列表: 历史 + 当前(更新详情) + 新推荐
             val newPlaylist = history + currentFullItem + relatedItems
             
             // 3. 更新列表，保持当前索引不变
             PlaylistManager.setPlaylist(newPlaylist, currentIndex)
             Logger.d("PlayerVM", " 播放列表已扩展: 保留 ${history.size} 项历史, 更新后续 ${relatedItems.size} 项")
        } else {
            // 新播放逻辑：当前 + 推荐
            val playlist = listOf(currentFullItem) + relatedItems
            PlaylistManager.setPlaylist(playlist, 0)
            Logger.d("PlayerVM", " 播放列表已重置: 1 + ${relatedItems.size} 项")
        }
        
        // 🚀 [优化] 预加载前 2 个推荐视频的 PlayUrl
        preloadRelatedPlayUrls(related.take(2))
    }
    
    /**
     * 🚀 [新增] 预加载推荐视频的 PlayUrl
     * 异步获取视频详情（获取 cid）并缓存 PlayUrl，切换视频时更快
     */
    private fun preloadRelatedPlayUrls(videos: List<com.android.purebilibili.data.model.response.RelatedVideo>) {
        if (videos.isEmpty()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            for (video in videos) {
                try {
                    // 获取视频详情（主要是为了获取 cid）
                    // getVideoDetails 返回 Pair<ViewInfo, PlayUrlData>
                    val detailResult = com.android.purebilibili.data.repository.VideoRepository.getVideoDetails(video.bvid)
                    val (viewInfo, _) = detailResult.getOrNull() ?: continue
                    
                    // 检查 PlayUrl 是否已缓存
                    if (com.android.purebilibili.core.cache.PlayUrlCache.get(video.bvid, viewInfo.cid) != null) {
                        Logger.d("PlayerVM", "🚀 Preload skip (cached): ${video.bvid}")
                        continue
                    }
                    
                    // 获取默认画质
                    val defaultQuality = appContext?.let { com.android.purebilibili.core.util.NetworkUtils.getDefaultQualityId(it) } ?: 64
                    
                    // 预加载 PlayUrl（会自动缓存到 PlayUrlCache）
                    com.android.purebilibili.data.repository.VideoRepository.getPlayUrlData(
                        video.bvid, 
                        viewInfo.cid, 
                        defaultQuality
                    )
                    Logger.d("PlayerVM", "🚀 Preloaded PlayUrl: ${video.bvid}")
                } catch (e: Exception) {
                    // 预加载失败不影响正常播放，静默忽略
                    Logger.d("PlayerVM", "🚀 Preload failed (ignored): ${video.bvid}")
                }
            }
        }
    }
    
    fun retry() {
        val bvid = currentBvid.takeIf { it.isNotBlank() } ?: return
        
        //  检查当前错误类型，如果是全局冷却则清除所有冷却
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Error && 
            currentState.error is VideoLoadError.GlobalCooldown) {
            PlaybackCooldownManager.clearAll()
        } else {
            // 清除该视频的冷却状态，允许用户强制重试
            PlaybackCooldownManager.clearForVideo(bvid)
        }
        
        PlayUrlCache.invalidate(bvid, currentCid)
        currentBvid = ""
        loadVideo(bvid, autoPlay = true) // Retry should auto-play
    }
    
    /**
     *  重载视频 - 保持当前播放位置
     * 用于设置面板的"重载视频"功能
     */

    
    /**
     *  切换 CDN 线路
     * 在当前画质下切换到下一个 CDN
     */
    fun switchCdn() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        
        if (current.cdnCount <= 1) {
            viewModelScope.launch { toast("没有其他可用线路") }
            return
        }
        
        // 计算下一个 CDN 索引（循环）
        val nextIndex = (current.currentCdnIndex + 1) % current.cdnCount
        val nextVideoUrl = current.allVideoUrls.getOrNull(nextIndex) ?: return
        val nextAudioUrl = current.allAudioUrls.getOrNull(nextIndex)
        
        val currentPos = exoPlayer?.currentPosition ?: 0L
        
        viewModelScope.launch {
            Logger.d("PlayerVM", "📡 切换线路: ${current.currentCdnIndex + 1} → ${nextIndex + 1}")
            
            // 使用新的 URL 播放
            if (nextAudioUrl != null) {
                playbackUseCase.playDashVideo(nextVideoUrl, nextAudioUrl, currentPos)
            } else {
                playbackUseCase.playVideo(nextVideoUrl, currentPos)
            }
            
            // 更新状态
            _uiState.value = current.copy(
                playUrl = nextVideoUrl,
                audioUrl = nextAudioUrl,
                currentCdnIndex = nextIndex
            )
            
            toast("已切换到线路${nextIndex + 1}")
        }
    }
    
    /**
     *  切换到指定 CDN 线路
     */
    fun switchCdnTo(index: Int) {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        
        if (index < 0 || index >= current.cdnCount) return
        if (index == current.currentCdnIndex) {
            viewModelScope.launch { toast("已是当前线路") }
            return
        }
        
        val nextVideoUrl = current.allVideoUrls.getOrNull(index) ?: return
        val nextAudioUrl = current.allAudioUrls.getOrNull(index)
        
        val currentPos = exoPlayer?.currentPosition ?: 0L
        
        viewModelScope.launch {
            Logger.d("PlayerVM", "📡 切换到线路: ${index + 1}")
            
            if (nextAudioUrl != null) {
                playbackUseCase.playDashVideo(nextVideoUrl, nextAudioUrl, currentPos)
            } else {
                playbackUseCase.playVideo(nextVideoUrl, currentPos)
            }
            
            _uiState.value = current.copy(
                playUrl = nextVideoUrl,
                audioUrl = nextAudioUrl,
                currentCdnIndex = index
            )
            
            toast("已切换到线路${index + 1}")
        }
    }
    
    // ========== State Restoration ==========
    
    /**
     * [修复] 从缓存恢复 UI 状态，避免在返回前台时重复请求网络导致错误
     */
    fun restoreUiState(state: PlayerUiState.Success) {
        // 只有当前是非成功状态，或者虽然是成功状态但 BVID 不同时，才允许恢复
        // 这样可以避免覆盖当前可能更新的状态
        if (_uiState.value !is PlayerUiState.Success || 
            (_uiState.value as? PlayerUiState.Success)?.info?.bvid != state.info.bvid) {
            
            Logger.d("PlayerVM", "♻️ Restoring UI state from cache: ${state.info.title}")
            _uiState.value = state
            currentBvid = state.info.bvid
            currentCid = state.info.cid
            
            // 恢复播放器引用
            // 注意：restoreUiState 通常伴随着 setVideoInfo/MiniPlayerManager 的恢复
            // 这里主要负责 UI 数据的恢复
            
            // 重新绑定监听器等（如果是全新的 ViewModel）
            // ...
        } else {
            Logger.d("PlayerVM", "♻️ Skipping state restoration, already has valid state")
        }
    }

    // ========== Interaction ==========
    
    fun toggleFollow() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        viewModelScope.launch {
            interactionUseCase.toggleFollow(current.info.owner.mid, current.isFollowing)
                .onSuccess { _uiState.value = current.copy(isFollowing = it); toast(if (it) "\u5173\u6ce8\u6210\u529f" else "\u5df2\u53d6\u6d88\u5173\u6ce8") }
                .onFailure { toast(it.message ?: "\u64cd\u4f5c\u5931\u8d25") }
        }
    }
    
    fun toggleFavorite() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        viewModelScope.launch {
            interactionUseCase.toggleFavorite(current.info.aid, current.isFavorited, currentBvid)
                .onSuccess { 
                    val newStat = current.info.stat.copy(favorite = current.info.stat.favorite + if (it) 1 else -1)
                    _uiState.value = current.copy(info = current.info.copy(stat = newStat), isFavorited = it)
                    //  彩蛋：使用趣味消息（如果设置开启）
                    val message = if (it && appContext?.let { ctx -> com.android.purebilibili.core.store.SettingsManager.isEasterEggEnabledSync(ctx) } == true) {
                        com.android.purebilibili.core.util.EasterEggs.getFavoriteMessage()
                    } else {
                        if (it) "已收藏" else "已取消收藏"
                    }
                    toast(message)
                }
                .onFailure { toast(it.message ?: "\u64cd\u4f5c\u5931\u8d25") }
        }
    }
    
    fun toggleLike() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        viewModelScope.launch {
            interactionUseCase.toggleLike(current.info.aid, current.isLiked, currentBvid)
                .onSuccess { 
                    val newStat = current.info.stat.copy(like = current.info.stat.like + if (it) 1 else -1)
                    _uiState.value = current.copy(info = current.info.copy(stat = newStat), isLiked = it)
                    if (it) _likeBurstVisible.value = true
                    //  彩蛋：使用趣味消息（如果设置开启）
                    val message = if (it && appContext?.let { ctx -> com.android.purebilibili.core.store.SettingsManager.isEasterEggEnabledSync(ctx) } == true) {
                        com.android.purebilibili.core.util.EasterEggs.getLikeMessage()
                    } else {
                        if (it) "点赞成功" else "已取消点赞"
                    }
                    toast(message)
                }
                .onFailure { toast(it.message ?: "操作失败") }
        }
    }

    // ========== 评论发送对话框 ==========
    
    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog = _showCommentDialog.asStateFlow()

    // 表情包数据
    private val _emotePackages = MutableStateFlow<List<com.android.purebilibili.data.model.response.EmotePackage>>(emptyList())
    val emotePackages = _emotePackages.asStateFlow()
    private var isEmotesLoaded = false

    private fun loadEmotes() {
        if (isEmotesLoaded) return
        viewModelScope.launch {
            com.android.purebilibili.data.repository.CommentRepository.getEmotePackages()
                .onSuccess { 
                    _emotePackages.value = it 
                    isEmotesLoaded = true
                    android.util.Log.d("PlayerViewModel", "📦 Emotes loaded: ${it.size} packages")
                }
                .onFailure { Logger.e("PlayerViewModel", "Failed to load emotes", it) }
        }
    }
    
    fun showCommentInputDialog() {
        android.util.Log.d("PlayerViewModel", "📝 showCommentInputDialog called")
        _showCommentDialog.value = true
        // 懒加载表情包
        loadEmotes()
    }
    
    fun hideCommentInputDialog() {
        _showCommentDialog.value = false
    }

    // ========== 弹幕发送 ==========
    
    private val _showDanmakuDialog = MutableStateFlow(false)
    val showDanmakuDialog = _showDanmakuDialog.asStateFlow()
    
    private val _isSendingDanmaku = MutableStateFlow(false)
    val isSendingDanmaku = _isSendingDanmaku.asStateFlow()
    
    fun showDanmakuSendDialog() {
        _showDanmakuDialog.value = true
    }
    
    fun hideDanmakuSendDialog() {
        _showDanmakuDialog.value = false
    }
    
    /**
     * 发送弹幕
     * 
     * @param message 弹幕内容
     * @param color 颜色 (十进制 RGB)
     * @param mode 模式: 1=滚动, 4=底部, 5=顶部
     * @param fontSize 字号: 18=小, 25=中, 36=大
     */
    fun sendDanmaku(
        message: String,
        color: Int = 16777215,
        mode: Int = 1,
        fontSize: Int = 25
    ) {
        val current = _uiState.value as? PlayerUiState.Success ?: run {
            viewModelScope.launch { toast("视频未加载") }
            return
        }
        
        if (currentCid == 0L) {
            viewModelScope.launch { toast("视频未加载") }
            return
        }
        
        val progress = exoPlayer?.currentPosition ?: 0L
        
        viewModelScope.launch {
            _isSendingDanmaku.value = true
            
            com.android.purebilibili.data.repository.DanmakuRepository
                .sendDanmaku(
                    aid = current.info.aid,
                    cid = currentCid,
                    message = message,
                    progress = progress,
                    color = color,
                    fontSize = fontSize,
                    mode = mode
                )
                .onSuccess {
                    toast("发送成功")
                    _showDanmakuDialog.value = false
                    
                    // 本地即时显示弹幕
                    // 注意：这需要在 Composable 中通过 DanmakuManager 调用
                    // 这里只发送事件通知
                    _danmakuSentEvent.send(DanmakuSentData(message, color, mode, fontSize))
                }
                .onFailure { error ->
                    toast(error.message ?: "发送失败")
                }
            
            _isSendingDanmaku.value = false
        }
    }
    
    // 弹幕发送成功事件（用于本地显示）
    data class DanmakuSentData(val text: String, val color: Int, val mode: Int, val fontSize: Int)
    private val _danmakuSentEvent = Channel<DanmakuSentData>()
    val danmakuSentEvent = _danmakuSentEvent.receiveAsFlow()
    
    // ========== 弹幕上下文菜单 ==========
    data class DanmakuMenuState(
        val visible: Boolean = false,
        val text: String = "",
        val dmid: Long = 0,
        val uid: Long = 0, // 发送者 UID (如果可用)
        val isSelf: Boolean = false // 是否是自己发送的
    )
    
    private val _danmakuMenuState = MutableStateFlow(DanmakuMenuState())
    val danmakuMenuState = _danmakuMenuState.asStateFlow()
    
    fun showDanmakuMenu(dmid: Long, text: String, uid: Long = 0, isSelf: Boolean = false) {
        _danmakuMenuState.value = DanmakuMenuState(
            visible = true,
            text = text,
            dmid = dmid,
            uid = uid,
            isSelf = isSelf
        )
        // 暂停播放 (可选，防止弹幕飘走)
        // if (exoPlayer?.isPlaying == true) exoPlayer?.pause()
    }
    
    fun hideDanmakuMenu() {
        _danmakuMenuState.value = _danmakuMenuState.value.copy(visible = false)
        // 恢复播放?
    }

    /**
     * 撤回弹幕
     * 仅能撤回自己 2 分钟内的弹幕，每天 3 次机会
     * 
     * @param dmid 弹幕 ID
     */
    fun recallDanmaku(dmid: Long) {
        if (currentCid == 0L) {
            viewModelScope.launch { toast("视频未加载") }
            return
        }
        
        viewModelScope.launch {
            com.android.purebilibili.data.repository.DanmakuRepository
                .recallDanmaku(cid = currentCid, dmid = dmid)
                .onSuccess { message ->
                    toast(message.ifEmpty { "撤回成功" })
                }
                .onFailure { error ->
                    toast(error.message ?: "撤回失败")
                }
        }
    }

    /**
     * 点赞弹幕
     * 
     * @param dmid 弹幕 ID
     * @param like true=点赞, false=取消点赞
     */
    fun likeDanmaku(dmid: Long, like: Boolean = true) {
        if (currentCid == 0L) {
            viewModelScope.launch { toast("视频未加载") }
            return
        }
        
        viewModelScope.launch {
            com.android.purebilibili.data.repository.DanmakuRepository
                .likeDanmaku(cid = currentCid, dmid = dmid, like = like)
                .onSuccess {
                    toast(if (like) "点赞成功" else "已取消点赞")
                }
                .onFailure { error ->
                    toast(error.message ?: "操作失败")
                }
        }
    }

    /**
     * 举报弹幕
     * 
     * @param dmid 弹幕 ID
     * @param reason 举报原因: 1=违法/2=色情/3=广告/4=引战/5=辱骂/6=剧透/7=刷屏/8=其他
     */
    fun reportDanmaku(dmid: Long, reason: Int, content: String = "") {
        if (currentCid == 0L) {
            viewModelScope.launch { toast("视频未加载") }
            return
        }
        
        viewModelScope.launch {
            com.android.purebilibili.data.repository.DanmakuRepository
                .reportDanmaku(cid = currentCid, dmid = dmid, reason = reason, content = content)
                .onSuccess {
                    toast("举报成功")
                }
                .onFailure { error ->
                    toast(error.message ?: "举报失败")
                }
        }
    }
    
    // ========== 评论发送 ==========
    
    private val _commentInput = MutableStateFlow("")
    val commentInput = _commentInput.asStateFlow()
    
    private val _isSendingComment = MutableStateFlow(false)
    val isSendingComment = _isSendingComment.asStateFlow()
    
    private val _replyingToComment = MutableStateFlow<com.android.purebilibili.data.model.response.ReplyItem?>(null)
    val replyingToComment = _replyingToComment.asStateFlow()
    
    fun setCommentInput(text: String) {
        _commentInput.value = text
    }
    
    fun setReplyingTo(comment: com.android.purebilibili.data.model.response.ReplyItem?) {
        _replyingToComment.value = comment
    }
    
    fun clearReplyingTo() {
        _replyingToComment.value = null
    }
    
    /**
     * 发送评论
     * @param inputMessage 可选直接传入的内容，如果不传则使用 state 中的内容
     */
    fun sendComment(inputMessage: String? = null) {
        if (inputMessage != null) {
            _commentInput.value = inputMessage
        }
        val current = _uiState.value as? PlayerUiState.Success ?: return
        val message = _commentInput.value.trim()
        
        if (message.isEmpty()) {
            viewModelScope.launch { toast("请输入评论内容") }
            return
        }
        
        viewModelScope.launch {
            _isSendingComment.value = true
            
            val replyTo = _replyingToComment.value
            val root = replyTo?.rpid ?: 0L
            val parent = replyTo?.rpid ?: 0L
            
            com.android.purebilibili.data.repository.CommentRepository
                .addComment(
                    aid = current.info.aid,
                    message = message,
                    root = root,
                    parent = parent
                )
                .onSuccess { reply ->
                    toast(if (replyTo != null) "回复成功" else "评论成功")
                    _commentInput.value = ""
                    _replyingToComment.value = null
                    
                    // 通知 UI 刷新评论列表
                    _commentSentEvent.send(reply)
                }
                .onFailure { error ->
                    toast(error.message ?: "发送失败")
                }
            
            _isSendingComment.value = false
        }
    }
    
    // 评论发送成功事件
    private val _commentSentEvent = Channel<com.android.purebilibili.data.model.response.ReplyItem?>()
    val commentSentEvent = _commentSentEvent.receiveAsFlow()

    
    // ========== Settings: Codec & Audio ==========
    
    // ========== Settings: Codec & Audio ==========
    
    // Preferences StateFlows (Initialized in initWithContext)
    private val _videoCodecPreference = MutableStateFlow("hev1")
    val videoCodecPreference = _videoCodecPreference.asStateFlow()
    
    private val _audioQualityPreference = MutableStateFlow(-1)
    val audioQualityPreference = _audioQualityPreference.asStateFlow()
    
    fun setVideoCodec(codec: String) {
        _videoCodecPreference.value = codec // Optimistic update
        viewModelScope.launch {
            appContext?.let { 
                com.android.purebilibili.core.store.SettingsManager.setVideoCodec(it, codec)
                // Reload to apply changes if playing
                reloadVideo()
            }
        }
    }

    fun setAudioQuality(audioQuality: Int) {
        _audioQualityPreference.value = audioQuality // Optimistic update
        com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "🎵 setAudioQuality called with: $audioQuality")
        //  [调试] 显示 Toast 提示
        val label = when(audioQuality) {
            -1 -> "自动"
            30280 -> "192K"
            30250 -> "杜比全景声"
            30251 -> "Hi-Res无损"
            else -> "未知($audioQuality)"
        }
        toast("切换音质为: $label")

        viewModelScope.launch {
            appContext?.let { 
                com.android.purebilibili.core.store.SettingsManager.setAudioQuality(it, audioQuality)
                reloadVideo() // Reload to apply new audio quality
            }
        }
    }

    //  相互作用
    
    //  稍后再看
    fun toggleWatchLater() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        viewModelScope.launch {
            interactionUseCase.toggleWatchLater(current.info.aid, current.isInWatchLater, currentBvid)
                .onSuccess { inWatchLater ->
                    _uiState.value = current.copy(isInWatchLater = inWatchLater)
                    toast(if (inWatchLater) "已添加到稍后再看" else "已从稍后再看移除")
                }
                .onFailure { toast(it.message ?: "操作失败") }
        }
    }
    
    //  异步加载关注列表（用于推荐视频的已关注标签）
    private fun loadFollowingMids() {
        viewModelScope.launch {
            try {
                val mid = com.android.purebilibili.core.store.TokenManager.midCache ?: return@launch
                val allMids = mutableSetOf<Long>()
                var page = 1
                val pageSize = 50
                
                // 只加载前 200 个关注（4页），避免请求过多
                while (page <= 4) {
                    try {
                        val result = com.android.purebilibili.core.network.NetworkModule.api.getFollowings(mid, page, pageSize)
                        if (result.code == 0 && result.data != null) {
                            val list = result.data.list ?: break
                            if (list.isEmpty()) break
                            allMids.addAll(list.map { it.mid })
                            if (list.size < pageSize) break
                            page++
                        } else {
                            break
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
                
                // 更新 UI 状态
                val current = _uiState.value as? PlayerUiState.Success ?: return@launch
                _uiState.value = current.copy(followingMids = allMids)
                Logger.d("PlayerVM", " Loaded ${allMids.size} following mids")
            } catch (e: Exception) {
                Logger.d("PlayerVM", " Failed to load following mids: ${e.message}")
            }
        }
    }
    
    //  异步加载视频标签
    /**
     *  保存封面到相册
     */
    fun saveCover(context: android.content.Context) {
        val current = _uiState.value as? PlayerUiState.Success
        val coverUrl = current?.info?.pic ?: return
        val title = current.info.title
        
        viewModelScope.launch {
            val success = com.android.purebilibili.feature.download.DownloadManager.saveImageToGallery(context, coverUrl, title)
            if (success) toast("封面已保存到相册")
            else toast("保存失败")
        }
    }

    /**
     *  下载音频
     */
    fun downloadAudio(context: android.content.Context) {
        val current = _uiState.value as? PlayerUiState.Success ?: run {
            toast("无法获取视频信息")
            return
        }
        val audioUrl = current.audioUrl
        if (audioUrl.isNullOrEmpty()) {
            toast("无法获取音频地址")
            return
        }
        
        val task = com.android.purebilibili.feature.download.DownloadTask(
            bvid = current.info.bvid,
            cid = current.info.cid,
            title = current.info.title,
            cover = current.info.pic,
            ownerName = current.info.owner.name,
            ownerFace = current.info.owner.face,
            duration = exoPlayer?.duration?.toInt()?.div(1000) ?: 0,
            quality = 0,
            qualityDesc = "音频",
            videoUrl = "",
            audioUrl = audioUrl,
            isAudioOnly = true
        )
        
        val started = com.android.purebilibili.feature.download.DownloadManager.addTask(task)
        if (started) {
            toast("已开始下载音频")
        } else {
            toast("该任务已在下载中或已完成")
        }
    }

    private fun loadVideoTags(bvid: String) {
        viewModelScope.launch {
            try {
                val response = com.android.purebilibili.core.network.NetworkModule.api.getVideoTags(bvid)
                if (response.code == 0 && response.data != null) {
                    _uiState.update { current ->
                        if (current is PlayerUiState.Success) {
                            current.copy(videoTags = response.data)
                        } else current
                    }
                    Logger.d("PlayerVM", "🏷️ Loaded ${response.data.size} video tags")
                }
            } catch (e: Exception) {
                Logger.d("PlayerVM", " Failed to load video tags: ${e.message}")
            }
        }
    }
    
    // 🖼️ 异步加载视频预览图数据（用于进度条拖动预览）
    private fun loadVideoshot(bvid: String, cid: Long) {
        viewModelScope.launch {
            try {
                val videoshotData = VideoRepository.getVideoshot(bvid, cid)
                if (videoshotData != null && videoshotData.isValid) {
                    _uiState.update { current ->
                        if (current is PlayerUiState.Success) {
                            current.copy(videoshotData = videoshotData)
                        } else current
                    }
                    Logger.d("PlayerVM", "🖼️ Loaded videoshot: ${videoshotData.image.size} images, ${videoshotData.index.size} frames")
                }
            } catch (e: Exception) {
                Logger.d("PlayerVM", "🖼️ Failed to load videoshot: ${e.message}")
            }
        }
    }
    
    // 👀 [新增] 在线观看人数定时刷新 Job
    private var onlineCountJob: Job? = null
    
    // 👀 [新增] 获取并更新在线观看人数
    private fun startOnlineCountPolling(bvid: String, cid: Long) {
        // 取消之前的轮询
        onlineCountJob?.cancel()
        
        onlineCountJob = viewModelScope.launch {
            while (true) {
                try {
                    val response = com.android.purebilibili.core.network.NetworkModule.api.getOnlineCount(bvid, cid)
                    if (response.code == 0 && response.data != null) {
                        val onlineText = "${response.data.total}人正在看"
                        _uiState.update { current ->
                            if (current is PlayerUiState.Success) {
                                current.copy(onlineCount = onlineText)
                            } else current
                        }
                        Logger.d("PlayerVM", "👀 Online count: ${response.data.total}")
                    }
                } catch (e: Exception) {
                    Logger.d("PlayerVM", "👀 Failed to fetch online count: ${e.message}")
                }
                // 每 30 秒更新一次
                delay(30_000)
            }
        }
    }
    
    //  [新增] 异步加载视频章节/看点数据（用于进度条章节标记）
    private fun loadChapterInfo(bvid: String, cid: Long) {
        viewModelScope.launch {
            try {
                val response = com.android.purebilibili.core.network.NetworkModule.api.getPlayerInfo(bvid, cid)
                if (response.code == 0 && response.data != null) {
                    val points = response.data.viewPoints
                    if (points.isNotEmpty()) {
                        _viewPoints.value = points
                        Logger.d("PlayerVM", "📖 Loaded ${points.size} chapter points")
                    } else {
                        _viewPoints.value = emptyList()
                        Logger.d("PlayerVM", "📖 No chapter points for this video")
                    }
                }
            } catch (e: Exception) {
                Logger.d("PlayerVM", "📖 Failed to load chapter info: ${e.message}")
                _viewPoints.value = emptyList()
            }
        }
    }
    
    fun openCoinDialog() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        if (current.coinCount >= 2) { toast("\u5df2\u6295\u6ee12\u4e2a\u786c\u5e01"); return }
        _coinDialogVisible.value = true
    }
    
    fun closeCoinDialog() { _coinDialogVisible.value = false }
    
    fun doCoin(count: Int, alsoLike: Boolean) {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        _coinDialogVisible.value = false
        viewModelScope.launch {
            interactionUseCase.doCoin(current.info.aid, count, alsoLike, currentBvid)
                .onSuccess { 
                    var newState = current.copy(coinCount = minOf(current.coinCount + count, 2))
                    if (alsoLike && !current.isLiked) newState = newState.copy(isLiked = true)
                    _uiState.value = newState
                    //  彩蛋：使用趣味消息（如果设置开启）
                    val message = if (appContext?.let { ctx -> com.android.purebilibili.core.store.SettingsManager.isEasterEggEnabledSync(ctx) } == true) {
                        com.android.purebilibili.core.util.EasterEggs.getCoinMessage()
                    } else {
                        "投币成功"
                    }
                    toast(message)
                }
                .onFailure { toast(it.message ?: "\u6295\u5e01\u5931\u8d25") }
        }
    }
    
    fun doTripleAction() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        viewModelScope.launch {
            toast("\u6b63\u5728\u4e09\u8fde...")
            interactionUseCase.doTripleAction(current.info.aid)
                .onSuccess { result ->
                    var newState = current
                    if (result.likeSuccess) newState = newState.copy(isLiked = true)
                    if (result.coinSuccess) newState = newState.copy(coinCount = 2)
                    if (result.favoriteSuccess) newState = newState.copy(isFavorited = true)
                    _uiState.value = newState
                    if (result.allSuccess) _tripleCelebrationVisible.value = true
                    toast(result.toSummaryMessage())
                }
                .onFailure { toast(it.message ?: "\u4e09\u8fde\u5931\u8d25") }
        }
    }
    
    fun dismissLikeBurst() { _likeBurstVisible.value = false }
    fun dismissTripleCelebration() { _tripleCelebrationVisible.value = false }
    
    // ========== Download ==========
    
    //  下载对话框状态
    private val _showDownloadDialog = MutableStateFlow(false)
    val showDownloadDialog = _showDownloadDialog.asStateFlow()
    
    fun openDownloadDialog() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        _showDownloadDialog.value = true
    }
    
    fun closeDownloadDialog() {
        _showDownloadDialog.value = false
    }
    
    fun downloadWithQuality(qualityId: Int, customPath: String? = null) {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        _showDownloadDialog.value = false
        
        viewModelScope.launch {
            // 如果选择的画质不同，需要获取对应画质的 URL
            val videoUrl: String
            val audioUrl: String?
            val qualityDesc: String
            
            if (qualityId == current.currentQuality) {
                // 使用当前画质
                videoUrl = current.playUrl
                audioUrl = current.audioUrl
                qualityDesc = current.qualityLabels.getOrNull(
                    current.qualityIds.indexOf(qualityId)
                ) ?: "${qualityId}P"
            } else {
                // 从缓存或 API 获取指定画质的 URL
                val dashVideo = current.cachedDashVideos.find { it.id == qualityId }
                val dashAudio = current.cachedDashAudios.firstOrNull()
                
                if (dashVideo != null) {
                    videoUrl = dashVideo.getValidUrl() ?: current.playUrl
                    audioUrl = dashAudio?.getValidUrl() ?: current.audioUrl
                    qualityDesc = current.qualityLabels.getOrNull(
                        current.qualityIds.indexOf(qualityId)
                    ) ?: "${qualityId}P"
                } else {
                    // 使用当前画质
                    videoUrl = current.playUrl
                    audioUrl = current.audioUrl
                    qualityDesc = current.qualityLabels.getOrNull(
                        current.qualityIds.indexOf(current.currentQuality)
                    ) ?: "${current.currentQuality}P"
                }
            }
            
            // 创建下载任务
            val task = com.android.purebilibili.feature.download.DownloadTask(
                bvid = currentBvid,
                cid = currentCid,
                title = current.info.title,
                cover = current.info.pic,
                ownerName = current.info.owner.name,
                ownerFace = current.info.owner.face,
                duration = 0,
                quality = qualityId,
                qualityDesc = qualityDesc,
                videoUrl = videoUrl,
                audioUrl = audioUrl ?: "",
                customSaveDir = customPath // 📂 [新增] 支持指定下载目录
            )
            
            val added = com.android.purebilibili.feature.download.DownloadManager.addTask(task)
            if (added) {
                val pathMsg = if (customPath != null) "至 $customPath" else ""
                toast("开始下载: ${current.info.title} [$qualityDesc] $pathMsg")
                // 开始监听下载进度
                com.android.purebilibili.feature.download.DownloadManager.tasks.collect { tasks ->
                    val downloadTask = tasks[task.id]
                    _downloadProgress.value = downloadTask?.progress ?: -1f
                }
            } else {
                toast("下载任务已存在")
            }
        }
    }
    
    // ========== Quality ==========
    
    fun changeQuality(qualityId: Int, currentPos: Long) {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        if (current.isQualitySwitching) { toast("正在切换中..."); return }
        if (current.currentQuality == qualityId) { toast("已是当前清晰度"); return }
        
        //  [新增] 权限检查
        val permissionResult = qualityManager.checkQualityPermission(
            qualityId, current.isLoggedIn, current.isVip
        )
        
        when (permissionResult) {
            is QualityPermissionResult.RequiresVip -> {
                toast("${permissionResult.qualityLabel} 需要大会员")
                // 自动降级到最高可用画质
                val fallbackQuality = qualityManager.getMaxAvailableQuality(
                    current.qualityIds, current.isLoggedIn, current.isVip
                )
                if (fallbackQuality != current.currentQuality) {
                    changeQuality(fallbackQuality, currentPos)
                }
                return
            }
            is QualityPermissionResult.RequiresLogin -> {
                toast("${permissionResult.qualityLabel} 需要登录")
                return
            }
            is QualityPermissionResult.Permitted -> {
                // 继续切换
            }
        }
        
        _uiState.value = current.copy(isQualitySwitching = true, requestedQuality = qualityId)
        
        viewModelScope.launch {
            // [新增] 获取当前音频偏好
            val audioPref = appContext?.let { 
                com.android.purebilibili.core.store.SettingsManager.getAudioQualitySync(it) 
            } ?: -1
            
            val result = playbackUseCase.changeQualityFromCache(qualityId, current.cachedDashVideos, current.cachedDashAudios, currentPos, audioPref)
                ?: playbackUseCase.changeQualityFromApi(currentBvid, currentCid, qualityId, currentPos, audioPref)
            
            if (result != null) {
                _uiState.value = current.copy(
                    playUrl = result.videoUrl, audioUrl = result.audioUrl,
                    currentQuality = result.actualQuality, isQualitySwitching = false, requestedQuality = null,
                    //  [修复] 更新缓存的DASH流，否则后续画质切换可能失败
                    cachedDashVideos = result.cachedDashVideos.ifEmpty { current.cachedDashVideos },
                    cachedDashAudios = result.cachedDashAudios.ifEmpty { current.cachedDashAudios }
                )
                val label = current.qualityLabels.getOrNull(current.qualityIds.indexOf(result.actualQuality)) ?: "${result.actualQuality}"
                toast(if (result.wasFallback) " 已切换至 $label" else "✓ 已切换至 $label")
                //  记录画质切换事件
                AnalyticsHelper.logQualityChange(currentBvid, current.currentQuality, result.actualQuality)
            } else {
                _uiState.value = current.copy(isQualitySwitching = false, requestedQuality = null)
                toast("清晰度切换失败")
            }
        }
    }
    
    // ========== Page Switch ==========
    
    fun switchPage(pageIndex: Int) {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        val page = current.info.pages.getOrNull(pageIndex) ?: return
        if (page.cid == currentCid) { toast("\u5df2\u662f\u5f53\u524d\u5206P"); return }
        
        currentCid = page.cid
        _uiState.value = current.copy(isQualitySwitching = true)
        
        viewModelScope.launch {
            try {
                val playUrlData = VideoRepository.getPlayUrlData(currentBvid, page.cid, current.currentQuality)
                if (playUrlData != null) {
                    //  [新增] 获取音频/视频偏好
                    val videoCodecPreference = appContext?.let { 
                        com.android.purebilibili.core.store.SettingsManager.getVideoCodecSync(it) 
                    } ?: "hev1"
                    val audioQualityPreference = appContext?.let { 
                        com.android.purebilibili.core.store.SettingsManager.getAudioQualitySync(it) 
                    } ?: -1
                    
                    val isHevcSupported = com.android.purebilibili.core.util.MediaUtils.isHevcSupported()
                    val isAv1Supported = com.android.purebilibili.core.util.MediaUtils.isAv1Supported()
                    
                    val dashVideo = playUrlData.dash?.getBestVideo(
                        current.currentQuality,
                        preferCodec = videoCodecPreference,
                        isHevcSupported = isHevcSupported,
                        isAv1Supported = isAv1Supported
                    )
                    
                    val dashAudio = playUrlData.dash?.getBestAudio(audioQualityPreference)
                    
                    val videoUrl = dashVideo?.getValidUrl() ?: playUrlData.durl?.firstOrNull()?.url ?: ""
                    val audioUrl = dashAudio?.getValidUrl()
                    
                    if (videoUrl.isNotEmpty()) {
                        if (dashVideo != null) playbackUseCase.playDashVideo(videoUrl, audioUrl, 0L)
                        else playbackUseCase.playVideo(videoUrl, 0L)
                        
                        _uiState.value = current.copy(
                            info = current.info.copy(cid = page.cid), playUrl = videoUrl, audioUrl = audioUrl,
                            startPosition = 0L, isQualitySwitching = false,
                            cachedDashVideos = playUrlData.dash?.video ?: emptyList(),
                            cachedDashAudios = playUrlData.dash?.audio ?: emptyList()
                        )
                        toast("\u5df2\u5207\u6362\u81f3 P${pageIndex + 1}")
                        return@launch
                    }
                }
                _uiState.value = current.copy(isQualitySwitching = false)
                toast("\u5206P\u5207\u6362\u5931\u8d25")
            } catch (e: Exception) {
                _uiState.value = current.copy(isQualitySwitching = false)
            }
        }
    }
    
    // ==========  Plugin System (SponsorBlock等) ==========
    
    /**
     * 定期检查插件（约500ms一次）
     */
    private fun startPluginCheck() {
        pluginCheckJob?.cancel()
        pluginCheckJob = viewModelScope.launch {
            while (true) {
                delay(500)  // 每500ms检查一次
                val plugins = PluginManager.getEnabledPlayerPlugins()
                if (plugins.isEmpty()) continue
                
                val currentPos = playbackUseCase.getCurrentPosition()
                
                for (plugin in plugins) {
                    try {
                        when (val action = plugin.onPositionUpdate(currentPos)) {
                            is SkipAction.SkipTo -> {
                                playbackUseCase.seekTo(action.positionMs)
                                toast(action.reason)
                                Logger.d("PlayerVM", " Plugin ${plugin.name} skipped to ${action.positionMs}ms")
                            }
                            else -> {}
                        }
                    } catch (e: Exception) {
                        Logger.e("PlayerVM", "Plugin ${plugin.name} onPositionUpdate failed", e)
                    }
                }
            }
        }
    }
    
    fun dismissSponsorSkipButton() { _showSkipButton.value = false }
    
    // ========== Playback Control ==========
    
    fun seekTo(pos: Long) { playbackUseCase.seekTo(pos) }
    fun getPlayerCurrentPosition() = playbackUseCase.getCurrentPosition()
    fun getPlayerDuration() = playbackUseCase.getDuration()
    fun saveCurrentPosition() { playbackUseCase.savePosition(currentBvid) }
    
    fun restoreFromCache(cachedState: PlayerUiState.Success, startPosition: Long = -1L) {
        currentBvid = cachedState.info.bvid
        currentCid = cachedState.info.cid
        _uiState.value = if (startPosition >= 0) cachedState.copy(startPosition = startPosition) else cachedState
    }
    
    // ========== Private ==========
    
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            // [修复] 立即上报一次心跳，确保进入历史记录
            // 短时间观看也应该被记录
            if (currentBvid.isNotEmpty() && currentCid > 0) {
                try { 
                    VideoRepository.reportPlayHeartbeat(currentBvid, currentCid, 0)
                    Logger.d("PlayerVM", " Initial heartbeat reported for $currentBvid")
                }
                catch (e: Exception) {
                    Logger.d("PlayerVM", " Initial heartbeat failed: ${e.message}")
                }
            }
            
            // 之后每30秒上报一次
            while (true) {
                delay(30_000)
                if (exoPlayer?.isPlaying == true && currentBvid.isNotEmpty() && currentCid > 0) {
                    try { VideoRepository.reportPlayHeartbeat(currentBvid, currentCid, playbackUseCase.getCurrentPosition() / 1000) }
                    catch (_: Exception) {}
                }
            }
        }
    }
    
    fun toast(msg: String) { viewModelScope.launch { _toastEvent.send(msg) } }
    
    override fun onCleared() {
        super.onCleared()
        heartbeatJob?.cancel()
        pluginCheckJob?.cancel()
        onlineCountJob?.cancel()  // 👀 取消在线人数轮询
        
        //  通知插件系统：视频结束
        PluginManager.getEnabledPlayerPlugins().forEach { plugin ->
            try {
                plugin.onVideoEnd()
            } catch (e: Exception) {
                Logger.e("PlayerVM", "Plugin ${plugin.name} onVideoEnd failed", e)
            }
        }
        
        exoPlayer = null
    }
}