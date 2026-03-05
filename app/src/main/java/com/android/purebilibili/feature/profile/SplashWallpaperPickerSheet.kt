package com.android.purebilibili.feature.profile

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.store.SettingsManager
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*

/**
 * 🖼️ 开屏壁纸选择器 (用于设置页)
 * 仅用于选择开屏壁纸，简化的单一用途组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplashWallpaperPickerSheet(
    viewModel: ProfileViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val officialWallpapers by viewModel.officialWallpapers.collectAsState()
    val isLoading by viewModel.officialWallpapersLoading.collectAsState()
    val error by viewModel.officialWallpapersError.collectAsState()
    val saveState by viewModel.splashSaveState.collectAsState()

    var selectedUrl by remember { mutableStateOf<String?>(null) }
    var saveToGallery by remember { mutableStateOf(false) }
    var showSplashAdjustmentSheet by remember { mutableStateOf(false) }
    val initialSplashMobileBias by viewModel.getSplashAlignment(false).collectAsState(0f)
    val initialSplashTabletBias by viewModel.getSplashAlignment(true).collectAsState(0f)

    // 初始化加载
    LaunchedEffect(Unit) {
        if (officialWallpapers.isEmpty()) {
            viewModel.loadOfficialWallpapers()
        }
    }
    LaunchedEffect(officialWallpapers) {
        val randomPool = resolveVisibleSplashWallpaperPool(officialWallpapers)
        SettingsManager.setSplashRandomPoolUris(context, randomPool)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // 1. 顶部栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(CupertinoIcons.Default.Xmark, contentDescription = "关闭")
                    }

                    Text(
                        text = "选择开屏壁纸",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // 2. 内容区
            when {
                isLoading && officialWallpapers.isEmpty() -> {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null && officialWallpapers.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = error ?: "加载失败", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadOfficialWallpapers() }) {
                            Text("重试")
                        }
                    }
                }
                officialWallpapers.isEmpty() -> {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = "暂无壁纸", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(officialWallpapers) { item ->
                            val isSelected = selectedUrl == item.thumb || selectedUrl == item.image
                            val rawUrl = item.thumb.ifEmpty { item.image }
                            val imageUrl = normalizeSplashWallpaperUrl(rawUrl)

                            Column(
                                modifier = Modifier
                                    .clickable { selectedUrl = rawUrl }
                                    .animateContentSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(imageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .aspectRatio(9f / 16f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    )

                                    if (isSelected) {
                                        Icon(
                                            imageVector = CupertinoIcons.Default.CheckmarkCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(20.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.title.ifEmpty { "未命名" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 3. 底部操作栏
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val isSaving = saveState is WallpaperSaveState.Loading

                    if (showSplashAdjustmentSheet && selectedUrl != null) {
                        WallpaperAdjustmentSheet(
                            imageUri = normalizeSplashWallpaperUrl(selectedUrl),
                            initialMobileBias = initialSplashMobileBias,
                            initialTabletBias = initialSplashTabletBias,
                            onDismiss = { showSplashAdjustmentSheet = false },
                            onSave = { mBias, tBias ->
                                showSplashAdjustmentSheet = false
                                selectedUrl?.let { url ->
                                    viewModel.setAsSplashWallpaper(
                                        url = url,
                                        saveToGallery = saveToGallery,
                                        mobileBias = mBias,
                                        tabletBias = tBias
                                    ) {
                                        onDismiss()
                                        Toast.makeText(context, "开屏壁纸设置成功", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }

                    // 保存到相册开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { saveToGallery = !saveToGallery },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                                        text = "同时保存到相册",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = saveToGallery,
                            onCheckedChange = { saveToGallery = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    // 确认按钮
                    Button(
                        onClick = {
                            showSplashAdjustmentSheet = true
                        },
                        enabled = selectedUrl != null && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("设为开屏壁纸", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (saveState is WallpaperSaveState.Error) {
                        Text(
                            text = (saveState as WallpaperSaveState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
