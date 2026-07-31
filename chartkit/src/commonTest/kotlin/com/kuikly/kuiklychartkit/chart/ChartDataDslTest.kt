package com.kuikly.kuiklychartkit.chart

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChartDataDslTest {
    @Test
    fun pointDataDslBuildsSeriesWithAutomaticAndExplicitXValues() {
        val attr = LineChartAttr()

        attr.data {
            series("Visits") {
                point("Mon", 120f)
                point(x = 3f, value = 142f, label = "Wed")
            }
        }

        assertEquals(listOf(0f, 3f), attr.series.single().items.map { it.x })
        assertEquals(listOf("Mon", "Wed"), attr.series.single().items.map { it.label })
    }

    @Test
    fun categoricalAndPolarDataDslDelegateToExistingValidation() {
        val bars = BarChartAttr()
        bars.data { series("Conversion") { item("A", 86f); item("B", 132f) } }
        assertEquals(2, bars.series.single().items.size)

        val pie = PieChartAttr()
        pie.data { slice("Search", 260f) }
        assertEquals("Search", pie.entries.single().label)

        val heatmap = HeatmapChartAttr()
        heatmap.data { cell("Mon", "09:00", 42f) }
        assertEquals(42f, heatmap.entries.single().value)

        val radar = RadarChartAttr()
        radar.data {
            series("Current") { metric("Speed", 86f); metric("Quality", 72f); metric("Cost", 91f) }
        }
        assertEquals(3, radar.series.single().items.size)

        val mixed = MixedChartAttr()
        mixed.barData { series("Actual") { item("Mon", 86f) } }
        mixed.lineData { series("Target") { item("Mon", 96f) } }
        assertEquals("Actual", mixed.barSeries.single().name)
        assertEquals("Target", mixed.lineSeries.single().name)

        assertFailsWith<IllegalArgumentException> {
            BarChartAttr().data { series("") { item("A", 1f) } }
        }
    }
}
