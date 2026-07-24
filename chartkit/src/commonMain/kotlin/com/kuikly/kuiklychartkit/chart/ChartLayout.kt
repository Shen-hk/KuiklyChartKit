package com.kuikly.kuiklychartkit.chart

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal data class ChartRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)

    fun contains(x: Float, y: Float, extra: Float = 0f): Boolean =
        x >= left - extra && x <= right + extra && y >= top - extra && y <= bottom + extra
}

internal data class ChartMargins(
    val left: Float = 48f,
    val top: Float = 18f,
    val right: Float = 16f,
    val bottom: Float = 36f,
)

internal data class AxisScale(
    val min: Float,
    val max: Float,
    val step: Float,
    val ticks: List<Float>,
) {
    val range: Float get() = (max - min).coerceAtLeast(0.000001f)
}

internal object NiceScale {
    fun fromValues(values: List<Float>, tickCount: Int, includeZero: Boolean): AxisScale {
        val finite = values.filter { it.isFinite() }
        if (finite.isEmpty()) return AxisScale(0f, 1f, 0.25f, listOf(0f, 0.25f, 0.5f, 0.75f, 1f))

        var dataMin = finite.minOrNull() ?: 0f
        var dataMax = finite.maxOrNull() ?: 1f
        if (includeZero) {
            dataMin = min(dataMin, 0f)
            dataMax = max(dataMax, 0f)
        }
        if (abs(dataMax - dataMin) < 0.000001f) {
            val padding = max(abs(dataMin) * 0.1f, 1f)
            dataMin -= padding
            dataMax += padding
            if (includeZero) dataMin = min(dataMin, 0f)
        }

        val desiredTicks = tickCount.coerceIn(2, 10)
        val niceRange = niceNumber(dataMax - dataMin, round = false)
        val step = niceNumber(niceRange / (desiredTicks - 1), round = true).coerceAtLeast(0.000001f)
        val niceMin = floor(dataMin / step) * step
        val niceMax = ceil(dataMax / step) * step
        val ticks = mutableListOf<Float>()
        var tick = niceMin
        var guard = 0
        while (tick <= niceMax + step * 0.5f && guard < 100) {
            ticks += normalizeZero(tick)
            tick += step
            guard++
        }
        return AxisScale(normalizeZero(niceMin), normalizeZero(niceMax), step, ticks)
    }

    private fun niceNumber(value: Float, round: Boolean): Float {
        if (!value.isFinite() || value <= 0f) return 1f
        val exponent = floor(log10(value.toDouble())).toInt()
        val magnitude = 10.0.pow(exponent.toDouble()).toFloat()
        val fraction = value / magnitude
        val niceFraction = if (round) {
            when {
                fraction < 1.5f -> 1f
                fraction < 3f -> 2f
                fraction < 7f -> 5f
                else -> 10f
            }
        } else {
            when {
                fraction <= 1f -> 1f
                fraction <= 2f -> 2f
                fraction <= 5f -> 5f
                else -> 10f
            }
        }
        return niceFraction * magnitude
    }

    private fun normalizeZero(value: Float): Float = if (abs(value) < 0.000001f) 0f else value
}

internal data class LineChartLayout(
    val plot: ChartRect,
    val xScale: AxisScale,
    val yScale: AxisScale,
) {
    fun xFor(value: Float): Float = plot.left + ((value - xScale.min) / xScale.range) * plot.width
    fun yFor(value: Float): Float = plot.bottom - ((value - yScale.min) / yScale.range) * plot.height
}

internal data class BarChartLayout(
    val plot: ChartRect,
    val yScale: AxisScale,
    val categoryCount: Int,
) {
    val slotWidth: Float get() = if (categoryCount <= 0) plot.width else plot.width / categoryCount
    fun categoryCenter(index: Int): Float = plot.left + slotWidth * (index + 0.5f)
    fun yFor(value: Float): Float = plot.bottom - ((value - yScale.min) / yScale.range) * plot.height
}

internal object ChartLayoutEngine {
    fun line(
        width: Float,
        height: Float,
        series: List<ChartSeries<ChartPoint>>,
        margins: ChartMargins,
        tickCount: Int,
        includeZero: Boolean,
    ): LineChartLayout {
        val points = series.flatMap { it.items }.filter { it.x.isFinite() && it.y.isFinite() }
        val plot = plotRect(width, height, margins)
        return LineChartLayout(
            plot = plot,
            xScale = NiceScale.fromValues(points.map { it.x }, tickCount = 5, includeZero = false),
            yScale = NiceScale.fromValues(points.map { it.y }, tickCount, includeZero),
        )
    }

    fun bar(
        width: Float,
        height: Float,
        series: List<ChartSeries<BarEntry>>,
        margins: ChartMargins,
        tickCount: Int,
        includeZero: Boolean,
    ): BarChartLayout {
        val categoryCount = series.maxOfOrNull { it.items.size } ?: 0
        return BarChartLayout(
            plot = plotRect(width, height, margins),
            yScale = NiceScale.fromValues(
                series.flatMap { it.items }.map { it.value },
                tickCount,
                includeZero = includeZero,
            ),
            categoryCount = categoryCount,
        )
    }

    private fun plotRect(width: Float, height: Float, margins: ChartMargins): ChartRect {
        val safeWidth = width.coerceAtLeast(0f)
        val safeHeight = height.coerceAtLeast(0f)
        val left = margins.left.coerceIn(0f, safeWidth)
        val top = margins.top.coerceIn(0f, safeHeight)
        val right = (safeWidth - margins.right).coerceAtLeast(left)
        val bottom = (safeHeight - margins.bottom).coerceAtLeast(top)
        return ChartRect(left, top, right, bottom)
    }
}

internal data class RenderedLinePoint(
    val x: Float,
    val y: Float,
    val selection: ChartSelection<ChartPoint>,
)

internal data class RenderedBar(
    val bounds: ChartRect,
    val selection: ChartSelection<BarEntry>,
)

internal object ChartHitTest {
    fun line(points: List<RenderedLinePoint>, x: Float, y: Float, radius: Float = 24f): ChartSelection<ChartPoint>? {
        var best: RenderedLinePoint? = null
        var bestDistanceSquared = radius * radius
        points.forEach { point ->
            val dx = point.x - x
            val dy = point.y - y
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared <= bestDistanceSquared) {
                bestDistanceSquared = distanceSquared
                best = point
            }
        }
        return best?.selection
    }

    fun bar(bars: List<RenderedBar>, x: Float, y: Float): ChartSelection<BarEntry>? =
        bars.firstOrNull { it.bounds.contains(x, y, extra = 6f) }?.selection
}
