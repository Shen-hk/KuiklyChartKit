package com.kuikly.kuiklychartkit.chart.render

import com.kuikly.kuiklychartkit.chart.ChartPoint
import com.kuikly.kuiklychartkit.chart.ChartSeries
import com.kuikly.kuiklychartkit.chart.ChartViewport
import com.kuikly.kuiklychartkit.chart.LineChartAttr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LineRenderPlanTest {
    @Test
    fun freezesDslOptionsAndPreservesOriginalIndexesInTheRenderPlan() {
        val points = mutableListOf(
            ChartPoint(0f, 10f, "Mon"),
            ChartPoint(1f, Float.NaN, "Tue"),
            ChartPoint(2f, 30f, "Wed"),
        )
        val attr = LineChartAttr().apply {
            data(
                ChartSeries(
                    name = "Revenue",
                    items = points,
                ),
            )
            yAxis { tickCount = 4; includeZero = true }
            interaction { maxRenderPointCount = 4 }
        }

        val spec = attr.resolveRenderSpec()
        attr.yAxisOptions.tickCount = 8
        attr.interactionOptions.maxRenderPointCount = 20
        points[2] = ChartPoint(2f, 300f, "Wed")

        assertEquals(4, spec.style.yAxis.tickCount)
        assertEquals(4, spec.interaction.maxRenderPointCount)

        val plan = LineRenderPlanFactory.create(
            width = 320f,
            height = 180f,
            viewport = ChartViewport(0, 2),
            spec = spec,
            measureLabel = { it.length * 7f },
        )

        assertNotNull(plan)
        assertEquals(listOf(0, 2), plan.hitTargets.map { it.selection.itemIndex })
        assertEquals(30f, plan.hitTargets.last().selection.item.y)
        assertTrue(plan.layout.yScale.ticks.isNotEmpty())
    }
}
