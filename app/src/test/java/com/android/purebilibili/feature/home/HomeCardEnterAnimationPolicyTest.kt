package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeCardEnterAnimationPolicyTest {

    @Test
    fun baseDisabled_keepsEnterAnimationDisabled() {
        assertFalse(
            resolveHomeCardEnterAnimationEnabledAtMount(
                baseAnimationEnabled = false,
                isReturningFromDetail = false,
                isSwitchingCategory = false
            )
        )
    }

    @Test
    fun returningFromDetail_disablesEnterAnimationAtMount() {
        assertFalse(
            resolveHomeCardEnterAnimationEnabledAtMount(
                baseAnimationEnabled = true,
                isReturningFromDetail = true,
                isSwitchingCategory = false
            )
        )
    }

    @Test
    fun switchingCategory_disablesEnterAnimationAtMount() {
        assertFalse(
            resolveHomeCardEnterAnimationEnabledAtMount(
                baseAnimationEnabled = true,
                isReturningFromDetail = false,
                isSwitchingCategory = true
            )
        )
    }

    @Test
    fun normalHomeMount_enablesEnterAnimation() {
        assertTrue(
            resolveHomeCardEnterAnimationEnabledAtMount(
                baseAnimationEnabled = true,
                isReturningFromDetail = false,
                isSwitchingCategory = false
            )
        )
    }
}
