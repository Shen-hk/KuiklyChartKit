package com.kuikly.kuiklychartkit.chart

import com.kuikly.kuiklychartkit.chart.interaction.PolarChartHitTest
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeatmapLayoutEngineTest {
    @Test
    fun preservesCategoryOrderOriginalIndexesAndFiniteCells() {
        val layout = HeatmapLayoutEngine.layout(
            plot = ChartRect(20f, 10f, 220f, 110f),
            entries = listOf(
                HeatmapEntry("Mon", "Morning", 12f),
                HeatmapEntry("Tue", "Morning", Float.NaN),
                HeatmapEntry("Mon", "Evening", 30f),
                HeatmapEntry("Tue", "Evening", 48f),
            ),
            seriesName = "activity",
            cellGap = 4f,
        )

        assertEquals(listOf("Mon", "Tue"), layout.xLabels)
        assertEquals(listOf("Morning", "Evening"), layout.yLabels)
        assertEquals(listOf(0, 2, 3), layout.cells.map { it.selection.itemIndex })
        assertTrue(layout.cells.all { it.bounds.width > 0f && it.bounds.height > 0f })
        assertEquals(0f, layout.cells.minOf { it.fraction }, 0.001f)
        assertEquals(1f, layout.cells.maxOf { it.fraction }, 0.001f)
    }

    @Test
    fun keepsHeatmapCellsSquareAndCentersTheGrid() {
        val layout = HeatmapLayoutEngine.layout(
            plot = ChartRect(0f, 0f, 210f, 110f),
            entries = listOf(
                HeatmapEntry("Mon", "AM", 10f),
                HeatmapEntry("Tue", "AM", 20f),
                HeatmapEntry("Wed", "AM", 30f),
                HeatmapEntry("Mon", "PM", 40f),
                HeatmapEntry("Tue", "PM", 50f),
                HeatmapEntry("Wed", "PM", 60f),
            ),
            seriesName = "activity",
            cellGap = 4f,
        )

        assertEquals(22.5f, layout.plot.left, 0.001f)
        assertEquals(187.5f, layout.plot.right, 0.001f)
        assertEquals(0f, layout.plot.top, 0.001f)
        assertEquals(110f, layout.plot.bottom, 0.001f)
        assertTrue(layout.cells.all { cell -> kotlin.math.abs(cell.bounds.width - cell.bounds.height) < 0.001f })
        assertEquals(51f, layout.cells.first().bounds.width, 0.001f)
    }

    @Test
    fun keepsCellsSquareForDifferentRowAndColumnCounts() {
        val datasets = listOf(
            listOf(
                HeatmapEntry("Mon", "09:00", 1f), HeatmapEntry("Tue", "09:00", 2f),
                HeatmapEntry("Wed", "09:00", 3f), HeatmapEntry("Thu", "09:00", 4f),
                HeatmapEntry("Fri", "09:00", 5f),
                HeatmapEntry("Mon", "18:00", 6f), HeatmapEntry("Tue", "18:00", 7f),
                HeatmapEntry("Wed", "18:00", 8f), HeatmapEntry("Thu", "18:00", 9f),
                HeatmapEntry("Fri", "18:00", 10f),
            ),
            listOf(
                HeatmapEntry("Search", "Exposure", 1f), HeatmapEntry("Feed", "Exposure", 2f),
                HeatmapEntry("Live", "Exposure", 3f), HeatmapEntry("Social", "Exposure", 4f),
                HeatmapEntry("Search", "Click", 5f), HeatmapEntry("Feed", "Click", 6f),
                HeatmapEntry("Live", "Click", 7f), HeatmapEntry("Social", "Click", 8f),
                HeatmapEntry("Search", "Cart", 9f), HeatmapEntry("Feed", "Cart", 10f),
                HeatmapEntry("Live", "Cart", 11f), HeatmapEntry("Social", "Cart", 12f),
            ),
            listOf(
                HeatmapEntry("N", "Inbound", 1f), HeatmapEntry("E", "Inbound", 2f),
                HeatmapEntry("S", "Inbound", 3f), HeatmapEntry("W", "Inbound", 4f),
                HeatmapEntry("T", "Inbound", 5f), HeatmapEntry("F", "Inbound", 6f),
                HeatmapEntry("N", "Picking", 7f), HeatmapEntry("E", "Picking", 8f),
                HeatmapEntry("S", "Picking", 9f), HeatmapEntry("W", "Picking", 10f),
                HeatmapEntry("T", "Picking", 11f), HeatmapEntry("F", "Picking", 12f),
                HeatmapEntry("N", "Packing", 13f), HeatmapEntry("E", "Packing", 14f),
                HeatmapEntry("S", "Packing", 15f), HeatmapEntry("W", "Packing", 16f),
                HeatmapEntry("T", "Packing", 17f), HeatmapEntry("F", "Packing", 18f),
                HeatmapEntry("N", "Outbound", 19f), HeatmapEntry("E", "Outbound", 20f),
                HeatmapEntry("S", "Outbound", 21f), HeatmapEntry("W", "Outbound", 22f),
                HeatmapEntry("T", "Outbound", 23f), HeatmapEntry("F", "Outbound", 24f),
                HeatmapEntry("N", "Delivered", 25f), HeatmapEntry("E", "Delivered", 26f),
                HeatmapEntry("S", "Delivered", 27f), HeatmapEntry("W", "Delivered", 28f),
                HeatmapEntry("T", "Delivered", 29f), HeatmapEntry("F", "Delivered", 30f),
            ),
        )

        datasets.forEach { entries ->
            val layout = HeatmapLayoutEngine.layout(
                plot = ChartRect(48f, 16f, 346f, 212f),
                entries = entries,
                seriesName = "example",
                cellGap = 4f,
            )

            assertEquals(entries.size, layout.cells.size)
            assertTrue(layout.cells.all { cell -> kotlin.math.abs(cell.bounds.width - cell.bounds.height) < 0.001f })
        }
    }

    @Test
    fun heatmapEntranceSettlesEveryCellAtFullSize() {
        listOf(1, 12, 30).forEach { cellCount ->
            (0 until cellCount).forEach { index ->
                assertEquals(1f, heatmapCellEntranceProgress(1f, index, cellCount), 0.001f)
            }
        }
        assertTrue(heatmapCellEntranceProgress(0.2f, 11, 12) < 1f)
    }

    @Test
    fun equalValuesUseTheMiddleColorBucketAndHitRenderedBounds() {
        val layout = HeatmapLayoutEngine.layout(
            plot = ChartRect(0f, 0f, 160f, 80f),
            entries = listOf(
                HeatmapEntry("Mon", "AM", 10f),
                HeatmapEntry("Tue", "AM", 10f),
            ),
            seriesName = "activity",
            cellGap = 2f,
        )
        val first = layout.cells.first()

        assertTrue(layout.cells.all { it.fraction == 0.5f })
        assertEquals(first.selection, PolarChartHitTest.heatmap(
            layout.cells,
            (first.bounds.left + first.bounds.right) / 2f,
            (first.bounds.top + first.bounds.bottom) / 2f,
        ))
        assertNull(PolarChartHitTest.heatmap(layout.cells, 240f, 120f))
    }
}

class HeatmapColorScaleTest {
    @Test
    fun defaultsToGitHubStyleGreenLevelsForBuiltInThemes() {
        assertEquals(
            listOf(0xFF9BE9A8L, 0xFF40C463L, 0xFF30A14EL, 0xFF216E39L),
            defaultHeatmapColorScale(ChartTheme.light()).map { it.hexColor },
        )
        assertEquals(
            listOf(0xFF0E4429L, 0xFF006D32L, 0xFF26A641L, 0xFF39D353L),
            defaultHeatmapColorScale(ChartTheme.dark()).map { it.hexColor },
        )
    }

    @Test
    fun explicitColorScaleOverridesTheDefaultGreenLevels() {
        val customScale = listOf(Color(0xFF1D4ED8), Color(0xFF1E3A8A))

        assertEquals(
            customScale.map { it.hexColor },
            resolveHeatmapColorScale(customScale, ChartTheme.dark()).map { it.hexColor },
        )
    }
}

class RadarLayoutEngineTest {
    @Test
    fun buildsSharedAxesAndPreservesSeriesIndexes() {
        val layout = RadarLayoutEngine.layout(
            width = 280f,
            height = 240f,
            series = listOf(
                ChartSeries(
                    "current",
                    listOf(
                        RadarEntry("Quality", 82f),
                        RadarEntry("Reach", 64f),
                        RadarEntry("Speed", 91f),
                        RadarEntry("Cost", 48f),
                    ),
                ),
                ChartSeries(
                    "target",
                    listOf(
                        RadarEntry("Quality", 76f),
                        RadarEntry("Reach", 72f),
                        RadarEntry("Speed", 84f),
                        RadarEntry("Cost", 68f),
                    ),
                ),
            ),
            gridCount = 5,
            labelInset = 36f,
            legendVisible = true,
        )

        assertEquals(4, layout.axes.size)
        assertEquals(2, layout.series.size)
        assertTrue(layout.radius > 0f)
        assertTrue(layout.axes.first().y < layout.centerY)
        assertTrue(layout.series.all { it.closesPolygon })
        assertEquals(listOf(0, 1, 2, 3), layout.series.first().points.map { it.selection.itemIndex })
        assertEquals(1, layout.series[1].points.first().selection.seriesIndex)
    }

    @Test
    fun invalidRadarValuesBecomeGapsWithoutBreakingHitTesting() {
        val layout = RadarLayoutEngine.layout(
            width = 240f,
            height = 220f,
            series = listOf(
                ChartSeries(
                    "current",
                    listOf(
                        RadarEntry("A", 30f),
                        RadarEntry("B", Float.NaN),
                        RadarEntry("C", -2f),
                        RadarEntry("D", 70f),
                    ),
                ),
            ),
            gridCount = 4,
            labelInset = 28f,
            legendVisible = false,
        )
        val points = layout.series.single().points
        val last = points.last()

        assertEquals(listOf(0, 3), points.map { it.selection.itemIndex })
        assertEquals(2, layout.series.single().segments.size)
        assertFalse(layout.series.single().closesPolygon)
        assertEquals(last.selection, PolarChartHitTest.radar(points, last.x, last.y, radius = 8f))
        assertNull(PolarChartHitTest.radar(points, 500f, 500f, radius = 8f))
    }

    @Test
    fun zeroMetricsStayAtTheRadarCenter() {
        val layout = RadarLayoutEngine.layout(
            width = 240f,
            height = 220f,
            series = listOf(
                ChartSeries(
                    "zero",
                    listOf(RadarEntry("A", 0f), RadarEntry("B", 0f), RadarEntry("C", 0f)),
                ),
            ),
            gridCount = 4,
            labelInset = 28f,
            legendVisible = false,
        )

        assertEquals(0f, layout.scale.min)
        assertTrue(layout.series.single().points.all { point ->
            point.x == layout.centerX && point.y == layout.centerY
        })
    }

    @Test
    fun explicitRadarMaximumKeepsUpdatesOnOneStableScale() {
        val layout = RadarLayoutEngine.layout(
            width = 240f,
            height = 220f,
            series = listOf(
                ChartSeries("current", listOf(RadarEntry("A", 40f), RadarEntry("B", 70f), RadarEntry("C", 80f))),
            ),
            gridCount = 4,
            labelInset = 28f,
            legendVisible = false,
            maxValue = 100f,
        )

        assertEquals(0f, layout.scale.min)
        assertEquals(100f, layout.scale.max)
    }

    @Test
    fun trackerProjectsOntoOneAxisAndOnlyReplacesThatPolygonVertex() {
        val layout = RadarLayoutEngine.layout(
            width = 240f,
            height = 220f,
            series = listOf(
                ChartSeries(
                    "current",
                    listOf(
                        RadarEntry("A", 40f),
                        RadarEntry("B", 70f),
                        RadarEntry("C", 80f),
                    ),
                ),
            ),
            gridCount = 4,
            labelInset = 28f,
            legendVisible = false,
        )
        val original = layout.series.single().points.first()
        val axis = layout.axes.first()
        val previewValue = assertNotNull(
            RadarTrackerProjection.previewValue(
                layout = layout,
                selection = original.selection,
                x = layout.centerX + (axis.x - layout.centerX) * 0.8f,
                y = layout.centerY + (axis.y - layout.centerY) * 0.8f,
            ),
        )
        val tracker = RadarTracker(original.selection, previewValue)
        val replacement = assertNotNull(RadarTrackerProjection.point(layout, tracker))
        val updated = RadarTrackerProjection.replacePoint(layout.series, tracker, replacement)

        assertEquals(layout.scale.min + layout.scale.range * 0.8f, previewValue, 0.001f)
        assertEquals(original.selection, replacement.selection)
        assertEquals(replacement, updated.single().points.first())
        assertEquals(layout.series.single().points.drop(1), updated.single().points.drop(1))
        assertEquals(original, layout.series.single().points.first())
    }

    @Test
    fun trackerClampsDraggedValuesToTheRadarScale() {
        val layout = RadarLayoutEngine.layout(
            width = 240f,
            height = 220f,
            series = listOf(
                ChartSeries(
                    "current",
                    listOf(RadarEntry("A", 40f), RadarEntry("B", 70f), RadarEntry("C", 80f)),
                ),
            ),
            gridCount = 4,
            labelInset = 28f,
            legendVisible = false,
        )
        val selection = layout.series.single().points.first().selection
        val axis = layout.axes.first()

        val max = assertNotNull(
            RadarTrackerProjection.previewValue(
                layout,
                selection,
                layout.centerX + (axis.x - layout.centerX) * 2f,
                layout.centerY + (axis.y - layout.centerY) * 2f,
            ),
        )
        val min = assertNotNull(
            RadarTrackerProjection.previewValue(
                layout,
                selection,
                layout.centerX - (axis.x - layout.centerX),
                layout.centerY - (axis.y - layout.centerY),
            ),
        )

        assertEquals(layout.scale.max, max)
        assertEquals(layout.scale.min, min)
    }
}

class P2ChartDslValidationTest {
    @Test
    fun heatmapRejectsDuplicateCoordinatesAndInvalidGap() {
        val duplicate = assertFailsWith<IllegalArgumentException> {
            HeatmapChartAttr().data(
                HeatmapEntry("Mon", "AM", 1f),
                HeatmapEntry("Mon", "AM", 2f),
            )
        }
        assertTrue(duplicate.message.orEmpty().contains("duplicate"))

        val gap = assertFailsWith<IllegalArgumentException> {
            HeatmapChartAttr().heatmap { cellGap = -1f }
        }
        assertTrue(gap.message.orEmpty().contains("heatmap.cellGap"))
    }

    @Test
    fun radarRejectsMismatchedDimensionsAndInvalidGrid() {
        val mismatch = assertFailsWith<IllegalArgumentException> {
            RadarChartAttr().data(
                ChartSeries("A", listOf(RadarEntry("Q", 1f), RadarEntry("R", 2f), RadarEntry("S", 3f))),
                ChartSeries("B", listOf(RadarEntry("Q", 1f), RadarEntry("S", 2f), RadarEntry("R", 3f))),
            )
        }
        assertTrue(mismatch.message.orEmpty().contains("ordered dimension"))

        val grid = assertFailsWith<IllegalArgumentException> {
            RadarChartAttr().radar { gridCount = 1 }
        }
        assertTrue(grid.message.orEmpty().contains("radar.gridCount"))
    }
}

class ChartImageExportTest {
    @Test
    fun exportRequestRejectsInvalidSampleSize() {
        val error = assertFailsWith<IllegalArgumentException> {
            ChartImageExportRequest(sampleSize = 0)
        }

        assertTrue(error.message.orEmpty().contains("imageExport.sampleSize"))
    }

    @Test
    fun exportResponseKeepsSuccessAndFailurePayloadsDeterministic() {
        val success = parseChartImageExportResponse(
            JSONObject().put("code", 0).put("data", "data:image/png;base64,abc"),
        )
        val failure = parseChartImageExportResponse(
            JSONObject().put("code", 8).put("message", "not supported"),
        )

        assertTrue(success.isSuccess)
        assertEquals("data:image/png;base64,abc", success.data)
        assertFalse(failure.isSuccess)
        assertEquals("not supported", failure.message)
        assertNotNull(failure.message)
    }
}
