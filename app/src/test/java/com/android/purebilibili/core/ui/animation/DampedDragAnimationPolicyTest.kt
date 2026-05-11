package com.android.purebilibili.core.ui.animation

import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DampedDragAnimationPolicyTest {

    @Test
    fun `release target caps fast fling to configured step count`() {
        val motionSpec = resolveBottomBarMotionSpec()

        val target = resolveDampedDragReleaseTargetIndex(
            currentValue = 2.1f,
            velocityPxPerSecond = 6400f,
            itemWidthPx = 80f,
            itemCount = 6,
            motionSpec = motionSpec
        )

        assertEquals(3, target)
    }

    @Test
    fun `release target clamps overscroll to bounds`() {
        val motionSpec = resolveBottomBarMotionSpec()

        val startTarget = resolveDampedDragReleaseTargetIndex(
            currentValue = -0.42f,
            velocityPxPerSecond = -2800f,
            itemWidthPx = 72f,
            itemCount = 5,
            motionSpec = motionSpec
        )
        val endTarget = resolveDampedDragReleaseTargetIndex(
            currentValue = 4.42f,
            velocityPxPerSecond = 2800f,
            itemWidthPx = 72f,
            itemCount = 5,
            motionSpec = motionSpec
        )

        assertEquals(0, startTarget)
        assertEquals(4, endTarget)
    }

    @Test
    fun `velocity conversion guards invalid item width`() {
        assertEquals(
            0f,
            resolveDampedDragVelocityItemsPerSecond(
                velocityPxPerSecond = 1200f,
                itemWidthPx = 0f
            )
        )
    }

    @Test
    fun `drag updates snap immediately while release keeps animated settling`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()
        val dragSource = source
            .substringAfter("fun onDrag(dragAmountPx: Float, itemWidthPx: Float)")
            .substringBefore("fun setPressed(pressed: Boolean)")
        val releaseSource = source
            .substringAfter("fun onDragEnd(")
            .substringBefore("fun updateIndex(index: Int)")

        assertTrue(dragSource.contains("animatable.stop()"))
        assertTrue(dragSource.contains("animatable.snapTo(newValue)"))
        assertTrue(dragSource.contains("offsetAnimation.stop()"))
        assertTrue(dragSource.contains("offsetAnimation.snapTo(desiredDragOffsetPx)"))
        assertFalse(dragSource.contains("animatable.animateTo(\n                targetValue = newValue"))
        assertTrue(releaseSource.contains("animatable.animateTo("))
        assertTrue(releaseSource.contains("offsetAnimation.animateTo(0f"))
    }

    @Test
    fun `release settle pulse is emitted only after drag release settles`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()
        val releaseSource = source
            .substringAfter("fun onDragEnd(")
            .substringBefore("fun updateIndex(index: Int)")
        val updateIndexSource = source
            .substringAfter("fun updateIndex(index: Int)")
            .substringBefore("}\n}\n\n/**\n * 创建并记住阻尼拖拽动画状态")

        assertTrue(source.contains("var settledReleaseCount by mutableIntStateOf(0)"))
        assertTrue(releaseSource.contains("settledReleaseCount += 1"))
        assertFalse(updateIndexSource.contains("settledReleaseCount += 1"))
    }
}
