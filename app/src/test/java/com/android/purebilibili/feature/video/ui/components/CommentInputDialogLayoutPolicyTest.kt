package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommentInputDialogLayoutPolicyTest {

    @Test
    fun portrait_keepsRoomyEditorForComfortableCommentInput() {
        val policy = resolveCommentInputDialogLayoutPolicy(
            isLandscape = false
        )

        assertEquals(84, policy.inputBoxMinHeightDp)
        assertEquals(136, policy.inputBoxMaxHeightDp)
        assertEquals(220, policy.emojiPanelHeightDp)
        assertEquals(16, policy.sheetHorizontalPaddingDp)
        assertEquals(40, policy.toolbarToolButtonSizeDp)
        assertEquals(6, policy.toolbarToolSpacingDp)
        assertEquals(16, policy.sendButtonHorizontalPaddingDp)
    }

    @Test
    fun landscape_compactsEditorToReduceVideoOcclusion() {
        val portraitPolicy = resolveCommentInputDialogLayoutPolicy(
            isLandscape = false
        )
        val landscapePolicy = resolveCommentInputDialogLayoutPolicy(
            isLandscape = true
        )

        assertEquals(64, landscapePolicy.inputBoxMinHeightDp)
        assertEquals(112, landscapePolicy.inputBoxMaxHeightDp)
        assertEquals(196, landscapePolicy.emojiPanelHeightDp)
        assertEquals(18, landscapePolicy.sendButtonHorizontalPaddingDp)
        assertTrue(landscapePolicy.inputBoxMinHeightDp < portraitPolicy.inputBoxMinHeightDp)
        assertTrue(landscapePolicy.inputBoxMaxHeightDp < portraitPolicy.inputBoxMaxHeightDp)
        assertTrue(landscapePolicy.emojiPanelHeightDp < portraitPolicy.emojiPanelHeightDp)
    }

    @Test
    fun progressInsertText_wrapsFormattedPlaybackTimeWithSpaces() {
        assertEquals(" 01:05 ", resolveCommentProgressInsertText(65_000L))
        assertEquals(" 00:00 ", resolveCommentProgressInsertText(-1L))
    }
}
