// 文件路径: feature/plugin/DanmakuEnhancePlugin.kt
package com.android.purebilibili.feature.plugin

import android.content.Context
import androidx.compose.foundation.layout.*
// 🍎 Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.plugin.DanmakuItem
import com.android.purebilibili.core.plugin.DanmakuPlugin
import com.android.purebilibili.core.plugin.DanmakuStyle
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.util.Logger
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoSwitchDefaults
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private const val TAG = "DanmakuEnhancePlugin"

/**
 * 💬 弹幕增强插件
 * 
 * 提供弹幕过滤和高亮功能：
 * - 关键词屏蔽
 * - 同传弹幕高亮
 */
class DanmakuEnhancePlugin : DanmakuPlugin {
    
    override val id = "danmaku_enhance"
    override val name = "弹幕增强"
    override val description = "关键词屏蔽、同传弹幕高亮"
    override val version = "1.0.0"
    override val author = "YangY"
    override val icon: ImageVector = CupertinoIcons.Default.TextBubble
    override val unavailable = true
    override val unavailableReason = "弹幕功能开发中"
    
    private var config: DanmakuEnhanceConfig = DanmakuEnhanceConfig()
    private var filteredCount = 0

    private suspend fun loadConfig(context: Context) {
        val jsonStr = PluginStore.getConfigJson(context, id)
        if (jsonStr != null) {
            try {
                config = Json.decodeFromString<DanmakuEnhanceConfig>(jsonStr)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to decode config", e)
            }
        }
    }
    
    override suspend fun onEnable() {
        filteredCount = 0
        Logger.d(TAG, "✅ 弹幕增强已启用")
    }
    
    override suspend fun onDisable() {
        Logger.d(TAG, "🔴 弹幕增强已禁用，本次过滤了 $filteredCount 条弹幕")
        filteredCount = 0
    }
    
    override fun filterDanmaku(danmaku: DanmakuItem): DanmakuItem? {
        if (!config.enableFilter) return danmaku
        
        val content = danmaku.content
        
        // 检查屏蔽关键词
        val blockedKeywords = config.blockedKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (blockedKeywords.any { content.contains(it, ignoreCase = true) }) {
            filteredCount++
            return null
        }
        
        return danmaku
    }
    
    override fun styleDanmaku(danmaku: DanmakuItem): DanmakuStyle? {
        if (!config.enableHighlight) return null
        
        val content = danmaku.content
        
        // 检查高亮关键词（同传弹幕通常包含【】）
        val highlightKeywords = config.highlightKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        if (highlightKeywords.any { content.contains(it) }) {
            return DanmakuStyle(
                borderColor = Color(0xFFFFD700),  // 金色边框
                backgroundColor = Color.Black.copy(alpha = 0.5f),
                bold = true
            )
        }
        
        return null
    }
    
    @Composable
    override fun SettingsContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var enableFilter by remember { mutableStateOf(config.enableFilter) }
        var enableHighlight by remember { mutableStateOf(config.enableHighlight) }
        var blockedKeywords by remember { mutableStateOf(config.blockedKeywords) }
        var highlightKeywords by remember { mutableStateOf(config.highlightKeywords) }
        
        // 加载配置
        LaunchedEffect(Unit) {
            loadConfig(context)
            enableFilter = config.enableFilter
            enableHighlight = config.enableHighlight
            blockedKeywords = config.blockedKeywords
            highlightKeywords = config.highlightKeywords
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 启用屏蔽
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用关键词屏蔽", style = MaterialTheme.typography.bodyLarge)
                }
                val primaryColor = MaterialTheme.colorScheme.primary
                CupertinoSwitch(
                    checked = enableFilter,
                    onCheckedChange = { newValue ->
                        enableFilter = newValue
                        config = config.copy(enableFilter = newValue)
                        scope.launch { 
                            PluginStore.setConfigJson(context, id, Json.encodeToString(config)) 
                        }
                    },
                    colors = CupertinoSwitchDefaults.colors(
                        thumbColor = Color.White,
                        checkedTrackColor = primaryColor,
                        uncheckedTrackColor = Color(0xFFE9E9EA)
                    )
                )
            }
            
            // 屏蔽关键词输入
            if (enableFilter) {
                OutlinedTextField(
                    value = blockedKeywords,
                    onValueChange = { newValue ->
                        blockedKeywords = newValue
                        config = config.copy(blockedKeywords = newValue)
                        scope.launch { 
                            PluginStore.setConfigJson(context, id, Json.encodeToString(config)) 
                        }
                    },
                    label = { Text("屏蔽关键词") },
                    placeholder = { Text("用逗号分隔，如：剧透,前方高能") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 启用高亮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用同传高亮", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "高亮显示同传/翻译弹幕",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val primaryColor = MaterialTheme.colorScheme.primary
                CupertinoSwitch(
                    checked = enableHighlight,
                    onCheckedChange = { newValue ->
                        enableHighlight = newValue
                        config = config.copy(enableHighlight = newValue)
                        scope.launch { 
                            PluginStore.setConfigJson(context, id, Json.encodeToString(config)) 
                        }
                    },
                    colors = CupertinoSwitchDefaults.colors(
                        thumbColor = Color.White,
                        checkedTrackColor = primaryColor,
                        uncheckedTrackColor = Color(0xFFE9E9EA)
                    )
                )
            }
            
            // 高亮关键词输入
            if (enableHighlight) {
                OutlinedTextField(
                    value = highlightKeywords,
                    onValueChange = { newValue ->
                        highlightKeywords = newValue
                        config = config.copy(highlightKeywords = newValue)
                        scope.launch { 
                            PluginStore.setConfigJson(context, id, Json.encodeToString(config)) 
                        }
                    },
                    label = { Text("高亮关键词") },
                    placeholder = { Text("用逗号分隔，如：【,】,同传") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            }
        }
    }
}

/**
 * 弹幕增强配置
 */
@Serializable
data class DanmakuEnhanceConfig(
    val enableFilter: Boolean = true,
    val enableHighlight: Boolean = true,
    val blockedKeywords: String = "剧透,前方高能",
    val highlightKeywords: String = "【,】,同传,翻译"
)
