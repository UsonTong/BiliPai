package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeNotInterestedPolicyTest {

    @Test
    fun `not interested blocks valid creator mids`() {
        val action = resolveHomeNotInterestedAction(
            VideoItem(
                bvid = "BV1",
                owner = Owner(mid = 42L, name = "UP-X", face = "face.jpg")
            )
        )

        assertEquals("BV1", action.bvid)
        assertTrue(action.shouldBlockCreator)
        assertEquals(42L, action.creatorMid)
        assertEquals("UP-X", action.creatorName)
        assertEquals("face.jpg", action.creatorFace)
    }

    @Test
    fun `not interested does not block missing creator mids`() {
        val action = resolveHomeNotInterestedAction(
            VideoItem(
                bvid = "BV2",
                owner = Owner(mid = 0L, name = "未知UP")
            )
        )

        assertFalse(action.shouldBlockCreator)
        assertEquals(0L, action.creatorMid)
    }
}
