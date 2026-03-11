package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.DynamicBasic
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.UgcSeasonMajor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicInteractionPolicyTest {

    @Test
    fun `video tab includes ugc season dynamics`() {
        val item = DynamicItem(type = "DYNAMIC_TYPE_UGC_SEASON")

        assertTrue(shouldIncludeDynamicItemInVideoTab(item))
    }

    @Test
    fun `video tab excludes word dynamics`() {
        val item = DynamicItem(type = "DYNAMIC_TYPE_WORD")

        assertFalse(shouldIncludeDynamicItemInVideoTab(item))
    }

    @Test
    fun `resolve dynamic comment target prefers basic fields for video dynamics`() {
        val item = DynamicItem(
            id_str = "966281785469042740",
            type = "DYNAMIC_TYPE_AV",
            basic = DynamicBasic(
                comment_id_str = "1129813966",
                comment_type = 1,
                rid_str = "0"
            )
        )

        val target = resolveDynamicCommentTarget(item)

        assertEquals(DynamicCommentTarget(oid = 1129813966L, type = 1), target)
    }

    @Test
    fun `resolve dynamic comment target falls back to ugc season aid`() {
        val item = DynamicItem(
            id_str = "966281785469042740",
            type = "DYNAMIC_TYPE_UGC_SEASON",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_UGC_SEASON",
                        ugc_season = UgcSeasonMajor(aid = 1456253104L)
                    )
                )
            )
        )

        val target = resolveDynamicCommentTarget(item)

        assertEquals(DynamicCommentTarget(oid = 1456253104L, type = 1), target)
    }

    @Test
    fun `resolve dynamic comment target uses article type from basic`() {
        val item = DynamicItem(
            id_str = "718372214316990512",
            type = "DYNAMIC_TYPE_ARTICLE",
            basic = DynamicBasic(
                comment_id_str = "37231101",
                comment_type = 12,
                rid_str = "37231101"
            )
        )

        val target = resolveDynamicCommentTarget(item)

        assertEquals(DynamicCommentTarget(oid = 37231101L, type = 12), target)
    }

    @Test
    fun `resolve dynamic comment target falls back to archive aid for av`() {
        val item = DynamicItem(
            id_str = "123",
            type = "DYNAMIC_TYPE_AV",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_ARCHIVE",
                        archive = ArchiveMajor(aid = "1756441068")
                    )
                )
            )
        )

        val target = resolveDynamicCommentTarget(item)

        assertEquals(DynamicCommentTarget(oid = 1756441068L, type = 1), target)
    }

    @Test
    fun `resolve dynamic comment target prefers dynamic id for opus detail even when basic points elsewhere`() {
        val item = DynamicItem(
            id_str = "967717348014293017",
            type = "DYNAMIC_TYPE_DRAW",
            basic = DynamicBasic(
                comment_id_str = "326122895",
                comment_type = 11,
                rid_str = "326122895"
            ),
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS"
                    )
                )
            )
        )

        val target = resolveDynamicCommentTarget(item)

        assertEquals(DynamicCommentTarget(oid = 967717348014293017L, type = 17), target)
    }

    @Test
    fun `resolve dynamic comment targets keeps desktop dynamic target before legacy basic fallback`() {
        val item = DynamicItem(
            id_str = "967717348014293017",
            type = "DYNAMIC_TYPE_DRAW",
            basic = DynamicBasic(
                comment_id_str = "326122895",
                comment_type = 11,
                rid_str = "326122895"
            ),
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(type = "MAJOR_TYPE_OPUS")
                )
            )
        )

        val targets = resolveDynamicCommentTargets(item)

        assertEquals(
            listOf(
                DynamicCommentTarget(oid = 967717348014293017L, type = 17),
                DynamicCommentTarget(oid = 326122895L, type = 11)
            ),
            targets
        )
    }
}
