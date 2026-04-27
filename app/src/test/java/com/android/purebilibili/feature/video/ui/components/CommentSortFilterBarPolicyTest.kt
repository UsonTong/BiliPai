package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommentSortFilterBarPolicyTest {

    @Test
    fun `sort segmented control leaves room for bottom bar matched indicator scale`() {
        val spec = resolveCommentSortSegmentedControlSpec(itemCount = 2)

        assertEquals(66, spec.itemWidthDp)
        assertEquals(38, spec.heightDp)
        assertEquals(24, spec.indicatorHeightDp)
        assertTrue(
            hasCommentSortIndicatorScaleClearance(
                containerHeightDp = spec.heightDp,
                indicatorHeightDp = spec.indicatorHeightDp
            )
        )
    }
}
