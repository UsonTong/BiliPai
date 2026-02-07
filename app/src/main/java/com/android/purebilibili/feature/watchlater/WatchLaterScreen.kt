// 文件路径: feature/watchlater/WatchLaterScreen.kt
package com.android.purebilibili.feature.watchlater

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import com.android.purebilibili.core.ui.animation.DissolvableVideoCard
import com.android.purebilibili.core.ui.animation.jiggleOnDissolve
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import com.android.purebilibili.core.ui.blur.unifiedBlur
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 辅助函数：格式化时长
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}

// 辅助函数：格式化数字
private fun formatNumber(num: Int): String {
    return when {
        num >= 10000 -> String.format("%.1f万", num / 10000f)
        else -> num.toString()
    }
}

// 辅助函数：修复封面 URL 协议（B站API可能返回http或缺少协议的URL）
private fun fixCoverUrl(url: String?): String {
    if (url.isNullOrEmpty()) return ""
    return when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> url.replaceFirst("http://", "https://")
        else -> url
    }
}

/**
 * 稍后再看 UI 状态
 */
data class WatchLaterUiState(
    val items: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val dissolvingIds: Set<String> = emptySet() // [新增] 用于已播放 Thanos Snap 动画的卡片
)

/**
 * 稍后再看 ViewModel
 */
class WatchLaterViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(WatchLaterUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val api = NetworkModule.api
                val response = api.getWatchLaterList()
                if (response.code == 0 && response.data != null) {
                    val items = response.data.list?.map { item ->
                        VideoItem(
                            id = item.aid,  // 存储 aid 用于删除
                            bvid = item.bvid ?: "",
                            title = item.title ?: "",
                            pic = item.pic ?: "",
                            duration = item.duration ?: 0,
                            owner = Owner(
                                mid = item.owner?.mid ?: 0L,
                                name = item.owner?.name ?: "",
                                face = item.owner?.face ?: ""
                            ),
                            stat = Stat(
                                view = item.stat?.view ?: 0,
                                danmaku = item.stat?.danmaku ?: 0,
                                reply = item.stat?.reply ?: 0,
                                like = item.stat?.like ?: 0,
                                coin = item.stat?.coin ?: 0,
                                favorite = item.stat?.favorite ?: 0,
                                share = item.stat?.share ?: 0
                            ),
                            pubdate = item.pubdate ?: 0L
                        )
                    } ?: emptyList()
                    _uiState.value = _uiState.value.copy(isLoading = false, items = items)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = response.message ?: "加载失败")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }
    
    // [新增] 开始消散动画
    fun startVideoDissolve(bvid: String) {
        _uiState.value = _uiState.value.copy(
            dissolvingIds = _uiState.value.dissolvingIds + bvid
        )
    }

    // [新增] 动画完成，执行删除
    fun completeVideoDissolve(bvid: String) {
        // 先从 UI 状态移除 ID（动画结束），然后调用删除逻辑
        _uiState.value = _uiState.value.copy(
            dissolvingIds = _uiState.value.dissolvingIds - bvid
        )
        // 查找对应的 aid 进行删除
        val item = _uiState.value.items.find { it.bvid == bvid }
        item?.let { deleteItem(it.id) }
    }

    /**
     * 从稀后再看删除视频
     */
    fun deleteItem(aid: Long) {
        // 乐观更新：直接从列表中移除，不需要重新请求
        val currentList = _uiState.value.items
        val newList = currentList.filter { it.id != aid }
        _uiState.value = _uiState.value.copy(items = newList)

        viewModelScope.launch {
            try {
                val api = NetworkModule.api
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache ?: ""
                if (csrf.isEmpty()) {
                    android.widget.Toast.makeText(getApplication(), "请先登录", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val response = api.deleteFromWatchLater(aid = aid, csrf = csrf)
                if (response.code == 0) {
                    // 从列表中移除该项
                    val currentItems = _uiState.value.items
                    _uiState.value = _uiState.value.copy(
                        items = currentItems.filter { 
                            // VideoItem 没有 aid 字段，需要通过其他方式匹配
                            // 由于删除是通过 aid 的，这里我们重新加载数据
                            true
                        }
                    )
                    // 重新加载数据以确保一致性
                    // loadData()
                    android.widget.Toast.makeText(getApplication(), "已从稍后再看移除", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(getApplication(), "移除失败: ${response.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(getApplication(), "移除失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 *  稍后再看页面
 */

// ... (existing imports)

/**
 *  稍后再看页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchLaterScreen(
    onBack: () -> Unit,
    onVideoClick: (String, Long) -> Unit,
    viewModel: WatchLaterViewModel = viewModel(),
    globalHazeState: HazeState? = null // [新增]
) {
    val state by viewModel.uiState.collectAsState()
    val hazeState = remember { HazeState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // 使用 Box 包裹实现毛玻璃背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .unifiedBlur(hazeState)
            ) {
                TopAppBar(
                    title = { Text("稍后再看", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                        }
                    },
                    actions = {
                        // 🎵 [新增] 全部播放按钮
                        if (state.items.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    // 设置外部播放列表
                                    val playlistItems = state.items.map { video ->
                                        com.android.purebilibili.feature.video.player.PlaylistItem(
                                            bvid = video.bvid,
                                            title = video.title,
                                            cover = video.pic,
                                            owner = video.owner?.name ?: "",
                                            duration = video.duration.toLong()
                                        )
                                    }
                                    com.android.purebilibili.feature.video.player.PlaylistManager.setExternalPlaylist(playlistItems, 0)
                                    // 导航到第一个视频
                                    onVideoClick(state.items.first().bvid, 0L)
                                }
                            ) {
                                Icon(
                                    CupertinoIcons.Filled.Play,
                                    contentDescription = "全部播放",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior
                )
                
                // 分割线 (仅在滚动时显示? 这里简化一直显示细线或跟随滚动)
                // 暂时不加显式分割线，依靠毛玻璃效果
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState) // 内容作为模糊源
                .then(if (globalHazeState != null) Modifier.hazeSource(globalHazeState) else Modifier) // [新增]
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error ?: "未知错误",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.loadData() }) {
                            Text("重试")
                        }
                    }
                }
                state.items.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            CupertinoIcons.Default.Clock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "稍后再看列表为空",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // 计算合适的列数
                    val windowSizeClass = com.android.purebilibili.core.util.LocalWindowSizeClass.current
                    val minColWidth = if (windowSizeClass.isExpandedScreen) 240.dp else 170.dp
                    
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minColWidth), // 使用 Adaptive 自适应列宽
                        contentPadding = PaddingValues(
                            start = 8.dp, 
                            end = 8.dp, 
                            top = padding.calculateTopPadding() + 8.dp, 
                            bottom = padding.calculateBottomPadding() + 8.dp + 80.dp // [新增] 底部Padding
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {

                        itemsIndexed(
                            items = state.items,
                            key = { _, item -> item.bvid } 
                        ) { index, item ->
                            val isDissolving = item.bvid in state.dissolvingIds
                            
                            DissolvableVideoCard(
                                isDissolving = isDissolving,
                                onDissolveComplete = { viewModel.completeVideoDissolve(item.bvid) },
                                cardId = item.bvid,
                                modifier = Modifier.jiggleOnDissolve(item.bvid)
                            ) {
                                ElegantVideoCard(
                                    video = item,
                                    index = index,
                                    animationEnabled = true, // 保留首页卡片动画
                                    transitionEnabled = true, // 共享元素过渡
                                    showPublishTime = true,
                                    // 触发 Thanos 响指动画 (开始消散)
                                    onDismiss = { viewModel.startVideoDissolve(item.bvid) },  
                                    onClick = { bvid, _ -> onVideoClick(bvid, 0L) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchLaterVideoCard(
    item: VideoItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = fixCoverUrl(item.pic),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // 时长
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(item.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
        
        // 信息
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = item.owner?.name ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${formatNumber(item.stat?.view ?: 0)}播放",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
