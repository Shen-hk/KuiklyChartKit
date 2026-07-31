package com.kuikly.kuiklychartkit.chart.interaction

/** Reusable state holder for compatible immutable chart-data replacements. */
internal class ChartSnapshotTransition<T>(
    private val compatible: (T, T) -> Boolean,
    private val interpolate: (T, T, Float) -> T,
) {
    private var from: T? = null
    private var target: T? = null

    fun resolve(
        next: T,
        enabled: Boolean,
        progress: Float,
        onStart: () -> Unit,
        onCancel: () -> Unit,
    ): T {
        val previousTarget = target
        if (previousTarget == null) {
            target = next
            return next
        }
        if (previousTarget != next) {
            val current = from?.let { interpolate(it, previousTarget, progress) } ?: previousTarget
            onCancel()
            if (enabled && compatible(current, next)) {
                from = current
                target = next
                onStart()
                // The host resets progress in onStart; returning current avoids a one-frame jump.
                return current
            } else {
                from = null
                target = next
            }
        }
        return from?.let { interpolate(it, next, progress) } ?: next
    }

    fun finish() {
        from = null
    }

    fun clear() {
        from = null
        target = null
    }
}
