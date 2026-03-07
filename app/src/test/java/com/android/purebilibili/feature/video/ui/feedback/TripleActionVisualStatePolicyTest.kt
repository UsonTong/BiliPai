package com.android.purebilibili.feature.video.ui.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripleActionVisualStatePolicyTest {

    @Test
    fun `full triple success activates all three action states`() {
        val state = resolveTripleActionVisualState(
            currentLiked = false,
            currentCoinCount = 0,
            currentFavorited = false,
            likeSuccess = true,
            coinSuccess = true,
            favoriteSuccess = true
        )

        assertTrue(state.isLiked)
        assertEquals(2, state.coinCount)
        assertTrue(state.isFavorited)
    }

    @Test
    fun `partial triple success preserves unchanged actions`() {
        val state = resolveTripleActionVisualState(
            currentLiked = false,
            currentCoinCount = 0,
            currentFavorited = true,
            likeSuccess = true,
            coinSuccess = false,
            favoriteSuccess = false
        )

        assertTrue(state.isLiked)
        assertEquals(0, state.coinCount)
        assertTrue(state.isFavorited)
    }
}
