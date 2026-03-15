package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SearchTopBarLayoutPolicyTest {

    @Test
    fun topBarLayout_removesInlineHotToggleAndKeepsPlaceholderSingleLine() {
        val spec = resolveSearchTopBarLayoutSpec()

        assertFalse(spec.showInlineHotToggle)
        assertEquals(1, spec.placeholderMaxLines)
    }
}
