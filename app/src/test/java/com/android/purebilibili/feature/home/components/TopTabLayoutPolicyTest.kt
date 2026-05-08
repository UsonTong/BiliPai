package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabLayoutPolicyTest {

    @Test
    fun `visible slot count should stay in compact range`() {
        assertEquals(4, resolveTopTabVisibleSlots(1))
        assertEquals(4, resolveTopTabVisibleSlots(4))
        assertEquals(5, resolveTopTabVisibleSlots(5, longestLabelLength = 6))
        assertEquals(4, resolveTopTabVisibleSlots(5, longestLabelLength = 9))
        assertEquals(4, resolveTopTabVisibleSlots(8, longestLabelLength = 10))
    }

    @Test
    fun `floating style should enforce wider min width to avoid clipping`() {
        assertEquals(72f, resolveTopTabItemWidthDp(260f, 5, isFloatingStyle = true), 0.001f)
    }

    @Test
    fun `docked style should keep a denser minimum width`() {
        assertEquals(64f, resolveTopTabItemWidthDp(260f, 5, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun `wide containers should use proportional width`() {
        assertEquals(100f, resolveTopTabItemWidthDp(500f, 5, isFloatingStyle = true), 0.001f)
    }

    @Test
    fun `ios top tab action shares centered slot with visible categories`() {
        assertEquals(1, resolveTopTabVisibleCategorySlots(1, longestLabelLength = 2))
        assertEquals(5, resolveTopTabVisibleCategorySlots(5, longestLabelLength = 6))
        assertEquals(100f, resolveTopTabActionSlotWidthDp(600f, 5, longestLabelLength = 6), 0.001f)
        assertEquals(100f, resolveTopTabItemWidthDp(500f, 5, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun `live route decision should follow category key not localized label`() {
        assertTrue(shouldRouteTopTabToLivePage("LIVE"))
        assertTrue(shouldRouteTopTabToLivePage("live"))
        assertFalse(shouldRouteTopTabToLivePage("直播"))
        assertFalse(shouldRouteTopTabToLivePage("RECOMMEND"))
    }

    @Test
    fun `md3 top tabs keep four visible slots on every device width`() {
        assertEquals(4, resolveMd3TopTabVisibleSlots())
        assertEquals(80f, resolveMd3TopTabItemWidthDp(containerWidthDp = 320f), 0.001f)
        assertEquals(90f, resolveMd3TopTabItemWidthDp(containerWidthDp = 360f), 0.001f)
        assertEquals(160f, resolveMd3TopTabItemWidthDp(containerWidthDp = 640f), 0.001f)
    }

    @Test
    fun `ios top tabs reserve enough height for icon label modes`() {
        assertEquals(52f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 2).value, 0.001f)
        assertEquals(52f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 1).value, 0.001f)
        assertEquals(58f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 0).value, 0.001f)
        assertEquals(56f, resolveIosTopTabRowHeight(isFloatingStyle = false, labelMode = 0).value, 0.001f)
    }

    @Test
    fun `top tab item content policy avoids clipping icon plus text`() {
        assertEquals(42f, resolveTopTabContentMinHeightDp(labelMode = 0), 0.001f)
        assertEquals(36f, resolveTopTabContentMinHeightDp(labelMode = 1), 0.001f)
        assertEquals(36f, resolveTopTabContentMinHeightDp(labelMode = 2), 0.001f)
        assertEquals(2f, resolveTopTabContentVerticalPaddingDp(labelMode = 0), 0.001f)
        assertEquals(4f, resolveTopTabContentVerticalPaddingDp(labelMode = 1), 0.001f)
        assertEquals(4f, resolveTopTabContentVerticalPaddingDp(labelMode = 2), 0.001f)
    }

    @Test
    fun `md3 top tabs keep selected category within four visible slots`() {
        assertEquals(
            listOf(0, 1, 2, 3),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 0)
        )
        assertEquals(
            listOf(0, 1, 2, 4),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 4)
        )
        assertEquals(
            3,
            resolveMd3SelectedVisibleIndex(
                visibleIndices = listOf(0, 1, 2, 4),
                selectedIndex = 4
            )
        )
    }
}
