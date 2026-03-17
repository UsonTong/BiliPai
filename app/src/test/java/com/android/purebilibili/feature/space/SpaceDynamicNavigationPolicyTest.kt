package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceDynamicArchive
import com.android.purebilibili.data.model.response.SpaceDynamicContent
import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.data.model.response.SpaceDynamicMajor
import com.android.purebilibili.data.model.response.SpaceDynamicModules
import com.android.purebilibili.data.model.response.SpaceDynamicOpus
import com.android.purebilibili.data.model.response.SpaceDynamicDrawItem
import com.android.purebilibili.feature.dynamic.components.DynamicCardPrimaryAction
import com.android.purebilibili.feature.dynamic.components.resolveDynamicCardPrimaryAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SpaceDynamicNavigationPolicyTest {

    @Test
    fun resolveSpaceDynamicClickAction_prefersVideoWhenArchiveBvidExists() {
        val dynamic = SpaceDynamicItem(
            id_str = "12345",
            modules = SpaceDynamicModules(
                module_dynamic = SpaceDynamicContent(
                    major = SpaceDynamicMajor(
                        archive = SpaceDynamicArchive(bvid = "BV1xx411c7mD")
                    )
                )
            )
        )

        val action = resolveSpaceDynamicClickAction(dynamic)

        assertTrue(action is SpaceDynamicClickAction.OpenVideo)
        assertEquals("BV1xx411c7mD", (action as SpaceDynamicClickAction.OpenVideo).bvid)
    }

    @Test
    fun resolveSpaceDynamicClickAction_usesDesktopDynamicDetailWhenNoVideo() {
        val dynamic = SpaceDynamicItem(id_str = "987654321")

        val action = resolveSpaceDynamicClickAction(dynamic)

        assertTrue(action is SpaceDynamicClickAction.OpenDynamicDetail)
        assertEquals("987654321", (action as SpaceDynamicClickAction.OpenDynamicDetail).dynamicId)
    }

    @Test
    fun resolveSpaceDynamicClickAction_returnsNoneWhenNoBvidAndNoId() {
        val dynamic = SpaceDynamicItem(id_str = "   ")

        val action = resolveSpaceDynamicClickAction(dynamic)

        assertTrue(action is SpaceDynamicClickAction.None)
    }

    @Test
    fun resolveSpaceDynamicClickAction_matches_shared_dynamic_primary_action() {
        val dynamic = SpaceDynamicItem(
            id_str = "2468",
            modules = SpaceDynamicModules(
                module_dynamic = SpaceDynamicContent(
                    major = SpaceDynamicMajor(
                        opus = SpaceDynamicOpus(
                            title = "图文标题",
                            pics = listOf(SpaceDynamicDrawItem(src = "a"))
                        )
                    )
                )
            )
        )

        val sharedAction = assertIs<DynamicCardPrimaryAction.OpenDynamicDetail>(
            resolveDynamicCardPrimaryAction(resolveSpaceDynamicCardItem(dynamic))
        )
        val spaceAction = assertIs<SpaceDynamicClickAction.OpenDynamicDetail>(
            resolveSpaceDynamicClickAction(dynamic)
        )

        assertEquals(sharedAction.dynamicId, spaceAction.dynamicId)
    }
}
