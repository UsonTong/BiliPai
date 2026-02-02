package com.android.purebilibili.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.components.IOSSectionTitle
import com.android.purebilibili.core.ui.animation.staggeredEntrance
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.InfoCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsSettingsScreen(
    onBack: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("小贴士", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Outlined.ChevronBackward, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Box(modifier = Modifier.staggeredEntrance(0, isVisible)) {
                    IOSSectionTitle("隐藏操作")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(1, isVisible)) {
                    TipsSection()
                }
            }
        }
    }
}

@Composable
private fun TipsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TipItem(
            titile = "双击快速回顶",
            content = "在首页，双击底部的 \"首页\" 图标，或双击左下角的 \"推荐\" Tab，均可快速回到列表顶部。"
        )
        TipDivider()
        TipItem(
            titile = "长按预览视频",
            content = "长按视频卡片，可以快速预览封面大图，并进行 \"稍后再看\" 或 \"不感兴趣\" 操作。"
        )
        TipDivider()
        TipItem(
            titile = "左右滑动切换",
            content = "在首页支持左右滑动快速切换不同的频道分区。"
        )
    }
}

@Composable
private fun TipItem(titile: String, content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = CupertinoIcons.Filled.InfoCircle,
            contentDescription = null,
            tint = com.android.purebilibili.core.theme.iOSBlue,
            modifier = Modifier.size(20.dp).padding(top = 2.dp) // Align with text top
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = titile,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TipDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp), // Indent to align with text
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        thickness = 0.5.dp
    )
}
