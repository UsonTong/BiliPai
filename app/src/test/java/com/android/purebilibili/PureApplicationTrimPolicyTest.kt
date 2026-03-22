package com.android.purebilibili

import android.content.ComponentCallbacks2
import com.android.purebilibili.app.PureApplicationRuntimeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PureApplicationTrimPolicyTest {

    @Test
    fun `ui hidden should clear memory cache`() {
        assertTrue(
            PureApplicationRuntimeConfig.shouldClearImageMemoryCacheOnTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
            )
        )
    }

    @Test
    fun `low memory levels should clear memory cache`() {
        assertTrue(
            PureApplicationRuntimeConfig.shouldClearImageMemoryCacheOnTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
            )
        )
        assertTrue(
            PureApplicationRuntimeConfig.shouldClearImageMemoryCacheOnTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
            )
        )
        assertTrue(
            PureApplicationRuntimeConfig.shouldClearImageMemoryCacheOnTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_COMPLETE
            )
        )
    }

    @Test
    fun `image memory cache percent uses tighter budget`() {
        assertEquals(0.15, PureApplicationRuntimeConfig.resolveImageMemoryCachePercent(), 0.0001)
    }
}
