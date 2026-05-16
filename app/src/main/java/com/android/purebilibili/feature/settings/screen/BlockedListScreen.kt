package com.android.purebilibili.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.data.repository.BilibiliBlockedListSyncRepository
import com.android.purebilibili.data.repository.BlockedUpRepository
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.components.IOSSectionTitle
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedListScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { BlockedUpRepository(context) }
    val syncRepository = remember { BilibiliBlockedListSyncRepository(repository) }
    val blockedUps by repository.getAllBlockedUps().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var syncingBlockedList by remember { mutableStateOf(false) }
    var blockedListSyncMessage by remember { mutableStateOf<String?>(null) }

    AdaptiveScaffold(
        topBar = {
            AdaptiveTopAppBar(
                title = "黑名单管理",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSurfaceTokens.cardContainer(),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = AppSurfaceTokens.groupedListContainer()
    ) { padding ->
        BlockedListContent(
            blockedUps = blockedUps,
            syncingBlockedList = syncingBlockedList,
            blockedListSyncMessage = blockedListSyncMessage,
            onSyncBlockedList = {
                if (!syncingBlockedList) {
                    scope.launch {
                        syncingBlockedList = true
                        blockedListSyncMessage = "正在同步 B站黑名单..."
                        val result = syncRepository.importFromBilibili()
                        blockedListSyncMessage = result.fold(
                            onSuccess = { it.message },
                            onFailure = { it.message ?: "同步 B站黑名单失败" }
                        )
                        syncingBlockedList = false
                    }
                }
            },
            onUnblock = { mid ->
                scope.launch {
                    blockedListSyncMessage = repository.unblockUpWithBilibiliSync(mid).message
                }
            },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun BlockedListContent(
    blockedUps: List<com.android.purebilibili.core.database.entity.BlockedUp>,
    syncingBlockedList: Boolean = false,
    blockedListSyncMessage: String? = null,
    onSyncBlockedList: (() -> Unit)? = null,
    onUnblock: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (blockedUps.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "暂无屏蔽的 UP 主",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onSyncBlockedList != null) {
                Spacer(modifier = Modifier.height(16.dp))
                BlockedListSyncAction(
                    syncing = syncingBlockedList,
                    message = blockedListSyncMessage,
                    onSync = onSyncBlockedList,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (onSyncBlockedList != null) {
                    BlockedListSyncAction(
                        syncing = syncingBlockedList,
                        message = blockedListSyncMessage,
                        onSync = onSyncBlockedList
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                IOSSectionTitle("已屏蔽的 UP 主")
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(blockedUps, key = { it.mid }) { up ->
                BlockedUpItem(
                    name = up.name,
                    face = up.face,
                    onUnblock = { onUnblock(up.mid) }
                )
            }
        }
    }
}

@Composable
private fun BlockedListSyncAction(
    syncing: Boolean,
    message: String?,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(AppSurfaceTokens.cardContainer())
            .padding(12.dp)
    ) {
        Button(
            onClick = onSync,
            enabled = !syncing,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            if (syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (syncing) "同步中" else "同步 B站黑名单")
        }
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BlockedUpItem(
    name: String,
    face: String,
    onUnblock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(AppSurfaceTokens.cardContainer())
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = face,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.2f)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        TextButton(onClick = onUnblock) {
            Text("解除屏蔽", color = Color.Red)
        }
    }
}
