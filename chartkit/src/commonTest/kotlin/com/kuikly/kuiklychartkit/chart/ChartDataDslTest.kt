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

    @Test
    fun bulkDataDslBuildsSeriesAndEntries() {
        val labels = listOf("Mon", "Tue", "Wed")
        val values = listOf(120f, 168f, 142f)

        val line = LineChartAttr()
        line.data {
            series("Visits") {
                points(labels, values)
            }
            series(
                name = "Orders",
                items = listOf(ChartPoint(0f, 42f, "Mon"), ChartPoint(1f, 51f, "Tue")),
            )
        }
        assertEquals(listOf(0f, 1f, 2f), line.series.first().items.map { it.x })
        assertEquals("Orders", line.series[1].name)

        val bar = BarChartAttr()
        bar.data {
            series("Revenue") {
                items(labels, values)
            }
        }
        assertEquals("Tue", bar.series.single().items[1].label)

        val pie = PieChartAttr()
        pie.data {
            slices(labels, values)
        }
        assertEquals(3, pie.entries.size)

        val heatmap = HeatmapChartAttr()
        heatmap.data {
            cells(
                listOf(
                    HeatmapEntry("Mon", "09:00", 42f),
                    HeatmapEntry("Tue", "09:00", 58f),
                ),
            )
        }
        assertEquals("Tue", heatmap.entries[1].xLabel)

        val radar = RadarChartAttr()
        radar.data {
            series("Current") {
                metrics(listOf("Speed", "Quality", "Cost"), listOf(86f, 72f, 91f))
            }
            series(
                name = "Target",
                items = listOf(RadarEntry("Speed", 80f), RadarEntry("Quality", 82f), RadarEntry("Cost", 88f)),
            )
        }
        assertEquals("Target", radar.series[1].name)
    }

    @Test
    fun bulkDataDslRejectsMismatchedLabelsAndValues() {
        assertFailsWith<IllegalArgumentException> {
            LineChartAttr().data {
                series("Visits") {
                    points(labels = listOf("Mon"), values = listOf(120f, 168f))
                }
            }
        }

        assertFailsWith<IllegalArgumentException> {
            BarChartAttr().data {
                series("Revenue") {
                    items(labels = listOf("Mon"), values = emptyList())
                }
            }
        }
    }
}
