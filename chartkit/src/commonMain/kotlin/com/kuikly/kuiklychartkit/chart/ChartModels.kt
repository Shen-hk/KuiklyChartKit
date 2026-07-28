package com.kuikly.kuiklychartkit.chart

import com.tencent.kuikly.core.base.Color
import kotlin.math.abs
import kotlin.math.round

/**
 * A numeric point in a line series.
 *
 * Points are connected in input order. [label] is used for the X-axis and
 * Tooltip when it is not blank. Non-finite coordinates are treated as gaps.
 */
data class ChartPoint(
    /** Numeric horizontal coordinate used for scaling and ordering. */
    val x: Float,
    /** Numeric vertical coordinate. */
    val y: Float,
    /** Optional human-readable X-axis label. */
    val label: String = "",
)

/** A category and finite numeric value rendered by a bar chart. */
data class BarEntry(
    /** Category label displayed on the X-axis and in the Tooltip. */
    val label: String,
    /** Bar value. Positive and negative values are drawn around a zero baseline. */
    val value: Float,
)

/** A labeled non-negative value rendered as one pie or donut slice. */
data class PieEntry(
    /** Slice label displayed in the legend and selection feedback. */
    val label: String,
    /** Slice magnitude. Only finite values greater than zero are rendered. */
    val value: Float,
    /** Optional slice color; null uses the corresponding theme palette color. */
    val color: Color? = null,
)

/**
 * A named immutable data series.
 *
 * A null [color] uses the corresponding palette color from the active
 * [ChartTheme]. The original item indexes are preserved in selection callbacks.
 */
data class ChartSeries<T>(
    /** Non-blank name displayed in the legend and selection result. */
    val name: String,
    /** Input snapshot. Mutate data by supplying a new list to the chart DSL. */
    val items: List<T>,
    /** Optional series color; null delegates color selection to the theme. */
    val color: Color? = null,
)

/** A click hit resolved back to the original series and input item. */
data class ChartSelection<T>(
    /** Zero-based index in the `data(...)` series arguments. */
    val seriesIndex: Int,
    /** Zero-based index in the original series item list. */
    val itemIndex: Int,
    /** Name copied from the selected series. */
    val seriesName: String,
    /** Original caller-owned immutable item. */
    val item: T,
)

/** Identifies which renderer produced a [MixedChartSelection]. */
enum class MixedSeriesType {
    /** A grouped categorical bar series. */
    BAR,
    /** A categorical line series drawn above the bars. */
    LINE,
}

/**
 * A selection from a categorical mixed chart.
 *
 * [seriesIndex] is relative to the corresponding bar or line series list,
 * while [itemIndex] always points to the original item in that series.
 */
data class MixedChartSelection(
    /** Renderer kind that produced this selection. */
    val seriesType: MixedSeriesType,
    /** Zero-based index inside the matching bar or line series list. */
    val seriesIndex: Int,
    /** Zero-based index in the original series item list. */
    val itemIndex: Int,
    /** Name copied from the selected series. */
    val seriesName: String,
    /** Original caller-owned categorical item. */
    val item: BarEntry,
)

/**
 * A set of selections sharing one logical X value during an opt-in tracker gesture.
 *
 * [x] is a data coordinate, never a platform-specific screen coordinate. The
 * selections preserve the caller's original series and item indexes.
 */
data class ChartTracker<T>(
    val x: Float,
    val selections: List<ChartSelection<T>>,
) {
    init {
        require(selections.isNotEmpty()) { "ChartTracker.selections must not be empty" }
    }
}

/** A bounded, inclusive item range rendered by an opt-in chart viewport. */
data class ChartViewport(
    val startIndex: Int,
    val endIndex: Int,
) {
    init {
        require(startIndex >= 0) { "ChartViewport.startIndex must be >= 0" }
        require(endIndex >= startIndex) { "ChartViewport.endIndex must be >= startIndex" }
    }

    val itemCount: Int get() = endIndex - startIndex + 1
}

/** Bar grouping policy reserved by the public M0 DSL. */
enum class BarMode {
    /** Accepts at most one series and rejects ambiguous multi-series input. */
    SINGLE,
    /** One or more series laid out side by side per category; the default. */
    GROUPED,
}

/**
 * Cross-platform visual tokens used by every ChartKit renderer.
 *
 * Supplying an empty [palette] is rejected immediately because all series need
 * a deterministic fallback color.
 */
data class ChartTheme(
    /** Canvas fill color. */
    val backgroundColor: Color,
    /** Cartesian axis and baseline color. */
    val axisColor: Color,
    /** Grid line color. */
    val gridColor: Color,
    /** Axis, legend, value-label and empty-state text color. */
    val labelColor: Color,
    /** Tooltip panel fill color. */
    val tooltipBackgroundColor: Color,
    /** Tooltip label color. */
    val tooltipTextColor: Color,
    /** Selection guide and marker color. */
    val selectionColor: Color,
    /** Non-empty fallback series palette. */
    val palette: List<Color>,
) {
    init {
        require(palette.isNotEmpty()) { "ChartTheme.palette must not be empty" }
    }

    companion object {
        /** Neutral light theme suitable for light application surfaces. */
        fun light(): ChartTheme = ChartTheme(
            backgroundColor = Color(0xFFFFFFFF),
            axisColor = Color(0xFF9AA4B2),
            gridColor = Color(0xFFE8EDF3),
            labelColor = Color(0xFF5D6878),
            tooltipBackgroundColor = Color(0xE6232935),
            tooltipTextColor = Color(0xFFFFFFFF),
            selectionColor = Color(0xFF2563EB),
            palette = defaultPalette(),
        )

        /** High-contrast dark theme suitable for dark application surfaces. */
        fun dark(): ChartTheme = ChartTheme(
            backgroundColor = Color(0xFF111827),
            axisColor = Color(0xFF697586),
            gridColor = Color(0xFF2B3545),
            labelColor = Color(0xFFD4D9E2),
            tooltipBackgroundColor = Color(0xF2F8FAFC),
            tooltipTextColor = Color(0xFF172033),
            selectionColor = Color(0xFF60A5FA),
            palette = listOf(
                Color(0xFF60A5FA),
                Color(0xFF2DD4BF),
                Color(0xFFFBBF24),
                Color(0xFFC084FC),
                Color(0xFF4ADE80),
                Color(0xFFFB7185),
            ),
        )

        /** Light theme with a blue/cyan/teal analytical palette. */
        fun ocean(): ChartTheme = light().copy(
            selectionColor = Color(0xFF0284C7),
            palette = listOf(
                Color(0xFF0284C7),
                Color(0xFF06B6D4),
                Color(0xFF14B8A6),
                Color(0xFF6366F1),
                Color(0xFF0EA5E9),
                Color(0xFF22C55E),
            ),
        )

        /** Light theme with orange/red/pink emphasis colors. */
        fun sunset(): ChartTheme = light().copy(
            selectionColor = Color(0xFFF97316),
            palette = listOf(
                Color(0xFFF97316),
                Color(0xFFEF4444),
                Color(0xFFEAB308),
                Color(0xFFEC4899),
                Color(0xFFA855F7),
                Color(0xFFFB7185),
            ),
        )

        private fun defaultPalette(): List<Color> = listOf(
            Color(0xFF2563EB),
            Color(0xFF0D9488),
            Color(0xFFF59E0B),
            Color(0xFF7C3AED),
            Color(0xFF16A34A),
            Color(0xFFE11D48),
        )
    }
}

/** Shared X/Y-axis options configured from an `xAxis {}` or `yAxis {}` block. */
class AxisOptions {
    /** Whether the axis and its labels are rendered. */
    var visible: Boolean = true
    /** Requested Y-axis tick count; ChartKit validates the supported range 2..10. */
    var tickCount: Int = 5
    /** Whether zero must be included when calculating the numeric domain. */
    var includeZero: Boolean = false
    /** Optional deterministic numeric label formatter. */
    var labelFormatter: ((Float) -> String)? = null

    /** Formats [value] with [labelFormatter] or ChartKit's compact default. */
    fun format(value: Float): String = labelFormatter?.invoke(value) ?: formatChartValue(value)
}

/** Cartesian grid options. */
class GridOptions {
    /** Whether horizontal grid lines are rendered. */
    var visible: Boolean = true
    /** Grid stroke width in logical pixels. */
    var lineWidth: Float = 1f
}

/** Legend options shared by line and bar charts. */
class LegendOptions {
    /** Whether the series legend is rendered. */
    var visible: Boolean = true
}

/** Click Tooltip options and opt-in tracker settings. */
class TooltipOptions {
    /** Whether selection guides and Tooltip content are shown after a hit. */
    var enabled: Boolean = true
    /** Enables the long-press tracker for line and area charts. Disabled by default. */
    var trackerEnabled: Boolean = false
    /** Keeps the final tracker position visible after the long press ends. */
    var keepTrackerOnRelease: Boolean = false
    /** Optional formatter for the selected numeric value. */
    var valueFormatter: ((Float) -> String)? = null

    /** Formats [value] with [valueFormatter] or ChartKit's compact default. */
    fun format(value: Float): String = valueFormatter?.invoke(value) ?: formatChartValue(value)
}

/** Opt-in dense Cartesian browsing options. A zero [visibleItemCount] means all items are visible. */
class InteractionOptions {
    /** Enables horizontal panning when the visible window is smaller than the data set. */
    var enablePan: Boolean = false
    /** Requested number of items in the line-chart window; zero keeps every item visible. */
    var visibleItemCount: Int = 0
    /**
     * Maximum rendered points per visible series after min/max sampling.
     *
     * Zero selects an automatic budget based on the plot width.
     */
    var maxRenderPointCount: Int = 0
}

/** Line renderer appearance options. */
class LineOptions {
    /** Uses cubic curves between points when true; otherwise straight segments. */
    var smooth: Boolean = false
    /** Whether circular point markers are rendered. */
    var showPoints: Boolean = true
    /** Line stroke width in logical pixels. */
    var lineWidth: Float = 2f
    /** Point marker radius in logical pixels. */
    var pointRadius: Float = 3.5f
}

/** Area fill options used by [AreaChartAttr]. */
class AreaOptions {
    /**
     * Fill colors selected by series index.
     *
     * ARGB colors with a translucent alpha are recommended so grid lines and
     * overlapping series remain visible. When there are more series than
     * colors, the list is reused cyclically.
     */
    var fillColors: List<Color> = listOf(Color(0x332563EB))
}

/** Polar geometry and label options used by [PieChartAttr]. */
class PieOptions {
    /** `0` renders a pie; values greater than zero render a donut. */
    var innerRadiusRatio: Float = 0.58f
    /** Clockwise start angle in degrees; -90 starts at twelve o'clock. */
    var startAngleDegrees: Float = -90f
    /** Visual gap between adjacent slices in degrees. */
    var gapAngleDegrees: Float = 1.5f
    /** Whether sufficiently large slices display percentage labels. */
    var showValueLabels: Boolean = true
    /** Optional text shown in the center of a donut. */
    var centerLabel: String = ""
}

/** Bar renderer appearance options. */
class BarOptions {
    /** Single- or multi-series category layout policy; defaults to [BarMode.GROUPED]. */
    var mode: BarMode = BarMode.GROUPED
    /** Fraction of each category slot available to its bars, in `(0, 1]`. */
    var barWidthRatio: Float = 0.68f
    /** Whether finite values are drawn near each bar. */
    /** Shows the selected bar's value label; all values stay hidden by default. */
    var showValueLabels: Boolean = false
    /** Requested bar corner radius in logical pixels. */
    var cornerRadius: Float = 4f
}

internal fun formatChartValue(value: Float): String {
    if (!value.isFinite()) return "--"
    val absolute = abs(value)
    val divisor: Float
    val suffix: String
    when {
        absolute >= 1_000_000_000f -> {
            divisor = 1_000_000_000f
            suffix = "B"
        }
        absolute >= 1_000_000f -> {
            divisor = 1_000_000f
            suffix = "M"
        }
        absolute >= 1_000f -> {
            divisor = 1_000f
            suffix = "K"
        }
        else -> {
            divisor = 1f
            suffix = ""
        }
    }
    val scaled = value / divisor
    val rounded = round(scaled * 100f) / 100f
    val text = if (abs(rounded - rounded.toInt()) < 0.0001f) {
        rounded.toInt().toString()
    } else {
        rounded.toString().trimEnd('0').trimEnd('.')
    }
    return text + suffix
}
