// 文件路径: feature/settings/SettingsViewModel.kt
package com.android.purebilibili.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.blur.BlurIntensity
import com.android.purebilibili.core.util.CacheUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val hwDecode: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    val dynamicColor: Boolean = true,
    val bgPlay: Boolean = false,
    val gestureSensitivity: Float = 1.0f,
    val themeColorIndex: Int = 0,
    val appIcon: String = "3D",
    val isBottomBarFloating: Boolean = true,
    val bottomBarLabelMode: Int = 1,  // 0=图标+文字, 1=仅图标, 2=仅文字
    val headerBlurEnabled: Boolean = true,
    val bottomBarBlurEnabled: Boolean = true,
    val blurIntensity: BlurIntensity = BlurIntensity.THIN,  // 🔥🔥 模糊强度
    val displayMode: Int = 0,
    val cardAnimationEnabled: Boolean = false,     // 🔥 卡片进场动画（默认关闭）
    val cardTransitionEnabled: Boolean = false,    // 🔥 卡片过渡动画（默认关闭）
    val cacheSize: String = "计算中...",
    val cacheBreakdown: CacheUtils.CacheBreakdown? = null,  // 🚀 详细缓存统计
    // 🧪 实验性功能
    val auto1080p: Boolean = true,
    val autoSkipOpEd: Boolean = false,
    val prefetchVideo: Boolean = false,
    val doubleTapLike: Boolean = true,
    // 🚀 空降助手
    val sponsorBlockEnabled: Boolean = false,
    val sponsorBlockAutoSkip: Boolean = true
)

// 内部数据类，用于分批合并流
private data class CoreSettings(
    val hwDecode: Boolean,
    val themeMode: AppThemeMode,
    val dynamicColor: Boolean,
    val bgPlay: Boolean
)

data class ExtraSettings(
    val gestureSensitivity: Float,
    val themeColorIndex: Int,
    val appIcon: String,
    val isBottomBarFloating: Boolean,
    val bottomBarLabelMode: Int,
    val headerBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val blurIntensity: BlurIntensity,  // 🔥🔥 添加模糊强度
    val displayMode: Int,
    val cardAnimationEnabled: Boolean,
    val cardTransitionEnabled: Boolean
)

// 🧪 实验性功能设置
data class ExperimentalSettings(
    val auto1080p: Boolean,
    val autoSkipOpEd: Boolean,
    val prefetchVideo: Boolean,
    val doubleTapLike: Boolean,
    // 🚀 空降助手
    val sponsorBlockEnabled: Boolean,
    val sponsorBlockAutoSkip: Boolean
)

private data class BaseSettings(
    val hwDecode: Boolean,
    val themeMode: AppThemeMode,
    val dynamicColor: Boolean,
    val bgPlay: Boolean,
    val gestureSensitivity: Float,
    val themeColorIndex: Int,
    val appIcon: String,
    val isBottomBarFloating: Boolean,
    val bottomBarLabelMode: Int,
    val headerBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val blurIntensity: BlurIntensity,  // 🔥🔥 模糊强度
    val displayMode: Int, // 🔥 新增
    val cardAnimationEnabled: Boolean, // 🔥 卡片进场动画
    val cardTransitionEnabled: Boolean // 🔥 卡片过渡动画
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    // 本地状态流：缓存大小
    private val _cacheSize = MutableStateFlow("计算中...")
    private val _cacheBreakdown = MutableStateFlow<CacheUtils.CacheBreakdown?>(null)

    // 🔥🔥 [核心修复] 分步合并，解决 combine 参数限制报错
    // 第 1 步：合并前 4 个设置
    private val coreSettingsFlow = combine(
        SettingsManager.getHwDecode(context),
        SettingsManager.getThemeMode(context),
        SettingsManager.getDynamicColor(context),
        SettingsManager.getBgPlay(context)
    ) { hwDecode, themeMode, dynamicColor, bgPlay ->
        CoreSettings(hwDecode, themeMode, dynamicColor, bgPlay)
    }
    
    // 第 2 步：合并界面设置 (分两组，每组最多5个)
    private val uiSettingsFlow1 = combine(
        SettingsManager.getGestureSensitivity(context),
        SettingsManager.getThemeColorIndex(context),
        SettingsManager.getAppIcon(context)
    ) { gestureSensitivity, themeColorIndex, appIcon ->
        Triple(gestureSensitivity, themeColorIndex, appIcon)
    }
    
    private val uiSettingsFlow2 = combine(
        SettingsManager.getBottomBarFloating(context),
        SettingsManager.getBottomBarLabelMode(context),
        SettingsManager.getDisplayMode(context),
        SettingsManager.getCardAnimationEnabled(context),
        SettingsManager.getCardTransitionEnabled(context)
    ) { isBottomBarFloating, labelMode, displayMode, cardAnimation, cardTransition ->
        listOf(isBottomBarFloating, labelMode, displayMode, cardAnimation, cardTransition)
    }
    
    private val uiSettingsFlow = combine(uiSettingsFlow1, uiSettingsFlow2) { ui1, ui2 ->
        listOf(ui1.first, ui1.second, ui1.third, ui2[0], ui2[1], ui2[2], ui2[3], ui2[4])
    }
    
    // 第 3 步：合并模糊设置 (3个)
    private val blurSettingsFlow = combine(
        SettingsManager.getHeaderBlurEnabled(context),
        SettingsManager.getBottomBarBlurEnabled(context),
        SettingsManager.getBlurIntensity(context)  // 🔥🔥 添加模糊强度
    ) { headerBlur, bottomBarBlur, blurIntensity ->
        Triple(headerBlur, bottomBarBlur, blurIntensity)
    }
    
    // 第 4 步：合并 UI 和 模糊设置
    private val extraSettingsFlow = combine(uiSettingsFlow, blurSettingsFlow) { ui, blur ->
        ExtraSettings(
            gestureSensitivity = ui[0] as Float,
            themeColorIndex = ui[1] as Int,
            appIcon = ui[2] as String,
            isBottomBarFloating = ui[3] as Boolean,
            bottomBarLabelMode = ui[4] as Int,
            displayMode = ui[5] as Int,
            headerBlurEnabled = blur.first,
            bottomBarBlurEnabled = blur.second,
            blurIntensity = blur.third,  // 🔥🔥 模糊强度
            cardAnimationEnabled = ui[6] as Boolean,
            cardTransitionEnabled = ui[7] as Boolean
        )
    }
    
    // 🧪 第 4.5 步：合并实验性功能设置
    private val experimentalSettingsFlow = combine(
        SettingsManager.getAuto1080p(context),
        SettingsManager.getAutoSkipOpEd(context),
        SettingsManager.getPrefetchVideo(context),
        SettingsManager.getDoubleTapLike(context),
        SettingsManager.getSponsorBlockEnabled(context),
        SettingsManager.getSponsorBlockAutoSkip(context)
    ) { values ->
        ExperimentalSettings(
            auto1080p = values[0] as Boolean,
            autoSkipOpEd = values[1] as Boolean,
            prefetchVideo = values[2] as Boolean,
            doubleTapLike = values[3] as Boolean,
            sponsorBlockEnabled = values[4] as Boolean,
            sponsorBlockAutoSkip = values[5] as Boolean
        )
    }
    
    // 第 5 步：合并两组设置
    private val baseSettingsFlow = combine(coreSettingsFlow, extraSettingsFlow) { core, extra ->
        BaseSettings(
            hwDecode = core.hwDecode,
            themeMode = core.themeMode,
            dynamicColor = core.dynamicColor,
            bgPlay = core.bgPlay,
            gestureSensitivity = extra.gestureSensitivity,
            themeColorIndex = extra.themeColorIndex,
            appIcon = extra.appIcon,
            isBottomBarFloating = extra.isBottomBarFloating,
            bottomBarLabelMode = extra.bottomBarLabelMode,
            headerBlurEnabled = extra.headerBlurEnabled,
            bottomBarBlurEnabled = extra.bottomBarBlurEnabled,
            blurIntensity = extra.blurIntensity,  // 🔥🔥 模糊强度
            displayMode = extra.displayMode,
            cardAnimationEnabled = extra.cardAnimationEnabled,
            cardTransitionEnabled = extra.cardTransitionEnabled
        )
    }

    // 第 6 步：与缓存大小和实验性功能合并
    private val cacheFlow = combine(_cacheSize, _cacheBreakdown) { size, breakdown ->
        Pair(size, breakdown)
    }
    
    val state: StateFlow<SettingsUiState> = combine(
        baseSettingsFlow,
        cacheFlow,
        experimentalSettingsFlow
    ) { settings, cache, experimental ->
        SettingsUiState(
            hwDecode = settings.hwDecode,
            themeMode = settings.themeMode,
            dynamicColor = settings.dynamicColor,
            bgPlay = settings.bgPlay,
            gestureSensitivity = settings.gestureSensitivity,
            themeColorIndex = settings.themeColorIndex,
            appIcon = settings.appIcon,
            isBottomBarFloating = settings.isBottomBarFloating,
            bottomBarLabelMode = settings.bottomBarLabelMode,
            headerBlurEnabled = settings.headerBlurEnabled,
            bottomBarBlurEnabled = settings.bottomBarBlurEnabled,
            blurIntensity = settings.blurIntensity,  // 🔥🔥 模糊强度
            displayMode = settings.displayMode,
            cardAnimationEnabled = settings.cardAnimationEnabled,
            cardTransitionEnabled = settings.cardTransitionEnabled,
            cacheSize = cache.first,
            cacheBreakdown = cache.second,  // 🚀 详细缓存统计
            // 🧪 实验性功能
            auto1080p = experimental.auto1080p,
            autoSkipOpEd = experimental.autoSkipOpEd,
            prefetchVideo = experimental.prefetchVideo,
            doubleTapLike = experimental.doubleTapLike,
            // 🚀 空降助手
            sponsorBlockEnabled = experimental.sponsorBlockEnabled,
            sponsorBlockAutoSkip = experimental.sponsorBlockAutoSkip
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        refreshCacheSize()
    }

    // --- 功能方法 ---

    // 🚀 优化：同时获取缓存大小和详细统计
    fun refreshCacheSize() {
        viewModelScope.launch { 
            val breakdown = CacheUtils.getCacheBreakdown(context)
            _cacheSize.value = breakdown.format()
            _cacheBreakdown.value = breakdown
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            CacheUtils.clearAllCache(context)
            // 清理后立即刷新
            val breakdown = CacheUtils.getCacheBreakdown(context)
            _cacheSize.value = breakdown.format()
            _cacheBreakdown.value = breakdown
        }
    }

    fun toggleHwDecode(value: Boolean) { viewModelScope.launch { SettingsManager.setHwDecode(context, value) } }
    fun setThemeMode(mode: AppThemeMode) { 
        viewModelScope.launch { 
            SettingsManager.setThemeMode(context, mode)
        } 
    }
    fun toggleDynamicColor(value: Boolean) { viewModelScope.launch { SettingsManager.setDynamicColor(context, value) } }
    fun toggleBgPlay(value: Boolean) { viewModelScope.launch { SettingsManager.setBgPlay(context, value) } }
    // 🔥🔥 [新增] 手势灵敏度和主题色
    fun setGestureSensitivity(value: Float) { viewModelScope.launch { SettingsManager.setGestureSensitivity(context, value) } }
    fun setThemeColorIndex(index: Int) { 
        viewModelScope.launch { 
            SettingsManager.setThemeColorIndex(context, index)
            // 🔥 选择自定义主题色时，自动关闭动态取色
            if (index != 0) {
                SettingsManager.setDynamicColor(context, false)
            }
        }
    }

    // 🔥🔥 [新增] 切换应用图标
    fun setAppIcon(iconKey: String) {
        viewModelScope.launch {
            // 1. 保存偏好
            SettingsManager.setAppIcon(context, iconKey)
            
            // 2. 应用 Alias
            val pm = context.packageManager
            val packageName = context.packageName
            
            // alias 映射
            val allAliases = listOf(
                "3D" to "${packageName}.MainActivityAlias3D",
                "NewYear" to "${packageName}.MainActivityAliasNewYear",
                "Blue" to "${packageName}.MainActivityAliasBlue",
                "Retro" to "${packageName}.MainActivityAliasRetro",
                "Flat" to "${packageName}.MainActivityAliasFlat",
                "Flat Material" to "${packageName}.MainActivityAliasFlatMaterial",
                "Neon" to "${packageName}.MainActivityAliasNeon",
                "Telegram Blue" to "${packageName}.MainActivityAliasTelegramBlue",
                "Pink" to "${packageName}.MainActivityAliasPink",
                "Purple" to "${packageName}.MainActivityAliasPurple",
                "Green" to "${packageName}.MainActivityAliasGreen",
                "Dark" to "${packageName}.MainActivityAliasDark"
            )
            
            // 找到需要启用的 alias
            val targetAlias = allAliases.find { it.first == iconKey }?.second
                ?: "${packageName}.MainActivityAlias3D" // 默认3D
            
            // 🔥🔥 [修复] 先禁用所有 alias，再启用目标 alias
            // 避免出现多个启动器图标
            
            // 第一步：禁用所有 alias
            allAliases.forEach { (_, aliasFullName) ->
                pm.setComponentEnabledSetting(
                    android.content.ComponentName(packageName, aliasFullName),
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            }
            
            // 第二步：启用目标 alias
            pm.setComponentEnabledSetting(
                android.content.ComponentName(packageName, targetAlias),
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        }
    }

    // 🔥🔥 [新增] 切换底栏样式
    fun toggleBottomBarFloating(value: Boolean) { viewModelScope.launch { SettingsManager.setBottomBarFloating(context, value) } }
    
    // 🔥🔥 [新增] 底栏显示模式 (0=图标+文字, 1=仅图标, 2=仅文字)
    fun setBottomBarLabelMode(mode: Int) { viewModelScope.launch { SettingsManager.setBottomBarLabelMode(context, mode) } }
    
    // 🔥🔥 [新增] 模糊效果开关
    fun toggleHeaderBlur(value: Boolean) { viewModelScope.launch { SettingsManager.setHeaderBlurEnabled(context, value) } }
    fun toggleBottomBarBlur(value: Boolean) { viewModelScope.launch { SettingsManager.setBottomBarBlurEnabled(context, value) } }
    fun setBlurIntensity(intensity: BlurIntensity) { viewModelScope.launch { SettingsManager.setBlurIntensity(context, intensity) } }  // 🔥🔥 模糊强度设置
    
    // 🔥 [新增] 卡片进场动画开关
    fun toggleCardAnimation(value: Boolean) { viewModelScope.launch { SettingsManager.setCardAnimationEnabled(context, value) } }
    
    // 🔥 [新增] 卡片过渡动画开关
    fun toggleCardTransition(value: Boolean) { viewModelScope.launch { SettingsManager.setCardTransitionEnabled(context, value) } }
    
    // 🔥🔥 [新增] 首页展示模式
    fun setDisplayMode(mode: Int) { 
        viewModelScope.launch { 
            // 兼容旧的 shared preferences
            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putInt("display_mode", mode).apply()
            // 触发 flow 更新 (如果需要，或者仅仅依赖 prefs 监听? 这里简化处理，假设 ViewModel 只负责写，读在 flow 中)
            // 实际上这里的 flow 是基于 SettingsManager (DataStore) 的。
            // 如果 display_mode 还是 SharedPreferences，我们需要一个 flow 来通过 DataStore 或者手动构建。
            //为了简单统一，建议迁移到 SettingsManager。但为了不破坏 HomeScreen 读取，我们先保持 Prefs，
            // 并在 SettingsManager 中增加对 display_mode 的支持 (或者直接在这里用 MutableStateFlow 桥接?)
            // 鉴于 HomeScreen 可能直接读 Prefs，我们这里只需写 Prefs。
            // 但为了 UI 响应，我们需要通知 UIState。
            // 由于 SettingsManager 目前不管理 display_mode，我们需要添加它。
            // 既然要 refactor，就彻底点。
            SettingsManager.setDisplayMode(context, mode)
        } 
    }
    
    // 🧪🧪 [新增] 实验性功能
    fun toggleAuto1080p(value: Boolean) { viewModelScope.launch { SettingsManager.setAuto1080p(context, value) } }
    fun toggleAutoSkipOpEd(value: Boolean) { viewModelScope.launch { SettingsManager.setAutoSkipOpEd(context, value) } }
    fun togglePrefetchVideo(value: Boolean) { viewModelScope.launch { SettingsManager.setPrefetchVideo(context, value) } }
    fun toggleDoubleTapLike(value: Boolean) { viewModelScope.launch { SettingsManager.setDoubleTapLike(context, value) } }
    
    // 🚀🚀 [新增] 空降助手
    fun toggleSponsorBlock(value: Boolean) { viewModelScope.launch { SettingsManager.setSponsorBlockEnabled(context, value) } }
    fun toggleSponsorBlockAutoSkip(value: Boolean) { viewModelScope.launch { SettingsManager.setSponsorBlockAutoSkip(context, value) } }
}

// Move DisplayMode enum here to be accessible
enum class DisplayMode(val title: String, val description: String, val value: Int) {
    Grid("双列网格", "经典双列布局，信息密度高", 0),
    StoryCards("故事卡片", "电影海报风格，沉浸式体验", 1),
    GlassCards("玻璃拟态", "毛玻璃质感，现代设计", 2)
}