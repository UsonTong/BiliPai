package com.android.purebilibili.feature.settings

internal const val PREDICTIVE_BACK_TOGGLE_ACTIVE_SUBTITLE =
    "关闭后改用经典回退动效，减少系统手势冲突"
internal const val PREDICTIVE_BACK_TOGGLE_DEPENDENCY_SUBTITLE =
    "需先开启“过渡动画”后才能生效"

internal data class PredictiveBackToggleUiState(
    val enabled: Boolean,
    val checked: Boolean,
    val subtitle: String
)

internal fun resolvePredictiveBackToggleUiState(
    cardTransitionEnabled: Boolean,
    predictiveBackAnimationEnabled: Boolean
): PredictiveBackToggleUiState {
    if (!cardTransitionEnabled) {
        return PredictiveBackToggleUiState(
            enabled = false,
            checked = false,
            subtitle = PREDICTIVE_BACK_TOGGLE_DEPENDENCY_SUBTITLE
        )
    }
    return PredictiveBackToggleUiState(
        enabled = true,
        checked = predictiveBackAnimationEnabled,
        subtitle = PREDICTIVE_BACK_TOGGLE_ACTIVE_SUBTITLE
    )
}
