package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.data.model.response.ReplyContent
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ReplyMember
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubReplyDetailPresentationPolicyTest {

    @Test
    fun `section title should include current reply count`() {
        assertEquals("相关回复共14条", resolveSubReplyDetailSectionTitle(replyCount = 14))
    }

    @Test
    fun `conversation action should only show for directed reply text`() {
        assertTrue(
            shouldShowSubReplyConversationAction(
                buildReply(message = "回复 @前进四放映室：没错")
            )
        )
        assertFalse(
            shouldShowSubReplyConversationAction(
                buildReply(message = "又又又又更新？？？？")
            )
        )
    }

    @Test
    fun `auxiliary label should prefer garb card number when available`() {
        assertEquals(
            "NO.013992",
            resolveSubReplyAuxiliaryLabel(
                item = buildReply(
                    message = "test",
                    garbCardNumber = "13992"
                )
            )
        )
    }

    @Test
    fun `auxiliary label should stay hidden when no garb card number exists`() {
        assertEquals(
            null,
            resolveSubReplyAuxiliaryLabel(
                item = buildReply(message = "test")
            )
        )
    }

    @Test
    fun `light theme detail appearance should follow theme surface instead of dark palette`() {
        val appearance = resolveSubReplyDetailAppearance(
            surfaceColor = Color(0xFFFFFFFF),
            surfaceVariantColor = Color(0xFFF1F2F4),
            surfaceContainerHighColor = Color(0xFFE8EAF0),
            outlineVariantColor = Color(0xFFD9DCE3),
            onSurfaceColor = Color(0xFF1B1C1F),
            onSurfaceVariantColor = Color(0xFF6A6F76),
            primaryColor = Color(0xFFFB7299)
        )

        assertEquals(Color(0xFFFFFFFF), appearance.panelColor)
        assertEquals(Color(0xFF1B1C1F), appearance.primaryTextColor)
        assertEquals(Color(0xFF6A6F76), appearance.secondaryTextColor)
        assertEquals(Color(0xFFD9DCE3), appearance.dividerColor)
        assertEquals(Color(0xFFE8EAF0), appearance.sectionDividerColor)
        assertEquals(Color(0xFFFB7299), appearance.vipNameColor)
    }

    private fun buildReply(
        message: String,
        garbCardNumber: String = ""
    ): ReplyItem {
        return ReplyItem(
            rpid = 200L,
            member = ReplyMember(
                mid = "12",
                uname = "ReplyUser",
                garbCardNumber = garbCardNumber
            ),
            content = ReplyContent(message = message)
        )
    }
}
