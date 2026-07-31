package com.kuikly.kuiklychartkit.chart.interaction

import com.kuikly.kuiklychartkit.chart.ChartBrushSelection
import com.kuikly.kuiklychartkit.chart.ChartRect
import com.kuikly.kuiklychartkit.chart.RenderedLinePoint

/** Render-plan geometry consumed by the platform-neutral brush state machine. */
internal data class ChartBrushContext(
    val plot: ChartRect?,
    val renderedPoints: List<RenderedLinePoint>,
)

/**
 * Pure Kotlin state machine for an opt-in Cartesian range brush.
 *
 * The host owns display and callbacks; this controller only translates plot
 * coordinates into original inclusive item indexes using the RenderPlan hits.
 */
internal class ChartBrushController {
    private var startIndex: Int? = null

    val isActive: Boolean get() = startIndex != null

    fun begin(x: Float, y: Float, context: ChartBrushContext): ChartBrushSelection? {
        val plot = context.plot ?: return null
        if (!x.isFinite() || !y.isFinite() || !plot.contains(x, y)) return null
        val index = nearestIndex(context.renderedPoints, x) ?: return null
        startIndex = index
        return selection(index, index)
    }

    fun move(x: Float, y: Float, context: ChartBrushContext): ChartBrushSelection? {
        val start = startIndex ?: return null
        val plot = context.plot ?: return null
        if (!x.isFinite() || !y.isFinite()) return null
        val index = nearestIndex(context.renderedPoints, x.coerceIn(plot.left, plot.right)) ?: return null
        return selection(start, index)
    }

    fun end() {
        startIndex = null
    }

    fun cancel() {
        startIndex = null
    }

    private fun nearestIndex(points: List<RenderedLinePoint>, x: Float): Int? =
        ChartHitTest.lineTracker(points, x)?.selections?.minOfOrNull { it.itemIndex }

    private fun selection(first: Int, second: Int): ChartBrushSelection =
        ChartBrushSelection(minOf(first, second), maxOf(first, second))
}
