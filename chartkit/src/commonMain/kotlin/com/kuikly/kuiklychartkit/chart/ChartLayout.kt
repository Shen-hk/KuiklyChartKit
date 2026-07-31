package com.kuikly.kuiklychartkit.chart

import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

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
    fun fromValues(
        values: List<Float>,
        tickCount: Int,
        includeZero: Boolean,
        minValue: Float? = null,
        maxValue: Float? = null,
    ): AxisScale {
        val finite = values.filter { it.isFinite() }
        if (finite.isEmpty()) return AxisScale(0f, 1f, 0.25f, listOf(0f, 0.25f, 0.5f, 0.75f, 1f))

        var dataMin = minValue ?: finite.minOrNull() ?: 0f
        var dataMax = maxValue ?: finite.maxOrNull() ?: 1f
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
        yMin: Float? = null,
        yMax: Float? = null,
    ): LineChartLayout {
        val points = series.flatMap { it.items }.filter { it.x.isFinite() && it.y.isFinite() }
        val plot = plotRect(width, height, margins)
        return LineChartLayout(
            plot = plot,
            xScale = NiceScale.fromValues(points.map { it.x }, tickCount = 5, includeZero = false),
            yScale = NiceScale.fromValues(
                values = points.map { it.y },
                tickCount = tickCount,
                includeZero = includeZero,
                minValue = yMin,
                maxValue = yMax,
            ),
        )
    }

    fun bar(
        width: Float,
        height: Float,
        series: List<ChartSeries<BarEntry>>,
        margins: ChartMargins,
        tickCount: Int,
        includeZero: Boolean,
        yMin: Float? = null,
        yMax: Float? = null,
    ): BarChartLayout {
        val categoryCount = series.maxOfOrNull { it.items.size } ?: 0
        return BarChartLayout(
            plot = plotRect(width, height, margins),
            yScale = NiceScale.fromValues(
                series.flatMap { it.items }.map { it.value },
                tickCount,
                includeZero = includeZero,
                minValue = yMin,
                maxValue = yMax,
            ),
            categoryCount = categoryCount,
        )
    }

    fun mixed(
        width: Float,
        height: Float,
        barSeries: List<ChartSeries<BarEntry>>,
        lineSeries: List<ChartSeries<BarEntry>>,
        margins: ChartMargins,
        tickCount: Int,
        includeZero: Boolean,
        yMin: Float? = null,
        yMax: Float? = null,
    ): BarChartLayout = bar(
        width = width,
        height = height,
        series = barSeries + lineSeries,
        margins = margins,
        tickCount = tickCount,
        includeZero = includeZero,
        yMin = yMin,
        yMax = yMax,
    )

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

/** Pure viewport calculations shared by rendering, gestures and common tests. */
internal object LineViewportController {
    const val UNSET_START_INDEX = -1

    fun resolve(itemCount: Int, requestedStartIndex: Int, visibleItemCount: Int): ChartViewport? {
        return resolve(itemCount, null, requestedStartIndex, visibleItemCount)
    }

    fun resolve(
        itemCount: Int,
        requestedViewport: ChartViewport?,
        requestedStartIndex: Int = UNSET_START_INDEX,
        visibleItemCount: Int = 0,
    ): ChartViewport? {
        if (itemCount <= 0) return null
        requestedViewport?.let { viewport ->
            val visibleCount = viewport.itemCount.coerceIn(1, itemCount)
            val maxStartIndex = itemCount - visibleCount
            val startIndex = viewport.startIndex.coerceIn(0, maxStartIndex)
            return ChartViewport(startIndex, startIndex + visibleCount - 1)
        }
        val visibleCount = if (visibleItemCount <= 0) itemCount else visibleItemCount.coerceIn(1, itemCount)
        val maxStartIndex = itemCount - visibleCount
        val startIndex = if (requestedStartIndex == UNSET_START_INDEX) {
            maxStartIndex
        } else {
            requestedStartIndex.coerceIn(0, maxStartIndex)
        }
        return ChartViewport(startIndex, startIndex + visibleCount - 1)
    }

    fun isPannable(itemCount: Int, viewport: ChartViewport): Boolean = itemCount > viewport.itemCount

    fun pan(itemCount: Int, viewport: ChartViewport, deltaItems: Int): ChartViewport {
        val maxStartIndex = (itemCount - viewport.itemCount).coerceAtLeast(0)
        val startIndex = (viewport.startIndex + deltaItems).coerceIn(0, maxStartIndex)
        return ChartViewport(startIndex, startIndex + viewport.itemCount - 1)
    }

    /** Resolves a pinch scale around [focusIndex] while preserving original item indexes. */
    fun zoom(
        itemCount: Int,
        viewport: ChartViewport,
        focusIndex: Int,
        scale: Float,
        minVisibleItemCount: Int,
        maxVisibleItemCount: Int,
    ): ChartViewport {
        if (itemCount <= 0 || !scale.isFinite() || scale <= 0f) return viewport
        val minimum = minVisibleItemCount.coerceIn(1, itemCount)
        val maximum = if (maxVisibleItemCount <= 0) itemCount else maxVisibleItemCount.coerceIn(minimum, itemCount)
        val currentCount = viewport.itemCount.coerceIn(minimum, maximum)
        val targetCount = (currentCount / scale).roundToInt().coerceIn(minimum, maximum)
        val clampedFocus = focusIndex.coerceIn(0, itemCount - 1)
        val focusFraction = if (viewport.itemCount <= 1) 0.5f else {
            ((clampedFocus - viewport.startIndex).toFloat() / (viewport.itemCount - 1)).coerceIn(0f, 1f)
        }
        val startIndex = (clampedFocus - focusFraction * (targetCount - 1)).roundToInt()
            .coerceIn(0, itemCount - targetCount)
        return ChartViewport(startIndex, startIndex + targetCount - 1)
    }
}

/** Deterministic min/max bucket sampling that preserves source indexes and extrema. */
internal object LineSampler {
    fun sample(
        points: List<IndexedValue<ChartPoint>>,
        maxPointCount: Int,
    ): List<IndexedValue<ChartPoint>> {
        if (points.size <= maxPointCount || maxPointCount <= 0) return points
        if (maxPointCount == 1) return listOf(points.first())
        if (maxPointCount <= 3) return listOf(points.first(), points.last()).distinctBy { it.index }

        val bucketCount = ((maxPointCount - 2) / 2).coerceAtLeast(1)
        val interiorCount = points.size - 2
        val sampled = mutableListOf(points.first())
        repeat(bucketCount) { bucketIndex ->
            val start = 1 + (interiorCount * bucketIndex) / bucketCount
            val endExclusive = 1 + (interiorCount * (bucketIndex + 1)) / bucketCount
            if (start >= endExclusive) return@repeat
            val bucket = points.subList(start, endExclusive)
            val minimum = bucket.minByOrNull { it.value.y }
            val maximum = bucket.maxByOrNull { it.value.y }
            listOfNotNull(minimum, maximum)
                .distinctBy { it.index }
                .sortedBy { it.index }
                .forEach(sampled::add)
        }
        sampled += points.last()
        return sampled.distinctBy { it.index }.sortedBy { it.index }
    }

    fun sampleSegments(
        segments: List<List<IndexedValue<ChartPoint>>>,
        maxPointCount: Int,
    ): List<List<IndexedValue<ChartPoint>>> {
        val flattened = segments.flatten()
        if (flattened.size <= maxPointCount || maxPointCount <= 0) return segments

        val sampledIndexes = sample(flattened, maxPointCount)
            .mapTo(mutableSetOf()) { it.index }
        return segments.map { segment ->
            segment.filter { it.index in sampledIndexes }
        }
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

internal data class RenderedPieSlice(
    val centerX: Float,
    val centerY: Float,
    val innerRadius: Float,
    val outerRadius: Float,
    val startAngle: Float,
    val endAngle: Float,
    val fraction: Float,
    val selection: ChartSelection<PieEntry>,
) {
    val middleAngle: Float get() = startAngle + (endAngle - startAngle) / 2f
}

internal data class RenderedMixedBar(
    val bounds: ChartRect,
    val selection: MixedChartSelection,
)

internal data class RenderedMixedLinePoint(
    val x: Float,
    val y: Float,
    val selection: MixedChartSelection,
)

internal data class RenderedMixedChart(
    val bars: List<RenderedMixedBar>,
    val linePoints: List<RenderedMixedLinePoint>,
)

/** Pure polar layout shared by the pie renderer, hit testing and common tests. */
internal object PieLayoutEngine {
    private const val CHART_PADDING = 14f
    private const val LEGEND_HEIGHT = 32f

    fun layout(
        width: Float,
        height: Float,
        entries: List<PieEntry>,
        seriesName: String,
        innerRadiusRatio: Float,
        startAngleDegrees: Float,
        gapAngleDegrees: Float,
        legendVisible: Boolean,
    ): List<RenderedPieSlice> {
        val indexedEntries = entries.withIndex().filter { (_, entry) ->
            entry.value.isFinite() && entry.value > 0f
        }
        if (width <= 0f || height <= 0f || indexedEntries.isEmpty()) return emptyList()

        val topInset = if (legendVisible) LEGEND_HEIGHT else 0f
        val availableHeight = (height - topInset).coerceAtLeast(0f)
        val outerRadius = (min(width, availableHeight) / 2f - CHART_PADDING).coerceAtLeast(0f)
        if (outerRadius <= 0f) return emptyList()

        val centerX = width / 2f
        val centerY = topInset + availableHeight / 2f
        val innerRadius = outerRadius * innerRadiusRatio
        val total = indexedEntries.sumOf { it.value.value.toDouble() }
        val fullCircle = (PI * 2.0).toFloat()
        val requestedGap = gapAngleDegrees / 180f * PI.toFloat()
        var cursor = startAngleDegrees / 180f * PI.toFloat()

        return indexedEntries.map { indexed ->
            val fraction = (indexed.value.value.toDouble() / total).toFloat()
            val rawSweep = fullCircle * fraction
            val actualGap = if (indexedEntries.size == 1) 0f else min(requestedGap, rawSweep * 0.4f)
            val startAngle = cursor + actualGap / 2f
            val endAngle = cursor + rawSweep - actualGap / 2f
            cursor += rawSweep
            RenderedPieSlice(
                centerX = centerX,
                centerY = centerY,
                innerRadius = innerRadius,
                outerRadius = outerRadius,
                startAngle = startAngle,
                endAngle = endAngle,
                fraction = fraction,
                selection = ChartSelection(
                    seriesIndex = 0,
                    itemIndex = indexed.index,
                    seriesName = seriesName,
                    item = indexed.value,
                ),
            )
        }
    }
}
