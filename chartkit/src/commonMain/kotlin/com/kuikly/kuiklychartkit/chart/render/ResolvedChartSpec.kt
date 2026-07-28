package com.kuikly.kuiklychartkit.chart.render

import com.kuikly.kuiklychartkit.chart.AxisOptions
import com.kuikly.kuiklychartkit.chart.ChartTheme
import com.kuikly.kuiklychartkit.chart.ChartPoint
import com.kuikly.kuiklychartkit.chart.ChartSeries
import com.kuikly.kuiklychartkit.chart.GridOptions
import com.kuikly.kuiklychartkit.chart.InteractionOptions
import com.kuikly.kuiklychartkit.chart.LegendOptions
import com.kuikly.kuiklychartkit.chart.LineChartAttr
import com.kuikly.kuiklychartkit.chart.LineOptions
import com.kuikly.kuiklychartkit.chart.TooltipOptions

/**
 * 提供给渲染器的不可变坐标轴配置副本。
 *
 * Kuikly DSL 的 Attr 对象仍保持可变；渲染器只接收此快照，因此一次 Canvas
 * 绘制不会读取到配置更新过程中的中间状态。
 */
internal data class ResolvedAxisSpec(
    val visible: Boolean,
    val tickCount: Int,
    val includeZero: Boolean,
    val min: Float?,
    val max: Float?,
    val labelFormatter: ((Float) -> String)?,
) {
    fun format(value: Float): String = labelFormatter?.invoke(value)
        ?: com.kuikly.kuiklychartkit.chart.formatChartValue(value)
}

internal data class ResolvedGridSpec(
    val visible: Boolean,
    val lineWidth: Float,
)

internal data class ResolvedLegendSpec(val visible: Boolean)

internal data class ResolvedTooltipSpec(
    val enabled: Boolean,
    val trackerEnabled: Boolean,
    val keepTrackerOnRelease: Boolean,
    val valueFormatter: ((Float) -> String)?,
) {
    fun format(value: Float): String = valueFormatter?.invoke(value)
        ?: com.kuikly.kuiklychartkit.chart.formatChartValue(value)
}

internal data class ResolvedLineStyle(
    val smooth: Boolean,
    val showPoints: Boolean,
    val lineWidth: Float,
    val pointRadius: Float,
)

/** 所有笛卡尔图表渲染器共用的不可变视觉配置。 */
internal data class ResolvedCartesianStyle(
    val theme: ChartTheme,
    val xAxis: ResolvedAxisSpec,
    val yAxis: ResolvedAxisSpec,
    val grid: ResolvedGridSpec,
    val legend: ResolvedLegendSpec,
    val tooltip: ResolvedTooltipSpec,
    val entranceProgress: Float,
)

/** 折线、面积或迷你趋势图一次绘制所需的不可变输入快照。 */
internal data class ResolvedLineChartSpec(
    val series: List<ChartSeries<ChartPoint>>,
    val scaleSeries: List<ChartSeries<ChartPoint>>,
    val style: ResolvedCartesianStyle,
    val line: ResolvedLineStyle,
    val interaction: ResolvedInteractionSpec,
)

internal data class ResolvedInteractionSpec(
    val enablePan: Boolean,
    val visibleItemCount: Int,
    val maxRenderPointCount: Int,
)

internal fun AxisOptions.resolve(): ResolvedAxisSpec = ResolvedAxisSpec(
    visible = visible,
    tickCount = tickCount,
    includeZero = includeZero,
    min = min,
    max = max,
    labelFormatter = labelFormatter,
)

internal fun GridOptions.resolve(): ResolvedGridSpec = ResolvedGridSpec(visible, lineWidth)

internal fun LegendOptions.resolve(): ResolvedLegendSpec = ResolvedLegendSpec(visible)

internal fun TooltipOptions.resolve(): ResolvedTooltipSpec = ResolvedTooltipSpec(
    enabled,
    trackerEnabled,
    keepTrackerOnRelease,
    valueFormatter,
)

internal fun LineOptions.resolve(): ResolvedLineStyle = ResolvedLineStyle(
    smooth,
    showPoints,
    lineWidth,
    pointRadius,
)

internal fun InteractionOptions.resolve(): ResolvedInteractionSpec = ResolvedInteractionSpec(
    enablePan,
    visibleItemCount,
    maxRenderPointCount,
)

internal fun LineChartAttr.resolveRenderSpec(
    seriesSnapshot: List<ChartSeries<ChartPoint>> = series,
    scaleSeriesSnapshot: List<ChartSeries<ChartPoint>> = seriesSnapshot,
): ResolvedLineChartSpec = ResolvedLineChartSpec(
    series = seriesSnapshot.snapshot(),
    scaleSeries = scaleSeriesSnapshot.snapshot(),
    style = ResolvedCartesianStyle(
        theme = theme,
        xAxis = xAxisOptions.resolve(),
        yAxis = yAxisOptions.resolve(),
        grid = gridOptions.resolve(),
        legend = legendOptions.resolve(),
        tooltip = tooltipOptions.resolve(),
        entranceProgress = entranceProgress.coerceIn(0f, 1f),
    ),
    line = lineOptions.resolve(),
    interaction = interactionOptions.resolve(),
)

private fun <T> List<ChartSeries<T>>.snapshot(): List<ChartSeries<T>> = map { series ->
    series.copy(items = series.items.toList())
}
