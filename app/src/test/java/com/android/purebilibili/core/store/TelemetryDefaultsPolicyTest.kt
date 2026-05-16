package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertTrue

class TelemetryDefaultsPolicyTest {

    @Test
    fun analyticsIsEnabledByDefaultAlongsideCrashAndPlayerDiagnostics() {
        assertTrue(DEFAULT_CRASH_TRACKING_ENABLED)
        assertTrue(DEFAULT_ANALYTICS_ENABLED)
        assertTrue(DEFAULT_PLAYER_DIAGNOSTIC_LOGGING_ENABLED)
    }
}
