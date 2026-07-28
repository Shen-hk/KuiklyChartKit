package com.kuikly.kuiklychartkit.chart.interaction

import com.kuikly.kuiklychartkit.chart.ChartSelection
import com.kuikly.kuiklychartkit.chart.ChartTracker
import com.kuikly.kuiklychartkit.chart.ChartPoint
import com.kuikly.kuiklychartkit.chart.BarEntry
import com.kuikly.kuiklychartkit.chart.MixedChartSelection
import com.kuikly.kuiklychartkit.chart.PieEntry
import com.kuikly.kuiklychartkit.chart.RenderedBar
import com.kuikly.kuiklychartkit.chart.RenderedLinePoint
import com.kuikly.kuiklychartkit.chart.RenderedMixedBar
import com.kuikly.kuiklychartkit.chart.RenderedMixedLinePoint
import com.kuikly.kuiklychartkit.chart.RenderedPieSlice
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/** 仅基于渲染计划输出的几何信息进行命中测试。 */
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

    fun lineTracker(points: List<RenderedLinePoint>, x: Float): ChartTracker<ChartPoint>? {
        val nearest = points.minByOrNull { point -> abs(point.x - x) } ?: return null
        val selections = points.asSequence()
            .filter { point -> abs(point.x - nearest.x) <= 0.5f }
            .map { point -> point.selection }
            .sortedWith(compareBy<ChartSelection<ChartPoint>> { it.seriesIndex }.thenBy { it.itemIndex })
            .toList()
        return ChartTracker(nearest.selection.item.x, selections)
    }

    fun lineSlotWidth(points: List<RenderedLinePoint>): Float? {
        val slots = points.map { it.x }.sorted().fold(mutableListOf<Float>()) { unique, value ->
            if (unique.isEmpty() || abs(value - unique.last()) > 0.5f) unique += value
            unique
        }
        if (slots.size < 2) return null
        return slots.zipWithNext().map { (left, right) -> right - left }.average().toFloat().takeIf { it > 0f }
    }

    fun bar(bars: List<RenderedBar>, x: Float, y: Float): ChartSelection<BarEntry>? =
        bars.firstOrNull { it.bounds.contains(x, y, extra = 6f) }?.selection

    fun pie(slices: List<RenderedPieSlice>, x: Float, y: Float): ChartSelection<PieEntry>? {
        val fullCircle = (PI * 2.0).toFloat()
        return slices.firstOrNull { slice ->
            val dx = x - slice.centerX
            val dy = y - slice.centerY
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared < slice.innerRadius * slice.innerRadius ||
                distanceSquared > slice.outerRadius * slice.outerRadius
            ) {
                false
            } else {
                val angle = atan2(dy, dx)
                val relative = ((angle - slice.startAngle) % fullCircle + fullCircle) % fullCircle
                relative <= slice.endAngle - slice.startAngle
            }
        }?.selection
    }

    fun mixed(
        linePoints: List<RenderedMixedLinePoint>,
        bars: List<RenderedMixedBar>,
        x: Float,
        y: Float,
        lineRadius: Float = 24f,
    ): MixedChartSelection? {
        var nearestLine: RenderedMixedLinePoint? = null
        var nearestDistanceSquared = lineRadius * lineRadius
        linePoints.forEach { point ->
            val dx = point.x - x
            val dy = point.y - y
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared <= nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared
                nearestLine = point
            }
        }
        return nearestLine?.selection ?: bars.firstOrNull { it.bounds.contains(x, y, extra = 6f) }?.selection
    }
}
