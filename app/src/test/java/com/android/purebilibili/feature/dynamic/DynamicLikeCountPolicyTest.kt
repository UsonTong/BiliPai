package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.DynamicStatModule
import com.android.purebilibili.data.model.response.StatItem
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicLikeCountPolicyTest {

    @Test
    fun applyDynamicLikeCountChange_incrementsWhenLiked() {
        val items = listOf(
            DynamicItem(
                id_str = "100",
                modules = DynamicModules(
                    module_stat = DynamicStatModule(
                        like = StatItem(count = 9)
                    )
                )
            )
        )

        val updated = applyDynamicLikeCountChange(items, dynamicId = "100", toLiked = true)

        assertEquals(10, updated.first().modules.module_stat?.like?.count)
    }

    @Test
    fun applyDynamicLikeCountChange_decrementsWhenUnliked() {
        val items = listOf(
            DynamicItem(
                id_str = "100",
                modules = DynamicModules(
                    module_stat = DynamicStatModule(
                        like = StatItem(count = 9)
                    )
                )
            )
        )

        val updated = applyDynamicLikeCountChange(items, dynamicId = "100", toLiked = false)

        assertEquals(8, updated.first().modules.module_stat?.like?.count)
    }

    @Test
    fun applyDynamicLikeCountChange_neverGoesBelowZero() {
        val items = listOf(
            DynamicItem(
                id_str = "100",
                modules = DynamicModules(
                    module_stat = DynamicStatModule(
                        like = StatItem(count = 0)
                    )
                )
            )
        )

        val updated = applyDynamicLikeCountChange(items, dynamicId = "100", toLiked = false)

        assertEquals(0, updated.first().modules.module_stat?.like?.count)
    }
}
