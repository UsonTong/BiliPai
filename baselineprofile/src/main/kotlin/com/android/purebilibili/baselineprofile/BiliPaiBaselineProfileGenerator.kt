package com.android.purebilibili.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiliPaiBaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        baselineProfileRule.collect(
            packageName = "com.android.purebilibili",
            includeInStartupProfile = true,
            maxIterations = 8
        ) {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()

            // Core startup and navigation hotspots.
            cycleMainTabs()

            // Feed-heavy screens for Compose + Haze + list rendering paths.
            repeat(2) {
                clickBottomTab("动态")
                scrollFeedOnce()
                scrollFeedOnce(reverse = true)

                clickBottomTab("首页")
                scrollFeedOnce()

                clickBottomTab("历史")
                scrollFeedOnce()
            }

            clickBottomTab("首页")
        }
    }

    private fun MacrobenchmarkScope.cycleMainTabs() {
        clickBottomTab("首页")
        clickBottomTab("动态")
        clickBottomTab("历史")
        clickBottomTab("我的")
        clickBottomTab("首页")
    }

    private fun MacrobenchmarkScope.clickBottomTab(label: String) {
        val byDesc = device.wait(Until.findObject(By.desc(label)), 2_000)
        if (byDesc != null) {
            byDesc.click()
            device.waitForIdle()
            return
        }

        val byText = device.wait(Until.findObject(By.text(label)), 2_000)
        if (byText != null) {
            byText.click()
            device.waitForIdle()
        }
    }

    private fun MacrobenchmarkScope.scrollFeedOnce(reverse: Boolean = false) {
        val x = device.displayWidth / 2
        val fromY = if (reverse) device.displayHeight / 3 else (device.displayHeight * 3) / 4
        val toY = if (reverse) (device.displayHeight * 3) / 4 else device.displayHeight / 3
        device.swipe(x, fromY, x, toY, 24)
        device.waitForIdle()
    }
}
