package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.store.normalizeLiquidGlassStrength

internal const val PREDICTIVE_BACK_TOGGLE_TITLE = "启用预测性返回预览"
internal const val PREDICTIVE_BACK_TOGGLE_ACTIVE_SUBTITLE =
    "当前使用系统原生返回预览，关闭后改用经典回退动画"
internal const val PREDICTIVE_BACK_TOGGLE_INACTIVE_SUBTITLE =
    "当前使用经典回退动画，开启后跟随系统返回预览"
internal const val PREDICTIVE_BACK_TOGGLE_DEPENDENCY_SUBTITLE =
    "需先开启“过渡动画”后，才能启用返回预览效果"

internal data class PredictiveBackToggleUiState(
    val title: String,
    val enabled: Boolean,
    val checked: Boolean,
    val subtitle: String
)

internal data class LiquidGlassPreviewUiState(
    val modeLabel: String,
    val subtitle: String,
    val normalizedStrength: Float,
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
    mode: LiquidGlassMode,
    strength: Float
): LiquidGlassPreviewUiState {
    val normalizedStrength = normalizeLiquidGlassStrength(strength)
    val subtitle = when (mode) {
        LiquidGlassMode.CLEAR -> "更清晰、更通透，折射更克制"
        LiquidGlassMode.BALANCED -> "通透与柔化平衡，适合作为默认效果"
        LiquidGlassMode.FROSTED -> "更柔和、更雾化，适合弱化背景干扰"
    }
    return LiquidGlassPreviewUiState(
        modeLabel = mode.label,
        subtitle = subtitle,
        normalizedStrength = normalizedStrength,
        strengthLabel = "${(normalizedStrength * 100).toInt()}%"
    )
}
