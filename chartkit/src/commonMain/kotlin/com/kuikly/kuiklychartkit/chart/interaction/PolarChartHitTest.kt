package com.kuikly.kuiklychartkit.chart.interaction

import com.kuikly.kuiklychartkit.chart.ChartSelection
import com.kuikly.kuiklychartkit.chart.HeatmapEntry
import com.kuikly.kuiklychartkit.chart.RadarEntry
import com.kuikly.kuiklychartkit.chart.RenderedHeatmapCell
import com.kuikly.kuiklychartkit.chart.RenderedRadarPoint

/** 极坐标与矩阵图表渲染计划的命中测试。 */
internal object PolarChartHitTest {
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
