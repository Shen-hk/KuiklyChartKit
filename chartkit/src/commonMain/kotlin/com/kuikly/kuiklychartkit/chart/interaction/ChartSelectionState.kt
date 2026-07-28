package com.kuikly.kuiklychartkit.chart.interaction

/**
 * 图表视图持有的不可变选中状态。
 *
 * 选中状态转换位于绘制器之外，使 Canvas 只负责渲染状态；后续键盘或无障碍
 * 选中也可以复用同一套转换逻辑。
 */
internal data class ChartSelectionState<T>(
    val selection: T? = null,
) {
    fun select(candidate: T?): ChartSelectionState<T> = copy(selection = candidate)

    fun clear(): ChartSelectionState<T> = if (selection == null) this else copy(selection = null)
}
