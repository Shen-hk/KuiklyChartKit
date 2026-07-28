package com.kuikly.kuiklychartkit.chart

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.Timer
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.TextAlign
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Attribute DSL for [LineChartView]. */
open class LineChartAttr : ComposeAttr() {
    internal var series: List<ChartSeries<ChartPoint>> by observable(emptyList())

    /** Entry reveal progress supplied by the host, from 0 (hidden) to 1 (complete). */
    var entranceProgress: Float by observable(1f)

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

    /** Mutable options configured by [interaction]. */
    val interactionOptions = InteractionOptions()

    /** Replaces the immutable series snapshot rendered by this chart. */
    open fun data(vararg value: ChartSeries<ChartPoint>) {
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

    /** Configures opt-in dense-data interaction. */
    fun interaction(block: InteractionOptions.() -> Unit) {
        interactionOptions.apply(block)
        require(interactionOptions.visibleItemCount >= 0) { "interaction.visibleItemCount must be >= 0" }
        require(interactionOptions.visibleItemCount == 0 || interactionOptions.visibleItemCount >= 2) {
            "interaction.visibleItemCount must be 0 or >= 2"
        }
        require(interactionOptions.maxRenderPointCount == 0 || interactionOptions.maxRenderPointCount >= 4) {
            "interaction.maxRenderPointCount must be 0 or >= 4"
        }
    }

    /** Configures the chart-local entry reveal without animating its container. */
    fun entrance(block: EntranceOptions.() -> Unit) = EntranceOptions(entranceProgress).apply(block).also {
        entranceProgress = it.progress.coerceIn(0f, 1f)
    }
}

/** Attribute DSL for [AreaChartView], sharing the line-chart coordinate and interaction model. */
class AreaChartAttr : LineChartAttr() {
    /** Mutable options configured by [area]. */
    val areaOptions = AreaOptions()

    init {
        yAxisOptions.includeZero = true
    }

    /** Configures the colors used below each area boundary. */
    fun area(block: AreaOptions.() -> Unit) {
        areaOptions.apply(block)
        require(areaOptions.fillColors.isNotEmpty()) { "area.fillColors must not be empty" }
    }
}

/** Compact single-series defaults for [SparklineChartView]. */
class SparklineChartAttr : LineChartAttr() {
    /** Whether click selection and [LineChartEvent.onItemSelected] are enabled. */
    var selectable: Boolean by observable(false)

    init {
        xAxisOptions.visible = false
        yAxisOptions.visible = false
        yAxisOptions.includeZero = false
        gridOptions.visible = false
        legendOptions.visible = false
        tooltipOptions.enabled = false
        lineOptions.smooth = true
        lineOptions.showPoints = false
    }

    /** Replaces the single immutable series rendered by this sparkline. */
    override fun data(vararg value: ChartSeries<ChartPoint>) {
        require(value.size <= 1) { "SparklineChart accepts at most one series" }
        super.data(*value)
    }
}

/** Attribute DSL for [BarChartView]. */
class BarChartAttr : ComposeAttr() {
    internal var series: List<ChartSeries<BarEntry>> by observable(emptyList())

    /** Entry reveal progress supplied by the host, from 0 (hidden) to 1 (complete). */
    var entranceProgress: Float by observable(1f)

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

    /** Configures the chart-local entry reveal without animating its container. */
    fun entrance(block: EntranceOptions.() -> Unit) = EntranceOptions(entranceProgress).apply(block).also {
        entranceProgress = it.progress.coerceIn(0f, 1f)
    }
}

/** Attribute DSL for [PieChartView]. */
class PieChartAttr : ComposeAttr() {
    internal var entries: List<PieEntry> by observable(emptyList())

    /** Entry reveal progress supplied by the host, from 0 (hidden) to 1 (complete). */
    var entranceProgress: Float by observable(1f)

    /** Stable series name returned in selection callbacks. */
    var seriesName: String by observable("数据占比")

    /** Active visual theme. Reassigning it triggers a redraw. */
    var theme: ChartTheme by observable(ChartTheme.light())

    /** Mutable options configured by [legend]. */
    val legendOptions = LegendOptions()

    /** Mutable options configured by [tooltip]. */
    val tooltipOptions = TooltipOptions()

    /** Mutable options configured by [pie]. */
    val pieOptions = PieOptions()

    /** Replaces the immutable slice snapshot rendered by this chart. */
    fun data(vararg value: PieEntry) {
        value.forEach { require(it.label.isNotBlank()) { "PieChart entry label must not be blank" } }
        entries = value.toList()
    }

    /** Configures the slice legend. */
    fun legend(block: LegendOptions.() -> Unit) = legendOptions.apply(block)

    /** Configures click selection feedback and value formatting. */
    fun tooltip(block: TooltipOptions.() -> Unit) = tooltipOptions.apply(block)

    /** Configures pie/donut geometry and percentage labels. */
    fun pie(block: PieOptions.() -> Unit) {
        pieOptions.apply(block)
        require(pieOptions.innerRadiusRatio in 0f..0.85f) {
            "pie.innerRadiusRatio must be in [0, 0.85]"
        }
        require(pieOptions.startAngleDegrees.isFinite()) { "pie.startAngleDegrees must be finite" }
        require(pieOptions.gapAngleDegrees in 0f..10f) {
            "pie.gapAngleDegrees must be in [0, 10]"
        }
    }

    /** Configures the chart-local entry reveal without animating its container. */
    fun entrance(block: EntranceOptions.() -> Unit) = EntranceOptions(entranceProgress).apply(block).also {
        entranceProgress = it.progress.coerceIn(0f, 1f)
    }
}

/** Attribute DSL for [MixedChartView]. */
class MixedChartAttr : ComposeAttr() {
    internal var barSeries: List<ChartSeries<BarEntry>> by observable(emptyList())
    internal var lineSeries: List<ChartSeries<BarEntry>> by observable(emptyList())

    /** Entry reveal progress supplied by the host, from 0 (hidden) to 1 (complete). */
    var entranceProgress: Float by observable(1f)

    /** Active visual theme. Reassigning it triggers a redraw. */
    var theme: ChartTheme by observable(ChartTheme.light())

    /** Mutable category-axis options configured by [xAxis]. */
    val xAxisOptions = AxisOptions()

    /** Mutable shared numeric-axis options configured by [yAxis]. */
    val yAxisOptions = AxisOptions().apply { includeZero = true }

    /** Mutable grid options configured by [grid]. */
    val gridOptions = GridOptions()

    /** Mutable combined legend options configured by [legend]. */
    val legendOptions = LegendOptions()

    /** Mutable click Tooltip options configured by [tooltip]. */
    val tooltipOptions = TooltipOptions()

    /** Mutable bar renderer options configured by [bars]. */
    val barOptions = BarOptions()

    /** Mutable line renderer options configured by [line]. */
    val lineOptions = LineOptions()

    /** Replaces the categorical series rendered as bars. */
    fun barData(vararg value: ChartSeries<BarEntry>) {
        value.forEach { require(it.name.isNotBlank()) { "MixedChart bar series name must not be blank" } }
        require(barOptions.mode != BarMode.SINGLE || value.size <= 1) {
            "BarMode.SINGLE accepts at most one bar series"
        }
        barSeries = value.toList()
    }

    /** Replaces the categorical series rendered as lines. */
    fun lineData(vararg value: ChartSeries<BarEntry>) {
        value.forEach { require(it.name.isNotBlank()) { "MixedChart line series name must not be blank" } }
        lineSeries = value.toList()
    }

    /** Configures the shared category axis. */
    fun xAxis(block: AxisOptions.() -> Unit) = xAxisOptions.applyValidated(block)

    /** Configures the shared numeric axis. */
    fun yAxis(block: AxisOptions.() -> Unit) = yAxisOptions.applyValidated(block)

    /** Configures horizontal grid lines. */
    fun grid(block: GridOptions.() -> Unit) {
        gridOptions.apply(block)
        require(gridOptions.lineWidth >= 0f) { "grid.lineWidth must be >= 0" }
    }

    /** Configures the combined series legend. */
    fun legend(block: LegendOptions.() -> Unit) = legendOptions.apply(block)

    /** Configures click selection feedback and numeric formatting. */
    fun tooltip(block: TooltipOptions.() -> Unit) = tooltipOptions.apply(block)

    /** Configures grouping, width, labels and corner radius for bar series. */
    fun bars(block: BarOptions.() -> Unit) {
        barOptions.apply(block)
        require(barOptions.barWidthRatio > 0f && barOptions.barWidthRatio <= 1f) {
            "bars.barWidthRatio must be in (0, 1]"
        }
        require(barOptions.cornerRadius >= 0f) { "bars.cornerRadius must be >= 0" }
        require(barOptions.mode != BarMode.SINGLE || barSeries.size <= 1) {
            "BarMode.SINGLE accepts at most one bar series"
        }
    }

    /** Configures interpolation, stroke and markers for line series. */
    fun line(block: LineOptions.() -> Unit) {
        lineOptions.apply(block)
        require(lineOptions.lineWidth > 0f) { "line.lineWidth must be > 0" }
        require(lineOptions.pointRadius >= 0f) { "line.pointRadius must be >= 0" }
    }

    /** Configures the chart-local entry reveal without animating its container. */
    fun entrance(block: EntranceOptions.() -> Unit) = EntranceOptions(entranceProgress).apply(block).also {
        entranceProgress = it.progress.coerceIn(0f, 1f)
    }
}

/** Mutable entry-animation configuration shared by every chart renderer. */
class EntranceOptions(initialProgress: Float) {
    var progress: Float = initialProgress
}

private fun AxisOptions.applyValidated(block: AxisOptions.() -> Unit): AxisOptions {
    apply(block)
    require(tickCount in 2..10) { "axis.tickCount must be in 2..10" }
    return this
}

/** Event DSL emitted by [LineChartView], [AreaChartView] and [SparklineChartView]. */
class LineChartEvent : ComposeEvent() {
    internal var itemSelectedHandler: ((ChartSelection<ChartPoint>) -> Unit)? = null
    internal var trackerChangedHandler: ((ChartTracker<ChartPoint>?) -> Unit)? = null
    internal var viewportChangedHandler: ((ChartViewport) -> Unit)? = null

    /** Registers the callback invoked after a line point is hit by a click. */
    fun onItemSelected(handler: (ChartSelection<ChartPoint>) -> Unit) {
        itemSelectedHandler = handler
    }

    /**
     * Registers updates from the opt-in long-press tracker.
     *
     * A null value indicates that a non-persistent tracker was released or cleared.
     */
    fun onTrackerChanged(handler: (ChartTracker<ChartPoint>?) -> Unit) {
        trackerChangedHandler = handler
    }

    /** Registers an opt-in line-chart viewport change caused by horizontal panning. */
    fun onViewportChanged(handler: (ChartViewport) -> Unit) {
        viewportChangedHandler = handler
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

/** Event DSL emitted by [PieChartView]. */
class PieChartEvent : ComposeEvent() {
    internal var itemSelectedHandler: ((ChartSelection<PieEntry>) -> Unit)? = null

    /** Registers the callback invoked after a visible pie or donut slice is hit. */
    fun onItemSelected(handler: (ChartSelection<PieEntry>) -> Unit) {
        itemSelectedHandler = handler
    }
}

/** Event DSL emitted by [MixedChartView]. */
class MixedChartEvent : ComposeEvent() {
    internal var itemSelectedHandler: ((MixedChartSelection) -> Unit)? = null

    /** Registers the callback invoked after a mixed bar or line item is hit. */
    fun onItemSelected(handler: (MixedChartSelection) -> Unit) {
        itemSelectedHandler = handler
    }
}

/**
 * Shared implementation for Cartesian line and area charts.
 */
abstract class CartesianLineChartView<A : LineChartAttr> : ComposeView<A, LineChartEvent>() {
    private var selected: ChartSelection<ChartPoint>? by observable(null)
    private var tracker: ChartTracker<ChartPoint>? by observable(null)
    private var viewportStartIndex: Int by observable(LineViewportController.UNSET_START_INDEX)
    private var renderedPoints: List<RenderedLinePoint> = emptyList()
    private var panStartX: Float? = null
    private var panStartViewport: ChartViewport? = null

    protected open fun areaOptions(): AreaOptions? = null
    protected open fun selectionEnabled(): Boolean = true

    override fun body(): ViewBuilder {
        val chart = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event {
                    click { params ->
                        if (!chart.selectionEnabled()) return@click
                        val selection = ChartHitTest.line(chart.renderedPoints, params.x, params.y)
                        chart.selected = selection
                        if (selection != null) chart.event.itemSelectedHandler?.invoke(selection)
                    }
                    longPress { params ->
                        if (!chart.attr.tooltipOptions.trackerEnabled) return@longPress
                        when (params.state) {
                            "start", "move" -> chart.updateTracker(params.x)
                            "end" -> if (!chart.attr.tooltipOptions.keepTrackerOnRelease) chart.clearTracker()
                        }
                    }
                    pan { params ->
                        if (!chart.attr.interactionOptions.enablePan || chart.tracker != null) return@pan
                        when (params.state) {
                            "start" -> chart.beginPan(params.x)
                            "move" -> chart.updatePan(params.x)
                            "end" -> chart.endPan()
                        }
                    }
                }
            }) { context, width, height ->
                chart.renderedPoints = ChartCanvasPainter.drawLine(
                    context = context,
                    width = width,
                    height = height,
                    attr = chart.attr,
                    selected = chart.selected,
                    tracker = chart.tracker,
                    viewport = chart.currentViewport(),
                    areaOptions = chart.areaOptions(),
                )
            }
        }
    }

    private fun updateTracker(x: Float) {
        val updated = ChartHitTest.lineTracker(renderedPoints, x)
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

    private fun currentViewport(): ChartViewport? {
        val itemCount = attr.series.maxOfOrNull { it.items.size } ?: 0
        return LineViewportController.resolve(itemCount, viewportStartIndex, attr.interactionOptions.visibleItemCount)
    }

    private fun beginPan(x: Float) {
        val viewport = currentViewport() ?: return
        val itemCount = attr.series.maxOfOrNull { it.items.size } ?: return
        if (!LineViewportController.isPannable(itemCount, viewport)) return
        panStartX = x
        panStartViewport = viewport
    }

    private fun updatePan(x: Float) {
        val startX = panStartX ?: return
        val startViewport = panStartViewport ?: return
        val slotWidth = ChartHitTest.lineSlotWidth(renderedPoints) ?: return
        val itemCount = attr.series.maxOfOrNull { it.items.size } ?: return
        val deltaItems = ((startX - x) / slotWidth).roundToInt()
        val updated = LineViewportController.pan(itemCount, startViewport, deltaItems)
        if (updated.startIndex != viewportStartIndex) {
            viewportStartIndex = updated.startIndex
            event.viewportChangedHandler?.invoke(updated)
        }
    }

    private fun endPan() {
        panStartX = null
        panStartViewport = null
    }
}

/**
 * Kuikly ComposeView that renders a cross-platform line chart on [Canvas].
 *
 * Use the [LineChart] container extension instead of instantiating this class
 * directly in application pages.
 */
class LineChartView : CartesianLineChartView<LineChartAttr>() {
    override fun createAttr() = LineChartAttr()
    override fun createEvent() = LineChartEvent()
}

/**
 * Kuikly ComposeView that renders a filled Cartesian area chart on [Canvas].
 *
 * It preserves the line chart's selection, tracker and viewport callback
 * semantics while including zero in the Y domain by default.
 */
class AreaChartView : CartesianLineChartView<AreaChartAttr>() {
    override fun createAttr() = AreaChartAttr()
    override fun createEvent() = LineChartEvent()
    override fun areaOptions(): AreaOptions = attr.areaOptions
}

/**
 * Kuikly ComposeView for a compact, single-series, axis-free trend.
 *
 * It reuses the line renderer and event payload while keeping selection disabled
 * unless [SparklineChartAttr.selectable] is explicitly enabled.
 */
class SparklineChartView : CartesianLineChartView<SparklineChartAttr>() {
    override fun createAttr() = SparklineChartAttr()
    override fun createEvent() = LineChartEvent()
    override fun selectionEnabled(): Boolean = attr.selectable
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

/**
 * Kuikly ComposeView that renders a cross-platform pie or donut chart on [Canvas].
 *
 * Set `pie { innerRadiusRatio = 0f }` for a pie and a positive ratio for a donut.
 */
class PieChartView : ComposeView<PieChartAttr, PieChartEvent>() {
    private var selected: ChartSelection<PieEntry>? by observable(null)
    private var selectionProgress: Float by observable(0f)
    private var renderedSlices: List<RenderedPieSlice> = emptyList()
    private var selectionTimer: Timer? = null

    override fun createAttr() = PieChartAttr()
    override fun createEvent() = PieChartEvent()

    override fun body(): ViewBuilder {
        val chart = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event {
                    click { params ->
                        val selection = ChartHitTest.pie(chart.renderedSlices, params.x, params.y)
                        chart.selected = selection
                        if (selection != null) {
                            chart.animateSelection()
                            chart.event.itemSelectedHandler?.invoke(selection)
                        } else {
                            chart.cancelSelectionAnimation()
                            chart.selectionProgress = 0f
                        }
                    }
                }
            }) { context, width, height ->
                chart.renderedSlices = ChartCanvasPainter.drawPie(
                    context = context,
                    width = width,
                    height = height,
                    attr = chart.attr,
                    selected = chart.selected,
                    selectionProgress = chart.selectionProgress,
                )
            }
        }
    }

    private fun animateSelection() {
        cancelSelectionAnimation()
        selectionProgress = 0f
        var frame = 0
        val frameCount = 9
        val timer = Timer()
        selectionTimer = timer
        timer.schedule(delay = 16, period = 16) {
            frame += 1
            val linear = (frame.toFloat() / frameCount).coerceIn(0f, 1f)
            selectionProgress = 1f - (1f - linear) * (1f - linear)
            if (linear >= 1f) {
                timer.cancel()
                if (selectionTimer === timer) selectionTimer = null
            }
        }
    }

    private fun cancelSelectionAnimation() {
        selectionTimer?.cancel()
        selectionTimer = null
    }

    override fun viewDestroyed() {
        cancelSelectionAnimation()
        super.viewDestroyed()
    }
}

/** Kuikly ComposeView that renders categorical bars and lines in one coordinate system. */
class MixedChartView : ComposeView<MixedChartAttr, MixedChartEvent>() {
    private var selected: MixedChartSelection? by observable(null)
    private var rendered: RenderedMixedChart = RenderedMixedChart(emptyList(), emptyList())

    override fun createAttr() = MixedChartAttr()
    override fun createEvent() = MixedChartEvent()

    override fun body(): ViewBuilder {
        val chart = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event {
                    click { params ->
                        val selection = ChartHitTest.mixed(
                            linePoints = chart.rendered.linePoints,
                            bars = chart.rendered.bars,
                            x = params.x,
                            y = params.y,
                        )
                        chart.selected = selection
                        if (selection != null) chart.event.itemSelectedHandler?.invoke(selection)
                    }
                }
            }) { context, width, height ->
                chart.rendered = ChartCanvasPainter.drawMixed(
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

/** Adds a declaratively configured [AreaChartView] to this Kuikly container. */
fun ViewContainer<*, *>.AreaChart(init: AreaChartView.() -> Unit) {
    addChild(AreaChartView(), init)
}

/** Adds a declaratively configured [SparklineChartView] to this Kuikly container. */
fun ViewContainer<*, *>.SparklineChart(init: SparklineChartView.() -> Unit) {
    addChild(SparklineChartView(), init)
}

/** Adds a declaratively configured [BarChartView] to this Kuikly container. */
fun ViewContainer<*, *>.BarChart(init: BarChartView.() -> Unit) {
    addChild(BarChartView(), init)
}

/** Adds a declaratively configured [PieChartView] to this Kuikly container. */
fun ViewContainer<*, *>.PieChart(init: PieChartView.() -> Unit) {
    addChild(PieChartView(), init)
}

/** Adds a declaratively configured [MixedChartView] to this Kuikly container. */
fun ViewContainer<*, *>.MixedChart(init: MixedChartView.() -> Unit) {
    addChild(MixedChartView(), init)
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
        tracker: ChartTracker<ChartPoint>?,
        viewport: ChartViewport?,
        areaOptions: AreaOptions?,
    ): List<RenderedLinePoint> {
        fillRect(context, ChartRect(0f, 0f, width, height), attr.theme.backgroundColor)
        val sourceSeries = attr.series
        if (width <= 0f || height <= 0f || viewport == null) {
            drawEmpty(context, width, height, attr.theme)
            return emptyList()
        }
        val visibleItems = sourceSeries.map { series ->
            series.items.withIndex().filter { indexed -> indexed.index in viewport.startIndex..viewport.endIndex }
        }
        val visibleSeries = sourceSeries.mapIndexed { index, series ->
            series.copy(items = visibleItems[index].map { indexed -> indexed.value })
        }
        if (visibleSeries.none { series -> series.items.any { it.x.isFinite() && it.y.isFinite() } }) {
            drawEmpty(context, width, height, attr.theme)
            return emptyList()
        }

        context.font(LABEL_FONT_SIZE)
        val preliminary = ChartLayoutEngine.line(
            width, height, visibleSeries, ChartMargins(), attr.yAxisOptions.tickCount, attr.yAxisOptions.includeZero,
        )
        val yLabelWidth = preliminary.yScale.ticks.maxOfOrNull {
            context.measureText(attr.yAxisOptions.format(it)).width
        } ?: 32f
        val layout = ChartLayoutEngine.line(
            width = width,
            height = height,
            series = visibleSeries,
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
            val labelSource = visibleSeries.firstOrNull { series -> series.items.any { it.x.isFinite() && it.y.isFinite() } }
            if (labelSource != null) drawLineXLabels(context, layout, labelSource.items, attr.xAxisOptions, attr.theme)
        }
        if (attr.legendOptions.visible) drawLegend(context, sourceSeries, attr.theme, width)

        val rendered = mutableListOf<RenderedLinePoint>()
        val revealX = layout.plot.left + layout.plot.width * attr.entranceProgress.coerceIn(0f, 1f)
        sourceSeries.forEachIndexed { seriesIndex, series ->
            val color = series.color ?: attr.theme.palette[seriesIndex % attr.theme.palette.size]
            val rawSegments = mutableListOf<MutableList<IndexedValue<ChartPoint>>>()
            var currentRawSegment = mutableListOf<IndexedValue<ChartPoint>>()
            visibleItems[seriesIndex].forEach { indexed ->
                val point = indexed.value
                if (!point.x.isFinite() || !point.y.isFinite()) {
                    if (currentRawSegment.isNotEmpty()) rawSegments += currentRawSegment
                    currentRawSegment = mutableListOf()
                } else {
                    currentRawSegment += indexed
                }
            }
            if (currentRawSegment.isNotEmpty()) rawSegments += currentRawSegment

            val renderBudget = if (attr.interactionOptions.maxRenderPointCount > 0) {
                attr.interactionOptions.maxRenderPointCount
            } else {
                (layout.plot.width * 2f).roundToInt().coerceAtLeast(64)
            }
            val segments = LineSampler.sampleSegments(rawSegments, renderBudget).map { sampledSegment ->
                sampledSegment.map { indexed ->
                    val point = indexed.value
                    RenderedLinePoint(
                        x = layout.xFor(point.x),
                        y = layout.yFor(point.y),
                        selection = ChartSelection(seriesIndex, indexed.index, series.name, point),
                    )
                }
            }
            segments.forEach { segment ->
                val visibleSegment = revealLineSegment(segment, revealX)
                if (visibleSegment.isEmpty()) return@forEach
                if (areaOptions != null) {
                    val fillColor = areaOptions.fillColors[seriesIndex % areaOptions.fillColors.size]
                    drawAreaPath(
                        context = context,
                        points = visibleSegment,
                        baselineY = layout.yFor(0f).coerceIn(layout.plot.top, layout.plot.bottom),
                        color = fillColor,
                        options = attr.lineOptions,
                    )
                }
                drawLinePath(context, visibleSegment, color, attr.lineOptions)
            }
            val points = segments.flatten()
            if (attr.lineOptions.showPoints) {
                points.filter { it.x <= revealX }.forEach {
                    drawCircle(context, it.x, it.y, attr.lineOptions.pointRadius, color)
                }
            }
            rendered += points
        }

        if (tracker == null) {
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
        } else {
            drawLineTracker(context, width, height, layout.plot, rendered, tracker, attr)
        }
        return rendered
    }

    private fun drawLineTracker(
        context: CanvasContext,
        width: Float,
        height: Float,
        plot: ChartRect,
        rendered: List<RenderedLinePoint>,
        tracker: ChartTracker<ChartPoint>,
        attr: LineChartAttr,
    ) {
        val trackerSelections = tracker.selections.toSet()
        val trackerPoints = rendered.filter { it.selection in trackerSelections }
        val anchor = trackerPoints.minByOrNull { it.y } ?: return
        drawSelectionLine(context, plot, anchor.x, attr.theme.selectionColor)
        trackerPoints.forEach { point ->
            drawCircle(context, point.x, point.y, attr.lineOptions.pointRadius + 3f, attr.theme.backgroundColor)
            drawCircle(context, point.x, point.y, attr.lineOptions.pointRadius + 1f, attr.theme.selectionColor)
        }
        if (attr.tooltipOptions.enabled) {
            val label = tracker.selections.first().item.label.ifBlank { attr.xAxisOptions.format(tracker.x) }
            val values = tracker.selections.joinToString(" · ") { selection ->
                "${selection.seriesName}: ${attr.tooltipOptions.format(selection.item.y)}"
            }
            drawTooltip(context, width, height, anchor.x, anchor.y, "$label: $values", attr.theme)
        }
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
                val animatedValueY = zeroY + (valueY - zeroY) * attr.entranceProgress.coerceIn(0f, 1f)
                val left = groupLeft + seriesIndex * barWidth + 1f
                val right = groupLeft + (seriesIndex + 1) * barWidth - 1f
                val top = min(zeroY, animatedValueY)
                val bottom = max(zeroY, animatedValueY).coerceAtLeast(top + 1f)
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

    fun drawPie(
        context: CanvasContext,
        width: Float,
        height: Float,
        attr: PieChartAttr,
        selected: ChartSelection<PieEntry>?,
        selectionProgress: Float,
    ): List<RenderedPieSlice> {
        fillRect(context, ChartRect(0f, 0f, width, height), attr.theme.backgroundColor)
        val slices = PieLayoutEngine.layout(
            width = width,
            height = height,
            entries = attr.entries,
            seriesName = attr.seriesName.ifBlank { "数据占比" },
            innerRadiusRatio = attr.pieOptions.innerRadiusRatio,
            startAngleDegrees = attr.pieOptions.startAngleDegrees,
            gapAngleDegrees = attr.pieOptions.gapAngleDegrees,
            legendVisible = attr.legendOptions.visible,
        )
        if (slices.isEmpty()) {
            drawEmpty(context, width, height, attr.theme)
            return emptyList()
        }

        if (attr.legendOptions.visible) {
            drawLegend(
                context = context,
                series = slices.map { slice ->
                    ChartSeries<Unit>(
                        name = slice.selection.item.label,
                        items = emptyList(),
                        color = slice.selection.item.color
                            ?: attr.theme.palette[slice.selection.itemIndex % attr.theme.palette.size],
                    )
                },
                theme = attr.theme,
                width = width,
            )
        }

        val startAngle = slices.first().startAngle
        val revealAngle = startAngle + (PI.toFloat() * 2f) * attr.entranceProgress.coerceIn(0f, 1f)
        slices.forEach { slice ->
            val visibleSlice = revealPieSlice(slice, revealAngle) ?: return@forEach
            val displaySlice = if (slice.selection == selected) {
                expandPieSlice(visibleSlice, selectionProgress)
            } else {
                visibleSlice
            }
            val color = slice.selection.item.color
                ?: attr.theme.palette[slice.selection.itemIndex % attr.theme.palette.size]
            drawPieSlice(context, displaySlice, color, fill = true)
            if (attr.pieOptions.showValueLabels && slice.endAngle <= revealAngle && slice.fraction >= 0.06f) {
                val labelRadius = (displaySlice.innerRadius + displaySlice.outerRadius) / 2f
                val labelX = displaySlice.centerX + cos(displaySlice.middleAngle.toDouble()).toFloat() * labelRadius
                val labelY = displaySlice.centerY + sin(displaySlice.middleAngle.toDouble()).toFloat() * labelRadius
                context.font(10f)
                context.textAlign(TextAlign.CENTER)
                context.fillStyle(attr.theme.tooltipTextColor)
                context.fillText("${(slice.fraction * 100f).roundToInt()}%", labelX, labelY + 4f)
            }
        }

        if (attr.pieOptions.innerRadiusRatio > 0f && attr.entranceProgress >= 0.92f) {
            val first = slices.first()
            context.textAlign(TextAlign.CENTER)
            context.fillStyle(attr.theme.labelColor)
            context.font(11f)
            val centerLabel = attr.pieOptions.centerLabel.ifBlank { attr.seriesName }
            context.fillText(centerLabel, first.centerX, first.centerY - 3f)
            context.font(14f)
            val total = slices.sumOf { it.selection.item.value.toDouble() }.toFloat()
            context.fillText(attr.tooltipOptions.format(total), first.centerX, first.centerY + 16f)
        }

        selected?.let { selection ->
            slices.firstOrNull { it.selection == selection }?.let { slice ->
                val displaySlice = expandPieSlice(slice, selectionProgress)
                drawPieSlice(context, displaySlice, attr.theme.selectionColor, fill = false)
                if (attr.tooltipOptions.enabled) {
                    val anchorRadius = (displaySlice.innerRadius + displaySlice.outerRadius) / 2f
                    val anchorX = displaySlice.centerX + cos(displaySlice.middleAngle.toDouble()).toFloat() * anchorRadius
                    val anchorY = displaySlice.centerY + sin(displaySlice.middleAngle.toDouble()).toFloat() * anchorRadius
                    drawTooltip(
                        context = context,
                        width = width,
                        height = height,
                        anchorX = anchorX,
                        anchorY = anchorY,
                        text = "${selection.item.label}: ${attr.tooltipOptions.format(selection.item.value)} · " +
                            "${(slice.fraction * 100f).roundToInt()}%",
                        theme = attr.theme,
                    )
                }
            }
        }
        return slices
    }

    fun drawMixed(
        context: CanvasContext,
        width: Float,
        height: Float,
        attr: MixedChartAttr,
        selected: MixedChartSelection?,
    ): RenderedMixedChart {
        fillRect(context, ChartRect(0f, 0f, width, height), attr.theme.backgroundColor)
        val allSeries = attr.barSeries + attr.lineSeries
        if (width <= 0f || height <= 0f ||
            allSeries.none { series -> series.items.any { it.value.isFinite() } }
        ) {
            drawEmpty(context, width, height, attr.theme)
            return RenderedMixedChart(emptyList(), emptyList())
        }

        context.font(LABEL_FONT_SIZE)
        val preliminary = ChartLayoutEngine.mixed(
            width = width,
            height = height,
            barSeries = attr.barSeries,
            lineSeries = attr.lineSeries,
            margins = ChartMargins(),
            tickCount = attr.yAxisOptions.tickCount,
            includeZero = attr.yAxisOptions.includeZero,
        )
        val yLabelWidth = preliminary.yScale.ticks.maxOfOrNull {
            context.measureText(attr.yAxisOptions.format(it)).width
        } ?: 32f
        val layout = ChartLayoutEngine.mixed(
            width = width,
            height = height,
            barSeries = attr.barSeries,
            lineSeries = attr.lineSeries,
            margins = ChartMargins(
                left = if (attr.yAxisOptions.visible) yLabelWidth + 14f else 14f,
                top = if (attr.legendOptions.visible) 32f else 16f,
                right = 16f,
                bottom = if (attr.xAxisOptions.visible) 36f else 14f,
            ),
            tickCount = attr.yAxisOptions.tickCount,
            includeZero = attr.yAxisOptions.includeZero,
        )

        drawCartesianFrame(
            context,
            layout.plot,
            layout.yScale,
            attr.yAxisOptions,
            attr.gridOptions,
            attr.theme,
        )
        if (attr.xAxisOptions.visible) {
            val labelSource = allSeries.firstOrNull { series -> series.items.any { it.value.isFinite() } }
            if (labelSource != null) drawBarXLabels(context, layout, labelSource.items, attr.theme)
        }
        if (attr.legendOptions.visible) drawLegend(context, allSeries, attr.theme, width)

        val renderedBars = mutableListOf<RenderedMixedBar>()
        val groupWidth = layout.slotWidth * attr.barOptions.barWidthRatio
        val barWidth = (groupWidth / attr.barSeries.size.coerceAtLeast(1)).coerceAtLeast(1f)
        val zeroY = layout.yFor(0f).coerceIn(layout.plot.top, layout.plot.bottom)
        attr.barSeries.forEachIndexed { seriesIndex, series ->
            val color = series.color ?: attr.theme.palette[seriesIndex % attr.theme.palette.size]
            series.items.forEachIndexed { itemIndex, entry ->
                if (!entry.value.isFinite() || itemIndex >= layout.categoryCount) return@forEachIndexed
                val groupLeft = layout.categoryCenter(itemIndex) - groupWidth / 2f
                val valueY = layout.yFor(entry.value).coerceIn(layout.plot.top, layout.plot.bottom)
                val barProgress = (attr.entranceProgress.coerceIn(0f, 1f) / 0.62f).coerceIn(0f, 1f)
                val animatedValueY = zeroY + (valueY - zeroY) * barProgress
                val left = groupLeft + seriesIndex * barWidth + 1f
                val right = groupLeft + (seriesIndex + 1) * barWidth - 1f
                val top = min(zeroY, animatedValueY)
                val bottom = max(zeroY, animatedValueY).coerceAtLeast(top + 1f)
                val bounds = ChartRect(left, top, right, bottom)
                fillRoundedRect(context, bounds, attr.barOptions.cornerRadius, color)
                renderedBars += RenderedMixedBar(
                    bounds = bounds,
                    selection = MixedChartSelection(
                        seriesType = MixedSeriesType.BAR,
                        seriesIndex = seriesIndex,
                        itemIndex = itemIndex,
                        seriesName = series.name,
                        item = entry,
                    ),
                )
                if (attr.barOptions.showValueLabels && barWidth >= 18f) {
                    context.font(10f)
                    context.textAlign(TextAlign.CENTER)
                    context.fillStyle(attr.theme.labelColor)
                    val labelY = if (entry.value >= 0f) {
                        (top - 5f).coerceAtLeast(layout.plot.top + 10f)
                    } else {
                        (bottom + 12f).coerceAtMost(layout.plot.bottom)
                    }
                    context.fillText(attr.tooltipOptions.format(entry.value), (left + right) / 2f, labelY)
                }
            }
        }

        val renderedLinePoints = mutableListOf<RenderedMixedLinePoint>()
        val lineRevealProgress = ((attr.entranceProgress.coerceIn(0f, 1f) - 0.35f) / 0.65f).coerceIn(0f, 1f)
        val lineRevealX = layout.plot.left + layout.plot.width * lineRevealProgress
        attr.lineSeries.forEachIndexed { seriesIndex, series ->
            val paletteIndex = attr.barSeries.size + seriesIndex
            val color = series.color ?: attr.theme.palette[paletteIndex % attr.theme.palette.size]
            val segments = mutableListOf<MutableList<RenderedMixedLinePoint>>()
            var segment = mutableListOf<RenderedMixedLinePoint>()
            series.items.forEachIndexed { itemIndex, entry ->
                if (!entry.value.isFinite()) {
                    if (segment.isNotEmpty()) segments += segment
                    segment = mutableListOf()
                } else {
                    segment += RenderedMixedLinePoint(
                        x = layout.categoryCenter(itemIndex),
                        y = layout.yFor(entry.value),
                        selection = MixedChartSelection(
                            seriesType = MixedSeriesType.LINE,
                            seriesIndex = seriesIndex,
                            itemIndex = itemIndex,
                            seriesName = series.name,
                            item = entry,
                        ),
                    )
                }
            }
            if (segment.isNotEmpty()) segments += segment
            segments.forEach { segment ->
                val visibleSegment = revealMixedLineSegment(segment, lineRevealX)
                if (visibleSegment.isNotEmpty()) drawMixedLinePath(context, visibleSegment, color, attr.lineOptions)
            }
            val points = segments.flatten()
            if (attr.lineOptions.showPoints) {
                points.filter { it.x <= lineRevealX }.forEach {
                    drawCircle(context, it.x, it.y, attr.lineOptions.pointRadius, color)
                }
            }
            renderedLinePoints += points
        }

        selected?.let { selection ->
            when (selection.seriesType) {
                MixedSeriesType.BAR -> renderedBars.firstOrNull { it.selection == selection }?.let { bar ->
                    strokeRect(context, bar.bounds, attr.theme.selectionColor, 2f)
                    if (attr.tooltipOptions.enabled) {
                        drawTooltip(
                            context,
                            width,
                            height,
                            (bar.bounds.left + bar.bounds.right) / 2f,
                            bar.bounds.top,
                            "${selection.seriesName} · ${selection.item.label}: " +
                                attr.tooltipOptions.format(selection.item.value),
                            attr.theme,
                        )
                    }
                }

                MixedSeriesType.LINE -> renderedLinePoints.firstOrNull { it.selection == selection }?.let { point ->
                    drawSelectionLine(context, layout.plot, point.x, attr.theme.selectionColor)
                    drawCircle(context, point.x, point.y, attr.lineOptions.pointRadius + 3f, attr.theme.backgroundColor)
                    drawCircle(context, point.x, point.y, attr.lineOptions.pointRadius + 1f, attr.theme.selectionColor)
                    if (attr.tooltipOptions.enabled) {
                        drawTooltip(
                            context,
                            width,
                            height,
                            point.x,
                            point.y,
                            "${selection.seriesName} · ${selection.item.label}: " +
                                attr.tooltipOptions.format(selection.item.value),
                            attr.theme,
                        )
                    }
                }
            }
        }
        return RenderedMixedChart(renderedBars, renderedLinePoints)
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

    private fun drawAreaPath(
        context: CanvasContext,
        points: List<RenderedLinePoint>,
        baselineY: Float,
        color: Color,
        options: LineOptions,
    ) {
        if (points.isEmpty()) return
        context.beginPath()
        context.moveTo(points.first().x, baselineY)
        context.lineTo(points.first().x, points.first().y)
        points.zipWithNext().forEach { (previous, current) ->
            if (options.smooth) {
                val controlX = (previous.x + current.x) / 2f
                context.bezierCurveTo(controlX, previous.y, controlX, current.y, current.x, current.y)
            } else {
                context.lineTo(current.x, current.y)
            }
        }
        context.lineTo(points.last().x, baselineY)
        context.closePath()
        context.fillStyle(color)
        context.fill()
    }

    private fun drawPieSlice(
        context: CanvasContext,
        slice: RenderedPieSlice,
        color: Color,
        fill: Boolean,
    ) {
        val outerStartX = slice.centerX + cos(slice.startAngle.toDouble()).toFloat() * slice.outerRadius
        val outerStartY = slice.centerY + sin(slice.startAngle.toDouble()).toFloat() * slice.outerRadius
        context.beginPath()
        if (slice.innerRadius <= 0f) {
            context.moveTo(slice.centerX, slice.centerY)
            context.lineTo(outerStartX, outerStartY)
            context.arc(
                slice.centerX,
                slice.centerY,
                slice.outerRadius,
                slice.startAngle,
                slice.endAngle,
                false,
            )
            context.closePath()
        } else {
            val innerEndX = slice.centerX + cos(slice.endAngle.toDouble()).toFloat() * slice.innerRadius
            val innerEndY = slice.centerY + sin(slice.endAngle.toDouble()).toFloat() * slice.innerRadius
            context.moveTo(outerStartX, outerStartY)
            context.arc(
                slice.centerX,
                slice.centerY,
                slice.outerRadius,
                slice.startAngle,
                slice.endAngle,
                false,
            )
            context.lineTo(innerEndX, innerEndY)
            context.arc(
                slice.centerX,
                slice.centerY,
                slice.innerRadius,
                slice.endAngle,
                slice.startAngle,
                true,
            )
            context.closePath()
        }
        if (fill) {
            context.fillStyle(color)
            context.fill()
        } else {
            context.strokeStyle(color)
            context.lineWidth(3f)
            context.stroke()
        }
    }

    /** Reveals a Cartesian segment from left to right, including an interpolated leading edge. */
    private fun revealLineSegment(
        points: List<RenderedLinePoint>,
        revealX: Float,
    ): List<RenderedLinePoint> {
        if (points.isEmpty() || revealX < points.first().x) return emptyList()
        if (revealX >= points.last().x) return points
        val visible = points.takeWhile { it.x <= revealX }.toMutableList()
        val next = points.getOrNull(visible.size) ?: return visible
        val previous = visible.lastOrNull() ?: return emptyList()
        val fraction = ((revealX - previous.x) / (next.x - previous.x)).coerceIn(0f, 1f)
        visible += previous.copy(y = previous.y + (next.y - previous.y) * fraction, x = revealX)
        return visible
    }

    /** Reveals a mixed-chart line from left to right, including an interpolated leading edge. */
    private fun revealMixedLineSegment(
        points: List<RenderedMixedLinePoint>,
        revealX: Float,
    ): List<RenderedMixedLinePoint> {
        if (points.isEmpty() || revealX < points.first().x) return emptyList()
        if (revealX >= points.last().x) return points
        val visible = points.takeWhile { it.x <= revealX }.toMutableList()
        val next = points.getOrNull(visible.size) ?: return visible
        val previous = visible.lastOrNull() ?: return emptyList()
        val fraction = ((revealX - previous.x) / (next.x - previous.x)).coerceIn(0f, 1f)
        visible += previous.copy(y = previous.y + (next.y - previous.y) * fraction, x = revealX)
        return visible
    }

    /** Clips the aggregate pie sweep at the current radial reveal angle. */
    private fun revealPieSlice(slice: RenderedPieSlice, revealAngle: Float): RenderedPieSlice? {
        if (revealAngle <= slice.startAngle) return null
        return slice.copy(endAngle = min(slice.endAngle, revealAngle))
    }

    /** Pops the selected slice outward along its middle angle and gives it a small emphasis scale. */
    private fun expandPieSlice(slice: RenderedPieSlice, progress: Float): RenderedPieSlice {
        val emphasis = progress.coerceIn(0f, 1f)
        val offset = 10f * emphasis
        val radiusScale = 1f + 0.055f * emphasis
        return slice.copy(
            centerX = slice.centerX + cos(slice.middleAngle.toDouble()).toFloat() * offset,
            centerY = slice.centerY + sin(slice.middleAngle.toDouble()).toFloat() * offset,
            innerRadius = slice.innerRadius * radiusScale,
            outerRadius = slice.outerRadius * radiusScale,
        )
    }

    private fun drawMixedLinePath(
        context: CanvasContext,
        points: List<RenderedMixedLinePoint>,
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
