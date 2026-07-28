package com.kuikly.kuiklychartkit.chart

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LineDataTransitionTest {
    @Test
    fun compatibleSnapshotsInterpolateOnlyChangedYValues() {
        val from = listOf(
            ChartSeries(
                "load",
                listOf(
                    ChartPoint(0f, 24f, "09:00"),
                    ChartPoint(1f, 48f, "09:15"),
                    ChartPoint(2f, 36f, "09:30"),
                ),
            ),
        )
        val to = listOf(
            ChartSeries(
                "load",
                listOf(
                    ChartPoint(0f, 24f, "09:00"),
                    ChartPoint(1f, 72f, "09:15"),
                    ChartPoint(2f, 36f, "09:30"),
                ),
            ),
        )

        val halfway = LineDataTransition.interpolate(from, to, 0.5f).single().items

        assertTrue(LineDataTransition.isCompatible(from, to))
        assertEquals(24f, halfway[0].y)
        assertEquals(60f, halfway[1].y)
        assertEquals(36f, halfway[2].y)
        assertEquals("09:15", halfway[1].label)
        assertEquals(1f, halfway[1].x)
    }

    @Test
    fun changedStructureOrNonFiniteValueSkipsTransition() {
        val source = listOf(
            ChartSeries("load", listOf(ChartPoint(0f, 24f, "09:00"), ChartPoint(1f, 48f, "09:15"))),
        )
        val changedX = listOf(
            ChartSeries("load", listOf(ChartPoint(0f, 24f, "09:00"), ChartPoint(2f, 48f, "09:15"))),
        )
        val nonFinite = listOf(
            ChartSeries("load", listOf(ChartPoint(0f, 24f, "09:00"), ChartPoint(1f, Float.NaN, "09:15"))),
        )

        assertFalse(LineDataTransition.isCompatible(source, changedX))
        assertFalse(LineDataTransition.isCompatible(source, nonFinite))
        assertEquals(nonFinite, LineDataTransition.interpolate(source, nonFinite, 0.5f))
    }

    @Test
    fun explicitYAxisBoundsStayStableAcrossDataChanges() {
        val layout = ChartLayoutEngine.line(
            width = 320f,
            height = 200f,
            series = listOf(
                ChartSeries("live", listOf(ChartPoint(0f, 18f), ChartPoint(1f, 84f))),
            ),
            margins = ChartMargins(),
            tickCount = 5,
            includeZero = false,
            yMin = 0f,
            yMax = 100f,
        )

        assertEquals(0f, layout.yScale.min)
        assertEquals(100f, layout.yScale.max)
    }
}
