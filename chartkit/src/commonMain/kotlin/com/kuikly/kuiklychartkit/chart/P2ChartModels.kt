package com.kuikly.kuiklychartkit.chart

import com.tencent.kuikly.core.base.Color

/** A labeled cell value rendered by [HeatmapChart]. */
data class HeatmapEntry(
    /** Horizontal category displayed below the heatmap grid. */
    val xLabel: String,
    /** Vertical category displayed beside the heatmap grid. */
    val yLabel: String,
    /** Finite numeric magnitude used to select a color-scale bucket. */
    val value: Float,
    /** Optional cell color that overrides the configured or built-in color scale. */
    val color: Color? = null,
)

/** A labeled non-negative metric rendered on one [RadarChart] axis. */
data class RadarEntry(
    /** Dimension label shared by every non-empty radar series. */
    val label: String,
    /** Metric magnitude. Negative and non-finite values are treated as gaps. */
    val value: Float,
)

/** One temporary radar-axis value emitted while the optional long-press tracker is active. */
data class RadarTracker(
    /** The original series item whose radial position is being previewed. */
    val selection: ChartSelection<RadarEntry>,
    /** The finite, clamped value currently projected onto that item's radial axis. */
    val previewValue: Float,
)

/** Visual options configured from a [HeatmapChartAttr.heatmap] block. */
class HeatmapOptions {
    /** Spacing between cells in logical pixels. */
    var cellGap: Float = 3f

    /** Whether values are drawn inside cells when the available space permits it. */
    var showValueLabels: Boolean = false

    /**
     * Optional ordered color scale. Empty uses GitHub-style green intensity
     * buckets, with a dark-canvas variant for [ChartTheme.dark]. Colors are
     * selected as discrete buckets from the normalized cell value.
     */
    var colorScale: List<Color> = emptyList()
}

/** Visual options configured from a [RadarChartAttr.radar] block. */
class RadarOptions {
    /** Number of concentric polygon grid rings. */
    var gridCount: Int = 5

    /** Whether visible metric vertices are marked with circles. */
    var showPoints: Boolean = true

    /** Whether values are drawn beside vertices when there is enough room. */
    var showValueLabels: Boolean = false

    /** Stroke width for every radar series in logical pixels. */
    var lineWidth: Float = 2f

    /**
     * Enables long-press tracking for radar vertices. Dragging projects the
     * active point onto its radial axis and previews the matching polygon area.
     * Disabled by default.
     */
    var trackerEnabled: Boolean = false

    /**
     * Optional per-series fill colors. Empty keeps radar polygons unfilled.
     * Supply translucent ARGB colors when comparing more than one series.
     */
    var fillColors: List<Color> = emptyList()
}
