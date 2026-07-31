package com.kuikly.kuiklychartkit.chart

import com.tencent.kuikly.core.base.Color

/** Marks the compact chart-data builders so nested DSL receivers cannot be mixed accidentally. */
@DslMarker
annotation class ChartDataDsl

/** Builds the named point series consumed by line, area and sparkline charts. */
@ChartDataDsl
class PointSeriesScope internal constructor() {
    private val points = mutableListOf<ChartPoint>()

    /** Adds a point with an automatically assigned, zero-based X coordinate. */
    fun point(label: String, value: Float) {
        points += ChartPoint(x = points.size.toFloat(), y = value, label = label)
    }

    /** Adds a point when the numerical X coordinate is meaningful to the caller. */
    fun point(x: Float, value: Float, label: String = "") {
        points += ChartPoint(x = x, y = value, label = label)
    }

    internal fun build(): List<ChartPoint> = points.toList()
}

/** Builds one or more line-compatible series without requiring callers to construct [ChartSeries]. */
@ChartDataDsl
class PointDataScope internal constructor() {
    private val series = mutableListOf<ChartSeries<ChartPoint>>()

    /** Adds a named series. Use [PointSeriesScope.point] for each item. */
    fun series(name: String, color: Color? = null, block: PointSeriesScope.() -> Unit) {
        series += ChartSeries(name, PointSeriesScope().apply(block).build(), color)
    }

    internal fun build(): List<ChartSeries<ChartPoint>> = series.toList()
}

/** Builds the named categorical series consumed by bar and mixed charts. */
@ChartDataDsl
class BarSeriesScope internal constructor() {
    private val entries = mutableListOf<BarEntry>()

    /** Adds one category/value pair. */
    fun item(label: String, value: Float) {
        entries += BarEntry(label, value)
    }

    internal fun build(): List<BarEntry> = entries.toList()
}

/** Builds one or more categorical bar series. */
@ChartDataDsl
class BarDataScope internal constructor() {
    private val series = mutableListOf<ChartSeries<BarEntry>>()

    /** Adds a named categorical series. */
    fun series(name: String, color: Color? = null, block: BarSeriesScope.() -> Unit) {
        series += ChartSeries(name, BarSeriesScope().apply(block).build(), color)
    }

    internal fun build(): List<ChartSeries<BarEntry>> = series.toList()
}

/** Builds pie or donut slices. */
@ChartDataDsl
class PieDataScope internal constructor() {
    private val entries = mutableListOf<PieEntry>()

    /** Adds one labeled slice. */
    fun slice(label: String, value: Float, color: Color? = null) {
        entries += PieEntry(label, value, color)
    }

    internal fun build(): List<PieEntry> = entries.toList()
}

/** Builds heatmap cells. */
@ChartDataDsl
class HeatmapDataScope internal constructor() {
    private val entries = mutableListOf<HeatmapEntry>()

    /** Adds one cell at the intersection of the two category labels. */
    fun cell(xLabel: String, yLabel: String, value: Float, color: Color? = null) {
        entries += HeatmapEntry(xLabel, yLabel, value, color)
    }

    internal fun build(): List<HeatmapEntry> = entries.toList()
}

/** Builds a named radar series. */
@ChartDataDsl
class RadarSeriesScope internal constructor() {
    private val entries = mutableListOf<RadarEntry>()

    /** Adds one radial dimension. */
    fun metric(label: String, value: Float) {
        entries += RadarEntry(label, value)
    }

    internal fun build(): List<RadarEntry> = entries.toList()
}

/** Builds one or more radar series. */
@ChartDataDsl
class RadarDataScope internal constructor() {
    private val series = mutableListOf<ChartSeries<RadarEntry>>()

    /** Adds a named radar series. */
    fun series(name: String, color: Color? = null, block: RadarSeriesScope.() -> Unit) {
        series += ChartSeries(name, RadarSeriesScope().apply(block).build(), color)
    }

    internal fun build(): List<ChartSeries<RadarEntry>> = series.toList()
}
