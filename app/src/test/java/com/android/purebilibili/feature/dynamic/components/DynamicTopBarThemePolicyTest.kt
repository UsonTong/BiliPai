package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicTopBarThemePolicyTest {

    @Test
    fun `selected dynamic tab uses current theme color`() {
        val themeColor = Color(0xFFFF6F6F)

        assertEquals(themeColor, resolveDynamicTabSelectedColor(themeColor))
    }
}
