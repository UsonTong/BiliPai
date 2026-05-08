package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.store.normalizeLiquidGlassProgress
import com.android.purebilibili.core.store.normalizeLiquidGlassStrength
import com.android.purebilibili.core.store.resolveLegacyLiquidGlassProgress

internal const val PREDICTIVE_BACK_TOGGLE_TITLE = "预测返回兼容动画"
internal const val PREDICTIVE_BACK_TOGGLE_ACTIVE_SUBTITLE =
    "当前使用预测返回兼容的稳定路由动画，系统预测返回入口保持关闭"
internal const val PREDICTIVE_BACK_TOGGLE_INACTIVE_SUBTITLE =
    "当前使用经典回退动画，不启用预测返回兼容动效"
internal const val PREDICTIVE_BACK_TOGGLE_DEPENDENCY_SUBTITLE =
    "需先开启“过渡动画”后，才能启用预测返回兼容动效"

internal data class PredictiveBackToggleUiState(
    val title: String,
    val enabled: Boolean,
    val checked: Boolean,
    val subtitle: String
)

internal data class LiquidGlassPreviewUiState(
    val modeLabel: String,
    val subtitle: String,
    val normalizedProgress: Float,
    val strengthLabel: String
)

internal fun resolvePredictiveBackToggleUiState(
    cardTransitionEnabled: Boolean,
    predictiveBackAnimationEnabled: Boolean
): PredictiveBackToggleUiState {
    if (!cardTransitionEnabled) {
        return PredictiveBackToggleUiState(
            title = PREDICTIVE_BACK_TOGGLE_TITLE,
            enabled = false,
            checked = false,
            subtitle = PREDICTIVE_BACK_TOGGLE_DEPENDENCY_SUBTITLE
        )
    }
    return PredictiveBackToggleUiState(
        title = PREDICTIVE_BACK_TOGGLE_TITLE,
        enabled = true,
        checked = predictiveBackAnimationEnabled,
        subtitle = if (predictiveBackAnimationEnabled) {
            PREDICTIVE_BACK_TOGGLE_ACTIVE_SUBTITLE
        } else {
            PREDICTIVE_BACK_TOGGLE_INACTIVE_SUBTITLE
        }
    )
}

internal fun resolveLiquidGlassPreviewUiState(
    progress: Float
): LiquidGlassPreviewUiState {
    val normalizedProgress = normalizeLiquidGlassProgress(progress)
    val (modeLabel, subtitle) = when {
        normalizedProgress < 0.34f -> "通透" to "更清晰、更通透，折射更明显"
        normalizedProgress < 0.68f -> "柔化" to "开始柔化背景，但仍保留液态折射"
        else -> "磨砂" to "更柔和、更雾化，适合弱化背景干扰"
    }
    return LiquidGlassPreviewUiState(
        modeLabel = modeLabel,
        subtitle = subtitle,
        normalizedProgress = normalizedProgress,
        strengthLabel = "${(normalizedProgress * 100).toInt()}%"
    )
}

internal fun resolveLiquidGlassPreviewUiState(
    mode: LiquidGlassMode,
    strength: Float
): LiquidGlassPreviewUiState {
    return resolveLiquidGlassPreviewUiState(
        progress = resolveLegacyLiquidGlassProgress(
            mode = mode,
            strength = normalizeLiquidGlassStrength(strength)
        )
    )
}
