package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class TopTabMotionVelocityTest {

    @Test
    fun `horizontal only when liquid glass disabled`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 1200f
        )

        assertEquals(1200f, velocity, 0.001f)
    }

    @Test
    fun `vertical does not contribute when liquid glass enabled`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 1000f
        )

        assertEquals(1000f, velocity, 0.001f)
    }

    @Test
    fun `result is clamped to avoid excessive distortion`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 5000f
        )

        assertEquals(4200f, velocity, 0.001f)
    }

    @Test
    fun `vertical motion alone does not mark interacting when liquid glass enabled`() {
        val interacting = shouldTopTabIndicatorBeInteracting(
            pagerIsScrolling = false,
            combinedVelocityPxPerSecond = 10f,
            liquidGlassEnabled = true
        )

        assertEquals(false, interacting)
    }

    @Test
    fun `vertical motion ignored when liquid glass disabled`() {
        val interacting = shouldTopTabIndicatorBeInteracting(
            pagerIsScrolling = false,
            combinedVelocityPxPerSecond = 10f,
            liquidGlassEnabled = false
        )

        assertEquals(false, interacting)
    }

    @Test
    fun `liquid glass top tab keeps enlarged interaction briefly after pager stops`() {
        assertEquals(
            140L,
            resolveTopTabIndicatorInteractionReleaseDelayMillis(liquidGlassEnabled = true)
        )
        assertEquals(
            0L,
            resolveTopTabIndicatorInteractionReleaseDelayMillis(liquidGlassEnabled = false)
        )
    }

    @Test
    fun `tiny pager jitter is ignored by horizontal delta resolver`() {
        val delta = resolveTopTabHorizontalDeltaPx(
            positionDeltaPages = 0.0008f,
            tabWidthPx = 92f
        )

        assertEquals(0f, delta, 0.0001f)
    }

    @Test
    fun `meaningful page movement produces horizontal delta`() {
        val delta = resolveTopTabHorizontalDeltaPx(
            positionDeltaPages = 0.25f,
            tabWidthPx = 100f
        )

        assertEquals(25f, delta, 0.0001f)
    }

    @Test
    fun `viewport shift uses first visible item index and offset`() {
        val shift = resolveTopTabIndicatorViewportShiftPx(
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffsetPx = 24,
            tabWidthPx = 92f
        )

        assertEquals(208f, shift, 0.0001f)
    }

    @Test
    fun `viewport shift returns zero for invalid width`() {
        val shift = resolveTopTabIndicatorViewportShiftPx(
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffsetPx = 24,
            tabWidthPx = 0f
        )

        assertEquals(0f, shift, 0.0001f)
    }

    @Test
    fun `indicator clamp shift follows manual top tab row scroll`() {
        val shift = resolveTopTabIndicatorViewportClampShiftPx(
            rowScrollOffsetPx = 240f,
            indicatorPanelOffsetPx = 8f
        )

        assertEquals(232f, shift, 0.0001f)
    }

    @Test
    fun `static top tab indicator policy keeps neutral color without motion effects`() {
        val policy = resolveTopTabStaticIndicatorVisualPolicy(useNeutralIndicatorTint = true)

        assertEquals(false, policy.isInMotion)
        assertEquals(false, policy.shouldRefract)
        assertEquals(true, policy.useNeutralTint)
    }

    @Test
    fun `top tab neutral indicator color stays muted over wallpaper`() {
        assertEquals(
            Color(0xFFEAF2EF).copy(alpha = 0.42f),
            resolveTopTabNeutralIndicatorColor(isDarkTheme = false, alpha = 0.42f)
        )
        assertEquals(
            Color(0xFFE1E8E5).copy(alpha = 0.38f),
            resolveTopTabNeutralIndicatorColor(isDarkTheme = true, alpha = 0.38f)
        )
    }

    @Test
    fun `top tab neutral indicator alpha avoids bottom bar opacity floor`() {
        assertEquals(
            0.42f,
            resolveTopTabNeutralIndicatorTintAlpha(isDarkTheme = false, configuredAlpha = 0.16f),
            0.001f
        )
        assertEquals(
            0.38f,
            resolveTopTabNeutralIndicatorTintAlpha(isDarkTheme = true, configuredAlpha = 0.16f),
            0.001f
        )
        assertEquals(
            0.72f,
            resolveTopTabNeutralIndicatorTintAlpha(isDarkTheme = false, configuredAlpha = 0.72f),
            0.001f
        )
    }

    @Test
    fun `follow scroll centers selected item on item boundaries while moving right`() {
        val target = resolveTopTabFollowScrollTarget(
            indicatorPosition = 4.2f,
            itemWidthPx = 100f,
            itemCount = 8,
            viewportWidthPx = 300f,
            currentFirstVisibleItemIndex = 0,
            currentFirstVisibleItemScrollOffsetPx = 0,
            maxScrollPx = 500f,
            edgeBufferPx = 20f
        )

        assertEquals(TopTabScrollTarget(firstVisibleItemIndex = 3, firstVisibleItemScrollOffsetPx = 0), target)
    }

    @Test
    fun `follow scroll centers selected item on item boundaries while moving left`() {
        val target = resolveTopTabFollowScrollTarget(
            indicatorPosition = 1f,
            itemWidthPx = 100f,
            itemCount = 8,
            viewportWidthPx = 300f,
            currentFirstVisibleItemIndex = 2,
            currentFirstVisibleItemScrollOffsetPx = 50,
            maxScrollPx = 500f,
            edgeBufferPx = 20f
        )

        assertEquals(TopTabScrollTarget(firstVisibleItemIndex = 0, firstVisibleItemScrollOffsetPx = 0), target)
    }

    @Test
    fun `follow scroll keeps middle selected category in the center slot`() {
        val target = resolveTopTabFollowScrollTarget(
            indicatorPosition = 3f,
            itemWidthPx = 100f,
            itemCount = 8,
            viewportWidthPx = 500f,
            currentFirstVisibleItemIndex = 0,
            currentFirstVisibleItemScrollOffsetPx = 0,
            maxScrollPx = 300f,
            edgeBufferPx = 20f
        )

        assertEquals(TopTabScrollTarget(firstVisibleItemIndex = 1, firstVisibleItemScrollOffsetPx = 0), target)
    }
}
