package com.kuikly.kuiklychartkit.chart

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.TextAlign
import com.kuikly.kuiklychartkit.chart.interaction.ChartSelectionState
import com.kuikly.kuiklychartkit.chart.interaction.PolarChartHitTest
import com.kuikly.kuiklychartkit.chart.interaction.ChartFrameAnimation
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Attribute DSL for [HeatmapChartView]. */
class HeatmapChartAttr : ComposeAttr() {
    internal var entries: List<HeatmapEntry> by observable(emptyList())

    /** Entry reveal progress supplied by the host, from 0 (hidden) to 1 (complete). */
    var entranceProgress: Float by observable(1f)

    /** Stable series name returned in selection callbacks. */
    var seriesName: String by observable("热度")

    /** Active visual theme. Reassigning it triggers a redraw. */
    var theme: ChartTheme by observable(ChartTheme.light())

    /** Mutable options configured by [tooltip]. */
    val tooltipOptions = TooltipOptions()

    /** Mutable options configured by [heatmap]. */
    val heatmapOptions = HeatmapOptions()

    /** Replaces the immutable heatmap cell snapshot. */
    fun data(vararg value: HeatmapEntry) {
        value.forEach {
            require(it.xLabel.isNotBlank()) { "HeatmapChart xLabel must not be blank" }
            require(it.yLabel.isNotBlank()) { "HeatmapChart yLabel must not be blank" }
        }
        val coordinates = value.map { it.xLabel to it.yLabel }
        require(coordinates.distinct().size == coordinates.size) {
            "HeatmapChart must not contain duplicate xLabel/yLabel cells"
        }
        entries = value.toList()
    }

    /** Configures click selection feedback and value formatting. */
    fun tooltip(block: TooltipOptions.() -> Unit) = tooltipOptions.apply(block)

    /** Configures heatmap cell spacing, green intensity buckets and optional values. */
    fun heatmap(block: HeatmapOptions.() -> Unit) {
        heatmapOptions.apply(block)
        require(heatmapOptions.cellGap >= 0f) { "heatmap.cellGap must be >= 0" }
    }

    /** Configures the chart-local entry reveal without animating its container. */
    fun entrance(block: EntranceOptions.() -> Unit) = EntranceOptions(entranceProgress).apply(block).also {
        entranceProgress = it.progress.coerceIn(0f, 1f)
    }
}

/** Attribute DSL for [RadarChartView]. */
class RadarChartAttr : ComposeAttr() {
    internal var series: List<ChartSeries<RadarEntry>> by observable(emptyList())

    /** Entry reveal progress supplied by the host, from 0 (hidden) to 1 (complete). */
    var entranceProgress: Float by observable(1f)

    /** Active visual theme. Reassigning it triggers a redraw. */
    var theme: ChartTheme by observable(ChartTheme.light())

    /** Mutable options configured by [legend]. */
    val legendOptions = LegendOptions()

    /** Mutable options configured by [tooltip]. */
    val tooltipOptions = TooltipOptions()

    /** Mutable options configured by [radar]. */
    val radarOptions = RadarOptions()

    /**
     * Replaces the immutable radar series snapshot.
     *
     * Every non-empty series must use the same ordered dimensions so a vertex
     * always carries the same visual and callback meaning across series.
     */
    fun data(vararg value: ChartSeries<RadarEntry>) {
        value.forEach { chartSeries ->
            require(chartSeries.name.isNotBlank()) { "RadarChart series name must not be blank" }
            chartSeries.items.forEach { entry ->
                require(entry.label.isNotBlank()) { "RadarChart dimension label must not be blank" }
            }
        }
        val dimensions = value.firstOrNull { it.items.isNotEmpty() }?.items?.map { it.label }
        dimensions?.let { labels ->
            require(labels.size >= 3) { "RadarChart requires at least 3 dimensions" }
            require(labels.distinct().size == labels.size) { "RadarChart dimension labels must be unique" }
            value.filter { it.items.isNotEmpty() }.forEach { chartSeries ->
                require(chartSeries.items.map { it.label } == labels) {
                    "RadarChart series must use the same ordered dimension labels"
                }
            }
        }
        series = value.toList()
    }

    /** Configures the series legend. */
    fun legend(block: LegendOptions.() -> Unit) = legendOptions.apply(block)

    /** Configures click selection feedback and value formatting. */
    fun tooltip(block: TooltipOptions.() -> Unit) = tooltipOptions.apply(block)

    /** Configures polygon grid density, stroke, vertices and optional fills. */
    fun radar(block: RadarOptions.() -> Unit) {
        radarOptions.apply(block)
        require(radarOptions.gridCount in 2..10) { "radar.gridCount must be in 2..10" }
        require(radarOptions.lineWidth > 0f) { "radar.lineWidth must be > 0" }
    }

    /** Configures the chart-local entry reveal without animating its container. */
    fun entrance(block: EntranceOptions.() -> Unit) = EntranceOptions(entranceProgress).apply(block).also {
        entranceProgress = it.progress.coerceIn(0f, 1f)
    }
}

/** Event DSL emitted by [HeatmapChartView]. */
class HeatmapChartEvent : ComposeEvent() {
    internal var itemSelectedHandler: ((ChartSelection<HeatmapEntry>) -> Unit)? = null

    /** Registers the callback invoked after a visible heatmap cell is selected. */
    fun onItemSelected(handler: (ChartSelection<HeatmapEntry>) -> Unit) {
        itemSelectedHandler = handler
    }
}

/** Event DSL emitted by [RadarChartView]. */
class RadarChartEvent : ComposeEvent() {
    internal var itemSelectedHandler: ((ChartSelection<RadarEntry>) -> Unit)? = null
    internal var trackerChangedHandler: ((RadarTracker?) -> Unit)? = null

    /** Registers the callback invoked after a visible radar vertex is selected. */
    fun onItemSelected(handler: (ChartSelection<RadarEntry>) -> Unit) {
        itemSelectedHandler = handler
    }

    /** Registers temporary previews from the opt-in long-press tracker. */
    fun onTrackerChanged(handler: (RadarTracker?) -> Unit) {
        trackerChangedHandler = handler
    }
}

/** Kuikly ComposeView that renders a two-dimensional category heatmap on [Canvas]. */
class HeatmapChartView : ComposeView<HeatmapChartAttr, HeatmapChartEvent>() {
    private var selectionState: ChartSelectionState<ChartSelection<HeatmapEntry>> by observable(ChartSelectionState())
    private var renderedCells: List<RenderedHeatmapCell> = emptyList()

    override fun createAttr() = HeatmapChartAttr()
    override fun createEvent() = HeatmapChartEvent()

    override fun body(): ViewBuilder {
        val chart = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event {
                    click { params ->
                        val selection = PolarChartHitTest.heatmap(chart.renderedCells, params.x, params.y)
                        chart.selectionState = chart.selectionState.select(selection)
                        if (selection != null) chart.event.itemSelectedHandler?.invoke(selection)
                    }
                }
            }) { context, width, height ->
                chart.renderedCells = P2ChartCanvasPainter.drawHeatmap(
                    context = context,
                    width = width,
                    height = height,
                    attr = chart.attr,
                    selected = chart.selectionState.selection,
                )
            }
        }
    }
}

/** Kuikly ComposeView that renders multi-series metrics in a shared polar grid. */
class RadarChartView : ComposeView<RadarChartAttr, RadarChartEvent>() {
    private var selectionState: ChartSelectionState<ChartSelection<RadarEntry>> by observable(ChartSelectionState())
    private var tracker: RadarTracker? by observable(null)
    private var renderedPoints: List<RenderedRadarPoint> = emptyList()
    private var layout: RadarLayout? = null
    private val trackerReturnAnimation = ChartFrameAnimation()

    override fun createAttr() = RadarChartAttr()
    override fun createEvent() = RadarChartEvent()

    override fun body(): ViewBuilder {
        val chart = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event {
                    click { params ->
                        val selection = PolarChartHitTest.radar(chart.renderedPoints, params.x, params.y)
                        chart.selectionState = chart.selectionState.select(selection)
                        if (selection != null) chart.event.itemSelectedHandler?.invoke(selection)
                    }
                    longPress { params ->
                        if (!chart.attr.radarOptions.trackerEnabled) return@longPress
                        when (params.state) {
                            "start" -> chart.beginTracker(params.x, params.y)
                            "move" -> chart.updateTracker(params.x, params.y)
                            "end" -> chart.releaseTracker()
                        }
                    }
                }
            }) { context, width, height ->
                val rendered = P2ChartCanvasPainter.drawRadar(
                    context = context,
                    width = width,
                    height = height,
                    attr = chart.attr,
                    selected = chart.selectionState.selection,
                    tracker = chart.tracker,
                )
                chart.renderedPoints = rendered.points
                chart.layout = rendered.layout
            }
        }
    }

    private fun beginTracker(x: Float, y: Float) {
        val selection = PolarChartHitTest.radar(renderedPoints, x, y) ?: return
        val previewValue = RadarTrackerProjection.previewValue(layout ?: return, selection, x, y) ?: return
        cancelTrackerReturn()
        updateTracker(RadarTracker(selection, previewValue))
    }

    private fun updateTracker(x: Float, y: Float) {
        val active = tracker ?: return
        val previewValue = RadarTrackerProjection.previewValue(layout ?: return, active.selection, x, y) ?: return
        updateTracker(active.copy(previewValue = previewValue))
    }

    private fun updateTracker(updated: RadarTracker) {
        if (tracker != updated) {
            tracker = updated
            event.trackerChangedHandler?.invoke(updated)
        }
    }

    private fun clearTracker() {
        if (tracker != null) {
            tracker = null
            event.trackerChangedHandler?.invoke(null)
        }
    }

    private fun releaseTracker() {
        val active = tracker ?: return
        val targetValue = active.selection.item.value
        if (abs(active.previewValue - targetValue) < 0.001f) {
            clearTracker()
            return
        }

        trackerReturnAnimation.start(frameCount = 10, onFrame = { progress ->
            val easedProgress = 1f - (1f - progress) * (1f - progress)
            updateTracker(active.copy(previewValue = active.previewValue + (targetValue - active.previewValue) * easedProgress))
        }, onFinished = ::clearTracker)
    }

    private fun cancelTrackerReturn() {
        trackerReturnAnimation.cancel()
    }

    override fun viewDestroyed() {
        cancelTrackerReturn()
        super.viewDestroyed()
    }
}

/** Adds a declaratively configured [HeatmapChartView] to this Kuikly container. */
fun ViewContainer<*, *>.HeatmapChart(init: HeatmapChartView.() -> Unit) {
    addChild(HeatmapChartView(), init)
}

/** Adds a declaratively configured [RadarChartView] to this Kuikly container. */
fun ViewContainer<*, *>.RadarChart(init: RadarChartView.() -> Unit) {
    addChild(RadarChartView(), init)
}

/**
 * Resolves the heatmap colors with an explicit scale taking precedence over the
 * GitHub-style green defaults.
 */
internal fun resolveHeatmapColorScale(
    colorScale: List<Color>,
    theme: ChartTheme,
): List<Color> = colorScale.ifEmpty { defaultHeatmapColorScale(theme) }

/**
 * Provides one green hue from low to high intensity. The canonical dark theme
 * uses a darker set so low values remain distinguishable on its dark canvas.
 */
internal fun defaultHeatmapColorScale(theme: ChartTheme): List<Color> =
    if (theme.backgroundColor.hexColor == DARK_CHART_BACKGROUND_HEX) {
        GITHUB_DARK_HEATMAP_COLOR_SCALE
    } else {
        GITHUB_LIGHT_HEATMAP_COLOR_SCALE
    }

private const val DARK_CHART_BACKGROUND_HEX = 0xFF111827L

private val GITHUB_LIGHT_HEATMAP_COLOR_SCALE = listOf(
    Color(0xFF9BE9A8),
    Color(0xFF40C463),
    Color(0xFF30A14E),
    Color(0xFF216E39),
)

private val GITHUB_DARK_HEATMAP_COLOR_SCALE = listOf(
    Color(0xFF0E4429),
    Color(0xFF006D32),
    Color(0xFF26A641),
    Color(0xFF39D353),
)

private data class RadarRenderResult(
    val points: List<RenderedRadarPoint>,
    val layout: RadarLayout?,
)

private object P2ChartCanvasPainter {
    private const val LABEL_FONT_SIZE = 11f
    private const val LEGEND_FONT_SIZE = 11f
    private const val EMPTY_FONT_SIZE = 13f

    fun drawHeatmap(
        context: CanvasContext,
        width: Float,
        height: Float,
        attr: HeatmapChartAttr,
        selected: ChartSelection<HeatmapEntry>?,
    ): List<RenderedHeatmapCell> {
        fillRect(context, ChartRect(0f, 0f, width, height), attr.theme.backgroundColor)
        val validEntries = attr.entries.filter { it.value.isFinite() }
        if (width <= 0f || height <= 0f || validEntries.isEmpty()) {
            drawEmpty(context, width, height, attr.theme)
            return emptyList()
        }

        context.font(LABEL_FONT_SIZE)
        val yLabelWidth = validEntries
            .map { it.yLabel }
            .distinct()
            .maxOfOrNull { context.measureText(it).width }
            ?: 0f
        val left = (yLabelWidth + 14f).coerceAtMost(width * 0.42f)
        val plot = ChartRect(
            left = left,
            top = 16f,
            right = (width - 14f).coerceAtLeast(left),
            bottom = (height - 34f).coerceAtLeast(16f),
        )
        val layout = HeatmapLayoutEngine.layout(
            plot = plot,
            entries = attr.entries,
            seriesName = attr.seriesName.ifBlank { "热度" },
            cellGap = attr.heatmapOptions.cellGap,
        )
        if (layout.cells.isEmpty()) {
            drawEmpty(context, width, height, attr.theme)
            return emptyList()
        }

        drawHeatmapLabels(context, layout, attr.theme)
        val colors = resolveHeatmapColorScale(attr.heatmapOptions.colorScale, attr.theme)
        layout.cells.forEachIndexed { index, cell ->
            val cellProgress = heatmapCellEntranceProgress(
                entranceProgress = attr.entranceProgress,
                cellIndex = index,
                cellCount = layout.cells.size,
            )
            val animatedBounds = scaleRectFromCenter(cell.bounds, cellProgress)
            val colorIndex = (cell.fraction * (colors.size - 1)).roundToInt().coerceIn(0, colors.lastIndex)
            val color = cell.selection.item.color ?: colors[colorIndex]
            fillRoundedRect(context, animatedBounds, 3f, color)
            if (
                attr.heatmapOptions.showValueLabels && cellProgress >= 0.9f &&
                cell.bounds.width >= 34f && cell.bounds.height >= 22f
            ) {
                context.font(10f)
                context.textAlign(TextAlign.CENTER)
                context.fillStyle(attr.theme.tooltipTextColor)
                val text = ellipsize(
                    context,
                    attr.tooltipOptions.format(cell.selection.item.value),
                    cell.bounds.width - 6f,
                )
                context.fillText(text, (cell.bounds.left + cell.bounds.right) / 2f, (cell.bounds.top + cell.bounds.bottom) / 2f + 3f)
            }
        }

        selected?.let { selection ->
            layout.cells.firstOrNull { it.selection == selection }?.let { cell ->
                strokeRect(context, cell.bounds, attr.theme.selectionColor, 2f)
                if (attr.tooltipOptions.enabled) {
                    drawTooltip(
                        context = context,
                        width = width,
                        height = height,
                        anchorX = (cell.bounds.left + cell.bounds.right) / 2f,
                        anchorY = (cell.bounds.top + cell.bounds.bottom) / 2f,
                        text = "${selection.seriesName} · ${selection.item.xLabel} / ${selection.item.yLabel}: " +
                            attr.tooltipOptions.format(selection.item.value),
                        theme = attr.theme,
                    )
                }
            }
        }
        return layout.cells
    }

    fun drawRadar(
        context: CanvasContext,
        width: Float,
        height: Float,
        attr: RadarChartAttr,
        selected: ChartSelection<RadarEntry>?,
        tracker: RadarTracker?,
    ): RadarRenderResult {
        fillRect(context, ChartRect(0f, 0f, width, height), attr.theme.backgroundColor)
        if (width <= 0f || height <= 0f || attr.series.isEmpty()) {
            drawEmpty(context, width, height, attr.theme)
            return RadarRenderResult(emptyList(), null)
        }

        context.font(LABEL_FONT_SIZE)
        val labels = attr.series.firstOrNull { it.items.isNotEmpty() }?.items?.map { it.label }.orEmpty()
        val maxLabelWidth = labels.maxOfOrNull { context.measureText(it).width } ?: 0f
        val layout = RadarLayoutEngine.layout(
            width = width,
            height = height,
            series = attr.series,
            gridCount = attr.radarOptions.gridCount,
            labelInset = (maxLabelWidth / 2f + 14f).coerceAtLeast(28f),
            legendVisible = attr.legendOptions.visible,
        )
        val renderedPoints = layout.series.flatMap { it.points }
        if (layout.axes.isEmpty() || renderedPoints.isEmpty()) {
            drawEmpty(context, width, height, attr.theme)
            return RadarRenderResult(emptyList(), layout)
        }

        val trackerPoint = tracker?.let { RadarTrackerProjection.point(layout, it) }
        val visualSeries = RadarTrackerProjection.replacePoint(layout.series, tracker, trackerPoint)
        val visualPoints = visualSeries.flatMap { it.points }
        val gridProgress = (attr.entranceProgress.coerceIn(0f, 1f) / 0.45f).coerceIn(0f, 1f)
        val shapeProgress = ((attr.entranceProgress.coerceIn(0f, 1f) - 0.2f) / 0.8f).coerceIn(0f, 1f)

        if (attr.legendOptions.visible) drawLegend(context, attr.series, attr.theme, width)
        drawRadarGrid(context, layout, attr.theme, gridProgress)
        drawRadarLabels(context, layout, attr.theme)

        visualSeries.forEachIndexed { seriesIndex, renderedSeries ->
            val animatedSeries = renderedSeries.copy(
                points = scaleRadarPoints(renderedSeries.points, layout, shapeProgress),
                segments = renderedSeries.segments.map { segment -> scaleRadarPoints(segment, layout, shapeProgress) },
            )
            val sourceSeries = attr.series[seriesIndex]
            val color = sourceSeries.color ?: attr.theme.palette[seriesIndex % attr.theme.palette.size]
            val fillColor = attr.radarOptions.fillColors.getOrNull(seriesIndex)
            if (fillColor != null && animatedSeries.closesPolygon) {
                drawRadarPath(
                    context = context,
                    points = animatedSeries.points,
                    color = color,
                    lineWidth = attr.radarOptions.lineWidth,
                    close = true,
                    fillColor = fillColor,
                )
            } else {
                animatedSeries.segments.forEach { segment ->
                    drawRadarPath(
                        context = context,
                        points = segment,
                        color = color,
                        lineWidth = attr.radarOptions.lineWidth,
                        close = animatedSeries.closesPolygon,
                        fillColor = null,
                    )
                }
            }
            if (attr.radarOptions.showPoints) {
                animatedSeries.points.forEach { point -> drawCircle(context, point.x, point.y, 3.5f, color) }
            }
            if (attr.radarOptions.showValueLabels && layout.radius >= 72f) {
                context.font(10f)
                context.textAlign(TextAlign.CENTER)
                context.fillStyle(attr.theme.labelColor)
                animatedSeries.points.forEach { point ->
                    val value = if (tracker?.selection == point.selection) tracker.previewValue else point.selection.item.value
                    context.fillText(
                        attr.tooltipOptions.format(value),
                        point.x,
                        point.y - 7f,
                    )
                }
            }
        }

        trackerPoint?.let { point ->
            drawCircle(context, point.x, point.y, 8f, attr.theme.backgroundColor)
            drawCircle(context, point.x, point.y, 5f, attr.theme.selectionColor)
            if (attr.tooltipOptions.enabled) {
                val activeTracker = tracker ?: return@let
                drawTooltip(
                    context = context,
                    width = width,
                    height = height,
                    anchorX = point.x,
                    anchorY = point.y,
                    text = "${activeTracker.selection.seriesName} 路 ${activeTracker.selection.item.label}: " +
                        attr.tooltipOptions.format(activeTracker.previewValue),
                    theme = attr.theme,
                )
            }
        }

        selected?.let { selection ->
            visualPoints.firstOrNull { it.selection == selection }?.let { point ->
                drawCircle(context, point.x, point.y, 7f, attr.theme.backgroundColor)
                drawCircle(context, point.x, point.y, 5f, attr.theme.selectionColor)
                if (attr.tooltipOptions.enabled) {
                    drawTooltip(
                        context = context,
                        width = width,
                        height = height,
                        anchorX = point.x,
                        anchorY = point.y,
                        text = "${selection.seriesName} · ${selection.item.label}: " +
                            attr.tooltipOptions.format(selection.item.value),
                        theme = attr.theme,
                    )
                }
            }
        }
        return RadarRenderResult(visualPoints, layout)
    }

    private fun drawHeatmapLabels(context: CanvasContext, layout: HeatmapLayout, theme: ChartTheme) {
        if (layout.xLabels.isEmpty() || layout.yLabels.isEmpty()) return
        val cellWidth = layout.plot.width / layout.xLabels.size
        val cellHeight = layout.plot.height / layout.yLabels.size
        context.font(LABEL_FONT_SIZE)
        context.fillStyle(theme.labelColor)
        context.textAlign(TextAlign.CENTER)
        layout.xLabels.forEachIndexed { index, label ->
            val maxWidth = (cellWidth - 4f).coerceAtLeast(10f)
            context.fillText(
                ellipsize(context, label, maxWidth),
                layout.plot.left + cellWidth * (index + 0.5f),
                layout.plot.bottom + 18f,
            )
        }
        context.textAlign(TextAlign.RIGHT)
        layout.yLabels.forEachIndexed { index, label ->
            context.fillText(
                ellipsize(context, label, (layout.plot.left - 8f).coerceAtLeast(10f)),
                layout.plot.left - 8f,
                layout.plot.top + cellHeight * (index + 0.5f) + 4f,
            )
        }
    }

    private fun drawRadarGrid(
        context: CanvasContext,
        layout: RadarLayout,
        theme: ChartTheme,
        progress: Float,
    ) {
        for (ring in 1..layout.gridCount) {
            val fraction = ring.toFloat() / layout.gridCount * progress
            context.beginPath()
            layout.axes.forEachIndexed { index, axis ->
                val x = layout.centerX + (axis.x - layout.centerX) * fraction
                val y = layout.centerY + (axis.y - layout.centerY) * fraction
                if (index == 0) context.moveTo(x, y) else context.lineTo(x, y)
            }
            context.closePath()
            context.strokeStyle(theme.gridColor)
            context.lineWidth(1f)
            context.stroke()
        }
        layout.axes.forEach { axis ->
            context.beginPath()
            context.strokeStyle(theme.axisColor)
            context.lineWidth(1f)
            context.moveTo(layout.centerX, layout.centerY)
            context.lineTo(
                layout.centerX + (axis.x - layout.centerX) * progress,
                layout.centerY + (axis.y - layout.centerY) * progress,
            )
            context.stroke()
        }
    }

    private fun drawRadarLabels(context: CanvasContext, layout: RadarLayout, theme: ChartTheme) {
        context.font(LABEL_FONT_SIZE)
        context.textAlign(TextAlign.CENTER)
        context.fillStyle(theme.labelColor)
        val maxWidth = (layout.radius * 0.8f).coerceAtLeast(36f)
        layout.axes.forEach { axis ->
            val x = axis.x + cos(axis.angle.toDouble()).toFloat() * 10f
            val y = axis.y + sin(axis.angle.toDouble()).toFloat() * 10f
            context.fillText(ellipsize(context, axis.label, maxWidth), x, y + 4f)
        }
    }

    /** Grows a radar polygon from its polar origin while retaining final hit targets. */
    private fun scaleRadarPoints(
        points: List<RenderedRadarPoint>,
        layout: RadarLayout,
        progress: Float,
    ): List<RenderedRadarPoint> = points.map { point ->
        point.copy(
            x = layout.centerX + (point.x - layout.centerX) * progress,
            y = layout.centerY + (point.y - layout.centerY) * progress,
        )
    }

    /** Gives each heatmap cell a compact, staggered grid-reveal origin. */
    private fun scaleRectFromCenter(bounds: ChartRect, progress: Float): ChartRect {
        val centerX = (bounds.left + bounds.right) / 2f
        val centerY = (bounds.top + bounds.bottom) / 2f
        val halfWidth = bounds.width * progress / 2f
        val halfHeight = bounds.height * progress / 2f
        return ChartRect(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
    }

    private fun drawRadarPath(
        context: CanvasContext,
        points: List<RenderedRadarPoint>,
        color: Color,
        lineWidth: Float,
        close: Boolean,
        fillColor: Color?,
    ) {
        if (points.isEmpty()) return
        context.beginPath()
        context.moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { point -> context.lineTo(point.x, point.y) }
        if (close) context.closePath()
        if (fillColor != null) {
            context.fillStyle(fillColor)
            context.fill()
        }
        context.strokeStyle(color)
        context.lineWidth(lineWidth)
        context.lineCapRound()
        context.stroke()
    }

    private fun <T> drawLegend(
        context: CanvasContext,
        series: List<ChartSeries<T>>,
        theme: ChartTheme,
        width: Float,
    ) {
        context.font(LEGEND_FONT_SIZE)
        context.textAlign(TextAlign.LEFT)
        var x = 12f
        series.forEachIndexed { index, item ->
            val labelWidth = context.measureText(item.name).width
            if (x + labelWidth + 28f > width) return
            val color = item.color ?: theme.palette[index % theme.palette.size]
            drawCircle(context, x + 4f, 15f, 4f, color)
            context.fillStyle(theme.labelColor)
            context.fillText(item.name, x + 12f, 19f)
            x += labelWidth + 30f
        }
    }

    private fun drawTooltip(
        context: CanvasContext,
        width: Float,
        height: Float,
        anchorX: Float,
        anchorY: Float,
        text: String,
        theme: ChartTheme,
    ) {
        context.font(11f)
        val maxTextWidth = (width - 32f).coerceAtLeast(20f)
        val displayText = ellipsize(context, text, maxTextWidth)
        val boxWidth = (context.measureText(displayText).width + 20f).coerceAtMost((width - 12f).coerceAtLeast(40f))
        val boxHeight = 30f
        val left = (anchorX + 10f).let { preferred ->
            if (preferred + boxWidth <= width - 6f) preferred else anchorX - boxWidth - 10f
        }.coerceIn(6f, (width - boxWidth - 6f).coerceAtLeast(6f))
        val top = (anchorY - boxHeight - 10f).coerceIn(6f, (height - boxHeight - 6f).coerceAtLeast(6f))
        fillRoundedRect(context, ChartRect(left, top, left + boxWidth, top + boxHeight), 6f, theme.tooltipBackgroundColor)
        context.textAlign(TextAlign.LEFT)
        context.fillStyle(theme.tooltipTextColor)
        context.fillText(displayText, left + 10f, top + 19f)
    }

    private fun ellipsize(context: CanvasContext, text: String, maxWidth: Float): String {
        if (context.measureText(text).width <= maxWidth) return text
        val suffix = "…"
        var low = 0
        var high = text.length
        while (low < high) {
            val middle = (low + high + 1) / 2
            if (context.measureText(text.take(middle) + suffix).width <= maxWidth) low = middle else high = middle - 1
        }
        return text.take(low) + suffix
    }

    private fun drawEmpty(context: CanvasContext, width: Float, height: Float, theme: ChartTheme) {
        context.font(EMPTY_FONT_SIZE)
        context.textAlign(TextAlign.CENTER)
        context.fillStyle(theme.labelColor)
        context.fillText("暂无数据", width / 2f, height / 2f)
    }

    private fun drawCircle(context: CanvasContext, x: Float, y: Float, radius: Float, color: Color) {
        if (radius <= 0f) return
        context.beginPath()
        context.arc(x, y, radius, 0f, (PI * 2).toFloat(), false)
        context.closePath()
        context.fillStyle(color)
        context.fill()
    }

    private fun fillRect(context: CanvasContext, rect: ChartRect, color: Color) {
        if (rect.width <= 0f || rect.height <= 0f) return
        context.beginPath()
        context.moveTo(rect.left, rect.top)
        context.lineTo(rect.right, rect.top)
        context.lineTo(rect.right, rect.bottom)
        context.lineTo(rect.left, rect.bottom)
        context.closePath()
        context.fillStyle(color)
        context.fill()
    }

    private fun fillRoundedRect(context: CanvasContext, rect: ChartRect, radius: Float, color: Color) {
        if (rect.width <= 0f || rect.height <= 0f) return
        val r = min(radius, min(rect.width, rect.height) / 2f).coerceAtLeast(0f)
        context.beginPath()
        context.moveTo(rect.left + r, rect.top)
        context.lineTo(rect.right - r, rect.top)
        context.quadraticCurveTo(rect.right, rect.top, rect.right, rect.top + r)
        context.lineTo(rect.right, rect.bottom - r)
        context.quadraticCurveTo(rect.right, rect.bottom, rect.right - r, rect.bottom)
        context.lineTo(rect.left + r, rect.bottom)
        context.quadraticCurveTo(rect.left, rect.bottom, rect.left, rect.bottom - r)
        context.lineTo(rect.left, rect.top + r)
        context.quadraticCurveTo(rect.left, rect.top, rect.left + r, rect.top)
        context.closePath()
        context.fillStyle(color)
        context.fill()
    }

    private fun strokeRect(context: CanvasContext, rect: ChartRect, color: Color, width: Float) {
        context.beginPath()
        context.moveTo(rect.left, rect.top)
        context.lineTo(rect.right, rect.top)
        context.lineTo(rect.right, rect.bottom)
        context.lineTo(rect.left, rect.bottom)
        context.closePath()
        context.strokeStyle(color)
        context.lineWidth(width)
        context.stroke()
    }
}
