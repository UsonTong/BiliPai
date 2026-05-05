package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.core.ui.motion.MotionSpringConfig

internal enum class VideoSharedTransitionProfile {
    COVER_ONLY,
    COVER_AND_METADATA
}

internal enum class VideoSharedBoundsMotionRole {
    COVER_FORWARD,
    COVER_RETURN,
    METADATA_FOLLOW
}

internal const val VIDEO_SHARED_COVER_ASPECT_RATIO = 16f / 10f

internal fun resolveVideoSharedTransitionProfile(): VideoSharedTransitionProfile {
    // Prefer a unified return choreography: cover + key metadata move back together.
    return VideoSharedTransitionProfile.COVER_AND_METADATA
}

internal fun shouldEnableVideoCoverSharedTransition(
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    return transitionEnabled &&
        hasSharedTransitionScope &&
        hasAnimatedVisibilityScope
}

internal fun shouldEnableVideoMetadataSharedTransition(
    coverSharedEnabled: Boolean,
    isQuickReturnLimited: Boolean,
    profile: VideoSharedTransitionProfile = resolveVideoSharedTransitionProfile()
): Boolean {
    if (!coverSharedEnabled) return false
    // Keep metadata linked during quick return to avoid cover-only snapback.
    if (isQuickReturnLimited && profile == VideoSharedTransitionProfile.COVER_ONLY) return false
    return profile == VideoSharedTransitionProfile.COVER_AND_METADATA
}

internal fun resolveVideoSharedBoundsSpringConfig(
    role: VideoSharedBoundsMotionRole
): MotionSpringConfig {
    return when (role) {
        VideoSharedBoundsMotionRole.COVER_FORWARD -> MotionSpringConfig(
            dampingRatio = 0.86f,
            stiffness = 430f
        )
        VideoSharedBoundsMotionRole.COVER_RETURN -> MotionSpringConfig(
            dampingRatio = 0.94f,
            stiffness = 560f
        )
        VideoSharedBoundsMotionRole.METADATA_FOLLOW -> MotionSpringConfig(
            dampingRatio = 0.98f,
            stiffness = 680f
        )
    }
}

internal fun resolveVideoCoverSharedBoundsMotionRole(
    initialBounds: Rect,
    targetBounds: Rect
): VideoSharedBoundsMotionRole {
    return if (targetBounds.area() < initialBounds.area()) {
        VideoSharedBoundsMotionRole.COVER_RETURN
    } else {
        VideoSharedBoundsMotionRole.COVER_FORWARD
    }
}

internal fun resolveVideoCoverSharedBoundsTransformSpec(
    initialBounds: Rect,
    targetBounds: Rect
): SpringSpec<Rect> {
    return resolveVideoSharedBoundsSpringConfig(
        resolveVideoCoverSharedBoundsMotionRole(
            initialBounds = initialBounds,
            targetBounds = targetBounds
        )
    ).toRectSpringSpec()
}

internal fun resolveVideoMetadataSharedBoundsTransformSpec(): SpringSpec<Rect> {
    return resolveVideoSharedBoundsSpringConfig(
        VideoSharedBoundsMotionRole.METADATA_FOLLOW
    ).toRectSpringSpec()
}

internal fun resolveVideoReturnCoverSharedBoundsTransformSpec(): SpringSpec<Rect> {
    return resolveVideoSharedBoundsSpringConfig(
        VideoSharedBoundsMotionRole.COVER_RETURN
    ).toRectSpringSpec()
}

private fun Rect.area(): Float {
    return (width * height).coerceAtLeast(0f)
}

private fun MotionSpringConfig.toRectSpringSpec(): SpringSpec<Rect> {
    return spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )
}
