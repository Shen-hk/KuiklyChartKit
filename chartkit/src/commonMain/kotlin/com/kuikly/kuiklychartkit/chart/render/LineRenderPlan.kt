package com.kuikly.kuiklychartkit.chart.render

import com.kuikly.kuiklychartkit.chart.ChartLayoutEngine
import com.kuikly.kuiklychartkit.chart.ChartMargins
import com.kuikly.kuiklychartkit.chart.ChartPoint
import com.kuikly.kuiklychartkit.chart.ChartSelection
import com.kuikly.kuiklychartkit.chart.ChartSeries
import com.kuikly.kuiklychartkit.chart.ChartViewport
import com.kuikly.kuiklychartkit.chart.LineChartLayout
import com.kuikly.kuiklychartkit.chart.LineSampler
import com.kuikly.kuiklychartkit.chart.RenderedLinePoint
import com.tencent.kuikly.core.base.Color
import kotlin.math.roundToInt

/** 折线 Canvas 绘制器消费的一组不可变系列几何数据。 */
internal data class LineSeriesRenderPlan(
    val colorIndex: Int,
    val color: Color,
    val segments: List<List<RenderedLinePoint>>,
)

/**
 * 折线、面积和迷你趋势图一次 Canvas 绘制所解析出的几何与数据计划。
 *
 * 此计划也是命中目标的唯一来源，因此交互层不需要从原始数据重新推导屏幕坐标。
 */
internal data class LineRenderPlan(
    val layout: LineChartLayout,
    val xLabelItems: List<ChartPoint>?,
    val series: List<LineSeriesRenderPlan>,
) {
    val hitTargets: List<RenderedLinePoint> get() = series.flatMap { it.segments.flatten() }
}

internal object LineRenderPlanFactory {
    fun create(
        width: Float,
        height: Float,
        viewport: ChartViewport?,
        spec: ResolvedLineChartSpec,
        measureLabel: (String) -> Float,
    ): LineRenderPlan? {
        if (width <= 0f || height <= 0f || viewport == null) return null

        val layoutItems = spec.series.map { series ->
            series.items.withIndex().filter { it.index in viewport.startIndex..viewport.endIndex }
        }
        val renderStartIndex = (viewport.startIndex - 1).coerceAtLeast(0)
        val renderEndIndex = viewport.endIndex + 1
        val renderedItems = spec.series.map { series ->
            series.items.withIndex().filter { it.index in renderStartIndex..renderEndIndex }
        }
        val visibleSeries = spec.series.mapIndexed { index, series ->
            series.copy(items = layoutItems[index].map { it.value })
        }
        val visibleScaleSeries = spec.scaleSeries.map { series ->
            series.copy(items = series.items.withIndex().filter {
                it.index in viewport.startIndex..viewport.endIndex
            }.map { it.value })
        }
        if (visibleSeries.none { series -> series.items.any { it.x.isFinite() && it.y.isFinite() } }) return null

        val preliminary = ChartLayoutEngine.line(
            width = width,
            height = height,
            series = visibleScaleSeries,
            margins = ChartMargins(),
            tickCount = spec.style.yAxis.tickCount,
            includeZero = spec.style.yAxis.includeZero,
            yMin = spec.style.yAxis.min,
            yMax = spec.style.yAxis.max,
        )
        val yLabelWidth = preliminary.yScale.ticks.maxOfOrNull {
            measureLabel(spec.style.yAxis.format(it))
        } ?: 32f
        val layout = ChartLayoutEngine.line(
            width = width,
            height = height,
            series = visibleScaleSeries,
            margins = ChartMargins(
                left = if (spec.style.yAxis.visible) yLabelWidth + 14f else 14f,
                top = if (spec.style.legend.visible) 32f else 16f,
                right = 16f,
                bottom = if (spec.style.xAxis.visible) 36f else 14f,
            ),
            tickCount = spec.style.yAxis.tickCount,
            includeZero = spec.style.yAxis.includeZero,
            yMin = spec.style.yAxis.min,
            yMax = spec.style.yAxis.max,
        )
        val renderBudget = if (spec.interaction.maxRenderPointCount > 0) {
            spec.interaction.maxRenderPointCount
        } else {
            (layout.plot.width * 2f).roundToInt().coerceAtLeast(64)
        }
        val plannedSeries = spec.series.mapIndexed { seriesIndex, series ->
            val rawSegments = mutableListOf<MutableList<IndexedValue<ChartPoint>>>()
            var segment = mutableListOf<IndexedValue<ChartPoint>>()
            renderedItems[seriesIndex].forEach { indexed ->
                val point = indexed.value
                if (!point.x.isFinite() || !point.y.isFinite()) {
                    if (segment.isNotEmpty()) rawSegments += segment
                    segment = mutableListOf()
                } else {
                    segment += indexed
                }
            }
            if (segment.isNotEmpty()) rawSegments += segment
            LineSeriesRenderPlan(
                colorIndex = seriesIndex,
                color = series.color ?: spec.style.theme.palette[seriesIndex % spec.style.theme.palette.size],
                segments = LineSampler.sampleSegments(rawSegments, renderBudget).map { sampled ->
                    sampled.map { indexed ->
                        val point = indexed.value
                        RenderedLinePoint(
                            x = layout.xFor(point.x),
                            y = layout.yFor(point.y),
                            selection = ChartSelection(seriesIndex, indexed.index, series.name, point),
                        )
                    }
                },
            )
        }
        val xLabelItems = visibleSeries.firstOrNull { series ->
            series.items.any { it.x.isFinite() && it.y.isFinite() }
        }?.items
        return LineRenderPlan(layout, xLabelItems, plannedSeries)
    }
}
