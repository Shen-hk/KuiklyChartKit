package com.kuikly.kuiklychartkit.chart

/** Immutable mixed-chart input used by the shared data-transition controller. */
internal data class MixedChartData(
    val barSeries: List<ChartSeries<BarEntry>>,
    val lineSeries: List<ChartSeries<BarEntry>>,
)

/** Safe value interpolation for non-line chart snapshots. */
internal object ChartDataTransition {
    fun barsCompatible(from: List<ChartSeries<BarEntry>>, to: List<ChartSeries<BarEntry>>): Boolean =
        compatibleSeries(from, to) { old, new ->
            old.label == new.label && old.value.isFinite() && new.value.isFinite()
        }

    fun interpolateBars(
        from: List<ChartSeries<BarEntry>>,
        to: List<ChartSeries<BarEntry>>,
        progress: Float,
    ): List<ChartSeries<BarEntry>> {
        if (!barsCompatible(from, to)) return to
        return interpolateSeries(from, to, progress) { old, new, fraction ->
            new.copy(value = interpolateFinite(old.value, new.value, fraction))
        }
    }

    fun pieCompatible(from: List<PieEntry>, to: List<PieEntry>): Boolean =
        from.size == to.size && from.indices.all { index ->
            val old = from[index]
            val new = to[index]
            old.label == new.label && old.value.isFinite() && new.value.isFinite() && old.value > 0f && new.value > 0f
        }

    fun interpolatePie(from: List<PieEntry>, to: List<PieEntry>, progress: Float): List<PieEntry> {
        if (!pieCompatible(from, to)) return to
        val fraction = progress.coerceIn(0f, 1f)
        return to.mapIndexed { index, entry ->
            entry.copy(value = interpolateFinite(from[index].value, entry.value, fraction))
        }
    }

    fun heatmapCompatible(from: List<HeatmapEntry>, to: List<HeatmapEntry>): Boolean =
        from.size == to.size && from.indices.all { index ->
            val old = from[index]
            val new = to[index]
            old.xLabel == new.xLabel && old.yLabel == new.yLabel && old.value.isFinite() && new.value.isFinite()
        }

    fun interpolateHeatmap(from: List<HeatmapEntry>, to: List<HeatmapEntry>, progress: Float): List<HeatmapEntry> {
        if (!heatmapCompatible(from, to)) return to
        val fraction = progress.coerceIn(0f, 1f)
        return to.mapIndexed { index, entry ->
            entry.copy(value = interpolateFinite(from[index].value, entry.value, fraction))
        }
    }

    fun radarCompatible(from: List<ChartSeries<RadarEntry>>, to: List<ChartSeries<RadarEntry>>): Boolean =
        compatibleSeries(from, to) { old, new ->
            old.label == new.label && old.value.isFinite() && new.value.isFinite() && old.value >= 0f && new.value >= 0f
        }

    fun interpolateRadar(
        from: List<ChartSeries<RadarEntry>>,
        to: List<ChartSeries<RadarEntry>>,
        progress: Float,
    ): List<ChartSeries<RadarEntry>> {
        if (!radarCompatible(from, to)) return to
        return interpolateSeries(from, to, progress) { old, new, fraction ->
            new.copy(value = interpolateFinite(old.value, new.value, fraction))
        }
    }

    fun mixedCompatible(from: MixedChartData, to: MixedChartData): Boolean =
        barsCompatible(from.barSeries, to.barSeries) && barsCompatible(from.lineSeries, to.lineSeries)

    fun interpolateMixed(from: MixedChartData, to: MixedChartData, progress: Float): MixedChartData = MixedChartData(
        barSeries = interpolateBars(from.barSeries, to.barSeries, progress),
        lineSeries = interpolateBars(from.lineSeries, to.lineSeries, progress),
    )

    private fun <T> compatibleSeries(
        from: List<ChartSeries<T>>,
        to: List<ChartSeries<T>>,
        itemsCompatible: (T, T) -> Boolean,
    ): Boolean = from.size == to.size && from.indices.all { seriesIndex ->
        val oldSeries = from[seriesIndex]
        val newSeries = to[seriesIndex]
        oldSeries.name == newSeries.name &&
            oldSeries.items.size == newSeries.items.size &&
            oldSeries.items.indices.all { itemIndex -> itemsCompatible(oldSeries.items[itemIndex], newSeries.items[itemIndex]) }
    }

    private fun <T> interpolateSeries(
        from: List<ChartSeries<T>>,
        to: List<ChartSeries<T>>,
        progress: Float,
        interpolateItem: (T, T, Float) -> T,
    ): List<ChartSeries<T>> {
        if (!compatibleSeries(from, to) { _, _ -> true }) return to
        val fraction = progress.coerceIn(0f, 1f)
        return to.mapIndexed { seriesIndex, targetSeries ->
            val sourceSeries = from[seriesIndex]
            targetSeries.copy(
                items = targetSeries.items.mapIndexed { itemIndex, targetItem ->
                    interpolateItem(sourceSeries.items[itemIndex], targetItem, fraction)
                },
            )
        }
    }

    private fun interpolateFinite(from: Float, to: Float, fraction: Float): Float =
        (from.toDouble() + (to.toDouble() - from.toDouble()) * fraction.toDouble()).toFloat()
}
