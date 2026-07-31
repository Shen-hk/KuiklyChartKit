package com.kuikly.kuiklychartkit.chart

import com.kuikly.kuiklychartkit.chart.interaction.ChartSnapshotTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChartSnapshotTransitionTest {
    @Test
    fun compatibleReplacementStartsAndRetargetsFromCurrentInterpolatedSnapshot() {
        val transition = ChartSnapshotTransition<Int>(
            compatible = { _, _ -> true },
            interpolate = { from, to, progress -> (from + (to - from) * progress).toInt() },
        )
        var started = 0
        var cancelled = 0

        assertEquals(10, transition.resolve(10, true, 1f, { started += 1 }, { cancelled += 1 }))
        assertEquals(10, transition.resolve(20, true, 1f, { started += 1 }, { cancelled += 1 }))
        assertEquals(15, transition.resolve(30, true, 0.5f, { started += 1 }, { cancelled += 1 }))

        assertEquals(2, started)
        assertEquals(2, cancelled)
    }

    @Test
    fun incompatibleReplacementUsesTheTargetImmediately() {
        val transition = ChartSnapshotTransition<Int>(compatible = { _, _ -> false }, interpolate = { _, to, _ -> to })
        var started = false

        transition.resolve(10, true, 1f, {}, {})
        assertEquals(20, transition.resolve(20, true, 1f, { started = true }, {}))

        assertFalse(started)
    }
}
