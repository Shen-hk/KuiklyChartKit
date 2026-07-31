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

    /** Adds existing point snapshots while keeping their caller-owned X coordinates. */
    fun points(value: Iterable<ChartPoint>) {
        points += value
    }

    /** Adds existing point snapshots while keeping their caller-owned X coordinates. */
    fun points(vararg value: ChartPoint) {
        points += value
    }

    /** Adds label/value pairs with automatically assigned, zero-based X coordinates. */
    fun points(labels: List<String>, values: List<Float>) {
        require(labels.size == values.size) { "PointSeriesScope.labels and values must have the same size" }
        labels.zip(values).forEach { (label, value) -> point(label, value) }
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

    /** Adds a named series from an existing immutable point snapshot. */
    fun series(name: String, items: Iterable<ChartPoint>, color: Color? = null) {
        series += ChartSeries(name, items.toList(), color)
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

    /** Adds existing category/value entries. */
    fun items(value: Iterable<BarEntry>) {
        entries += value
    }

    /** Adds existing category/value entries. */
    fun items(vararg value: BarEntry) {
        entries += value
    }

    /** Adds category labels and numeric values in matching order. */
    fun items(labels: List<String>, values: List<Float>) {
        require(labels.size == values.size) { "BarSeriesScope.labels and values must have the same size" }
        labels.zip(values).forEach { (label, value) -> item(label, value) }
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

    /** Adds a named categorical series from an existing immutable entry snapshot. */
    fun series(name: String, items: Iterable<BarEntry>, color: Color? = null) {
        series += ChartSeries(name, items.toList(), color)
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

    /** Adds existing pie or donut slices. */
    fun slices(value: Iterable<PieEntry>) {
        entries += value
    }

    /** Adds existing pie or donut slices. */
    fun slices(vararg value: PieEntry) {
        entries += value
    }

    /** Adds slice labels and numeric values in matching order. */
    fun slices(labels: List<String>, values: List<Float>) {
        require(labels.size == values.size) { "PieDataScope.labels and values must have the same size" }
        labels.zip(values).forEach { (label, value) -> slice(label, value) }
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

    /** Adds existing heatmap cell snapshots. */
    fun cells(value: Iterable<HeatmapEntry>) {
        entries += value
    }

    /** Adds existing heatmap cell snapshots. */
    fun cells(vararg value: HeatmapEntry) {
        entries += value
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

    /** Adds existing radar metric snapshots. */
    fun metrics(value: Iterable<RadarEntry>) {
        entries += value
    }

    /** Adds existing radar metric snapshots. */
    fun metrics(vararg value: RadarEntry) {
        entries += value
    }

    /** Adds metric labels and numeric values in matching order. */
    fun metrics(labels: List<String>, values: List<Float>) {
        require(labels.size == values.size) { "RadarSeriesScope.labels and values must have the same size" }
        labels.zip(values).forEach { (label, value) -> metric(label, value) }
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

    /** Adds a named radar series from an existing immutable metric snapshot. */
    fun series(name: String, items: Iterable<RadarEntry>, color: Color? = null) {
        series += ChartSeries(name, items.toList(), color)
    }

    internal fun build(): List<ChartSeries<RadarEntry>> = series.toList()
}
