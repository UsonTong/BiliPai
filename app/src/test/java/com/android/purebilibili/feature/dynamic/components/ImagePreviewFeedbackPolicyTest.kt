package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.util.HapticType
import kotlin.test.Test
import kotlin.test.assertEquals

class ImagePreviewFeedbackPolicyTest {

    @Test
    fun saveFeedback_usesLightHapticOnSuccess() {
        assertEquals(HapticType.LIGHT, resolveImagePreviewSaveFeedback(success = true))
    }

    @Test
    fun saveFeedback_usesHeavyHapticOnFailure() {
        assertEquals(HapticType.HEAVY, resolveImagePreviewSaveFeedback(success = false))
    }
}
