package com.kuikly.kuiklychartkit.chart

import com.kuikly.kuiklychartkit.chart.interaction.ChartBrushContext
import com.kuikly.kuiklychartkit.chart.interaction.ChartBrushController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChartBrushControllerTest {
    @Test
    fun brushOnlyStartsInsidePlotAndReturnsOriginalInclusiveIndexes() {
        val controller = ChartBrushController()
        val context = context()

        assertNull(controller.begin(5f, 50f, context))
        assertFalse(controller.isActive)

        assertEquals(ChartBrushSelection(1, 1), controller.begin(50f, 50f, context))
        assertTrue(controller.isActive)
        assertEquals(ChartBrushSelection(1, 3), controller.move(95f, 50f, context))
    }

    @Test
    fun reverseDragNormalizesTheSelectedRangeAndClampsOutsideMoves() {
        val controller = ChartBrushController()
        val context = context()

        controller.begin(95f, 50f, context)

        assertEquals(ChartBrushSelection(0, 3), controller.move(-200f, 50f, context))
        controller.end()
        assertFalse(controller.isActive)
    }

    @Test
    fun invalidCoordinatesDoNotStartOrChangeBrush() {
        val controller = ChartBrushController()
        val context = context()

        assertNull(controller.begin(Float.NaN, 50f, context))
        controller.begin(50f, 50f, context)
        assertNull(controller.move(Float.POSITIVE_INFINITY, 50f, context))
        assertTrue(controller.isActive)
    }

    private fun context(): ChartBrushContext = ChartBrushContext(
        plot = ChartRect(10f, 10f, 100f, 90f),
        renderedPoints = listOf(
            point(20f, 0), point(50f, 1), point(75f, 2), point(95f, 3),
        ),
    )

    private fun point(x: Float, index: Int) = RenderedLinePoint(
        x = x,
        y = 50f,
        selection = ChartSelection(0, index, "series", ChartPoint(index.toFloat(), index.toFloat())),
    )
}
