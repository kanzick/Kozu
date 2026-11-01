package vn.kozu.game.mania.gamestage

import vn.kozu.game.mania.gamestage.ui.TouchInputRouter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchInputRouterTest {
    @Test
    fun wideChartTouchHitsOnlyColumnsWithinItsTouchRegion() {
        val router = TouchInputRouter(keys = 8, left = 0f, columnWidth = 100f, touchRadius = 24f)

        assertArrayEquals(intArrayOf(0), router.press(pointerId = 1L, x = 50f)!!.pressed)
        assertArrayEquals(intArrayOf(1), router.press(pointerId = 2L, x = 99f)!!.pressed)
    }

    @Test
    fun touchRegionCanHitThreeOrFourNarrowColumns() {
        val threeColumnRouter = TouchInputRouter(keys = 10, left = 0f, columnWidth = 30f, touchRadius = 40f)
        val fourColumnRouter = TouchInputRouter(keys = 12, left = 0f, columnWidth = 20f, touchRadius = 24f)

        assertArrayEquals(intArrayOf(1, 2, 3), threeColumnRouter.press(pointerId = 1L, x = 75f)!!.pressed)
        assertArrayEquals(intArrayOf(4, 5, 6, 7), fourColumnRouter.press(pointerId = 1L, x = 120f)!!.pressed)
    }

    @Test
    fun horizontalSwipeReleasesOldColumnsAndPressesNewColumns() {
        val router = TouchInputRouter(keys = 8, left = 0f, columnWidth = 100f, touchRadius = 24f)
        router.press(pointerId = 1L, x = 50f)

        val changes = router.move(pointerId = 1L, x = 150f)

        assertArrayEquals(intArrayOf(0), changes.released)
        assertArrayEquals(intArrayOf(1), changes.pressed)
    }

    @Test
    fun horizontalSwipeAcrossOverlappingWideLanesKeepsSharedLaneHeld() {
        val router = TouchInputRouter(keys = 8, left = 0f, columnWidth = 100f, touchRadius = 24f)
        router.press(pointerId = 1L, x = 99f)

        val changes = router.move(pointerId = 1L, x = 201f)

        assertArrayEquals(intArrayOf(0), changes.released)
        assertArrayEquals(intArrayOf(2), changes.pressed)
    }

    @Test
    fun aSecondPointerKeepsTheSharedColumnDownDuringSwipe() {
        val router = TouchInputRouter(keys = 4, left = 0f, columnWidth = 100f, touchRadius = 24f)
        router.press(pointerId = 1L, x = 50f)
        router.press(pointerId = 2L, x = 50f)

        val changes = router.move(pointerId = 1L, x = 150f)

        assertTrue(changes.released.isEmpty())
        assertArrayEquals(intArrayOf(1), changes.pressed)
    }

    @Test
    fun sixKeyChartsStillRouteTouchToOneColumn() {
        val router = TouchInputRouter(keys = 6, left = 0f, columnWidth = 40f, touchRadius = 24f)

        assertArrayEquals(intArrayOf(3), router.press(pointerId = 1L, x = 140f)!!.pressed)
    }

    @Test
    fun acceptsTenSimultaneousPointersAndRejectsAnEleventh() {
        val router = TouchInputRouter(keys = 4, left = 0f, columnWidth = 40f, touchRadius = 24f)

        repeat(TouchInputRouter.MAX_SIMULTANEOUS_TOUCHES) { pointer ->
            assertNotNull(router.press(pointer.toLong(), x = pointer % 4 * 40f + 20f))
        }
        assertNull(router.press(10L, x = 20f))

        router.release(0L)
        assertNotNull(router.press(10L, x = 20f))
    }

    @Test
    fun releasingOnePointerKeepsOverlappingColumnsHeldUntilTheLastPointerIsLifted() {
        val router = TouchInputRouter(keys = 8, left = 0f, columnWidth = 100f, touchRadius = 24f)
        router.press(1L, x = 99f)
        router.press(2L, x = 101f)

        assertTrue(router.release(1L).released.isEmpty())
        assertArrayEquals(intArrayOf(0, 1), router.release(2L).released)
    }

}
