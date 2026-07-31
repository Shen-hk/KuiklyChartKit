package com.kuikly.kuiklychartkit.chart

import com.kuikly.kuiklychartkit.chart.interaction.ChartHitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class NiceScaleTest {
    @Test
    fun emptyValuesReturnSafeDefaultScale() {
        val scale = NiceScale.fromValues(emptyList(), tickCount = 5, includeZero = false)

        assertEquals(0f, scale.min)
        assertEquals(1f, scale.max)
        assertEquals(5, scale.ticks.size)
    }

    @Test
    fun equalValuesAreExpandedWithoutDivisionByZero() {
        val scale = NiceScale.fromValues(listOf(42f, 42f), tickCount = 5, includeZero = false)

        assertTrue(scale.min < 42f)
        assertTrue(scale.max > 42f)
        assertTrue(scale.range > 0f)
    }

    @Test
    fun includeZeroExpandsPositiveDomain() {
        val scale = NiceScale.fromValues(listOf(80f, 120f), tickCount = 4, includeZero = true)

        assertTrue(scale.min <= 0f)
        assertTrue(scale.max >= 120f)
        assertTrue(scale.ticks.any { it == 0f })
    }

    @Test
    fun invalidValuesAreIgnored() {
        val scale = NiceScale.fromValues(
            listOf(Float.NaN, Float.POSITIVE_INFINITY, -5f, 10f),
            tickCount = 5,
            includeZero = false,
        )

        assertTrue(scale.min <= -5f)
        assertTrue(scale.max >= 10f)
        assertFalse(scale.ticks.any { !it.isFinite() })
    }
}

class ChartLayoutEngineTest {
    @Test
    fun lineCoordinatesMapScaleEdgesToPlotEdges() {
        val series = listOf(
            ChartSeries(
                name = "series",
                items = listOf(ChartPoint(0f, -10f), ChartPoint(10f, 20f)),
            )
        )
        val layout = ChartLayoutEngine.line(
            width = 320f,
            height = 240f,
            series = series,
            margins = ChartMargins(),
            tickCount = 5,
            includeZero = true,
        )

        assertEquals(layout.plot.left, layout.xFor(layout.xScale.min), 0.001f)
        assertEquals(layout.plot.right, layout.xFor(layout.xScale.max), 0.001f)
        assertEquals(layout.plot.top, layout.yFor(layout.yScale.max), 0.001f)
        assertEquals(layout.plot.bottom, layout.yFor(layout.yScale.min), 0.001f)
    }

    @Test
    fun barLayoutUsesMaximumCategoryCountAndZeroBaseline() {
        val layout = ChartLayoutEngine.bar(
            width = 300f,
            height = 220f,
            series = listOf(
                ChartSeries("A", listOf(BarEntry("1", 10f), BarEntry("2", -4f))),
                ChartSeries("B", listOf(BarEntry("1", 8f))),
            ),
            margins = ChartMargins(),
            tickCount = 5,
            includeZero = true,
        )

        assertEquals(2, layout.categoryCount)
        assertTrue(layout.yScale.min <= 0f)
        assertTrue(layout.yScale.max >= 10f)
        assertTrue(layout.categoryCenter(0) < layout.categoryCenter(1))
    }

    @Test
    fun barAndMixedLayoutsHonorExplicitYAxisBounds() {
        val bars = ChartLayoutEngine.bar(
            width = 300f,
            height = 220f,
            series = listOf(ChartSeries("A", listOf(BarEntry("one", 40f)))),
            margins = ChartMargins(),
            tickCount = 5,
            includeZero = true,
            yMin = 0f,
            yMax = 200f,
        )
        val mixed = ChartLayoutEngine.mixed(
            width = 300f,
            height = 220f,
            barSeries = listOf(ChartSeries("actual", listOf(BarEntry("one", 40f)))),
            lineSeries = listOf(ChartSeries("target", listOf(BarEntry("one", 80f)))),
            margins = ChartMargins(),
            tickCount = 5,
            includeZero = true,
            yMin = 0f,
            yMax = 200f,
        )

        assertEquals(0f, bars.yScale.min)
        assertEquals(200f, bars.yScale.max)
        assertEquals(0f, mixed.yScale.min)
        assertEquals(200f, mixed.yScale.max)
    }

    @Test
    fun viewportDefaultsToTheLatestWindowAndClampsPanAtBothEnds() {
        val initial = LineViewportController.resolve(
            itemCount = 10,
            requestedStartIndex = LineViewportController.UNSET_START_INDEX,
            visibleItemCount = 4,
        )

        assertNotNull(initial)
        assertEquals(6, initial.startIndex)
        assertEquals(9, initial.endIndex)
        assertTrue(LineViewportController.isPannable(10, initial))

        val earlier = LineViewportController.pan(10, initial, deltaItems = -20)
        val later = LineViewportController.pan(10, earlier, deltaItems = 20)

        assertEquals(0, earlier.startIndex)
        assertEquals(3, earlier.endIndex)
        assertEquals(6, later.startIndex)
        assertEquals(9, later.endIndex)
    }

    @Test
    fun viewportUsesAllItemsWhenNoWindowIsRequested() {
        val viewport = LineViewportController.resolve(3, LineViewportController.UNSET_START_INDEX, 0)

        assertNotNull(viewport)
        assertEquals(0, viewport.startIndex)
        assertEquals(2, viewport.endIndex)
        assertFalse(LineViewportController.isPannable(3, viewport))
        assertNull(LineViewportController.resolve(0, LineViewportController.UNSET_START_INDEX, 4))
    }

    @Test
    fun controlledViewportClampsToTheLatestAvailableOriginalIndexes() {
        val viewport = LineViewportController.resolve(
            itemCount = 6,
            requestedViewport = ChartViewport(startIndex = 4, endIndex = 9),
        )

        assertNotNull(viewport)
        assertEquals(0, viewport.startIndex)
        assertEquals(5, viewport.endIndex)
    }

    @Test
    fun pinchZoomKeepsTheFocusIndexAndHonorsTheConfiguredBounds() {
        val source = ChartViewport(4, 11)
        val zoomed = LineViewportController.zoom(
            itemCount = 20,
            viewport = source,
            focusIndex = 9,
            scale = 2f,
            minVisibleItemCount = 3,
            maxVisibleItemCount = 10,
        )
        val clampedOut = LineViewportController.zoom(
            itemCount = 20,
            viewport = zoomed,
            focusIndex = 9,
            scale = 100f,
            minVisibleItemCount = 3,
            maxVisibleItemCount = 10,
        )
        val clampedIn = LineViewportController.zoom(
            itemCount = 20,
            viewport = clampedOut,
            focusIndex = 9,
            scale = 0.001f,
            minVisibleItemCount = 3,
            maxVisibleItemCount = 10,
        )

        assertEquals(4, zoomed.itemCount)
        assertTrue(9 in zoomed.startIndex..zoomed.endIndex)
        assertEquals(3, clampedOut.itemCount)
        assertEquals(10, clampedIn.itemCount)
        assertTrue(9 in clampedIn.startIndex..clampedIn.endIndex)
    }

    @Test
    fun mixedLayoutSharesCategorySlotsAndNumericDomain() {
        val layout = ChartLayoutEngine.mixed(
            width = 320f,
            height = 240f,
            barSeries = listOf(
                ChartSeries("actual", listOf(BarEntry("A", -20f), BarEntry("B", 80f))),
            ),
            lineSeries = listOf(
                ChartSeries(
                    "target",
                    listOf(BarEntry("A", 100f), BarEntry("B", 120f), BarEntry("C", 140f)),
                ),
            ),
            margins = ChartMargins(),
            tickCount = 5,
            includeZero = true,
        )

        assertEquals(3, layout.categoryCount)
        assertTrue(layout.yScale.min <= -20f)
        assertTrue(layout.yScale.max >= 140f)
        assertTrue(layout.categoryCenter(0) < layout.categoryCenter(2))
    }
}

class ChartHitTestTest {
    @Test
    fun lineReturnsNearestPointWithinRadius() {
        val first = ChartSelection(0, 0, "A", ChartPoint(1f, 10f, "one"))
        val second = ChartSelection(0, 1, "A", ChartPoint(2f, 20f, "two"))
        val points = listOf(
            RenderedLinePoint(20f, 30f, first),
            RenderedLinePoint(80f, 90f, second),
        )

        val selected = ChartHitTest.line(points, x = 23f, y = 31f, radius = 12f)

        assertNotNull(selected)
        assertEquals(0, selected.itemIndex)
        assertNull(ChartHitTest.line(points, x = 200f, y = 200f, radius = 12f))
    }

    @Test
    fun barUsesRenderedBounds() {
        val selection = ChartSelection(0, 0, "orders", BarEntry("search", 100f))
        val bars = listOf(RenderedBar(ChartRect(10f, 20f, 40f, 100f), selection))

        assertEquals(selection, ChartHitTest.bar(bars, 20f, 50f))
        assertNull(ChartHitTest.bar(bars, 100f, 100f))
    }

    @Test
    fun lineTrackerGroupsAllSeriesAtTheNearestRenderedXSlot() {
        val firstSeries = ChartSelection(0, 1, "A", ChartPoint(2f, 20f, "two"))
        val secondSeries = ChartSelection(1, 1, "B", ChartPoint(2f, 40f, "two"))
        val points = listOf(
            RenderedLinePoint(20f, 80f, ChartSelection(0, 0, "A", ChartPoint(1f, 10f, "one"))),
            RenderedLinePoint(80f, 60f, firstSeries),
            RenderedLinePoint(80.3f, 20f, secondSeries),
        )

        val tracker = ChartHitTest.lineTracker(points, x = 75f)

        assertNotNull(tracker)
        assertEquals(2f, tracker.x)
        assertEquals(listOf(firstSeries, secondSeries), tracker.selections)
        assertNull(ChartHitTest.lineTracker(emptyList(), x = 10f))
    }

    @Test
    fun lineSlotWidthDeduplicatesSeriesAtTheSameXCoordinate() {
        val points = listOf(
            RenderedLinePoint(10f, 10f, ChartSelection(0, 0, "A", ChartPoint(1f, 1f))),
            RenderedLinePoint(10.2f, 20f, ChartSelection(1, 0, "B", ChartPoint(1f, 2f))),
            RenderedLinePoint(50f, 10f, ChartSelection(0, 1, "A", ChartPoint(2f, 1f))),
            RenderedLinePoint(90f, 10f, ChartSelection(0, 2, "A", ChartPoint(3f, 1f))),
        )

        val slotWidth = assertNotNull(ChartHitTest.lineSlotWidth(points))
        assertEquals(40f, slotWidth, 0.001f)
        assertNull(ChartHitTest.lineSlotWidth(points.take(1)))
    }

    @Test
    fun pieHitTestHonorsSliceAnglesAndDonutHole() {
        val slices = PieLayoutEngine.layout(
            width = 100f,
            height = 100f,
            entries = listOf(PieEntry("A", 50f), PieEntry("B", 50f)),
            seriesName = "share",
            innerRadiusRatio = 0.5f,
            startAngleDegrees = -90f,
            gapAngleDegrees = 0f,
            legendVisible = false,
        )

        assertEquals(2, slices.size)
        assertEquals("A", ChartHitTest.pie(slices, x = 80f, y = 50f)?.item?.label)
        assertEquals("B", ChartHitTest.pie(slices, x = 20f, y = 50f)?.item?.label)
        assertNull(ChartHitTest.pie(slices, x = 50f, y = 50f))
        assertNull(ChartHitTest.pie(slices, x = 100f, y = 100f))
    }

    @Test
    fun mixedHitTestPrioritizesLinePointsThenFallsBackToBars() {
        val lineSelection = MixedChartSelection(
            MixedSeriesType.LINE,
            0,
            1,
            "target",
            BarEntry("B", 80f),
        )
        val barSelection = MixedChartSelection(
            MixedSeriesType.BAR,
            0,
            1,
            "actual",
            BarEntry("B", 70f),
        )
        val linePoints = listOf(RenderedMixedLinePoint(50f, 40f, lineSelection))
        val bars = listOf(RenderedMixedBar(ChartRect(40f, 30f, 60f, 100f), barSelection))

        assertEquals(lineSelection, ChartHitTest.mixed(linePoints, bars, x = 52f, y = 42f))
        assertEquals(barSelection, ChartHitTest.mixed(linePoints, bars, x = 50f, y = 90f))
        assertNull(ChartHitTest.mixed(linePoints, bars, x = 150f, y = 150f))
    }
}

class LineSamplerTest {
    @Test
    fun minMaxBucketsPreserveExtremaOrderAndOriginalIndexes() {
        val points = List(5_000) { index ->
            val value = when (index) {
                123 -> 10_000f
                4_567 -> -10_000f
                else -> ((index % 97) - 48).toFloat()
            }
            IndexedValue(index, ChartPoint(index.toFloat(), value, index.toString()))
        }

        val sampled = LineSampler.sample(points, maxPointCount = 240)

        assertTrue(sampled.size <= 240)
        assertEquals(0, sampled.first().index)
        assertEquals(4_999, sampled.last().index)
        assertTrue(sampled.any { it.index == 123 })
        assertTrue(sampled.any { it.index == 4_567 })
        assertEquals(sampled.map { it.index }.sorted(), sampled.map { it.index })
    }

    @Test
    fun recordsDenseSamplingBaselineForRequiredPointCounts() {
        listOf(100, 1_000, 5_000).forEach { pointCount ->
            val points = List(pointCount) { index ->
                IndexedValue(
                    index,
                    ChartPoint(
                        x = index.toFloat(),
                        y = 180f + ((index % 37) - 18) * 0.8f + index * 0.01f,
                    ),
                )
            }
            var sampled = emptyList<IndexedValue<ChartPoint>>()
            val mark = TimeSource.Monotonic.markNow()
            repeat(100) {
                sampled = LineSampler.sample(points, maxPointCount = 240)
            }
            val elapsed = mark.elapsedNow()

            if (pointCount <= 240) {
                assertEquals(pointCount, sampled.size)
            } else {
                assertTrue(sampled.size < pointCount)
            }
            assertTrue(sampled.size <= 240)
            println(
                "PERF points=$pointCount sampled=${sampled.size} iterations=100 " +
                    "totalUs=${elapsed.inWholeMicroseconds} avgUs=${elapsed.inWholeMicroseconds / 100}",
            )
        }
    }

    @Test
    fun segmentedSamplingKeepsGapsAndHonorsTheSeriesBudget() {
        val segments = List(3) { segmentIndex ->
            List(100) { itemIndex ->
                val sourceIndex = segmentIndex * 101 + itemIndex
                IndexedValue(
                    sourceIndex,
                    ChartPoint(sourceIndex.toFloat(), (sourceIndex % 41).toFloat()),
                )
            }
        }

        val sampled = LineSampler.sampleSegments(segments, maxPointCount = 12)

        assertEquals(segments.size, sampled.size)
        assertTrue(sampled.flatten().size <= 12)
        sampled.forEachIndexed { segmentIndex, sampledSegment ->
            val sourceIndexes = segments[segmentIndex].mapTo(mutableSetOf()) { it.index }
            assertTrue(sampledSegment.all { it.index in sourceIndexes })
        }
    }
}

class PieLayoutEngineTest {
    @Test
    fun filtersNonPositiveValuesAndPreservesOriginalIndexes() {
        val slices = PieLayoutEngine.layout(
            width = 240f,
            height = 220f,
            entries = listOf(
                PieEntry("A", 30f),
                PieEntry("invalid", Float.NaN),
                PieEntry("zero", 0f),
                PieEntry("B", 70f),
            ),
            seriesName = "share",
            innerRadiusRatio = 0.6f,
            startAngleDegrees = -90f,
            gapAngleDegrees = 1f,
            legendVisible = true,
        )

        assertEquals(listOf(0, 3), slices.map { it.selection.itemIndex })
        assertEquals(1f, slices.sumOf { it.fraction.toDouble() }.toFloat(), 0.001f)
        assertEquals(slices.first().outerRadius * 0.6f, slices.first().innerRadius, 0.001f)
    }

    @Test
    fun largeValuesStayFiniteAndSingleSliceHasNoGap() {
        val large = PieLayoutEngine.layout(
            width = 200f,
            height = 200f,
            entries = listOf(PieEntry("A", Float.MAX_VALUE), PieEntry("B", Float.MAX_VALUE)),
            seriesName = "share",
            innerRadiusRatio = 0f,
            startAngleDegrees = -90f,
            gapAngleDegrees = 2f,
            legendVisible = false,
        )
        val single = PieLayoutEngine.layout(
            width = 200f,
            height = 200f,
            entries = listOf(PieEntry("all", 1f)),
            seriesName = "share",
            innerRadiusRatio = 0f,
            startAngleDegrees = -90f,
            gapAngleDegrees = 2f,
            legendVisible = false,
        ).single()

        assertTrue(large.all { it.fraction.isFinite() })
        assertEquals(0.5f, large.first().fraction, 0.001f)
        assertEquals((kotlin.math.PI * 2.0).toFloat(), single.endAngle - single.startAngle, 0.001f)
    }
}

class ChartValueFormatterTest {
    @Test
    fun formatsCommonMagnitudesDeterministically() {
        assertEquals("950", formatChartValue(950f))
        assertEquals("1.5K", formatChartValue(1_500f))
        assertEquals("2M", formatChartValue(2_000_000f))
        assertEquals("--", formatChartValue(Float.NaN))
    }
}

class ChartDslValidationTest {
    @Test
    fun rejectsInvalidAxisConfigurationWithFieldName() {
        val error = assertFailsWith<IllegalArgumentException> {
            LineChartAttr().yAxis { tickCount = 1 }
        }

        assertTrue(error.message.orEmpty().contains("axis.tickCount"))
    }

    @Test
    fun rejectsBlankSeriesName() {
        val error = assertFailsWith<IllegalArgumentException> {
            LineChartAttr().data(ChartSeries("", listOf(ChartPoint(1f, 2f))))
        }

        assertTrue(error.message.orEmpty().contains("series name"))
    }

    @Test
    fun singleBarModeRejectsMultipleSeriesRegardlessOfDslOrder() {
        val first = ChartSeries("A", listOf(BarEntry("one", 1f)))
        val second = ChartSeries("B", listOf(BarEntry("one", 2f)))

        val dataFirst = BarChartAttr().apply { data(first, second) }
        assertFailsWith<IllegalArgumentException> {
            dataFirst.bars { mode = BarMode.SINGLE }
        }

        val modeFirst = BarChartAttr().apply { bars { mode = BarMode.SINGLE } }
        assertFailsWith<IllegalArgumentException> {
            modeFirst.data(first, second)
        }
    }

    @Test
    fun areaChartIncludesZeroAndRejectsAnEmptyFillPalette() {
        val attr = AreaChartAttr()

        assertTrue(attr.yAxisOptions.includeZero)
        val error = assertFailsWith<IllegalArgumentException> {
            attr.area { fillColors = emptyList() }
        }
        assertTrue(error.message.orEmpty().contains("area.fillColors"))
    }

    @Test
    fun pieChartValidatesLabelsAndPolarGeometry() {
        assertFailsWith<IllegalArgumentException> {
            PieChartAttr().data(PieEntry("", 10f))
        }

        val ratioError = assertFailsWith<IllegalArgumentException> {
            PieChartAttr().pie { innerRadiusRatio = 0.9f }
        }
        assertTrue(ratioError.message.orEmpty().contains("pie.innerRadiusRatio"))

        val gapError = assertFailsWith<IllegalArgumentException> {
            PieChartAttr().pie { gapAngleDegrees = 11f }
        }
        assertTrue(gapError.message.orEmpty().contains("pie.gapAngleDegrees"))
    }

    @Test
    fun mixedChartValidatesSeriesNamesAndSingleBarMode() {
        assertFailsWith<IllegalArgumentException> {
            MixedChartAttr().lineData(ChartSeries("", listOf(BarEntry("A", 1f))))
        }

        val first = ChartSeries("A", listOf(BarEntry("one", 1f)))
        val second = ChartSeries("B", listOf(BarEntry("one", 2f)))
        val attr = MixedChartAttr().apply { barData(first, second) }
        assertFailsWith<IllegalArgumentException> {
            attr.bars { mode = BarMode.SINGLE }
        }
    }

    @Test
    fun sparklineUsesCompactDefaultsAndRejectsMultipleSeries() {
        val attr = SparklineChartAttr()

        assertFalse(attr.xAxisOptions.visible)
        assertFalse(attr.yAxisOptions.visible)
        assertFalse(attr.gridOptions.visible)
        assertFalse(attr.legendOptions.visible)
        assertFalse(attr.tooltipOptions.enabled)
        assertFalse(attr.lineOptions.showPoints)
        assertTrue(attr.lineOptions.smooth)
        assertFalse(attr.selectable)

        val first = ChartSeries("A", listOf(ChartPoint(1f, 1f)))
        val second = ChartSeries("B", listOf(ChartPoint(1f, 2f)))
        val error = assertFailsWith<IllegalArgumentException> {
            attr.data(first, second)
        }
        assertTrue(error.message.orEmpty().contains("SparklineChart"))
    }

    @Test
    fun lineSamplingBudgetRejectsAmbiguousSmallValues() {
        val error = assertFailsWith<IllegalArgumentException> {
            LineChartAttr().interaction { maxRenderPointCount = 3 }
        }

        assertTrue(error.message.orEmpty().contains("interaction.maxRenderPointCount"))
    }

    @Test
    fun interactionValidatesViewportZoomBounds() {
        val minimum = assertFailsWith<IllegalArgumentException> {
            LineChartAttr().interaction { minVisibleItemCount = 0 }
        }
        val maximum = assertFailsWith<IllegalArgumentException> {
            MixedChartAttr().interaction {
                minVisibleItemCount = 5
                maxVisibleItemCount = 4
            }
        }

        assertTrue(minimum.message.orEmpty().contains("interaction.minVisibleItemCount"))
        assertTrue(maximum.message.orEmpty().contains("interaction.maxVisibleItemCount"))
    }

    @Test
    fun crosshairAndBrushAreOptInAndKeepExistingTrackerDefaults() {
        val attr = LineChartAttr()

        assertFalse(attr.crosshairOptions.enabled)
        assertTrue(attr.crosshairOptions.showHorizontalGuide)
        assertFalse(attr.brushOptions.enabled)
        assertFalse(attr.brushOptions.zoomToSelectionOnRelease)

        attr.crosshair { enabled = true }
        attr.brush {
            enabled = true
            zoomToSelectionOnRelease = true
        }

        assertTrue(attr.crosshairOptions.enabled)
        assertTrue(attr.brushOptions.enabled)
        assertTrue(attr.brushOptions.zoomToSelectionOnRelease)
    }
}
