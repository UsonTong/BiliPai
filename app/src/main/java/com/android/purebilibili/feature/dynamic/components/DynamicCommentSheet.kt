// 文件路径: feature/dynamic/components/DynamicCommentSheet.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.data.model.response.ReplyItem
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*

/**
 * 🔥 动态评论底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicCommentSheet(
    comments: List<ReplyItem>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPostComment: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "评论 ${if (comments.isNotEmpty()) "(${comments.size})" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(
                        CupertinoIcons.Default.Xmark,
                        contentDescription = "关闭",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // 评论列表
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            CupertinoIcons.Default.BubbleLeft,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "暂无评论",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(comments) { reply ->
                        CommentItem(reply = reply)
                    }
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // 评论输入框
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("发一条友善的评论", fontSize = 14.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onPostComment(commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("发送")
                }
            }
            
            // 底部安全区
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 🔥 单条评论项
 */
@Composable
private fun CommentItem(reply: ReplyItem) {
    val member = reply.member
    
    Row(modifier = Modifier.fillMaxWidth()) {
        // 头像
        AsyncImage(
            model = member.avatar.let { 
                if (it.startsWith("http://")) it.replace("http://", "https://") else it 
            },
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // 用户名 + 时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.uname,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTime(reply.ctime),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 评论内容
            Text(
                text = reply.content.message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 点赞数
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    CupertinoIcons.Default.Heart,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${reply.like}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        diff < 604800 -> "${diff / 86400}天前"
        else -> {
            val date = java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA)
                .format(java.util.Date(timestamp * 1000))
            date
        }
    }
}
