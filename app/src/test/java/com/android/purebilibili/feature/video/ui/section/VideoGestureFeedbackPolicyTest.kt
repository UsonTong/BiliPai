package com.android.purebilibili.feature.video.ui.section

import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoGestureFeedbackPolicyTest {

    @Test
    fun `resolveOrientationSwitchHintText returns landscape copy`() {
        assertEquals("已切换到横屏", resolveOrientationSwitchHintText(isFullscreen = true))
    }

    @Test
    fun `resolveOrientationSwitchHintText returns portrait copy`() {
        assertEquals("已切换到竖屏", resolveOrientationSwitchHintText(isFullscreen = false))
    }

    @Test
    fun `resolveGestureIndicatorLabel returns brightness label`() {
        assertEquals("亮度", resolveGestureIndicatorLabel(VideoGestureMode.Brightness))
    }

    @Test
    fun `resolveGestureIndicatorLabel returns volume label`() {
        assertEquals("音量", resolveGestureIndicatorLabel(VideoGestureMode.Volume))
    }

    @Test
    fun `resolveGestureIndicatorLabel returns empty for non level mode`() {
        assertEquals("", resolveGestureIndicatorLabel(VideoGestureMode.Seek))
    }

    @Test
    fun `resolveGestureDisplayIcon maps brightness level`() {
        assertEquals(
            CupertinoIcons.Outlined.SunMax,
            resolveGestureDisplayIcon(VideoGestureMode.Brightness, 0.2f, null)
        )
        assertEquals(
            CupertinoIcons.Default.SunMax,
            resolveGestureDisplayIcon(VideoGestureMode.Brightness, 0.52f, null)
        )
        assertEquals(
            CupertinoIcons.Default.SunMax,
            resolveGestureDisplayIcon(VideoGestureMode.Brightness, 0.92f, null)
        )
    }

    @Test
    fun `resolveGestureDisplayIcon maps volume level`() {
        assertEquals(
            CupertinoIcons.Default.SpeakerSlash,
            resolveGestureDisplayIcon(VideoGestureMode.Volume, 0f, null)
        )
        assertEquals(
            CupertinoIcons.Default.Speaker,
            resolveGestureDisplayIcon(VideoGestureMode.Volume, 0.3f, null)
        )
        assertEquals(
            CupertinoIcons.Default.SpeakerWave2,
            resolveGestureDisplayIcon(VideoGestureMode.Volume, 0.9f, null)
        )
    }

    @Test
    fun `resolveGestureDisplayIcon falls back for unsupported mode`() {
        assertEquals(
            CupertinoIcons.Filled.SunMax,
            resolveGestureDisplayIcon(VideoGestureMode.Seek, 0.5f, null)
        )
    }

    @Test
    fun `shouldTriggerFullscreenBySwipe follows default direction`() {
        assertEquals(
            true,
            shouldTriggerFullscreenBySwipe(
                isFullscreen = false,
                reverseGesture = false,
                totalDragDistanceY = -80f,
                thresholdPx = 50f
            )
        )
        assertEquals(
            true,
            shouldTriggerFullscreenBySwipe(
                isFullscreen = true,
                reverseGesture = false,
                totalDragDistanceY = 80f,
                thresholdPx = 50f
            )
        )
    }

    @Test
    fun `shouldTriggerFullscreenBySwipe supports reverse direction`() {
        assertEquals(
            true,
            shouldTriggerFullscreenBySwipe(
                isFullscreen = false,
                reverseGesture = true,
                totalDragDistanceY = 80f,
                thresholdPx = 50f
            )
        )
        assertEquals(
            true,
            shouldTriggerFullscreenBySwipe(
                isFullscreen = true,
                reverseGesture = true,
                totalDragDistanceY = -80f,
                thresholdPx = 50f
            )
        )
    }

    @Test
    fun `resolveVerticalGestureMode uses portrait swipe setting for upward entry`() {
        assertEquals(
            VideoGestureMode.SwipeToFullscreen,
            resolveVerticalGestureMode(
                isFullscreen = false,
                isSwipeUp = true,
                startX = 500f,
                leftZoneEnd = 300f,
                rightZoneStart = 700f,
                portraitSwipeToFullscreenEnabled = true,
                centerSwipeToFullscreenEnabled = false
            )
        )
    }

    @Test
    fun `resolveVerticalGestureMode uses center setting in fullscreen independently`() {
        assertEquals(
            VideoGestureMode.SwipeToFullscreen,
            resolveVerticalGestureMode(
                isFullscreen = true,
                isSwipeUp = false,
                startX = 500f,
                leftZoneEnd = 300f,
                rightZoneStart = 700f,
                portraitSwipeToFullscreenEnabled = false,
                centerSwipeToFullscreenEnabled = true
            )
        )
    }

    @Test
    fun `resolveVerticalGestureMode returns none when center swipe disabled`() {
        assertEquals(
            VideoGestureMode.None,
            resolveVerticalGestureMode(
                isFullscreen = true,
                isSwipeUp = false,
                startX = 500f,
                leftZoneEnd = 300f,
                rightZoneStart = 700f,
                portraitSwipeToFullscreenEnabled = false,
                centerSwipeToFullscreenEnabled = false
            )
        )
    }

    @Test
    fun `resolveVerticalGestureMode disables brightness volume when setting off`() {
        assertEquals(
            VideoGestureMode.None,
            resolveVerticalGestureMode(
                isFullscreen = true,
                isSwipeUp = false,
                startX = 120f,
                leftZoneEnd = 300f,
                rightZoneStart = 700f,
                portraitSwipeToFullscreenEnabled = false,
                centerSwipeToFullscreenEnabled = false,
                slideVolumeBrightnessEnabled = false
            )
        )
        assertEquals(
            VideoGestureMode.None,
            resolveVerticalGestureMode(
                isFullscreen = true,
                isSwipeUp = false,
                startX = 920f,
                leftZoneEnd = 300f,
                rightZoneStart = 700f,
                portraitSwipeToFullscreenEnabled = false,
                centerSwipeToFullscreenEnabled = false,
                slideVolumeBrightnessEnabled = false
            )
        )
    }

    @Test
    fun `shouldShowDanmakuLayers respects pip no danmaku setting`() {
        assertEquals(
            true,
            shouldShowDanmakuLayers(
                isInPipMode = true,
                danmakuEnabled = true,
                isPortraitFullscreen = false,
                pipNoDanmakuEnabled = false
            )
        )
        assertEquals(
            false,
            shouldShowDanmakuLayers(
                isInPipMode = true,
                danmakuEnabled = true,
                isPortraitFullscreen = false,
                pipNoDanmakuEnabled = true
            )
        )
        assertEquals(
            false,
            shouldShowDanmakuLayers(
                isInPipMode = false,
                danmakuEnabled = false,
                isPortraitFullscreen = false,
                pipNoDanmakuEnabled = false
            )
        )
    }
}
