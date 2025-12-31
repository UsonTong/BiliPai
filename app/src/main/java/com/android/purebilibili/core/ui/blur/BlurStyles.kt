// 文件路径: core/ui/blur/BlurStyles.kt
package com.android.purebilibili.core.ui.blur

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * 🔥🔥 模糊强度枚举
 * 用户可选的三种模糊强度
 */
enum class BlurIntensity {
    THIN,        // 标准 - 平衡美观与性能（默认）
    THICK,       // 浓郁 - 强烈磨砂质感
    APPLE_DOCK   // 🔥 玻璃拟态风格 - 高饱和度 + 精细模糊
}

/**
 * 🎨 模糊样式管理
 * 
 * 模糊 + 饱和度增强 + 半透明底色 + 顶部高光 + 精细边框
 */
object BlurStyles {
    
    @OptIn(ExperimentalHazeMaterialsApi::class)
    @Composable
    fun getBlurStyle(intensity: BlurIntensity): HazeStyle {
        return when (intensity) {
            BlurIntensity.THIN -> HazeMaterials.thin()             // 标准 - 轻度模糊
            BlurIntensity.APPLE_DOCK -> HazeMaterials.ultraThin()  // 🔥 玻璃拟态 - 最强模糊 + 背景透色
            BlurIntensity.THICK -> HazeMaterials.thick()           // 🔥 浓郁 - 最强模糊，完全遮盖
        }
    }
    
    /**
     * 🔥🔥 获取不同模糊强度对应的背景透明度
     * - 玻璃拟态: 极低透明度（0.15）让背景颜色透出
     * - 浓郁: 高透明度（0.6）遮盖背景颜色
     * - 标准: 中等透明度
     */
    fun getBackgroundAlpha(intensity: BlurIntensity): Float {
        return when (intensity) {
            BlurIntensity.THIN -> 0.4f         // 标准 - 中等
            BlurIntensity.APPLE_DOCK -> 0.15f  // 🔥 玻璃拟态 - 极低，背景透出
            BlurIntensity.THICK -> 0.6f        // 🔥 浓郁 - 高透明度，遮盖背景
        }
    }
    
    /**
     * 🍎 玻璃拟态风格模糊
     * 
     * 特点：
     * - 高模糊半径（看不清背后内容）
     * - 饱和度增强
     * - 半透明底色提升可读性
     */
    @Composable
    private fun createAppleDockStyle(): HazeStyle {
        val surfaceColor = MaterialTheme.colorScheme.surface
        val isDark = surfaceColor.red < 0.5f
        
        return HazeStyle(
            blurRadius = 40.dp,  // 🔥🔥 大幅提高模糊半径（从 24dp → 40dp）
            backgroundColor = if (isDark) {
                Color.Black.copy(alpha = 0.6f)   // 🔥 提高不透明度
            } else {
                Color.White.copy(alpha = 0.7f)   // 🔥 提高不透明度
            },
            tint = HazeTint(
                color = surfaceColor.copy(alpha = if (isDark) 0.7f else 0.8f)  // 🔥 增强 tint
            )
        )
    }
}
