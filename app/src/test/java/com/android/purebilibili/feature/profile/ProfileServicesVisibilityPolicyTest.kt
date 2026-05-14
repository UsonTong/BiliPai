package com.android.purebilibili.feature.profile

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileServicesVisibilityPolicyTest {

    @Test
    fun `history service hides when history is already visible in bottom bar`() {
        assertFalse(
            shouldShowProfileHistoryService(
                bottomBarVisibleTabIds = listOf("HOME", "DYNAMIC", "HISTORY", "PROFILE")
            )
        )
    }

    @Test
    fun `history service stays visible when history is not in bottom bar`() {
        assertTrue(
            shouldShowProfileHistoryService(
                bottomBarVisibleTabIds = listOf("HOME", "DYNAMIC", "PROFILE")
            )
        )
    }

    @Test
    fun `immersive mobile services use compact island and separate account actions`() {
        val source = File("src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt").readText()
        val servicesSource = source.substringAfter("fun ServicesSection(")
            .substringBefore("@Composable\nprivate fun ProfileFavoriteFolderShortcutGrid")

        assertTrue(servicesSource.contains("val useImmersiveServiceLayout = borderColor != null"))
        assertTrue(servicesSource.contains("ProfileServicesListIsland("))
        assertTrue(servicesSource.contains("ProfileServiceRow("))
        assertTrue(servicesSource.contains("ProfileAccountActionArea("))
        assertTrue(servicesSource.contains("compactHorizontal = true"))
        assertTrue(servicesSource.contains("onMoreClick = onFavoriteClick"))
        assertFalse(servicesSource.contains("ProfileFloatingServiceItem("))
        val immersiveBranchSource = servicesSource.substringAfter("if (useImmersiveServiceLayout) {")
            .substringBefore("} else {")
        assertFalse(immersiveBranchSource.contains("ProfileFloatingServiceItem("))
    }
}
