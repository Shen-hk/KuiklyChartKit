package com.kuikly.kuiklychartkit.chart

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
}
