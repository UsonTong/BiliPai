package com.android.purebilibili.feature.video.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackCdnFallbackPolicyTest {

    @Test
    fun `cdn rewrite keeps original playback pair as fallback`() {
        val state = buildPlaybackCdnFallbackState(
            selectedVideoUrl = "https://cn-sh-ct-01-01.bilivideo.com/video.m4s",
            selectedAudioUrl = "https://cn-sh-ct-01-01.bilivideo.com/audio.m4s",
            originalVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            originalAudioUrl = "https://upos-sz-mirrorali.bilivideo.com/audio.m4s",
            regionLabel = "上海"
        )

        assertTrue(state.usesCdnRewrite)
        assertEquals("https://upos-sz-mirrorali.bilivideo.com/video.m4s", state.fallbackVideoUrl)
        assertEquals("https://upos-sz-mirrorali.bilivideo.com/audio.m4s", state.fallbackAudioUrl)
        assertTrue(shouldFallbackFromCdnRewrite(state, playbackReady = false))
    }

    @Test
    fun `unchanged playback url does not arm cdn fallback`() {
        val state = buildPlaybackCdnFallbackState(
            selectedVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            selectedAudioUrl = "https://upos-sz-mirrorali.bilivideo.com/audio.m4s",
            originalVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            originalAudioUrl = "https://upos-sz-mirrorali.bilivideo.com/audio.m4s",
            regionLabel = null
        )

        assertFalse(state.usesCdnRewrite)
        assertFalse(shouldFallbackFromCdnRewrite(state, playbackReady = false))
    }

    @Test
    fun `cdn fallback can only fire once`() {
        val state = buildPlaybackCdnFallbackState(
            selectedVideoUrl = "https://cn-sh-ct-01-01.bilivideo.com/video.m4s",
            selectedAudioUrl = null,
            originalVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            originalAudioUrl = null,
            regionLabel = "上海"
        )

        val consumed = state.markFallbackConsumed()

        assertFalse(shouldFallbackFromCdnRewrite(consumed, playbackReady = false))
    }
}
