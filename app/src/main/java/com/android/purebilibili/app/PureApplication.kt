// 文件路径: app/PureApplication.kt
package com.android.purebilibili.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.profileinstaller.ProfileInstaller
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.android.purebilibili.core.lifecycle.BackgroundManager
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.store.allManagedAppIconLauncherAliases
import com.android.purebilibili.core.store.normalizeAppIconKey
import com.android.purebilibili.core.store.resolveAppIconLauncherAlias
import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.feature.plugin.AdFilterPlugin
import com.android.purebilibili.feature.plugin.DanmakuEnhancePlugin
import com.android.purebilibili.feature.plugin.EyeProtectionPlugin
import com.android.purebilibili.feature.plugin.SponsorBlockPlugin
import com.android.purebilibili.feature.plugin.TodayWatchPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

private const val TAG = "PureApplication"

internal fun shouldBlockStartupForHomeVisualDefaultsMigration(): Boolean = false
internal fun shouldDeferPlaylistRestoreAtStartup(): Boolean = true
internal fun shouldDeferTelemetryInitAtStartup(): Boolean = true
internal fun deferredNonCriticalStartupDelayMs(): Long = 900L
internal fun shouldRequestDex2OatProfileInstall(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.N
internal fun dex2OatProfileInstallDelayMs(): Long = 2_500L
internal fun shouldClearImageMemoryCacheOnTrimLevel(level: Int): Boolean {
    return when (level) {
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
        ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> true
        else -> false
    }
}

//  实现 ImageLoaderFactory 以提供自定义 Coil 配置
//  实现 ComponentCallbacks2 响应系统内存警告
class PureApplication : Application(), ImageLoaderFactory, ComponentCallbacks2 {
    
    //  保存 ImageLoader 引用以便在 onTrimMemory 中使用
    private var _imageLoader: ImageLoader? = null

    private val telemetryListener = object : BackgroundManager.BackgroundStateListener {
        override fun onEnterBackground() {
            AnalyticsHelper.onAppBackground()
            CrashReporter.setAppForegroundState(false)
        }

        override fun onEnterForeground() {
            AnalyticsHelper.onAppForeground()
            CrashReporter.setAppForegroundState(true)
        }
    }
    
    //  Coil 图片加载器 - 优化内存和磁盘缓存
    override fun newImageLoader(): ImageLoader {
        val memoryCachePercent = 0.30
        val diskCacheBytes = 150L * 1024 * 1024
        return ImageLoader.Builder(this)
            //  内存缓存预算（移动/平板主仓）
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .build()
            }
            //  磁盘缓存预算（移动/平板主仓）
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(diskCacheBytes)
                    .build()
            }
            .okHttpClient { NetworkModule.okHttpClient } // 🔥 [Fix] 共享 OkHttpClient 以获得 DNS 修复
            //  优先使用缓存
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            //  启用 Bitmap 复用减少内存分配
            .allowRgb565(true)
            .crossfade(true)
            .build()
            .also { _imageLoader = it }  // 保存引用
    }
    
    override fun onCreate() {
        //  [关键] 必须在 super.onCreate() 之前设置！
        // 这样系统在初始化时就能读取到正确的夜间模式配置
        applyThemePreference()
        
        super.onCreate()

        // 启动即确保首页视觉默认值生效：底栏悬浮 + 液态玻璃 + 顶部模糊
        // 冷启动路径不阻塞主线程，迁移改为后台执行。
        if (shouldBlockStartupForHomeVisualDefaultsMigration()) {
            runBlocking(Dispatchers.IO) {
                SettingsManager.ensureHomeVisualDefaults(this@PureApplication)
            }
        } else {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                SettingsManager.ensureHomeVisualDefaults(this@PureApplication)
            }
        }
        
        //  关键初始化（同步，必须在启动时完成）
        NetworkModule.init(this)
        TokenManager.init(this)
        com.android.purebilibili.data.repository.VideoRepository.init(this) //  [新增] 初始化 VideoRepo
        BackgroundManager.init(this)  // 📱 后台状态管理
        com.android.purebilibili.core.store.PlayerSettingsCache.init(this) // 🎬 [新增] 播放器设置缓存
        initPlaylistRestore() // 🎵 [优化] 播放队列恢复可延后，避免阻塞冷启动主线程
        
        createNotificationChannel()
        
        initTelemetry() // [优化] 埋点初始化支持延后到首屏阶段之后
        scheduleDex2OatProfileInstall()
        
        //  [冷启动优化] 延迟非关键初始化到主线程空闲时 (IdleHandler 确保首帧绘制后再执行)
        Looper.myQueue().addIdleHandler {
            // [Moved] 插件系统初始化
            PluginManager.initialize(this)
            PluginManager.register(SponsorBlockPlugin())
            PluginManager.register(AdFilterPlugin())
            PluginManager.register(DanmakuEnhancePlugin())
            PluginManager.register(EyeProtectionPlugin())
            PluginManager.register(TodayWatchPlugin())
            Logger.d(TAG, " Plugin system initialized with 5 built-in plugins")

            // [Moved] JSON 规则插件系统初始化
            com.android.purebilibili.core.plugin.json.JsonPluginManager.initialize(this)
            Logger.d(TAG, " JSON plugin system initialized")
            
            // [Moved] 下载管理器 initialization (IO heavy)
            com.android.purebilibili.feature.download.DownloadManager.init(this)
            
            // [Moved] 同步配置
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                val sponsorBlockEnabled = com.android.purebilibili.core.store.SettingsManager
                    .getSponsorBlockEnabled(this@PureApplication)
                    .first()
                PluginManager.setEnabled("sponsor_block", sponsorBlockEnabled)
                Logger.d(TAG, " SponsorBlock plugin synced: enabled=$sponsorBlockEnabled")
                
                SettingsManager.forceDanmakuDefaults(this@PureApplication)
            }

            //  恢复 WBI 密钥缓存
            WbiKeyManager.restoreFromStorage(this)
            
            //  同步应用图标状态（确保只有一个图标在桌面显示）
            syncAppIconState()
            
            //  异步预热 WBI Keys，减少首次视频加载延迟
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    WbiKeyManager.getWbiKeys()
                    Logger.d(TAG, " WBI Keys preloaded successfully")
                } catch (e: Exception) {
                    android.util.Log.w(TAG, " WBI Keys preload failed: ${e.message}")
                }
            }
            
            false // 返回 false 表示只执行一次
        }
    }

    private fun initPlaylistRestore() {
        if (shouldDeferPlaylistRestoreAtStartup()) {
            Handler(Looper.getMainLooper()).postDelayed({
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    com.android.purebilibili.feature.video.player.PlaylistManager.init(this@PureApplication)
                }
            }, deferredNonCriticalStartupDelayMs())
            return
        }
        com.android.purebilibili.feature.video.player.PlaylistManager.init(this)
    }

    private fun initTelemetry() {
        if (shouldDeferTelemetryInitAtStartup()) {
            Handler(Looper.getMainLooper()).postDelayed({
                initCrashlytics()
                initAnalytics()
                attachTelemetryListener()
            }, deferredNonCriticalStartupDelayMs())
            return
        }
        initCrashlytics()
        initAnalytics()
        attachTelemetryListener()
    }

    private fun scheduleDex2OatProfileInstall() {
        if (!shouldRequestDex2OatProfileInstall(Build.VERSION.SDK_INT)) return
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                ProfileInstaller.writeProfile(this)
            }.onSuccess {
                Logger.d(TAG, "📦 Requested ART profile installation for dex2oat")
            }.onFailure { throwable ->
                Logger.w(TAG, "⚠️ ART profile installation request failed", throwable)
            }
        }, dex2OatProfileInstallDelayMs())
    }

    private fun attachTelemetryListener() {
        // 监听全局前后台状态，增强会话与崩溃上下文
        BackgroundManager.addListener(telemetryListener)
        if (!BackgroundManager.isInBackground) {
            AnalyticsHelper.onAppForeground()
            CrashReporter.setAppForegroundState(true)
        }
    }
    
    //  初始化 Firebase Crashlytics
    private fun initCrashlytics() {
        try {
            //  读取用户设置（默认开启）
            val prefs = getSharedPreferences("crash_tracking", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("enabled", true)  // 默认开启
            
            CrashReporter.init(this)
            CrashReporter.installGlobalExceptionHandler()
            CrashReporter.setEnabled(enabled)
            
            if (enabled) {
                CrashReporter.syncUserContext(
                    mid = TokenManager.midCache,
                    isVip = TokenManager.isVipCache,
                    privacyModeEnabled = SettingsManager.isPrivacyModeEnabledSync(this)
                )
            }
            
            Logger.d(TAG, " Firebase Crashlytics initialized (enabled=$enabled)")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to init Crashlytics", e)
        }
    }
    
    // � 初始化 Firebase Analytics
    private fun initAnalytics() {
        try {
            // 初始化 AnalyticsHelper
            AnalyticsHelper.init(this)
            
            //  读取用户设置（默认开启）
            val prefs = getSharedPreferences("analytics_tracking", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("enabled", true)  // 默认开启
            
            //  根据用户设置启用/禁用 Analytics
            AnalyticsHelper.setEnabled(enabled)
            
            if (enabled) {
                AnalyticsHelper.syncUserContext(
                    mid = TokenManager.midCache,
                    isVip = TokenManager.isVipCache,
                    privacyModeEnabled = SettingsManager.isPrivacyModeEnabledSync(this)
                )
                // 记录应用打开事件
                AnalyticsHelper.logAppOpen()
            }
            
            Logger.d(TAG, " Firebase Analytics initialized (enabled=$enabled)")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to init Analytics", e)
        }
    }
    
    // � [后台内存优化] 响应系统内存警告
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (shouldClearImageMemoryCacheOnTrimLevel(level)) {
            _imageLoader?.memoryCache?.clear()
            if (level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
                Logger.d(TAG, "🚨 TRIM_MEMORY_COMPLETE, released image memory cache")
            } else {
                System.gc()
                Logger.d(TAG, " Low memory trim(level=$level), cleared image memory cache")
            }
        } else if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            Logger.d(TAG, " UI hidden, keep image memory cache for faster resume")
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        _imageLoader?.memoryCache?.clear()
        Logger.d(TAG, "🚨 onLowMemory, cleared all caches")
    }

    private fun createNotificationChannel() {
        // 仅在 Android 8.0 (API 26) 及以上需要通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "media_playback_channel" // 这个 ID 需要保持固定
            val channelName = "媒体播放"
            val channelDescription = "显示正在播放的视频控制条"

            // 重要：媒体通知的优先级通常设为 LOW
            // 这样可以显示在状态栏和下拉栏，但不会发出提示音打断视频声音
            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                setShowBadge(false) // 媒体通知通常不需要在图标上显示角标
                setSound(null, null) // 关键：设为静音，防止切歌时发出系统提示音
            }

            // 向系统注册渠道
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     *  应用主题偏好 - 在 Splash Screen 显示前调用
     * 
     * 这解决了：用户在应用内强制深色模式，但系统是浅色时，启动屏仍然是白色的问题。
     * 通过 AppCompatDelegate.setDefaultNightMode() 强制系统使用正确的深色/浅色模式。
     */
    private fun applyThemePreference() {
        // 同步读取保存的主题设置（必须同步，因为 Splash Screen 马上就会显示）
        val prefs = getSharedPreferences("theme_cache", Context.MODE_PRIVATE)
        val themeModeValue = prefs.getInt("theme_mode", 0)  // 0 = FOLLOW_SYSTEM
        
        val nightMode = when (themeModeValue) {
            0 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM  // 跟随系统
            1 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO             // 浅色
            2 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES            // 深色
            else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
        Logger.d(TAG, " Applied theme mode: $themeModeValue -> nightMode=$nightMode")
    }
    
    /**
     *  同步应用图标状态
     * 
     * 在 Application.onCreate 时调用，确保启动器图标与用户偏好一致。
     * 
     * 修复：重装后检测 icon 偏好与 Manifest 默认状态冲突，自动重置为默认图标。
     */
    private fun syncAppIconState() {
        // [Optim] Use IO dispatcher to prevent ANR during startup (PackageManager is heavy)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pm = packageManager
                val packageName = this@PureApplication.packageName
                val compatAlias = android.content.ComponentName(packageName, "${packageName}.MainActivityAlias3D")
                
                // 读取用户保存的图标偏好
                val currentIcon = normalizeAppIconKey(
                    SettingsManager.getAppIcon(this@PureApplication).first()
                )
                val cacheSynced = this@PureApplication
                    .getSharedPreferences("app_icon_cache", Context.MODE_PRIVATE)
                    .edit()
                    .putString("current_icon", currentIcon)
                    .commit()
                Logger.d(TAG, " Synced app icon cache from DataStore: $currentIcon (success=$cacheSynced)")

                val allUniqueAliases = allManagedAppIconLauncherAliases(packageName)
                val targetAlias = resolveAppIconLauncherAlias(packageName, currentIcon)
                
                val targetAliasComponent = android.content.ComponentName(packageName, targetAlias)
                val targetState = pm.getComponentEnabledSetting(targetAliasComponent)

                // 保留兼容入口（无 Launcher 图标），确保旧 IDE 运行配置可用
                pm.setComponentEnabledSetting(
                    compatAlias,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                
                // 如果目标alias是disabled（说明之前被禁用了，可能是重装），强制重置为默认(icon_3d)
                if (currentIcon != "icon_3d" && targetState == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    Logger.d(TAG, " Detected reinstall: target icon '$currentIcon' is disabled, resetting to 'icon_3d'")
                    
                    SettingsManager.setAppIcon(this@PureApplication, "icon_3d")
                    
                    // 确保 3D 图标被启用
                    val aliasDefault = android.content.ComponentName(packageName, "${packageName}.MainActivityAlias3DLauncher")
                    pm.setComponentEnabledSetting(
                        aliasDefault,
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                    // 禁用其他所有alias
                    allUniqueAliases.filter { it != "${packageName}.MainActivityAlias3DLauncher" }.forEach { aliasFullName ->
                        pm.setComponentEnabledSetting(
                            android.content.ComponentName(packageName, aliasFullName),
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            android.content.pm.PackageManager.DONT_KILL_APP
                        )
                    }
                    Logger.d(TAG, " Reset to default 3D icon")
                    return@launch
                }
                
                // 同步所有 alias 状态：只有目标启用，其他禁用
                allUniqueAliases.forEach { aliasFullName ->
                    try {
                        val currentState = pm.getComponentEnabledSetting(
                            android.content.ComponentName(packageName, aliasFullName)
                        )
                        val shouldBeEnabled = aliasFullName == targetAlias
                        val targetState = if (shouldBeEnabled) {
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        } else {
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        }
                        
                        // 只在状态不一致时修改，减少不必要的操作
                        if (currentState != targetState) {
                            pm.setComponentEnabledSetting(
                                android.content.ComponentName(packageName, aliasFullName),
                                targetState,
                                android.content.pm.PackageManager.DONT_KILL_APP
                            )
                        }
                    } catch (e: Exception) {
                        //  [容错] 忽略不存在的组件，防止崩溃
                        Logger.d(TAG, "⚠️ Component $aliasFullName not found, skipping")
                    }
                }
                
                Logger.d(TAG, " Synced app icon state: $currentIcon")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to sync app icon state", e)
            }
        }
    }
}
