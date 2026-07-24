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
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Attribute DSL for [LineChartView]. */
class LineChartAttr : ComposeAttr() {
    internal var series: List<ChartSeries<ChartPoint>> by observable(emptyList())

    /** Active visual theme. Reassigning it triggers a redraw. */
    var theme: ChartTheme by observable(ChartTheme.light())

    /** Mutable options configured by [xAxis]. */
    val xAxisOptions = AxisOptions()

    /** Mutable options configured by [yAxis]. */
    val yAxisOptions = AxisOptions()

    /** Mutable options configured by [grid]. */
    val gridOptions = GridOptions()

    /** Mutable options configured by [legend]. */
    val legendOptions = LegendOptions()

    /** Mutable options configured by [tooltip]. */
    val tooltipOptions = TooltipOptions()

    /** Mutable options configured by [line]. */
    val lineOptions = LineOptions()

    /** Replaces the immutable series snapshot rendered by this chart. */
    fun data(vararg value: ChartSeries<ChartPoint>) {
        value.forEach { require(it.name.isNotBlank()) { "LineChart series name must not be blank" } }
        series = value.toList()
    }

    /** Configures the horizontal axis. */
    fun xAxis(block: AxisOptions.() -> Unit) = xAxisOptions.applyValidated(block)

    /** Configures the vertical numeric axis. */
    fun yAxis(block: AxisOptions.() -> Unit) = yAxisOptions.applyValidated(block)

    /** Configures horizontal grid lines. */
    fun grid(block: GridOptions.() -> Unit) {
        gridOptions.apply(block)
        require(gridOptions.lineWidth >= 0f) { "grid.lineWidth must be >= 0" }
    }

    /** Configures the series legend. */
    fun legend(block: LegendOptions.() -> Unit) = legendOptions.apply(block)

    /** Configures click selection feedback and value formatting. */
    fun tooltip(block: TooltipOptions.() -> Unit) = tooltipOptions.apply(block)

    /** Configures line interpolation, stroke and markers. */
    fun line(block: LineOptions.() -> Unit) {
        lineOptions.apply(block)
        require(lineOptions.lineWidth > 0f) { "line.lineWidth must be > 0" }
        require(lineOptions.pointRadius >= 0f) { "line.pointRadius must be >= 0" }
    }
}

/** Attribute DSL for [BarChartView]. */
class BarChartAttr : ComposeAttr() {
    internal var series: List<ChartSeries<BarEntry>> by observable(emptyList())

    /** Active visual theme. Reassigning it triggers a redraw. */
    var theme: ChartTheme by observable(ChartTheme.light())

    /** Mutable options configured by [xAxis]. */
    val xAxisOptions = AxisOptions()

    /** Mutable options configured by [yAxis]. Zero is included by default. */
    val yAxisOptions = AxisOptions().apply { includeZero = true }

    /** Mutable options configured by [grid]. */
    val gridOptions = GridOptions()

    /** Mutable options configured by [legend]. */
    val legendOptions = LegendOptions()

    /** Mutable options configured by [tooltip]. */
    val tooltipOptions = TooltipOptions()

    /** Mutable options configured by [bars]. */
    val barOptions = BarOptions()

    /** Replaces the immutable series snapshot rendered by this chart. */
    fun data(vararg value: ChartSeries<BarEntry>) {
        value.forEach { require(it.name.isNotBlank()) { "BarChart series name must not be blank" } }
        require(barOptions.mode != BarMode.SINGLE || value.size <= 1) {
            "BarMode.SINGLE accepts at most one series"
        }
        series = value.toList()
    }

    /** Configures the category axis. */
    fun xAxis(block: AxisOptions.() -> Unit) = xAxisOptions.applyValidated(block)

    /** Configures the vertical numeric axis. */
    fun yAxis(block: AxisOptions.() -> Unit) = yAxisOptions.applyValidated(block)

    /** Configures horizontal grid lines. */
    fun grid(block: GridOptions.() -> Unit) {
        gridOptions.apply(block)
        require(gridOptions.lineWidth >= 0f) { "grid.lineWidth must be >= 0" }
    }

    /** Configures the series legend. */
    fun legend(block: LegendOptions.() -> Unit) = legendOptions.apply(block)

    /** Configures click selection feedback and value formatting. */
    fun tooltip(block: TooltipOptions.() -> Unit) = tooltipOptions.apply(block)

    /** Configures grouping, width, labels and corner radius. */
    fun bars(block: BarOptions.() -> Unit) {
        barOptions.apply(block)
        require(barOptions.barWidthRatio > 0f && barOptions.barWidthRatio <= 1f) {
            "bars.barWidthRatio must be in (0, 1]"
        }
        require(barOptions.cornerRadius >= 0f) { "bars.cornerRadius must be >= 0" }
        require(barOptions.mode != BarMode.SINGLE || series.size <= 1) {
            "BarMode.SINGLE accepts at most one series"
        }
    }
}

private fun AxisOptions.applyValidated(block: AxisOptions.() -> Unit): AxisOptions {
    apply(block)
    require(tickCount in 2..10) { "axis.tickCount must be in 2..10" }
    return this
}

/** Event DSL emitted by [LineChartView]. */
class LineChartEvent : ComposeEvent() {
    internal var itemSelectedHandler: ((ChartSelection<ChartPoint>) -> Unit)? = null

    /** Registers the callback invoked after a line point is hit by a click. */
    fun onItemSelected(handler: (ChartSelection<ChartPoint>) -> Unit) {
        itemSelectedHandler = handler
    }
}

/** Event DSL emitted by [BarChartView]. */
class BarChartEvent : ComposeEvent() {
    internal var itemSelectedHandler: ((ChartSelection<BarEntry>) -> Unit)? = null

    /** Registers the callback invoked after a bar is hit by a click. */
    fun onItemSelected(handler: (ChartSelection<BarEntry>) -> Unit) {
        itemSelectedHandler = handler
    }
}

/**
 * Kuikly ComposeView that renders a cross-platform line chart on [Canvas].
 *
 * Use the [LineChart] container extension instead of instantiating this class
 * directly in application pages.
 */
class LineChartView : ComposeView<LineChartAttr, LineChartEvent>() {
    private var selected: ChartSelection<ChartPoint>? by observable(null)
    private var renderedPoints: List<RenderedLinePoint> = emptyList()

    override fun createAttr() = LineChartAttr()
    override fun createEvent() = LineChartEvent()

    override fun body(): ViewBuilder {
        val chart = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event {
                    click { params ->
                        val selection = ChartHitTest.line(chart.renderedPoints, params.x, params.y)
                        chart.selected = selection
                        if (selection != null) chart.event.itemSelectedHandler?.invoke(selection)
                    }
                }
            }) { context, width, height ->
                chart.renderedPoints = ChartCanvasPainter.drawLine(
                    context = context,
                    width = width,
                    height = height,
                    attr = chart.attr,
                    selected = chart.selected,
                )
            }
        }
    }
}

/**
 * Kuikly ComposeView that renders a cross-platform bar chart on [Canvas].
 *
 * Use the [BarChart] container extension instead of instantiating this class
 * directly in application pages.
 */
class BarChartView : ComposeView<BarChartAttr, BarChartEvent>() {
    private var selected: ChartSelection<BarEntry>? by observable(null)
    private var renderedBars: List<RenderedBar> = emptyList()

    override fun createAttr() = BarChartAttr()
    override fun createEvent() = BarChartEvent()

    override fun body(): ViewBuilder {
        val chart = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event {
                    click { params ->
                        val selection = ChartHitTest.bar(chart.renderedBars, params.x, params.y)
                        chart.selected = selection
                        if (selection != null) chart.event.itemSelectedHandler?.invoke(selection)
                    }
                }
            }) { context, width, height ->
                chart.renderedBars = ChartCanvasPainter.drawBar(
                    context = context,
                    width = width,
                    height = height,
                    attr = chart.attr,
                    selected = chart.selected,
                )
            }
        }
    }
}

/** Adds a declaratively configured [LineChartView] to this Kuikly container. */
fun ViewContainer<*, *>.LineChart(init: LineChartView.() -> Unit) {
    addChild(LineChartView(), init)
}

/** Adds a declaratively configured [BarChartView] to this Kuikly container. */
fun ViewContainer<*, *>.BarChart(init: BarChartView.() -> Unit) {
    addChild(BarChartView(), init)
}

private object ChartCanvasPainter {
    private const val LABEL_FONT_SIZE = 11f
    private const val LEGEND_FONT_SIZE = 11f
    private const val EMPTY_FONT_SIZE = 13f

    fun drawLine(
        context: CanvasContext,
        width: Float,
        height: Float,
        attr: LineChartAttr,
        selected: ChartSelection<ChartPoint>?,
    ): List<RenderedLinePoint> {
        fillRect(context, ChartRect(0f, 0f, width, height), attr.theme.backgroundColor)
        val sourceSeries = attr.series
        if (width <= 0f || height <= 0f || sourceSeries.none { series -> series.items.any { it.x.isFinite() && it.y.isFinite() } }) {
            drawEmpty(context, width, height, attr.theme)
            return emptyList()
        }

        context.font(LABEL_FONT_SIZE)
        val preliminary = ChartLayoutEngine.line(
            width, height, sourceSeries, ChartMargins(), attr.yAxisOptions.tickCount, attr.yAxisOptions.includeZero,
        )
        val yLabelWidth = preliminary.yScale.ticks.maxOfOrNull {
            context.measureText(attr.yAxisOptions.format(it)).width
        } ?: 32f
        val layout = ChartLayoutEngine.line(
            width = width,
            height = height,
            series = sourceSeries,
            margins = ChartMargins(
                left = if (attr.yAxisOptions.visible) yLabelWidth + 14f else 14f,
                top = if (attr.legendOptions.visible) 32f else 16f,
                right = 16f,
                bottom = if (attr.xAxisOptions.visible) 36f else 14f,
            ),
            tickCount = attr.yAxisOptions.tickCount,
            includeZero = attr.yAxisOptions.includeZero,
        )

        drawCartesianFrame(context, layout.plot, layout.yScale, attr.yAxisOptions, attr.gridOptions, attr.theme)
        if (attr.xAxisOptions.visible) {
            val labelSource = sourceSeries.firstOrNull { series -> series.items.any { it.x.isFinite() && it.y.isFinite() } }
            if (labelSource != null) drawLineXLabels(context, layout, labelSource.items, attr.xAxisOptions, attr.theme)
        }
        if (attr.legendOptions.visible) drawLegend(context, sourceSeries, attr.theme, width)

        val rendered = mutableListOf<RenderedLinePoint>()
        sourceSeries.forEachIndexed { seriesIndex, series ->
            val color = series.color ?: attr.theme.palette[seriesIndex % attr.theme.palette.size]
            val segments = mutableListOf<MutableList<RenderedLinePoint>>()
            var currentSegment = mutableListOf<RenderedLinePoint>()
            series.items.forEachIndexed { itemIndex, point ->
                if (!point.x.isFinite() || !point.y.isFinite()) {
                    if (currentSegment.isNotEmpty()) segments += currentSegment
                    currentSegment = mutableListOf()
                } else {
                    currentSegment += RenderedLinePoint(
                        x = layout.xFor(point.x),
                        y = layout.yFor(point.y),
                        selection = ChartSelection(seriesIndex, itemIndex, series.name, point),
                    )
                }
            }
            if (currentSegment.isNotEmpty()) segments += currentSegment
            segments.forEach { drawLinePath(context, it, color, attr.lineOptions) }
            val points = segments.flatten()
            if (attr.lineOptions.showPoints) {
                points.forEach { drawCircle(context, it.x, it.y, attr.lineOptions.pointRadius, color) }
            }
            rendered += points
        }

        selected?.let { selection ->
            rendered.firstOrNull { it.selection == selection }?.let { point ->
                drawSelectionLine(context, layout.plot, point.x, attr.theme.selectionColor)
                drawCircle(context, point.x, point.y, attr.lineOptions.pointRadius + 3f, attr.theme.backgroundColor)
                drawCircle(context, point.x, point.y, attr.lineOptions.pointRadius + 1f, attr.theme.selectionColor)
                if (attr.tooltipOptions.enabled) {
                    val label = selection.item.label.ifBlank { attr.xAxisOptions.format(selection.item.x) }
                    drawTooltip(
                        context, width, height, point.x, point.y,
                        "${selection.seriesName} · $label: ${attr.tooltipOptions.format(selection.item.y)}",
                        attr.theme,
                    )
                }
            }
        }
        return rendered
    }

    fun drawBar(
        context: CanvasContext,
        width: Float,
        height: Float,
        attr: BarChartAttr,
        selected: ChartSelection<BarEntry>?,
    ): List<RenderedBar> {
        fillRect(context, ChartRect(0f, 0f, width, height), attr.theme.backgroundColor)
        val sourceSeries = attr.series
        if (width <= 0f || height <= 0f || sourceSeries.none { series -> series.items.any { it.value.isFinite() } }) {
            drawEmpty(context, width, height, attr.theme)
            return emptyList()
        }

        context.font(LABEL_FONT_SIZE)
        val preliminary = ChartLayoutEngine.bar(
            width, height, sourceSeries, ChartMargins(), attr.yAxisOptions.tickCount, includeZero = true,
        )
        val yLabelWidth = preliminary.yScale.ticks.maxOfOrNull {
            context.measureText(attr.yAxisOptions.format(it)).width
        } ?: 32f
        val layout = ChartLayoutEngine.bar(
            width = width,
            height = height,
            series = sourceSeries,
            margins = ChartMargins(
                left = if (attr.yAxisOptions.visible) yLabelWidth + 14f else 14f,
                top = if (attr.legendOptions.visible) 32f else 16f,
                right = 16f,
                bottom = if (attr.xAxisOptions.visible) 36f else 14f,
            ),
            tickCount = attr.yAxisOptions.tickCount,
            includeZero = true,
        )
        drawCartesianFrame(context, layout.plot, layout.yScale, attr.yAxisOptions, attr.gridOptions, attr.theme)
        if (attr.xAxisOptions.visible) {
            val labelSource = sourceSeries.firstOrNull { series -> series.items.any { it.value.isFinite() } }
            if (labelSource != null) drawBarXLabels(context, layout, labelSource.items, attr.theme)
        }
        if (attr.legendOptions.visible) drawLegend(context, sourceSeries, attr.theme, width)

        val rendered = mutableListOf<RenderedBar>()
        val drawnSeries = sourceSeries
        val groupWidth = layout.slotWidth * attr.barOptions.barWidthRatio
        val barWidth = (groupWidth / drawnSeries.size.coerceAtLeast(1)).coerceAtLeast(1f)
        val zeroY = layout.yFor(0f).coerceIn(layout.plot.top, layout.plot.bottom)
        drawnSeries.forEachIndexed { seriesIndex, series ->
            val color = series.color ?: attr.theme.palette[seriesIndex % attr.theme.palette.size]
            series.items.forEachIndexed { itemIndex, entry ->
                if (!entry.value.isFinite() || itemIndex >= layout.categoryCount) return@forEachIndexed
                val groupLeft = layout.categoryCenter(itemIndex) - groupWidth / 2f
                val valueY = layout.yFor(entry.value).coerceIn(layout.plot.top, layout.plot.bottom)
                val left = groupLeft + seriesIndex * barWidth + 1f
                val right = groupLeft + (seriesIndex + 1) * barWidth - 1f
                val top = min(zeroY, valueY)
                val bottom = max(zeroY, valueY).coerceAtLeast(top + 1f)
                val bounds = ChartRect(left, top, right, bottom)
                fillRoundedRect(context, bounds, attr.barOptions.cornerRadius, color)
                val selection = ChartSelection(seriesIndex, itemIndex, series.name, entry)
                rendered += RenderedBar(bounds, selection)
                if (attr.barOptions.showValueLabels && barWidth >= 18f) {
                    context.font(10f)
                    context.textAlign(TextAlign.CENTER)
                    context.fillStyle(attr.theme.labelColor)
                    val labelY = if (entry.value >= 0f) (top - 5f).coerceAtLeast(layout.plot.top + 10f) else (bottom + 12f).coerceAtMost(layout.plot.bottom)
                    context.fillText(attr.tooltipOptions.format(entry.value), (left + right) / 2f, labelY)
                }
            }
        }

        selected?.let { selection ->
            rendered.firstOrNull { it.selection == selection }?.let { bar ->
                strokeRect(context, bar.bounds, attr.theme.selectionColor, 2f)
                if (attr.tooltipOptions.enabled) {
                    drawTooltip(
                        context, width, height, (bar.bounds.left + bar.bounds.right) / 2f, bar.bounds.top,
                        "${selection.seriesName} · ${selection.item.label}: ${attr.tooltipOptions.format(selection.item.value)}",
                        attr.theme,
                    )
                }
            }
        }
        return rendered
    }

    private fun drawCartesianFrame(
        context: CanvasContext,
        plot: ChartRect,
        scale: AxisScale,
        axis: AxisOptions,
        grid: GridOptions,
        theme: ChartTheme,
    ) {
        context.font(LABEL_FONT_SIZE)
        scale.ticks.forEach { tick ->
            val y = plot.bottom - ((tick - scale.min) / scale.range) * plot.height
            if (grid.visible) {
                context.beginPath()
                context.setLineDash(listOf(4f, 4f))
                context.strokeStyle(theme.gridColor)
                context.lineWidth(grid.lineWidth)
                context.moveTo(plot.left, y)
                context.lineTo(plot.right, y)
                context.stroke()
                context.setLineDash(emptyList())
            }
            if (axis.visible) {
                context.textAlign(TextAlign.RIGHT)
                context.fillStyle(theme.labelColor)
                context.fillText(axis.format(tick), plot.left - 8f, y + 4f)
            }
        }
        if (axis.visible) {
            context.beginPath()
            context.strokeStyle(theme.axisColor)
            context.lineWidth(1f)
            context.moveTo(plot.left, plot.top)
            context.lineTo(plot.left, plot.bottom)
            context.lineTo(plot.right, plot.bottom)
            context.stroke()
        }
    }

    private fun drawLineXLabels(
        context: CanvasContext,
        layout: LineChartLayout,
        source: List<ChartPoint>,
        axis: AxisOptions,
        theme: ChartTheme,
    ) {
        val points = source.filter { it.x.isFinite() && it.y.isFinite() }
        if (points.isEmpty()) return
        context.font(LABEL_FONT_SIZE)
        val maxWidth = points.maxOfOrNull {
            context.measureText(it.label.ifBlank { axis.format(it.x) }).width
        } ?: 1f
        val spacing = if (points.size <= 1) layout.plot.width else layout.plot.width / (points.size - 1)
        val stride = ceil((maxWidth + 10f) / spacing.coerceAtLeast(1f)).toInt().coerceAtLeast(1)
        context.fillStyle(theme.labelColor)
        context.textAlign(TextAlign.CENTER)
        points.forEachIndexed { index, point ->
            if (index % stride == 0 || index == points.lastIndex) {
                context.fillText(point.label.ifBlank { axis.format(point.x) }, layout.xFor(point.x), layout.plot.bottom + 18f)
            }
        }
    }

    private fun drawBarXLabels(
        context: CanvasContext,
        layout: BarChartLayout,
        source: List<BarEntry>,
        theme: ChartTheme,
    ) {
        if (source.isEmpty()) return
        context.font(LABEL_FONT_SIZE)
        val maxWidth = source.maxOfOrNull { context.measureText(it.label).width } ?: 1f
        val stride = ceil((maxWidth + 10f) / layout.slotWidth.coerceAtLeast(1f)).toInt().coerceAtLeast(1)
        context.fillStyle(theme.labelColor)
        context.textAlign(TextAlign.CENTER)
        source.forEachIndexed { index, entry ->
            if (index % stride == 0 || index == source.lastIndex) {
                context.fillText(entry.label, layout.categoryCenter(index), layout.plot.bottom + 18f)
            }
        }
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

    private fun drawLinePath(
        context: CanvasContext,
        points: List<RenderedLinePoint>,
        color: Color,
        options: LineOptions,
    ) {
        if (points.isEmpty()) return
        context.beginPath()
        context.strokeStyle(color)
        context.lineWidth(options.lineWidth)
        context.lineCapRound()
        context.moveTo(points.first().x, points.first().y)
        points.zipWithNext().forEach { (previous, current) ->
            if (options.smooth) {
                val controlX = (previous.x + current.x) / 2f
                context.bezierCurveTo(controlX, previous.y, controlX, current.y, current.x, current.y)
            } else {
                context.lineTo(current.x, current.y)
            }
        }
        context.stroke()
    }

    private fun drawSelectionLine(context: CanvasContext, plot: ChartRect, x: Float, color: Color) {
        context.beginPath()
        context.setLineDash(listOf(4f, 4f))
        context.strokeStyle(color)
        context.lineWidth(1f)
        context.moveTo(x, plot.top)
        context.lineTo(x, plot.bottom)
        context.stroke()
        context.setLineDash(emptyList())
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
