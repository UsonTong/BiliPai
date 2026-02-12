package com.android.purebilibili.feature.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EyeProtectionPolicyTest {

    @Test
    fun `isWithinProtectionWindow handles cross-day time ranges`() {
        assertTrue(isWithinProtectionWindow(currentHour = 23, startHour = 22, endHour = 7))
        assertTrue(isWithinProtectionWindow(currentHour = 2, startHour = 22, endHour = 7))
        assertFalse(isWithinProtectionWindow(currentHour = 14, startHour = 22, endHour = 7))
    }

    @Test
    fun `isWithinProtectionWindow handles same-day time ranges`() {
        assertTrue(isWithinProtectionWindow(currentHour = 20, startHour = 18, endHour = 23))
        assertFalse(isWithinProtectionWindow(currentHour = 8, startHour = 18, endHour = 23))
    }

    @Test
    fun `shouldTriggerCareReminder respects interval and snooze`() {
        assertFalse(
            shouldTriggerCareReminder(
                usageMinutes = 29,
                intervalMinutes = 30,
                snoozeUntilMinute = null,
                lastReminderMinute = null
            )
        )

        assertTrue(
            shouldTriggerCareReminder(
                usageMinutes = 30,
                intervalMinutes = 30,
                snoozeUntilMinute = null,
                lastReminderMinute = null
            )
        )

        assertFalse(
            shouldTriggerCareReminder(
                usageMinutes = 60,
                intervalMinutes = 30,
                snoozeUntilMinute = 65,
                lastReminderMinute = 30
            )
        )
    }

    @Test
    fun `tuningForPreset returns expected defaults`() {
        val gentle = tuningForPreset(EyeCarePreset.GENTLE)
        val focus = tuningForPreset(EyeCarePreset.FOCUS)

        assertEquals(0.88f, gentle.brightnessLevel)
        assertEquals(45, gentle.reminderIntervalMinutes)
        assertEquals(0.65f, focus.brightnessLevel)
        assertEquals(25, focus.reminderIntervalMinutes)
    }

    @Test
    fun `isVisualEffectActive respects force and schedule`() {
        assertTrue(
            isVisualEffectActive(
                forceEnabled = true,
                nightModeEnabled = false,
                currentHour = 14,
                startHour = 22,
                endHour = 7
            )
        )

        assertTrue(
            isVisualEffectActive(
                forceEnabled = false,
                nightModeEnabled = true,
                currentHour = 23,
                startHour = 22,
                endHour = 7
            )
        )

        assertFalse(
            isVisualEffectActive(
                forceEnabled = false,
                nightModeEnabled = false,
                currentHour = 23,
                startHour = 22,
                endHour = 7
            )
        )
    }
}
