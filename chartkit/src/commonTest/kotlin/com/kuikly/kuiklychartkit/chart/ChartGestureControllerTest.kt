package com.kuikly.kuiklychartkit.chart

import com.kuikly.kuiklychartkit.chart.interaction.ChartGestureContext
import com.kuikly.kuiklychartkit.chart.interaction.ChartGestureController
import com.kuikly.kuiklychartkit.chart.interaction.ChartGestureTouch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChartGestureControllerTest {
    @Test
    fun panClampsAtBothViewportBounds() {
        val controller = ChartGestureController()
        val latest = context(viewport = ChartViewport(6, 9))

        controller.onTouchStart(100f, listOf(ChartGestureTouch(100f, 0f)), latest)
        assertEquals(ChartViewport(0, 3), controller.onTouchMove(500f, listOf(ChartGestureTouch(500f, 0f)), latest))

        controller.onTouchEnd(0)
        val earliest = context(viewport = ChartViewport(0, 3))
        controller.onTouchStart(100f, listOf(ChartGestureTouch(100f, 0f)), earliest)
        assertEquals(ChartViewport(6, 9), controller.onTouchMove(-500f, listOf(ChartGestureTouch(-500f, 0f)), earliest))
    }

    @Test
    fun pinchUsesRenderedFocalPointRatherThanViewportCenter() {
        val controller = ChartGestureController()
        val context = context(
            itemCount = 20,
            viewport = ChartViewport(4, 11),
            points = renderedPoints(4..11),
            minVisibleItemCount = 3,
            maxVisibleItemCount = 10,
        )

        controller.onTouchStart(90f, listOf(ChartGestureTouch(40f, 0f), ChartGestureTouch(140f, 0f)), context)
        val zoomed = controller.onTouchMove(140f, listOf(ChartGestureTouch(40f, 0f), ChartGestureTouch(240f, 0f)), context)

        assertEquals(ChartViewport(7, 10), zoomed)
        assertTrue(9 in zoomed!!.startIndex..zoomed.endIndex)
    }

    @Test
    fun doubleTapResetsToConfiguredLatestWindow() {
        val reset = ChartGestureController().onDoubleTap(
            context(viewport = ChartViewport(0, 3), visibleItemCount = 4),
        )

        assertEquals(ChartViewport(6, 9), reset)
    }

    @Test
    fun trackerReleaseHonorsPersistenceOption() {
        val controller = ChartGestureController()

        assertTrue(controller.shouldClearTracker(keepTrackerOnRelease = false))
        assertFalse(controller.shouldClearTracker(keepTrackerOnRelease = true))
    }

    @Test
    fun nonFiniteCoordinatesAndScaleDoNotChangeViewportState() {
        val controller = ChartGestureController()
        val context = context(viewport = ChartViewport(4, 7))

        controller.onTouchStart(Float.NaN, listOf(ChartGestureTouch(Float.NaN, 0f)), context)
        assertNull(controller.onTouchMove(Float.NaN, listOf(ChartGestureTouch(Float.NaN, 0f)), context))

        controller.onTouchStart(50f, listOf(ChartGestureTouch(0f, 0f), ChartGestureTouch(100f, 0f)), context)
        assertNull(controller.onTouchMove(50f, listOf(ChartGestureTouch(0f, 0f), ChartGestureTouch(Float.POSITIVE_INFINITY, 0f)), context))
        assertEquals(ChartViewport(4, 7), context.viewport)
    }

    private fun context(
        itemCount: Int = 10,
        viewport: ChartViewport = ChartViewport(6, 9),
        visibleItemCount: Int = 4,
        minVisibleItemCount: Int = 2,
        maxVisibleItemCount: Int = 0,
        points: List<RenderedLinePoint> = renderedPoints(0..9),
    ) = ChartGestureContext(
        itemCount = itemCount,
        viewport = viewport,
        resetViewport = null,
        visibleItemCount = visibleItemCount,
        minVisibleItemCount = minVisibleItemCount,
        maxVisibleItemCount = maxVisibleItemCount,
        enablePan = true,
        enableZoom = true,
        renderedPoints = points,
    )

    private fun renderedPoints(indexes: IntRange): List<RenderedLinePoint> = indexes.map { index ->
        RenderedLinePoint(
            x = index * 10f,
            y = 50f,
            selection = ChartSelection(0, index, "series", ChartPoint(index.toFloat(), index.toFloat())),
        )
    }
}
