package com.kuikly.kuiklychartkit.chart.interaction

import com.kuikly.kuiklychartkit.chart.ChartViewport
import com.kuikly.kuiklychartkit.chart.LineViewportController
import com.kuikly.kuiklychartkit.chart.RenderedLinePoint
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A platform-neutral touch coordinate consumed by [ChartGestureController]. */
internal data class ChartGestureTouch(val x: Float, val y: Float)

/** Immutable chart state supplied by the component for one gesture event. */
internal data class ChartGestureContext(
    val itemCount: Int,
    val viewport: ChartViewport?,
    val resetViewport: ChartViewport?,
    val visibleItemCount: Int,
    val minVisibleItemCount: Int,
    val maxVisibleItemCount: Int,
    val enablePan: Boolean,
    val enableZoom: Boolean,
    val renderedPoints: List<RenderedLinePoint>,
)

/** One viewport update plus the fractional content offset used for smooth panning. */
internal data class ChartGestureUpdate(
    val viewport: ChartViewport,
    val contentOffsetItems: Float = 0f,
)

/**
 * Stateful, pure-Kotlin interpreter for Cartesian pan, pinch and reset gestures.
 *
 * It never reads a platform event or mutates a component. The host forwards a
 * [ChartGestureContext] and applies the returned viewport when it changes.
 */
internal class ChartGestureController {
    private var panStart: PanStart? = null
    private var pinchStart: PinchStart? = null

    fun onTouchStart(x: Float, touches: List<ChartGestureTouch>, context: ChartGestureContext): ChartGestureUpdate? {
        if (touches.size >= 2) beginPinch(touches, context)
        else if (context.enablePan) beginPan(x, context)
        return null
    }

    fun onTouchMove(x: Float, touches: List<ChartGestureTouch>, context: ChartGestureContext): ChartGestureUpdate? {
        return when {
            touches.size >= 2 -> {
                if (pinchStart == null) beginPinch(touches, context)
                updatePinch(touches, context)
            }
            pinchStart == null && context.enablePan -> updatePan(x, context)
            else -> null
        }
    }

    fun onTouchEnd(touchCount: Int) {
        if (touchCount < 2) pinchStart = null
        if (touchCount <= 1) panStart = null
    }

    fun onDoubleTap(context: ChartGestureContext): ChartGestureUpdate? {
        if (!context.enablePan && !context.enableZoom) return null
        return LineViewportController.resolve(
            itemCount = context.itemCount,
            requestedViewport = context.resetViewport,
            visibleItemCount = context.visibleItemCount,
        )?.let(::ChartGestureUpdate)
    }

    /** True when a released long-press tracker must be cleared by the host. */
    fun shouldClearTracker(keepTrackerOnRelease: Boolean): Boolean = !keepTrackerOnRelease

    fun clear() {
        panStart = null
        pinchStart = null
    }

    private fun beginPan(x: Float, context: ChartGestureContext) {
        if (!x.isFinite()) return
        val viewport = context.resolvedViewport() ?: return
        if (!LineViewportController.isPannable(context.itemCount, viewport)) return
        panStart = PanStart(x, viewport)
    }

    private fun updatePan(x: Float, context: ChartGestureContext): ChartGestureUpdate? {
        val start = panStart ?: return null
        if (!x.isFinite()) return null
        val slotWidth = ChartHitTest.lineSlotWidth(context.renderedPoints)
        if (slotWidth == null || !slotWidth.isFinite() || slotWidth <= 0f) return null
        val rawDelta = ((start.x - x) / slotWidth).takeIf { it.isFinite() } ?: return null
        val minDelta = -start.viewport.startIndex.toFloat()
        val maxDelta = (context.itemCount - start.viewport.itemCount - start.viewport.startIndex).toFloat()
        val clampedDelta = rawDelta.coerceIn(minDelta, maxDelta)
        val target = LineViewportController.pan(context.itemCount, start.viewport, clampedDelta.roundToInt())
        val appliedDelta = (target.startIndex - start.viewport.startIndex).toFloat()
        return ChartGestureUpdate(target, contentOffsetItems = appliedDelta - clampedDelta)
    }

    private fun beginPinch(touches: List<ChartGestureTouch>, context: ChartGestureContext) {
        if (!context.enableZoom || touches.size < 2) return
        val viewport = context.resolvedViewport() ?: return
        val distance = distance(touches[0], touches[1]) ?: return
        val focusX = (touches[0].x + touches[1].x) / 2f
        if (!focusX.isFinite()) return
        val focusIndex = context.renderedPoints.minByOrNull { point -> abs(point.x - focusX) }
            ?.selection
            ?.itemIndex
            ?: (viewport.startIndex + viewport.endIndex) / 2
        pinchStart = PinchStart(viewport, focusIndex, distance)
        panStart = null
    }

    private fun updatePinch(touches: List<ChartGestureTouch>, context: ChartGestureContext): ChartGestureUpdate? {
        val start = pinchStart ?: return null
        if (touches.size < 2) return null
        val currentDistance = distance(touches[0], touches[1]) ?: return null
        val scale = currentDistance / start.distance
        if (!scale.isFinite() || scale <= 0f) return null
        return ChartGestureUpdate(LineViewportController.zoom(
            itemCount = context.itemCount,
            viewport = start.viewport,
            focusIndex = start.focusIndex,
            scale = scale,
            minVisibleItemCount = context.minVisibleItemCount,
            maxVisibleItemCount = context.maxVisibleItemCount,
        ))
    }

    private fun ChartGestureContext.resolvedViewport(): ChartViewport? = LineViewportController.resolve(
        itemCount = itemCount,
        requestedViewport = viewport,
        visibleItemCount = visibleItemCount,
    )

    private fun distance(first: ChartGestureTouch, second: ChartGestureTouch): Float? {
        if (!first.x.isFinite() || !first.y.isFinite() || !second.x.isFinite() || !second.y.isFinite()) return null
        val dx = first.x - second.x
        val dy = first.y - second.y
        return sqrt(dx * dx + dy * dy).takeIf { it.isFinite() && it > 0f }
    }

    private data class PanStart(val x: Float, val viewport: ChartViewport)
    private data class PinchStart(val viewport: ChartViewport, val focusIndex: Int, val distance: Float)
}
