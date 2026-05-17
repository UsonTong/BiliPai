package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.ArticleMajor
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.UgcSeasonMajor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicCardClickPolicyTest {

    @Test
    fun resolveDynamicCardPrimaryAction_prefersVideoWhenArchiveBvidExists() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        archive = ArchiveMajor(bvid = "BV1xx411c7mD")
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenVideo)
        assertEquals("BV1xx411c7mD", (action as DynamicCardPrimaryAction.OpenVideo).bvid)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_opensDynamicDetailWhenNoVideo() {
        val item = DynamicItem(id_str = "  987654321  ")

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenDynamicDetail)
        assertEquals("987654321", (action as DynamicCardPrimaryAction.OpenDynamicDetail).dynamicId)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_returnsNoneWhenNoVideoAndNoId() {
        val item = DynamicItem(id_str = "  ")

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.None)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_usesArchiveJumpUrlWhenBvidMissing() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        archive = ArchiveMajor(
                            bvid = "",
                            jump_url = "//www.bilibili.com/video/BV1d4421Z7nW/"
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenVideo)
        assertEquals("BV1d4421Z7nW", (action as DynamicCardPrimaryAction.OpenVideo).bvid)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_usesUgcSeasonJumpUrlWhenArchiveMissing() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        ugc_season = UgcSeasonMajor(
                            title = "合集标题",
                            jump_url = "//www.bilibili.com/video/BV1oeWNebEv2/"
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenVideo)
        assertEquals("BV1oeWNebEv2", (action as DynamicCardPrimaryAction.OpenVideo).bvid)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_usesUgcSeasonAidWhenJumpUrlMissing() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        ugc_season = UgcSeasonMajor(
                            title = "合集标题",
                            aid = 1129813966L
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenVideo)
        assertEquals("av1129813966", (action as DynamicCardPrimaryAction.OpenVideo).bvid)
    }

    @Test
    fun resolveDynamicWatchLaterAid_usesArchiveAidWhenVideoDynamicHasAid() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        archive = ArchiveMajor(aid = "1129813966", bvid = "BV1xx411c7mD")
                    )
                )
            )
        )

        assertEquals(1129813966L, resolveDynamicWatchLaterAid(item))
    }

    @Test
    fun resolveDynamicWatchLaterAid_usesOriginalItemForForwardedVideoDynamic() {
        val item = DynamicItem(
            id_str = "forward",
            orig = DynamicItem(
                id_str = "origin",
                modules = DynamicModules(
                    module_dynamic = DynamicContentModule(
                        major = DynamicMajor(
                            archive = ArchiveMajor(aid = "99887766", bvid = "BV1xx411c7mD")
                        )
                    )
                )
            )
        )

        assertEquals(99887766L, resolveDynamicWatchLaterAid(item))
    }

    @Test
    fun resolveDynamicWatchLaterAid_returnsNullForNonVideoDynamic() {
        val item = DynamicItem(id_str = "123")

        assertEquals(null, resolveDynamicWatchLaterAid(item))
    }

    @Test
    fun resolveDynamicCardMediaAction_previewsArticleCovers() {
        val item = DynamicItem(
            id_str = "1199344045210468386",
            type = "DYNAMIC_TYPE_ARTICLE",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_ARTICLE",
                        article = ArticleMajor(
                            covers = listOf(
                                "https://i0.hdslb.com/bfs/article/a.jpg",
                                "https://i0.hdslb.com/bfs/article/b.jpg"
                            )
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardMediaAction(item, clickedIndex = 1)

        assertTrue(action is DynamicCardMediaAction.PreviewImages)
        assertEquals(
            listOf(
                "https://i0.hdslb.com/bfs/article/a.jpg",
                "https://i0.hdslb.com/bfs/article/b.jpg"
            ),
            (action as DynamicCardMediaAction.PreviewImages).images
        )
        assertEquals(1, action.initialIndex)
    }

    @Test
    fun resolveArticleCoverDrawItems_filtersBlankCoversForRendering() {
        val drawItems = resolveArticleCoverDrawItems(
            ArticleMajor(
                covers = listOf(
                    " ",
                    " https://i0.hdslb.com/bfs/article/a.jpg ",
                    ""
                )
            )
        )

        assertEquals(1, drawItems.size)
        assertEquals("https://i0.hdslb.com/bfs/article/a.jpg", drawItems.first().src)
    }
}
