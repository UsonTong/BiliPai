package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitMainPlayerSyncPolicyTest {

    @Test
    fun noReloadWhenSnapshotBvidBlank() {
        assertFalse(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = " ",
                currentBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun reloadWhenCurrentBvidMissingButSnapshotExists() {
        assertTrue(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV1xx411c7mD",
                currentBvid = null
            )
        )
    }

    @Test
    fun noReloadWhenSnapshotMatchesCurrent() {
        assertFalse(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV1xx411c7mD",
                currentBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun reloadWhenSnapshotDiffersFromCurrent() {
        assertTrue(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV17x411w7KC",
                currentBvid = "BV1xx411c7mD"
            )
        )
    }
}
