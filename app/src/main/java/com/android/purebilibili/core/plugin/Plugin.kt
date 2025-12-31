// 文件路径: core/plugin/Plugin.kt
package com.android.purebilibili.core.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 🔌 BiliPai 插件基础接口
 * 
 * 所有插件必须实现此接口，定义插件的基本属性和生命周期方法。
 */
interface Plugin {
    /** 唯一标识符，如 "sponsorblock" */
    val id: String
    
    /** 显示名称，如 "空降助手" */
    val name: String
    
    /** 插件描述 */
    val description: String
    
    /** 版本号，如 "1.0.0" */
    val version: String
    
    /** 🆕 插件作者 */
    val author: String
        get() = "Unknown"
    
    /** 插件图标 (可选) */
    val icon: ImageVector?
        get() = null
    
    /** 🔥 是否暂不可用 (用于标识功能尚未完成) */
    val unavailable: Boolean
        get() = false
    
    /** 🔥 不可用原因描述 */
    val unavailableReason: String
        get() = "功能开发中"
    
    /**
     * 插件启用时调用
     */
    suspend fun onEnable() {}
    
    /**
     * 插件禁用时调用
     */
    suspend fun onDisable() {}
    
    /**
     * 插件配置界面 (可选)
     * 返回 null 表示无配置项
     */
    @Composable
    fun SettingsContent(): Unit = Unit
}
