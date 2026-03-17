package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicUserRequestPolicyTest {

    @Test
    fun `user dynamic result applies only when uid and token still match`() {
        assertTrue(
            shouldApplyUserDynamicsResult(
                selectedUid = 123L,
                requestUid = 123L,
                activeRequestToken = 10L,
                requestToken = 10L
            )
        )
        assertFalse(
            shouldApplyUserDynamicsResult(
                selectedUid = 456L,
                requestUid = 123L,
                activeRequestToken = 10L,
                requestToken = 10L
            )
        )
        assertFalse(
            shouldApplyUserDynamicsResult(
                selectedUid = 123L,
                requestUid = 123L,
                activeRequestToken = 11L,
                requestToken = 10L
            )
        )
    }

    @Test
    fun `selected user reload policy retries same user when scoped state is empty or failed`() {
        assertFalse(
            shouldReloadSelectedUserDynamics(
                previousUid = 123L,
                nextUid = 123L,
                currentItems = listOf(DynamicItem(id_str = "cached")),
                userError = null
            )
        )
        assertTrue(
            shouldReloadSelectedUserDynamics(
                previousUid = 123L,
                nextUid = 123L,
                currentItems = emptyList(),
                userError = null
            )
        )
        assertTrue(
            shouldReloadSelectedUserDynamics(
                previousUid = 123L,
                nextUid = 123L,
                currentItems = listOf(DynamicItem(id_str = "cached")),
                userError = "加载失败"
            )
        )
    }
}
