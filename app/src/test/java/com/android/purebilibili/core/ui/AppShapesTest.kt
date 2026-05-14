package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppShapesTest {

    @Test
    fun pillRadius_ios_is10Dp() {
        val dp = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Pill,
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        assertEquals(10.dp, dp)
    }

    @Test
    fun pillRadius_md3_is28Dp() {
        val dp = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Pill,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        assertEquals(28.dp, dp)
    }

    @Test
    fun pillRadius_miuix_is22Dp() {
        val dp = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Pill,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )
        assertEquals(22.dp, dp)
    }

    @Test
    fun cardRadius_scalesByPreset() {
        val ios = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Card,
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val md3 = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Card,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val miuix = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Card,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )
        // iOS = base * 1.00, MD3 = base * 0.90, Miuix = base * 1.15
        assertEquals(12.dp, ios)
        assertTrue(miuix.value > ios.value, "Miuix card radius should be larger than iOS")
        assertTrue(ios.value > md3.value, "iOS card radius should be larger than MD3")
    }

    @Test
    fun dialogRadius_scalesByPreset() {
        val ios = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Dialog,
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val md3 = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Dialog,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        assertEquals(14.dp, ios)
        assertTrue(ios.value > md3.value)
    }

    @Test
    fun fieldRadius_scalesByPreset() {
        val ios = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Field,
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        assertEquals(10.dp, ios)
    }
}
