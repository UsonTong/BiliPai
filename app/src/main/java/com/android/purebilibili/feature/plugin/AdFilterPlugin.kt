// 文件路径: feature/plugin/AdFilterPlugin.kt
package com.android.purebilibili.feature.plugin

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.plugin.FeedPlugin
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.ui.components.*
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private const val TAG = "AdFilterPlugin"

/**
 * 🚫 去广告增强插件 v2.0
 * 
 * 功能：
 * 1. 过滤广告/推广/商业合作内容
 * 2. 过滤标题党视频
 * 3. 过滤低质量视频（播放量低）
 * 4. UP主拉黑（按名称或MID）
 * 5. 自定义关键词屏蔽
 */
class AdFilterPlugin : FeedPlugin {
    
    override val id = "adfilter"
    override val name = "去广告增强"
    override val description = "过滤广告、拉黑UP主、屏蔽关键词"
    override val version = "2.0.0"
    override val author = "BiliPai项目组"
    override val icon: ImageVector = CupertinoIcons.Default.Xmark
    override val capabilityManifest: PluginCapabilityManifest = PluginCapabilityManifest(
        pluginId = id,
        displayName = name,
        version = version,
        apiVersion = 1,
        entryClassName = "com.android.purebilibili.feature.plugin.AdFilterPlugin",
        capabilities = setOf(
            PluginCapability.RECOMMENDATION_CANDIDATES,
            PluginCapability.LOCAL_FEEDBACK_READ,
            PluginCapability.PLUGIN_STORAGE
        )
    )
    
    private var config: AdFilterConfig = AdFilterConfig()
    private var filteredCount = 0
    
    //  配置版本号，用于检测是否需要重载
    @Volatile
    private var configVersion = 0
    @Volatile
    private var lastConfigReloadMs = 0L
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    //  内置广告关键词（强化版）
    private val AD_KEYWORDS = listOf(
        // 商业合作类
        "商业合作", "恰饭", "推广", "广告", "赞助", "植入",
        "合作推广", "品牌合作", "本期合作", "本视频由",
        // 平台推广类
        "官方活动", "官方推荐", "平台活动", "创作激励",
        // 淘宝/电商类
        "淘宝", "天猫", "京东", "拼多多", "双十一", "双11",
        "优惠券", "领券", "限时优惠", "好物推荐", "种草",
        // 游戏推广类
        "新游推荐", "游戏推广", "首发", "公测", "不删档"
    )
    
    //  标题党关键词（强化版）
    private val CLICKBAIT_KEYWORDS = listOf(
        "震惊", "惊呆了", "太厉害了", "绝了", "离谱", "疯了",
        "价值几万", "价值百万", "价值千万", "一定要看", "必看",
        "看哭了", "泪目", "破防了", "DNA动了", "YYDS",
        "封神", "炸裂", "神作", "预定年度", "史诗级",
        "99%的人不知道", "你一定不知道", "居然是这样",
        "原来是这样", "真相了", "曝光", "揭秘", "独家"
    )
    
    override suspend fun onEnable() {
        filteredCount = 0
        loadConfigSuspend()
        Logger.d(TAG, " 去广告增强v2.0已启用")
        Logger.d(TAG, " 拉黑UP主: ${config.blockedUpNames.size}个, 屏蔽关键词: ${config.blockedKeywords.size}个")
    }
    
    override suspend fun onDisable() {
        Logger.d(TAG, "🔴 去广告增强已禁用，本次过滤了 $filteredCount 条内容")
        filteredCount = 0
    }
    
    override fun shouldShowItem(item: VideoItem): Boolean {
        //  每次过滤前确保配置是最新的
        reloadConfigAsync()
        
        val title = item.title
        val upName = item.owner.name
        val upMid = item.owner.mid
        val viewCount = item.stat.view
        
        // 1️⃣ 检查UP主拉黑列表（按名称） - 支持模糊匹配和简繁体
        if (isUpNameBlocked(upName)) {
            filteredCount++
            Logger.d(TAG, "🚫 拉黑UP主[名称]: $upName - $title (列表: ${config.blockedUpNames})")
            return false
        }
        
        // 2️⃣ 检查UP主拉黑列表（按MID）
        if (config.blockedUpMids.contains(upMid)) {
            filteredCount++
            Logger.d(TAG, "🚫 拉黑UP主[MID]: $upMid - $title")
            return false
        }
        
        // 3️⃣ 检测广告/推广关键词
        if (config.filterSponsored) {
            if (AD_KEYWORDS.any { title.contains(it, ignoreCase = true) }) {
                filteredCount++
                Logger.d(TAG, "🚫 过滤广告: $title (UP: $upName)")
                return false
            }
        }
        
        // 4️⃣ 检测标题党
        if (config.filterClickbait) {
            if (CLICKBAIT_KEYWORDS.any { title.contains(it, ignoreCase = true) }) {
                filteredCount++
                Logger.d(TAG, "🚫 过滤标题党: $title")
                return false
            }
        }
        
        // 5️⃣ 检测自定义屏蔽关键词
        if (config.blockedKeywords.isNotEmpty()) {
            for (keyword in config.blockedKeywords) {
                if (keyword.isNotBlank() && title.contains(keyword, ignoreCase = true)) {
                    filteredCount++
                    Logger.d(TAG, "🚫 自定义屏蔽: $title (关键词: $keyword)")
                    return false
                }
            }
        }
        
        // 6️⃣ 过滤低质量视频（播放量过低）
        if (config.filterLowQuality && viewCount > 0 && viewCount < config.minViewCount) {
            filteredCount++
            Logger.d(TAG, "🚫 低播放量: $title (播放: $viewCount)")
            return false
        }
        
        return true
    }
    
    /**
     *  检查UP主名称是否在拉黑列表中
     * 支持：精确匹配、模糊匹配(contains)、简繁体转换
     */
    private fun isUpNameBlocked(upName: String): Boolean {
        val normalizedUpName = normalizeChineseChars(upName.lowercase())
        
        return config.blockedUpNames.any { blockedName ->
            val normalizedBlocked = normalizeChineseChars(blockedName.lowercase())
            
            // 精确匹配（忽略大小写和简繁体）
            normalizedUpName == normalizedBlocked ||
            // 模糊匹配：UP名包含拉黑词
            normalizedUpName.contains(normalizedBlocked) ||
            // 模糊匹配：拉黑词包含UP名
            normalizedBlocked.contains(normalizedUpName)
        }
    }
    
    /**
     *  简繁体字符转换表
     * 常用字符的简体→繁体映射，方便双向比较
     */
    private val SIMPLIFIED_TO_TRADITIONAL = mapOf(
        '说' to '說', '话' to '話', '语' to '語', '请' to '請', '让' to '讓',
        '这' to '這', '那' to '那', '哪' to '哪', '谁' to '誰', '什' to '什',
        '时' to '時', '间' to '間', '门' to '門', '网' to '網', '电' to '電',
        '视' to '視', '频' to '頻', '机' to '機', '会' to '會', '员' to '員',
        '学' to '學', '习' to '習', '写' to '寫', '画' to '畫', '图' to '圖',
        '书' to '書', '读' to '讀', '听' to '聽', '看' to '看', '见' to '見',
        '现' to '現', '发' to '發', '开' to '開', '关' to '關', '头' to '頭',
        '脑' to '腦', '乐' to '樂', '欢' to '歡', '爱' to '愛', '国' to '國',
        '华' to '華', '东' to '東', '车' to '車', '马' to '馬', '鸟' to '鳥'
    )
    
    /**
     * 将字符串中的繁体字统一转换为简体字（用于比较）
     */
    private fun normalizeChineseChars(text: String): String {
        val traditionalToSimplified = SIMPLIFIED_TO_TRADITIONAL.entries.associate { it.value to it.key }
        return text.map { char ->
            traditionalToSimplified[char] ?: char
        }.joinToString("")
    }
    
    //  公开方法：添加UP主到拉黑列表
    fun blockUploader(name: String, mid: Long) {
        if (name.isNotBlank() && !config.blockedUpNames.contains(name)) {
            config = config.copy(blockedUpNames = config.blockedUpNames + name)
        }
        if (mid > 0 && !config.blockedUpMids.contains(mid)) {
            config = config.copy(blockedUpMids = config.blockedUpMids + mid)
        }
        saveConfig()
        Logger.d(TAG, "➕ 已拉黑UP主: $name (MID: $mid)")
    }
    
    //  公开方法：移除UP主拉黑
    fun unblockUploader(name: String, mid: Long) {
        config = config.copy(
            blockedUpNames = config.blockedUpNames - name,
            blockedUpMids = config.blockedUpMids - mid
        )
        saveConfig()
        Logger.d(TAG, "➖ 已解除拉黑: $name (MID: $mid)")
    }
    
    private fun saveConfig() {
        ioScope.launch {
            try {
                val context = PluginManager.getContext()
                PluginStore.setConfigJson(context, id, Json.encodeToString(config))
            } catch (e: Exception) {
                Logger.e(TAG, "保存配置失败", e)
            }
        }
    }
    
    private suspend fun loadConfigSuspend() {
        try {
            val context = PluginManager.getContext()
            val jsonStr = PluginStore.getConfigJson(context, id)
            if (jsonStr != null) {
                config = Json.decodeFromString<AdFilterConfig>(jsonStr)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "加载配置失败", e)
        }
    }
    
    /**
     *  同步重载配置
     * 确保每次过滤使用最新的拉黑列表
     */
    private fun reloadConfigAsync() {
        val now = System.currentTimeMillis()
        if (now - lastConfigReloadMs < 1000L) return
        lastConfigReloadMs = now
        
        ioScope.launch {
            try {
                val context = PluginManager.getContext()
                val jsonStr = PluginStore.getConfigJson(context, id)
                if (jsonStr != null) {
                    val newConfig = Json.decodeFromString<AdFilterConfig>(jsonStr)
                    // 只有配置真的变了才更新
                    if (newConfig != config) {
                        config = newConfig
                        configVersion++
                        Logger.d(TAG, " 配置已重载 v$configVersion: 拉黑UP主=${config.blockedUpNames}")
                    }
                }
            } catch (_: Exception) {
                // 静默失败，使用现有配置
            }
        }
    }
    
    @Composable
    override fun SettingsContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var filterSponsored by remember { mutableStateOf(config.filterSponsored) }
        var filterClickbait by remember { mutableStateOf(config.filterClickbait) }
        var filterLowQuality by remember { mutableStateOf(config.filterLowQuality) }
        var blockedUpNames by remember { mutableStateOf(config.blockedUpNames) }
        var blockedKeywords by remember { mutableStateOf(config.blockedKeywords) }
        
        // 输入对话框状态
        var showAddUpDialog by remember { mutableStateOf(false) }
        var showAddKeywordDialog by remember { mutableStateOf(false) }
        var inputText by remember { mutableStateOf("") }
        
        // 加载配置
        LaunchedEffect(Unit) {
            loadConfigSuspend()
            filterSponsored = config.filterSponsored
            filterClickbait = config.filterClickbait
            filterLowQuality = config.filterLowQuality
            blockedUpNames = config.blockedUpNames
            blockedKeywords = config.blockedKeywords
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ========== 过滤开关 ==========
            
            // 商业合作过滤
            IOSSwitchItem(
                icon = CupertinoIcons.Default.Xmark,
                title = "过滤广告推广",
                subtitle = "隐藏商业合作、恰饭、推广等内容",
                checked = filterSponsored,
                onCheckedChange = { newValue ->
                    filterSponsored = newValue
                    config = config.copy(filterSponsored = newValue)
                    scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(config)) }
                },
                iconTint = Color(0xFFE91E63)
            )
            
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            
            // 标题党过滤
            IOSSwitchItem(
                icon = CupertinoIcons.Default.Star,
                title = "过滤标题党",
                subtitle = "隐藏震惊体、夸张标题视频",
                checked = filterClickbait,
                onCheckedChange = { newValue ->
                    filterClickbait = newValue
                    config = config.copy(filterClickbait = newValue)
                    scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(config)) }
                },
                iconTint = Color(0xFFFF9800)
            )
            
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            
            // 低质量过滤
            IOSSwitchItem(
                icon = CupertinoIcons.Default.Xmark,
                title = "过滤低播放量",
                subtitle = "隐藏播放量低于1000的视频",
                checked = filterLowQuality,
                onCheckedChange = { newValue ->
                    filterLowQuality = newValue
                    config = config.copy(filterLowQuality = newValue)
                    scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(config)) }
                },
                iconTint = Color(0xFF9E9E9E)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ========== UP主拉黑 ==========
            Text(
                text = "UP主拉黑",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // 已拉黑列表
            if (blockedUpNames.isEmpty()) {
                Text(
                    text = "暂无拉黑的UP主",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                blockedUpNames.forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            CupertinoIcons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                blockedUpNames = blockedUpNames - name
                                config = config.copy(blockedUpNames = blockedUpNames)
                                scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(config)) }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                CupertinoIcons.Default.Xmark,
                                contentDescription = "移除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // 添加UP主按钮
            OutlinedButton(
                onClick = { showAddUpDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(CupertinoIcons.Default.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加UP主拉黑")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ========== 自定义关键词 ==========
            Text(
                text = "自定义屏蔽关键词",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            if (blockedKeywords.isEmpty()) {
                Text(
                    text = "暂无自定义屏蔽词",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    blockedKeywords.take(5).forEach { keyword ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = keyword,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    CupertinoIcons.Default.Xmark,
                                    contentDescription = "移除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            blockedKeywords = blockedKeywords - keyword
                                            config = config.copy(blockedKeywords = blockedKeywords)
                                            scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(config)) }
                                        }
                                )
                            }
                        }
                    }
                }
                if (blockedKeywords.size > 5) {
                    Text(
                        text = "还有 ${blockedKeywords.size - 5} 个关键词...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // 添加关键词按钮
            OutlinedButton(
                onClick = { showAddKeywordDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(CupertinoIcons.Default.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加屏蔽关键词")
            }
        }
        
        // ========== 对话框 ==========
        
        // 添加UP主对话框
        if (showAddUpDialog) {
            AlertDialog(
                onDismissRequest = { showAddUpDialog = false; inputText = "" },
                title = { Text("添加UP主拉黑") },
                text = {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("UP主名称") },
                        placeholder = { Text("输入UP主名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                blockedUpNames = blockedUpNames + inputText.trim()
                                config = config.copy(blockedUpNames = blockedUpNames)
                                scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(config)) }
                            }
                            showAddUpDialog = false
                            inputText = ""
                        }
                    ) { Text("添加") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddUpDialog = false; inputText = "" }) { Text("取消") }
                }
            )
        }
        
        // 添加关键词对话框
        if (showAddKeywordDialog) {
            AlertDialog(
                onDismissRequest = { showAddKeywordDialog = false; inputText = "" },
                title = { Text("添加屏蔽关键词") },
                text = {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("关键词") },
                        placeholder = { Text("输入要屏蔽的关键词") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                blockedKeywords = blockedKeywords + inputText.trim()
                                config = config.copy(blockedKeywords = blockedKeywords)
                                scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(config)) }
                            }
                            showAddKeywordDialog = false
                            inputText = ""
                        }
                    ) { Text("添加") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddKeywordDialog = false; inputText = "" }) { Text("取消") }
                }
            )
        }
    }
}

/**
 * 去广告配置 v2.0
 */
@Serializable
data class AdFilterConfig(
    // 基础过滤开关
    val filterSponsored: Boolean = true,    // 过滤广告推广
    val filterClickbait: Boolean = true,    // 过滤标题党
    val filterLowQuality: Boolean = false,  // 过滤低质量
    val minViewCount: Int = 1000,           // 最低播放量
    
    // UP主拉黑
    val blockedUpNames: List<String> = emptyList(),  // 拉黑UP主名称
    val blockedUpMids: List<Long> = emptyList(),     // 拉黑UP主MID
    
    // 自定义关键词
    val blockedKeywords: List<String> = emptyList()  // 自定义屏蔽词
)
