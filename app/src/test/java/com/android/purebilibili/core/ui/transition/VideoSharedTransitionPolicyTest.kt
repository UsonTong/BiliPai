package com.android.purebilibili.core.ui.transition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoSharedTransitionPolicyTest {

    @Test
    fun coverSharedTransition_enabled_whenTransitionAndScopesAreReady() {
        assertTrue(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = true
            )
        )
    }

    @Test
    fun metadataSharedTransition_enabled_byDefault_forUnifiedVideoReturn() {
        assertEquals(VideoSharedTransitionProfile.COVER_AND_METADATA, resolveVideoSharedTransitionProfile())
        assertTrue(
            shouldEnableVideoMetadataSharedTransition(
                coverSharedEnabled = true,
                isQuickReturnLimited = false
            )
        )
    }

    @Test
    fun metadataSharedTransition_staysEnabled_evenWhenQuickReturnLimited() {
        assertTrue(
            shouldEnableVideoMetadataSharedTransition(
                coverSharedEnabled = true,
                isQuickReturnLimited = true
            )
        )
    }

    @Test
    fun sharedCoverAspectRatio_defaultsToHomeCardSixteenByTen() {
        assertEquals(1.6f, VIDEO_SHARED_COVER_ASPECT_RATIO, 0.0001f)
    }

    @Test
    fun coverBoundsMotion_usesDedicatedForwardAndReturnSprings() {
        val forward = resolveVideoSharedBoundsSpringConfig(VideoSharedBoundsMotionRole.COVER_FORWARD)
        val returned = resolveVideoSharedBoundsSpringConfig(VideoSharedBoundsMotionRole.COVER_RETURN)

        assertEquals(0.86f, forward.dampingRatio, 0.0001f)
        assertEquals(430f, forward.stiffness, 0.0001f)
        assertEquals(0.94f, returned.dampingRatio, 0.0001f)
        assertEquals(560f, returned.stiffness, 0.0001f)
    }

    @Test
    fun metadataBoundsMotion_isFasterAndMoreStableThanCoverReturn() {
        val returned = resolveVideoSharedBoundsSpringConfig(VideoSharedBoundsMotionRole.COVER_RETURN)
        val metadata = resolveVideoSharedBoundsSpringConfig(VideoSharedBoundsMotionRole.METADATA_FOLLOW)

        assertTrue(metadata.dampingRatio > returned.dampingRatio)
        assertTrue(metadata.stiffness > returned.stiffness)
    }

    @Test
    fun coverBoundsRole_usesReturnSpringWhenTargetIsSmaller() {
        val role = resolveVideoCoverSharedBoundsMotionRole(
            initialBounds = androidx.compose.ui.geometry.Rect(0f, 0f, 1080f, 680f),
            targetBounds = androidx.compose.ui.geometry.Rect(20f, 200f, 520f, 512f)
        )

        assertEquals(VideoSharedBoundsMotionRole.COVER_RETURN, role)
    }
}
