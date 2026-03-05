package com.android.purebilibili.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.navigation.NavBackStackEntry
import com.android.purebilibili.core.util.CardPositionManager

internal val IOS_RETURN_EASING = CubicBezierEasing(0.20f, 0.90f, 0.22f, 1.00f)

internal fun VideoPopExitDirection.toSlideDirection(): AnimatedContentTransitionScope.SlideDirection {
    return when (this) {
        VideoPopExitDirection.LEFT -> AnimatedContentTransitionScope.SlideDirection.Left
        VideoPopExitDirection.RIGHT -> AnimatedContentTransitionScope.SlideDirection.Right
        VideoPopExitDirection.DOWN -> AnimatedContentTransitionScope.SlideDirection.Down
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.resolveVideoCardReturnPopEnterTransition(
    targetRoute: String,
    cardTransitionEnabled: Boolean,
    predictiveBackAnimationEnabled: Boolean,
    isTabletLayout: Boolean,
    navMotionSpec: AppNavigationMotionSpec,
    allowNoOpSharedElement: Boolean = false
): EnterTransition {
    val sharedTransitionReady =
        CardPositionManager.lastClickedCardBounds != null &&
            CardPositionManager.isCardFullyVisible
    return when (
        resolveVideoCardReturnEnterAction(
            fromRoute = initialState.destination.route,
            targetRoute = targetRoute,
            cardTransitionEnabled = cardTransitionEnabled,
            predictiveBackAnimationEnabled = predictiveBackAnimationEnabled,
            isQuickReturnFromDetail = CardPositionManager.isQuickReturnFromDetail,
            sharedTransitionReady = sharedTransitionReady,
            isTabletLayout = isTabletLayout,
            allowNoOpSharedElement = allowNoOpSharedElement
        )
    ) {
        VideoCardReturnEnterAction.NO_OP -> EnterTransition.None
        VideoCardReturnEnterAction.SOFT_FADE -> {
            fadeIn(
                animationSpec = tween(
                    durationMillis = navMotionSpec.mediumFadeDurationMillis,
                    easing = IOS_RETURN_EASING
                ),
                initialAlpha = 0.98f
            )
        }
        VideoCardReturnEnterAction.SEAMLESS_FADE -> {
            fadeIn(
                animationSpec = tween(
                    durationMillis = navMotionSpec.mediumFadeDurationMillis,
                    easing = IOS_RETURN_EASING
                ),
                initialAlpha = 0.96f
            )
        }
        VideoCardReturnEnterAction.RIGHT_SLIDE,
        null -> slideEnterRight(navMotionSpec)
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.resolveSettingsExitTransition(
    linkedSettingsBackMotion: Boolean,
    navMotionSpec: AppNavigationMotionSpec
): ExitTransition {
    return if (linkedSettingsBackMotion) {
        slideExitLeft(navMotionSpec)
    } else {
        ExitTransition.None
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.resolveSettingsPopEnterTransition(
    linkedSettingsBackMotion: Boolean,
    navMotionSpec: AppNavigationMotionSpec
): EnterTransition {
    return if (linkedSettingsBackMotion) {
        slideEnterRight(navMotionSpec)
    } else {
        EnterTransition.None
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideEnterLeft(
    navMotionSpec: AppNavigationMotionSpec
): EnterTransition {
    return slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        tween(navMotionSpec.slideDurationMillis)
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideExitLeft(
    navMotionSpec: AppNavigationMotionSpec
): ExitTransition {
    return slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        tween(navMotionSpec.slideDurationMillis)
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideEnterRight(
    navMotionSpec: AppNavigationMotionSpec
): EnterTransition {
    return slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        tween(navMotionSpec.slideDurationMillis)
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideExitRight(
    navMotionSpec: AppNavigationMotionSpec
): ExitTransition {
    return slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        tween(navMotionSpec.slideDurationMillis)
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideEnterUp(
    navMotionSpec: AppNavigationMotionSpec
): EnterTransition {
    return slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Up,
        tween(navMotionSpec.slideDurationMillis)
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideExitDown(
    navMotionSpec: AppNavigationMotionSpec
): ExitTransition {
    return slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Down,
        tween(navMotionSpec.slideDurationMillis)
    )
}
