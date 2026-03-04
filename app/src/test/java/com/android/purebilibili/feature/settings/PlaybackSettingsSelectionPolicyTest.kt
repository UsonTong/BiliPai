package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.FullscreenAspectRatio
import com.android.purebilibili.core.store.FullscreenMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSettingsSelectionPolicyTest {

    @Test
    fun `resolveSelectionIndex should return matched option index`() {
        val options = listOf(
            PlaybackSegmentOption("avc1", "AVC"),
            PlaybackSegmentOption("hev1", "HEVC"),
            PlaybackSegmentOption("av01", "AV1")
        )

        assertEquals(1, resolveSelectionIndex(options, "hev1"))
    }

    @Test
    fun `resolveSelectionIndex should fallback to first option when value missing`() {
        val options = listOf(
            PlaybackSegmentOption(116, "1080P60"),
            PlaybackSegmentOption(80, "1080P"),
            PlaybackSegmentOption(64, "720P")
        )

        assertEquals(0, resolveSelectionIndex(options, 32))
        assertEquals("720P", resolveSelectionLabel(options, 64, fallbackLabel = "默认"))
        assertEquals("默认", resolveSelectionLabel(options, 32, fallbackLabel = "默认"))
    }

    @Test
    fun `resolveEffectiveMobileQuality should clamp to 480p when data saver active`() {
        assertEquals(32, resolveEffectiveMobileQuality(rawMobileQuality = 80, isDataSaverActive = true))
        assertEquals(16, resolveEffectiveMobileQuality(rawMobileQuality = 16, isDataSaverActive = true))
        assertEquals(80, resolveEffectiveMobileQuality(rawMobileQuality = 80, isDataSaverActive = false))
    }

    @Test
    fun `resolveDefaultPlaybackQualityOptions should only expose fixed quality tiers`() {
        val options = resolveDefaultPlaybackQualityOptions()

        assertEquals(listOf(116, 80, 64, 32, 16), options.map { it.value })
        assertEquals(listOf("1080P60", "1080P", "720P", "480P", "360P"), options.map { it.label })
    }

    @Test
    fun `resolveSegmentedSwipeTargetIndex should switch to adjacent option when drag exceeds threshold`() {
        assertEquals(
            3,
            resolveSegmentedSwipeTargetIndex(
                currentIndex = 2,
                totalDragPx = 42f,
                optionCount = 5,
                thresholdPx = 30f
            )
        )
        assertEquals(
            1,
            resolveSegmentedSwipeTargetIndex(
                currentIndex = 2,
                totalDragPx = -45f,
                optionCount = 5,
                thresholdPx = 30f
            )
        )
        assertEquals(
            2,
            resolveSegmentedSwipeTargetIndex(
                currentIndex = 2,
                totalDragPx = 10f,
                optionCount = 5,
                thresholdPx = 30f
            )
        )
    }

    @Test
    fun `resolveFullscreenModeSegmentOptions should expose only primary modes`() {
        val modes = resolveFullscreenModeSegmentOptions().map { it.value }
        assertEquals(
            listOf(
                FullscreenMode.AUTO,
                FullscreenMode.NONE,
                FullscreenMode.VERTICAL,
                FullscreenMode.HORIZONTAL
            ),
            modes
        )
    }

    @Test
    fun `resolveFullscreenAspectRatioSegmentOptions should expose fixed fullscreen ratios`() {
        val ratios = resolveFullscreenAspectRatioSegmentOptions().map { it.value }
        assertEquals(
            listOf(
                FullscreenAspectRatio.FIT,
                FullscreenAspectRatio.FILL,
                FullscreenAspectRatio.RATIO_16_9,
                FullscreenAspectRatio.RATIO_4_3,
                FullscreenAspectRatio.STRETCH
            ),
            ratios
        )
    }
}
