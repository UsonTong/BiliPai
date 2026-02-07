package com.android.purebilibili.feature.video.ui.pager

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.feature.video.ui.overlay.PortraitFullscreenOverlay
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
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
    onProgressUpdate: (Long) -> Unit = {},
    onUserClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    var isLoading by remember { mutableStateOf(false) }

    // [逻辑] 切换视频源
    LaunchedEffect(pagerState.currentPage) {
        val item = pageItems.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        
        // 提取信息
        val bvid = if (item is ViewInfo) item.bvid else (item as RelatedVideo).bvid
        val aid = if (item is ViewInfo) item.aid else (item as RelatedVideo).aid.toLong()
        
        // 通知外部
        onVideoChange(bvid)
        
        // 如果已经加载过这个视频，就不重新加载 (避免重复请求)
        if (currentPlayingBvid == bvid) return@LaunchedEffect

        // 停止上一个播放
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        isLoading = true
        currentPlayingBvid = bvid

        // 加载新视频
        launch {
            try {
                val result = com.android.purebilibili.data.repository.VideoRepository.getVideoDetails(
                    bvid = bvid,
                    aid = aid,
                    targetQuality = 64
                )
                
                result.fold(
                    onSuccess = { (_, playData) ->
                        val videoUrl = playData.dash?.video?.firstOrNull()?.baseUrl 
                            ?: playData.durl?.firstOrNull()?.url
                        val audioUrl = playData.dash?.audio?.firstOrNull()?.baseUrl

                        if (!videoUrl.isNullOrEmpty()) {
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

                            // [修复] 再次检查是否仍然是当前应该播放的视频，防止快速滑动时的竞态条件
                            if (currentPlayingBvid == bvid) {
                                exoPlayer.setMediaSource(finalSource)
                                exoPlayer.prepare()
                                
                                // 如果是初始视频且有进度
                                if (pagerState.currentPage == 0 && initialStartPositionMs > 0) {
                                    exoPlayer.seekTo(initialStartPositionMs)
                                }
                                
                                    exoPlayer.play()
                            } else {
                                com.android.purebilibili.core.util.Logger.d("PortraitVideoPager", "Discarded video load for $bvid as current is $currentPlayingBvid")
                            }
                        }
                        
                        // [修复] 只有当前视频加载完成，才取消 Loading
                        if (currentPlayingBvid == bvid) {
                            isLoading = false
                        }
                    },
                    onFailure = {
                        if (currentPlayingBvid == bvid) {
                            isLoading = false
                        }
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                if (currentPlayingBvid == bvid) {
                    isLoading = false
                }
            }
        }
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
                onUserClick = onUserClick
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
    onUserClick: (Long) -> Unit
) {
    val context = LocalContext.current
    
    // [修复] 手动监听 ExoPlayer 播放状态，确保 UI 及时更新
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying_: Boolean) {
                isPlaying = isPlaying_
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
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
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
            
            isFollowing = (currentUiState as? PlayerUiState.Success)?.followingMids?.contains(authorMid) == true,
            onFollowClick = { 
                viewModel.toggleFollow(authorMid, (currentUiState as? PlayerUiState.Success)?.followingMids?.contains(authorMid) == true)
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
            danmakuEnabled = true,
            isStatusBarHidden = true,
            
            onBack = onBack,
            onPlayPause = {
                if (isCurrentPage) {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
            },
            onSeek = { pos -> if (isCurrentPage) exoPlayer.seekTo(pos) },
            onSeekStart = { },
            onSpeedClick = { },
            onQualityClick = { },
            onRatioClick = { },
            onDanmakuToggle = { },
            onDanmakuInputClick = { 
                Toast.makeText(context, "暂不可用，后续更新", Toast.LENGTH_SHORT).show() 
            },
            onToggleStatusBar = { },
            
            showControls = isOverlayVisible && !showCommentSheet && !showDetailSheet
        )

        PortraitCommentSheet(
            visible = showCommentSheet,
            onDismiss = { showCommentSheet = false },
            commentViewModel = commentViewModel,
            aid = aid,
            onUserClick = onUserClick
        )
        
        PortraitDetailSheet(
            visible = showDetailSheet,
            onDismiss = { showDetailSheet = false },
            info = currentSuccess?.info 
        )
    }
}

