package com.kuikly.kuiklychartkit.chart

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** One laid-out heatmap cell retained for rendering and hit testing. */
internal data class RenderedHeatmapCell(
    val bounds: ChartRect,
    val fraction: Float,
    val selection: ChartSelection<HeatmapEntry>,
)

/** Pure heatmap geometry shared by Canvas rendering and common tests. */
internal data class HeatmapLayout(
    val plot: ChartRect,
    val xLabels: List<String>,
    val yLabels: List<String>,
    val cells: List<RenderedHeatmapCell>,
)

/** Deterministic two-dimensional category layout for [HeatmapChart]. */
internal object HeatmapLayoutEngine {
    fun layout(
        plot: ChartRect,
        entries: List<HeatmapEntry>,
        seriesName: String,
        cellGap: Float,
    ): HeatmapLayout {
        if (plot.width <= 0f || plot.height <= 0f) {
            return HeatmapLayout(plot, emptyList(), emptyList(), emptyList())
        }

        val retained = mutableListOf<IndexedValue<HeatmapEntry>>()
        val seenCoordinates = mutableSetOf<Pair<String, String>>()
        entries.forEachIndexed { index, entry ->
            if (!entry.value.isFinite() || entry.xLabel.isBlank() || entry.yLabel.isBlank()) return@forEachIndexed
            if (seenCoordinates.add(entry.xLabel to entry.yLabel)) {
                retained += IndexedValue(index, entry)
            }
        }
        if (retained.isEmpty()) return HeatmapLayout(plot, emptyList(), emptyList(), emptyList())

        val xLabels = mutableListOf<String>()
        val yLabels = mutableListOf<String>()
        retained.forEach { indexed ->
            if (indexed.value.xLabel !in xLabels) xLabels += indexed.value.xLabel
            if (indexed.value.yLabel !in yLabels) yLabels += indexed.value.yLabel
        }
        if (xLabels.isEmpty() || yLabels.isEmpty()) {
            return HeatmapLayout(plot, xLabels, yLabels, emptyList())
        }

        val cellWidth = plot.width / xLabels.size
        val cellHeight = plot.height / yLabels.size
        if (cellWidth <= 0f || cellHeight <= 0f) {
            return HeatmapLayout(plot, xLabels, yLabels, emptyList())
        }

        val gap = cellGap.coerceIn(0f, min(cellWidth, cellHeight) * 0.5f)
        val minimum = retained.minOf { it.value.value }
        val maximum = retained.maxOf { it.value.value }
        val range = maximum - minimum
        val cells = retained.map { indexed ->
            val entry = indexed.value
            val column = xLabels.indexOf(entry.xLabel)
            val row = yLabels.indexOf(entry.yLabel)
            val left = plot.left + column * cellWidth + gap / 2f
            val top = plot.top + row * cellHeight + gap / 2f
            val fraction = if (abs(range) < 0.000001f) {
                0.5f
            } else {
                ((entry.value - minimum) / range).coerceIn(0f, 1f)
            }
            RenderedHeatmapCell(
                bounds = ChartRect(left, top, left + cellWidth - gap, top + cellHeight - gap),
                fraction = fraction,
                selection = ChartSelection(
                    seriesIndex = 0,
                    itemIndex = indexed.index,
                    seriesName = seriesName,
                    item = entry,
                ),
            )
        }
        return HeatmapLayout(plot, xLabels, yLabels, cells)
    }
}

/** A radar dimension endpoint and its screen-space angle. */
internal data class RadarAxis(
    val label: String,
    val angle: Float,
    val x: Float,
    val y: Float,
)

/** One rendered radar metric with a selection payload from the original input. */
internal data class RenderedRadarPoint(
    val x: Float,
    val y: Float,
    val selection: ChartSelection<RadarEntry>,
)

/** Geometry for one radar series, including gaps caused by invalid values. */
internal data class RenderedRadarSeries(
    val points: List<RenderedRadarPoint>,
    val segments: List<List<RenderedRadarPoint>>,
    val closesPolygon: Boolean,
)

/** Pure polar geometry shared by the radar renderer and hit testing. */
internal data class RadarLayout(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val axes: List<RadarAxis>,
    val scale: AxisScale,
    val gridCount: Int,
    val series: List<RenderedRadarSeries>,
)

/** Deterministic radar layout that keeps non-finite and negative values as gaps. */
internal object RadarLayoutEngine {
    fun layout(
        width: Float,
        height: Float,
        series: List<ChartSeries<RadarEntry>>,
        gridCount: Int,
        labelInset: Float,
        legendVisible: Boolean,
    ): RadarLayout {
        val fallbackScale = NiceScale.fromValues(emptyList(), tickCount = 5, includeZero = true)
        val dimensionSource = series.firstOrNull { it.items.isNotEmpty() } ?: return emptyLayout(fallbackScale, gridCount)
        val labels = dimensionSource.items.map { it.label }
        if (width <= 0f || height <= 0f || labels.size < 3 || labels.any { it.isBlank() }) {
            return emptyLayout(fallbackScale, gridCount)
        }

        val topInset = if (legendVisible) 32f else 0f
        val availableHeight = (height - topInset).coerceAtLeast(0f)
        val radius = (min(width, availableHeight) / 2f - labelInset).coerceAtLeast(0f)
        if (radius <= 0f) return emptyLayout(fallbackScale, gridCount)

        val values = series.flatMap { chartSeries ->
            chartSeries.items.mapNotNull { entry -> entry.value.takeIf { it.isFinite() && it >= 0f } }
        }
        val suggestedScale = NiceScale.fromValues(
            values,
            tickCount = (gridCount + 1).coerceIn(2, 10),
            includeZero = true,
        )
        // A radial metric always starts at the center. NiceScale expands equal
        // zero values around zero, so normalize that special domain back to 0.
        val scale = if (suggestedScale.min < 0f) {
            val maximum = suggestedScale.max.coerceAtLeast(1f)
            AxisScale(
                min = 0f,
                max = maximum,
                step = maximum / gridCount,
                ticks = (0..gridCount).map { maximum * it / gridCount },
            )
        } else {
            suggestedScale
        }
        val centerX = width / 2f
        val centerY = topInset + availableHeight / 2f
        val fullCircle = (PI * 2.0).toFloat()
        val axes = labels.mapIndexed { index, label ->
            val angle = -PI.toFloat() / 2f + fullCircle * index / labels.size
            RadarAxis(
                label = label,
                angle = angle,
                x = centerX + cos(angle.toDouble()).toFloat() * radius,
                y = centerY + sin(angle.toDouble()).toFloat() * radius,
            )
        }

        val renderedSeries = series.mapIndexed { seriesIndex, chartSeries ->
            val segments = mutableListOf<MutableList<RenderedRadarPoint>>()
            var segment = mutableListOf<RenderedRadarPoint>()
            chartSeries.items.forEachIndexed { itemIndex, entry ->
                if (!entry.value.isFinite() || entry.value < 0f || itemIndex >= axes.size) {
                    if (segment.isNotEmpty()) segments += segment
                    segment = mutableListOf()
                } else {
                    val fraction = ((entry.value - scale.min) / scale.range).coerceIn(0f, 1f)
                    val axis = axes[itemIndex]
                    segment += RenderedRadarPoint(
                        x = centerX + (axis.x - centerX) * fraction,
                        y = centerY + (axis.y - centerY) * fraction,
                        selection = ChartSelection(
                            seriesIndex = seriesIndex,
                            itemIndex = itemIndex,
                            seriesName = chartSeries.name,
                            item = entry,
                        ),
                    )
                }
            }
            if (segment.isNotEmpty()) segments += segment
            val points = segments.flatten()
            RenderedRadarSeries(
                points = points,
                segments = segments,
                closesPolygon = segments.size == 1 && segments.single().size == axes.size && chartSeries.items.size == axes.size,
            )
        }

        return RadarLayout(centerX, centerY, radius, axes, scale, gridCount, renderedSeries)
    }

    private fun emptyLayout(scale: AxisScale, gridCount: Int): RadarLayout = RadarLayout(
        centerX = 0f,
        centerY = 0f,
        radius = 0f,
        axes = emptyList(),
        scale = scale,
        gridCount = gridCount,
        series = emptyList(),
    )
}

/** P2 hit testing uses the exact geometry emitted by the matching layout engine. */
internal object P2ChartHitTest {
    fun heatmap(cells: List<RenderedHeatmapCell>, x: Float, y: Float): ChartSelection<HeatmapEntry>? =
        cells.firstOrNull { it.bounds.contains(x, y) }?.selection

    fun radar(
        points: List<RenderedRadarPoint>,
        x: Float,
        y: Float,
        radius: Float = 24f,
    ): ChartSelection<RadarEntry>? {
        var best: RenderedRadarPoint? = null
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
}

/** Pure radial projection shared by the radar gesture and the canvas painter. */
internal object RadarTrackerProjection {
    fun previewValue(
        layout: RadarLayout,
        selection: ChartSelection<RadarEntry>,
        x: Float,
        y: Float,
    ): Float? {
        val axis = layout.axes.getOrNull(selection.itemIndex) ?: return null
        val radiusX = axis.x - layout.centerX
        val radiusY = axis.y - layout.centerY
        val radiusSquared = radiusX * radiusX + radiusY * radiusY
        if (radiusSquared <= 0f || layout.scale.range <= 0f) return null

        val pointerX = x - layout.centerX
        val pointerY = y - layout.centerY
        val fraction = ((pointerX * radiusX + pointerY * radiusY) / radiusSquared).coerceIn(0f, 1f)
        return layout.scale.min + layout.scale.range * fraction
    }

    fun point(layout: RadarLayout, tracker: RadarTracker): RenderedRadarPoint? {
        val axis = layout.axes.getOrNull(tracker.selection.itemIndex) ?: return null
        if (layout.scale.range <= 0f) return null
        val fraction = ((tracker.previewValue - layout.scale.min) / layout.scale.range).coerceIn(0f, 1f)
        return RenderedRadarPoint(
            x = layout.centerX + (axis.x - layout.centerX) * fraction,
            y = layout.centerY + (axis.y - layout.centerY) * fraction,
            selection = tracker.selection,
        )
    }

    fun replacePoint(
        series: List<RenderedRadarSeries>,
        tracker: RadarTracker?,
        replacement: RenderedRadarPoint?,
    ): List<RenderedRadarSeries> {
        if (tracker == null || replacement == null) return series
        return series.mapIndexed { seriesIndex, renderedSeries ->
            if (seriesIndex != tracker.selection.seriesIndex) return@mapIndexed renderedSeries
            fun replace(points: List<RenderedRadarPoint>) = points.map { point ->
                if (point.selection == tracker.selection) replacement else point
            }
            renderedSeries.copy(
                points = replace(renderedSeries.points),
                segments = renderedSeries.segments.map(::replace),
            )
        }
    }
}
