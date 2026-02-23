package com.android.purebilibili.core.ui.blur

import kotlin.test.Test
import kotlin.test.assertEquals

class UnifiedBlurPreferencePolicyTest {

    @Test
    fun providedIntensity_takesPriorityOverFallback() {
        val result = resolveUnifiedBlurIntensity(
            provided = BlurIntensity.APPLE_DOCK,
            fallback = BlurIntensity.THIN
        )

        assertEquals(BlurIntensity.APPLE_DOCK, result)
    }

    @Test
    fun fallbackUsed_whenNoProvidedIntensity() {
        val result = resolveUnifiedBlurIntensity(
            provided = null,
            fallback = BlurIntensity.THICK
        )

        assertEquals(BlurIntensity.THICK, result)
    }
}
