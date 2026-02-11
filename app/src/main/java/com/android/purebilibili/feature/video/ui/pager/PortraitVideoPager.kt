package com.android.purebilibili.feature.video.ui.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.danmaku.DanmakuManager
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.feature.video.ui.overlay.PortraitFullscreenOverlay
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 竖屏无缝滑动播放页面 (TikTok Style)
 * 
 * @param initialBvid 初始视频 BVID
 * @param initialInfo 初始视频详情
 * @param recommendations 推荐视频列表
 * @param onBack 返回回调
 * @param onVideoChange 切换视频回调 (当滑动到新视频时通知外部)
 */
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel

@UnstableApi
@Composable
fun PortraitVideoPager(
    initialBvid: String,
    initialInfo: ViewInfo,
    recommendations: List<RelatedVideo>,
    onBack: () -> Unit,
    onVideoChange: (String) -> Unit,
    viewModel: PlayerViewModel,
    commentViewModel: VideoCommentViewModel,
    initialStartPositionMs: Long = 0L,
    onProgressUpdate: (String, Long) -> Unit = { _, _ -> },
    onExitSnapshot: (String, Long) -> Unit = { _, _ -> },
    onSearchClick: () -> Unit = {},
    onUserClick: (Long) -> Unit,
    onRotateToLandscape: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val danmakuManager = rememberDanmakuManager()
    val danmakuEnabled by SettingsManager
        .getDanmakuEnabled(context)
        .collectAsState(initial = true)
    val danmakuOpacity by SettingsManager
        .getDanmakuOpacity(context)
        .collectAsState(initial = 0.85f)
    val danmakuFontScale by SettingsManager
        .getDanmakuFontScale(context)
        .collectAsState(initial = 1.0f)
    val danmakuSpeed by SettingsManager
        .getDanmakuSpeed(context)
        .collectAsState(initial = 1.0f)
    val danmakuDisplayArea by SettingsManager
        .getDanmakuArea(context)
        .collectAsState(initial = 0.5f)
    val danmakuMergeDuplicates by SettingsManager
        .getDanmakuMergeDuplicates(context)
        .collectAsState(initial = true)

    // 构造页面列表：第一个是当前视频，后续是推荐视频
    // 构造页面列表：第一个是当前视频，后续是推荐视频
    // [修复] 使用 remember { } 而不是 remember(key) 来避免因 ViewModel 更新导致的列表重建和死循环
    // 列表只会在进入时构建一次，后续的 viewModel.loadVideo 更新不会影响列表结构
    val pageItems = remember {
        val list = mutableListOf<Any>()
        list.add(initialInfo)
        list.addAll(recommendations)
        list
    }
    
    val pagerState = rememberPagerState(pageCount = { pageItems.size })

    // [核心] 单一播放器实例
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 1.0f
            }
    }

    // 释放播放器
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // [状态] 当前播放的视频 URL
    var currentPlayingBvid by remember { mutableStateOf<String?>(null) }
    var currentPlayingCid by remember { mutableStateOf(0L) }
    var currentPlayingAid by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var lastCommittedPage by remember { mutableIntStateOf(-1) }
    var activeLoadGeneration by remember { mutableIntStateOf(0) }
    var hasConsumedInitialSeek by remember { mutableStateOf(false) }
    var pendingAutoPlayGeneration by remember { mutableIntStateOf(-1) }

    DisposableEffect(exoPlayer) {
        danmakuManager.attachPlayer(exoPlayer)
        onDispose { }
    }

    DisposableEffect(exoPlayer, activeLoadGeneration) {
        val autoPlayListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY &&
                    pendingAutoPlayGeneration == activeLoadGeneration &&
                    !exoPlayer.isPlaying
                ) {
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && pendingAutoPlayGeneration == activeLoadGeneration) {
                    pendingAutoPlayGeneration = -1
                }
            }
        }
        exoPlayer.addListener(autoPlayListener)
        onDispose {
            exoPlayer.removeListener(autoPlayListener)
        }
    }

    // [核心] 仅在页面 settle 后切流，避免拖动过程频繁切换导致卡顿与竞态
    LaunchedEffect(pagerState, pageItems) {
        snapshotFlow {
            resolveCommittedPage(
                isScrollInProgress = pagerState.isScrollInProgress,
                currentPage = pagerState.currentPage,
                lastCommittedPage = lastCommittedPage
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { committedPage ->
                lastCommittedPage = committedPage
                val item = pageItems.getOrNull(committedPage) ?: return@collect

                val bvid = if (item is ViewInfo) item.bvid else (item as RelatedVideo).bvid
                val aid = if (item is ViewInfo) item.aid else (item as RelatedVideo).aid.toLong()

                onVideoChange(bvid)

                if (currentPlayingBvid == bvid) {
                    isLoading = false
                    return@collect
                }

                activeLoadGeneration += 1
                val requestGeneration = activeLoadGeneration

                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                danmakuManager.clear()
                isLoading = true
                currentPlayingBvid = bvid
                currentPlayingCid = 0L
                currentPlayingAid = aid
                pendingAutoPlayGeneration = requestGeneration

                launch {
                    try {
                        val result = com.android.purebilibili.data.repository.VideoRepository.getVideoDetails(
                            bvid = bvid,
                            aid = aid,
                            targetQuality = 64
                        )

                        result.fold(
                            onSuccess = { (info, playData) ->
                                val videoUrl = playData.dash?.video?.firstOrNull()?.baseUrl
                                    ?: playData.durl?.firstOrNull()?.url
                                val audioUrl = playData.dash?.audio?.firstOrNull()?.baseUrl

                                if (videoUrl.isNullOrEmpty()) {
                                    pendingAutoPlayGeneration = -1
                                    if (shouldApplyLoadResult(
                                            requestGeneration = requestGeneration,
                                            activeGeneration = activeLoadGeneration,
                                            expectedBvid = bvid,
                                            currentPlayingBvid = currentPlayingBvid
                                        )
                                    ) {
                                        isLoading = false
                                    }
                                    return@fold
                                }

                                val headers = mapOf(
                                    "Referer" to "https://www.bilibili.com",
                                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                )
                                val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                                    .setUserAgent(headers["User-Agent"])
                                    .setDefaultRequestProperties(headers)
                                val mediaSourceFactory = DefaultMediaSourceFactory(context)
                                    .setDataSourceFactory(dataSourceFactory)

                                val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUrl))
                                val finalSource = if (!audioUrl.isNullOrEmpty()) {
                                    val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
                                    MergingMediaSource(videoSource, audioSource)
                                } else {
                                    videoSource
                                }

                                if (!shouldApplyLoadResult(
                                        requestGeneration = requestGeneration,
                                        activeGeneration = activeLoadGeneration,
                                        expectedBvid = bvid,
                                        currentPlayingBvid = currentPlayingBvid
                                    )
                                ) {
                                    com.android.purebilibili.core.util.Logger.d(
                                        "PortraitVideoPager",
                                        "Discarded stale video load for $bvid (request=$requestGeneration, active=$activeLoadGeneration, current=$currentPlayingBvid)"
                                    )
                                    return@fold
                                }

                                currentPlayingCid = info.cid
                                currentPlayingAid = info.aid
                                exoPlayer.playWhenReady = true
                                exoPlayer.setMediaSource(finalSource)
                                exoPlayer.prepare()

                                if (committedPage == 0 && initialStartPositionMs > 0 && !hasConsumedInitialSeek) {
                                    exoPlayer.seekTo(initialStartPositionMs)
                                    hasConsumedInitialSeek = true
                                }

                                exoPlayer.play()
                                isLoading = false
                            },
                            onFailure = {
                                pendingAutoPlayGeneration = -1
                                if (shouldApplyLoadResult(
                                        requestGeneration = requestGeneration,
                                        activeGeneration = activeLoadGeneration,
                                        expectedBvid = bvid,
                                        currentPlayingBvid = currentPlayingBvid
                                    )
                                ) {
                                    isLoading = false
                                }
                            }
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        pendingAutoPlayGeneration = -1
                        if (shouldApplyLoadResult(
                                requestGeneration = requestGeneration,
                                activeGeneration = activeLoadGeneration,
                                expectedBvid = bvid,
                                currentPlayingBvid = currentPlayingBvid
                            )
                        ) {
                            isLoading = false
                        }
                    }
                }
            }
    }

    LaunchedEffect(currentPlayingCid, currentPlayingAid, danmakuEnabled, exoPlayer) {
        if (currentPlayingCid > 0 && danmakuEnabled) {
            danmakuManager.isEnabled = true
            var durationMs = exoPlayer.duration
            var retries = 0
            while (durationMs <= 0 && retries < 50) {
                delay(100)
                durationMs = exoPlayer.duration
                retries++
            }
            danmakuManager.loadDanmaku(currentPlayingCid, currentPlayingAid, durationMs.coerceAtLeast(0L))
        } else {
            danmakuManager.isEnabled = false
        }
    }

    LaunchedEffect(danmakuOpacity, danmakuFontScale, danmakuSpeed, danmakuDisplayArea, danmakuMergeDuplicates) {
        danmakuManager.updateSettings(
            opacity = danmakuOpacity,
            fontScale = danmakuFontScale,
            speed = danmakuSpeed,
            displayArea = danmakuDisplayArea,
            mergeDuplicates = danmakuMergeDuplicates
        )
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val item = pageItems.getOrNull(page)
        
        if (item != null) {
            VideoPageItem(
                item = item,
                isCurrentPage = page == pagerState.currentPage,
                onBack = onBack,
                viewModel = viewModel,
                commentViewModel = commentViewModel,
                exoPlayer = exoPlayer, // [核心] 传递共享播放器
                currentPlayingBvid = currentPlayingBvid, // [修复] 传递当前播放的 BVID 用于校验
                isLoading = if (page == pagerState.currentPage) isLoading else false, // 只有当前页显示 Loading
                danmakuManager = danmakuManager,
                danmakuEnabled = danmakuEnabled,
                onExitSnapshot = onExitSnapshot,
                onSearchClick = onSearchClick,
                onUserClick = onUserClick,
                onRotateToLandscape = onRotateToLandscape,
                onProgressUpdate = onProgressUpdate
            )
        }
    }
}

@UnstableApi
@Composable
private fun VideoPageItem(
    item: Any,
    isCurrentPage: Boolean,
    onBack: () -> Unit,
    viewModel: PlayerViewModel,
    commentViewModel: VideoCommentViewModel,
    exoPlayer: ExoPlayer,
    currentPlayingBvid: String?, // [新增]
    isLoading: Boolean,
    danmakuManager: DanmakuManager,
    danmakuEnabled: Boolean,
    onExitSnapshot: (String, Long) -> Unit,
    onSearchClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    onRotateToLandscape: () -> Unit,
    onProgressUpdate: (String, Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // [修复] 手动监听 ExoPlayer 播放状态，确保 UI 及时更新
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentVideoAspect by remember {
        mutableFloatStateOf(
            exoPlayer.videoSize
                .takeIf { it.width > 0 && it.height > 0 }
                ?.let { it.width.toFloat() / it.height.toFloat() }
                ?: (16f / 9f)
        )
    }
    
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying_: Boolean) {
                isPlaying = isPlaying_
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    currentVideoAspect = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }
    
    // 提取信息
    val bvid = if (item is ViewInfo) item.bvid else (item as RelatedVideo).bvid
    val aid = if (item is ViewInfo) item.aid else (item as RelatedVideo).aid.toLong()
    // [逻辑] 只有当播放器正在播放当前视频时，才显示 PlayerView
    val isPlayerReadyForThisVideo = bvid == currentPlayingBvid
    val title = if (item is ViewInfo) item.title else (item as RelatedVideo).title
    val cover = if (item is ViewInfo) item.pic else (item as RelatedVideo).pic
    val authorName = if (item is ViewInfo) item.owner.name else (item as RelatedVideo).owner.name
    val authorFace = if (item is ViewInfo) item.owner.face else (item as RelatedVideo).owner.face
    val authorMid = if (item is ViewInfo) item.owner.mid else (item as RelatedVideo).owner.mid

    // 提取时长
    val initialDuration = if (item is RelatedVideo) {
        item.duration * 1000L
    } else if (item is ViewInfo) {
        (item.pages.firstOrNull()?.duration ?: 0L) * 1000L
    } else {
        0L
    }

    // 互动状态
    var showCommentSheet by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var isOverlayVisible by remember { mutableStateOf(true) }

    // 进度状态 (从播放器获取)
    var progressState by remember { mutableStateOf(PlayerProgress(0, initialDuration, 0)) }
    
    // 如果是当前页，监听播放器进度
    LaunchedEffect(isCurrentPage, exoPlayer) {
        if (isCurrentPage) {
            while (true) {
                if (exoPlayer.isPlaying) {
                    val realDuration = if (exoPlayer.duration > 0) exoPlayer.duration else initialDuration
                    progressState = PlayerProgress(
                        current = exoPlayer.currentPosition,
                        duration = realDuration,
                        buffered = exoPlayer.bufferedPosition
                    )
                    onProgressUpdate(bvid, exoPlayer.currentPosition)
                }
                delay(200)
            }
        }
    }
    
    // 手势调整进度状态
    var isSeekGesture by remember { mutableStateOf(false) }
    var seekStartPosition by remember { mutableFloatStateOf(0f) }
    var seekTargetPosition by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isOverlayVisible = !isOverlayVisible },
                    onDoubleTap = {
                        if (isCurrentPage) {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }
                    }
                )
            }
            // 进度调整手势
            .pointerInput(progressState.duration) {
                detectHorizontalDragGestures(
                    onDragStart = { 
                        if (isCurrentPage && progressState.duration > 0) {
                            isSeekGesture = true
                            seekStartPosition = exoPlayer.currentPosition.toFloat()
                            seekTargetPosition = seekStartPosition
                        }
                    },
                    onDragEnd = {
                        if (isCurrentPage && isSeekGesture) {
                            exoPlayer.seekTo(seekTargetPosition.toLong())
                            danmakuManager.seekTo(seekTargetPosition.toLong())
                            isSeekGesture = false
                        }
                    },
                    onDragCancel = { isSeekGesture = false },
                    onHorizontalDrag = { _, dragAmount ->
                        if (isCurrentPage && isSeekGesture && progressState.duration > 0) {
                            val seekDelta = (dragAmount / size.width) * progressState.duration * 0.75f
                            seekTargetPosition = (seekTargetPosition + seekDelta).coerceIn(0f, progressState.duration.toFloat())
                        }
                    }
                )
            }
    ) {
        // [核心逻辑]
        // 始终保留 AndroidView 以确保 Surface 准备就绪，但只有当播放器加载了当前视频时才将其绑定或显示
        // 否则显示封面
        
        if (isCurrentPage && isPlayerReadyForThisVideo) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val safeAspect = currentVideoAspect.coerceAtLeast(0.1f)
                val containerAspect = if (maxHeight.value > 0f) {
                    maxWidth.value / maxHeight.value
                } else {
                    safeAspect
                }
                val viewportHeight = if (safeAspect > containerAspect) {
                    maxWidth / safeAspect
                } else {
                    maxHeight
                }
                val viewportWidth = if (safeAspect > containerAspect) {
                    maxWidth
                } else {
                    maxHeight * safeAspect
                }

                Box(
                    modifier = Modifier
                        .size(
                            width = viewportWidth.coerceAtMost(maxWidth),
                            height = viewportHeight.coerceAtMost(maxHeight)
                        )
                        .align(Alignment.Center)
                ) {
                    key(currentPlayingBvid, bvid) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                    keepScreenOn = true
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                                }
                            },
                            update = { view ->
                                if (view.player != exoPlayer) {
                                    view.player = exoPlayer
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (danmakuEnabled) {
                            AndroidView(
                                factory = { ctx ->
                                    com.bytedance.danmaku.render.engine.DanmakuView(ctx).apply {
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        danmakuManager.attachView(this)
                                    }
                                },
                                update = { view ->
                                    if (view.width > 0 && view.height > 0) {
                                        danmakuManager.attachView(view)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // 封面图 (在加载中、未匹配到视频、或未开始播放时显示)
        val showCover = isLoading || !isCurrentPage || !isPlayerReadyForThisVideo || (isCurrentPage && !isPlaying && progressState.current == 0L)
        
        if (showCover) {
            AsyncImage(
                model = FormatUtils.fixImageUrl(cover),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black), // 避免透明底
                contentScale = ContentScale.Crop
            )
            
            if (isLoading && isCurrentPage) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
        }

        // 暂停图标 (仅当前页且暂停时显示)
        // [修复] 使用响应式的 isPlaying 状态
        val showPauseIcon = isCurrentPage && !isPlaying && !isLoading && !isSeekGesture
        if (showPauseIcon) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Pause",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
        
        // 滑动进度提示
        if (isSeekGesture && progressState.duration > 0) {
            val targetTimeText = FormatUtils.formatDuration(seekTargetPosition.toLong())
            val totalTimeText = FormatUtils.formatDuration(progressState.duration)
            val deltaMs = (seekTargetPosition - seekStartPosition).toLong()
            val deltaText = if (deltaMs >= 0) "+${FormatUtils.formatDuration(deltaMs)}" else "-${FormatUtils.formatDuration(-deltaMs)}"
            
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = "$targetTimeText / $totalTimeText",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = deltaText,
                    color = if (deltaMs >= 0) Color(0xFF66FF66) else Color(0xFFFF6666),
                    fontSize = 14.sp
                )
            }
        }

        // Overlay & Interaction
    val currentUiState = viewModel.uiState.collectAsState().value
    val isCurrentModelVideo = (currentUiState as? PlayerUiState.Success)?.info?.bvid == bvid
    val currentSuccess = currentUiState as? PlayerUiState.Success
    val stat = if (item is ViewInfo) item.stat else (item as RelatedVideo).stat
    val isFollowing = (currentUiState as? PlayerUiState.Success)?.followingMids?.contains(authorMid) == true

    LaunchedEffect(isCurrentPage, authorMid) {
        if (isCurrentPage && authorMid > 0L) {
            viewModel.ensureFollowStatus(authorMid)
        }
    }

    PortraitFullscreenOverlay(
            title = title,
            authorName = authorName,
            authorFace = authorFace,
            isPlaying = if (isCurrentPage) isPlaying else false,
            progress = progressState,
            
            statView = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.view else stat.view,
            statLike = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.like else stat.like,
            statDanmaku = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.danmaku else stat.danmaku,
            statReply = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.reply else stat.reply,
            statFavorite = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.favorite else stat.favorite,
            statShare = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.share else stat.share,
            
            isLiked = if(isCurrentModelVideo) currentSuccess?.isLiked == true else false,
            isCoined = false,
            isFavorited = if(isCurrentModelVideo) currentSuccess?.isFavorited == true else false,
            
            isFollowing = isFollowing,
            onFollowClick = { 
                viewModel.toggleFollow(authorMid, isFollowing)
            },
            
            onDetailClick = { showDetailSheet = true },
            onLikeClick = { if (isCurrentModelVideo) viewModel.toggleLike() },
            onCoinClick = { },
            onFavoriteClick = { if (isCurrentModelVideo) viewModel.showFavoriteFolderDialog() },
            onCommentClick = { showCommentSheet = true },
            onShareClick = {
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "Check out this video: https://www.bilibili.com/video/$bvid")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share too..."))
            },
            
            currentSpeed = 1.0f,
            currentQualityLabel = "高清",
            currentRatio = VideoAspectRatio.FIT,
            danmakuEnabled = danmakuEnabled,
            isStatusBarHidden = true,
            
            onBack = {
                onExitSnapshot(bvid, exoPlayer.currentPosition)
                onBack()
            },
            onPlayPause = {
                if (isCurrentPage) {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
            },
            onSeek = {
                if (isCurrentPage) {
                    exoPlayer.seekTo(it)
                    danmakuManager.seekTo(it)
                }
            },
            onSeekStart = { },
            onSpeedClick = { },
            onQualityClick = { },
            onRatioClick = { },
            onDanmakuToggle = {
                val next = !danmakuEnabled
                danmakuManager.isEnabled = next
                scope.launch {
                    SettingsManager.setDanmakuEnabled(context, next)
                }
            },
            onDanmakuInputClick = {
                if (isCurrentPage) {
                    viewModel.showDanmakuSendDialog()
                }
            },
            onToggleStatusBar = { },
            onSearchClick = {
                onExitSnapshot(bvid, exoPlayer.currentPosition)
                onSearchClick()
            },
            onMoreClick = {
                showDetailSheet = true
            },
            onRotateToLandscape = {
                onExitSnapshot(bvid, exoPlayer.currentPosition)
                onRotateToLandscape()
            },
            
            showControls = isOverlayVisible && !showCommentSheet && !showDetailSheet
        )

        PortraitCommentSheet(
            visible = showCommentSheet,
            onDismiss = { showCommentSheet = false },
            commentViewModel = commentViewModel,
            aid = aid,
            upMid = authorMid,
            onUserClick = onUserClick
        )
        
        PortraitDetailSheet(
            visible = showDetailSheet,
            onDismiss = { showDetailSheet = false },
            info = currentSuccess?.info 
        )
    }
}
