package com.kuikly.kuiklychartkit.chart

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChartDataTransitionTest {
    @Test
    fun compatibleBarsInterpolateValuesWithoutChangingLabels() {
        val from = listOf(ChartSeries("sales", listOf(BarEntry("Jan", 20f), BarEntry("Feb", 40f))))
        val to = listOf(ChartSeries("sales", listOf(BarEntry("Jan", 60f), BarEntry("Feb", 80f))))

        val halfway = ChartDataTransition.interpolateBars(from, to, 0.5f)

        assertTrue(ChartDataTransition.barsCompatible(from, to))
        assertEquals(40f, halfway.single().items[0].value)
        assertEquals("Feb", halfway.single().items[1].label)
    }

    @Test
    fun pieInterpolationPreservesSliceIdentity() {
        val from = listOf(PieEntry("Android", 20f), PieEntry("iOS", 80f))
        val to = listOf(PieEntry("Android", 50f), PieEntry("iOS", 50f))

        val halfway = ChartDataTransition.interpolatePie(from, to, 0.5f)

        assertTrue(ChartDataTransition.pieCompatible(from, to))
        assertEquals(35f, halfway[0].value)
        assertEquals("iOS", halfway[1].label)
    }

    @Test
    fun heatmapInterpolationPreservesCellCoordinates() {
        val from = listOf(HeatmapEntry("Mon", "API", 10f), HeatmapEntry("Tue", "API", 30f))
        val to = listOf(HeatmapEntry("Mon", "API", 30f), HeatmapEntry("Tue", "API", 50f))

        val halfway = ChartDataTransition.interpolateHeatmap(from, to, 0.5f)

        assertTrue(ChartDataTransition.heatmapCompatible(from, to))
        assertEquals(20f, halfway[0].value)
        assertEquals("Tue", halfway[1].xLabel)
    }

    @Test
    fun radarInterpolationPreservesDimensionOrder() {
        val from = listOf(ChartSeries("current", listOf(RadarEntry("Speed", 20f), RadarEntry("Safety", 40f))))
        val to = listOf(ChartSeries("current", listOf(RadarEntry("Speed", 60f), RadarEntry("Safety", 80f))))

        val halfway = ChartDataTransition.interpolateRadar(from, to, 0.5f)

        assertTrue(ChartDataTransition.radarCompatible(from, to))
        assertEquals(40f, halfway.single().items[0].value)
        assertEquals("Safety", halfway.single().items[1].label)
    }

    @Test
    fun mixedInterpolationUpdatesBarsAndLinesTogether() {
        val from = MixedChartData(
            barSeries = listOf(ChartSeries("orders", listOf(BarEntry("Jan", 20f)))),
            lineSeries = listOf(ChartSeries("rate", listOf(BarEntry("Jan", 40f)))),
        )
        val to = MixedChartData(
            barSeries = listOf(ChartSeries("orders", listOf(BarEntry("Jan", 60f)))),
            lineSeries = listOf(ChartSeries("rate", listOf(BarEntry("Jan", 80f)))),
        )

        val halfway = ChartDataTransition.interpolateMixed(from, to, 0.5f)

        assertTrue(ChartDataTransition.mixedCompatible(from, to))
        assertEquals(40f, halfway.barSeries.single().items.single().value)
        assertEquals(60f, halfway.lineSeries.single().items.single().value)
    }

    @Test
    fun structuralAndBadValueChangesSkipTransitions() {
        val bars = listOf(ChartSeries("sales", listOf(BarEntry("Jan", 20f))))
        val pie = listOf(PieEntry("Android", 20f), PieEntry("iOS", 80f))
        val heatmap = listOf(HeatmapEntry("Mon", "API", 20f))
        val radar = listOf(ChartSeries("current", listOf(RadarEntry("Speed", 20f))))

        assertFalse(ChartDataTransition.barsCompatible(bars, emptyList()))
        assertFalse(ChartDataTransition.pieCompatible(pie, pie.reversed()))
        assertFalse(ChartDataTransition.heatmapCompatible(heatmap, listOf(HeatmapEntry("Tue", "API", 30f))))
        assertFalse(ChartDataTransition.radarCompatible(radar, listOf(ChartSeries("current", listOf(RadarEntry("Speed", Float.NaN))))))
    }

    @Test
    fun extremeFiniteValuesInterpolateWithoutOverflow() {
        val from = listOf(ChartSeries("range", listOf(BarEntry("now", -Float.MAX_VALUE))))
        val to = listOf(ChartSeries("range", listOf(BarEntry("now", Float.MAX_VALUE))))

        val halfway = ChartDataTransition.interpolateBars(from, to, 0.5f).single().items.single().value

        assertTrue(halfway.isFinite())
        assertEquals(0f, halfway)
    }
}
