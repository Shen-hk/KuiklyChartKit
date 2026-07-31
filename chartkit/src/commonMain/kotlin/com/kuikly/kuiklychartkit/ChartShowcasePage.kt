package com.kuikly.kuiklychartkit

import com.kuikly.kuiklychartkit.base.BasePager
import com.kuikly.kuiklychartkit.chart.AreaChart
import com.kuikly.kuiklychartkit.chart.BarChart
import com.kuikly.kuiklychartkit.chart.BarEntry
import com.kuikly.kuiklychartkit.chart.ChartPoint
import com.kuikly.kuiklychartkit.chart.ChartSeries
import com.kuikly.kuiklychartkit.chart.ChartTheme
import com.kuikly.kuiklychartkit.chart.HeatmapChart
import com.kuikly.kuiklychartkit.chart.HeatmapEntry
import com.kuikly.kuiklychartkit.chart.LineChart
import com.kuikly.kuiklychartkit.chart.LineSampler
import com.kuikly.kuiklychartkit.chart.MixedChart
import com.kuikly.kuiklychartkit.chart.MixedSeriesType
import com.kuikly.kuiklychartkit.chart.PieChart
import com.kuikly.kuiklychartkit.chart.PieEntry
import com.kuikly.kuiklychartkit.chart.RadarChart
import com.kuikly.kuiklychartkit.chart.RadarEntry
import com.kuikly.kuiklychartkit.chart.SparklineChart
import com.kuikly.kuiklychartkit.chart.defaultHeatmapColorScale
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.Timer
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import kotlin.random.Random

private const val CHART_SHOWCASE_PAGE = "chart_showcase"

private enum class ShowcaseThemeMode {
    LIGHT,
    DARK,
    OCEAN,
    SUNSET,
}

private data class HeatmapShowcaseScenario(
    val seriesName: String,
    val summary: String,
    val entries: List<HeatmapEntry>,
) {
    val columnCount: Int get() = entries.map { it.xLabel }.distinct().size
    val rowCount: Int get() = entries.map { it.yLabel }.distinct().size
}

private fun buildHeatmapEntries(
    xLabels: List<String>,
    yLabels: List<String>,
    values: List<List<Float>>,
): List<HeatmapEntry> {
    require(values.size == yLabels.size) { "Heatmap values must provide one row per yLabel" }
    require(values.all { it.size == xLabels.size }) { "Heatmap values must provide one value per xLabel" }
    return yLabels.flatMapIndexed { rowIndex, yLabel ->
        xLabels.mapIndexed { columnIndex, xLabel ->
            HeatmapEntry(
                xLabel = xLabel,
                yLabel = yLabel,
                value = values[rowIndex][columnIndex],
            )
        }
    }
}

private val heatmapShowcaseScenarios = listOf(
    HeatmapShowcaseScenario(
        seriesName = "客服咨询",
        summary = "工作日分时咨询热度",
        entries = buildHeatmapEntries(
            xLabels = listOf("周一", "周二", "周三", "周四", "周五"),
            yLabels = listOf("09:00", "12:00", "15:00", "18:00"),
            values = listOf(
                listOf(42f, 58f, 35f, 74f, 66f),
                listOf(81f, 94f, 72f, 88f, 97f),
                listOf(63f, 76f, 69f, 83f, 91f),
                listOf(39f, 52f, 47f, 61f, 56f),
            ),
        ),
    ),
    HeatmapShowcaseScenario(
        seriesName = "活动转化",
        summary = "四个渠道的漏斗表现",
        entries = buildHeatmapEntries(
            xLabels = listOf("搜索", "推荐", "直播", "社群"),
            yLabels = listOf("曝光", "点击", "加购"),
            values = listOf(
                listOf(98f, 86f, 91f, 74f),
                listOf(72f, 68f, 83f, 59f),
                listOf(41f, 35f, 57f, 28f),
            ),
        ),
    ),
    HeatmapShowcaseScenario(
        seriesName = "仓配履约",
        summary = "六仓五环节的负载变化",
        entries = buildHeatmapEntries(
            xLabels = listOf("华北仓", "华东仓", "华南仓", "西南仓", "中转仓", "前置仓"),
            yLabels = listOf("入库", "拣货", "打包", "出库", "签收"),
            values = listOf(
                listOf(44f, 63f, 58f, 47f, 51f, 39f),
                listOf(72f, 88f, 83f, 69f, 76f, 64f),
                listOf(61f, 79f, 75f, 66f, 70f, 57f),
                listOf(85f, 93f, 89f, 78f, 82f, 74f),
                listOf(52f, 67f, 64f, 55f, 58f, 49f),
            ),
        ),
    ),
)

private fun buildPerformancePoints(pointCount: Int): List<ChartPoint> = List(pointCount) { index ->
    ChartPoint(
        x = index.toFloat(),
        y = 180f + ((index % 37) - 18) * 0.8f + index * 0.01f,
        label = "#${index + 1}",
    )
}

private fun initialTrendData(): List<ChartSeries<ChartPoint>> = listOf(
    ChartSeries(
        name = "本周",
        items = listOf(
            ChartPoint(1f, 128f, "7/18"), ChartPoint(2f, 166f, "7/19"),
            ChartPoint(3f, 151f, "7/20"), ChartPoint(4f, 207f, "7/21"),
            ChartPoint(5f, 232f, "7/22"), ChartPoint(6f, 218f, "7/23"),
            ChartPoint(7f, 276f, "7/24"), ChartPoint(8f, 245f, "7/25"),
            ChartPoint(9f, 294f, "7/26"), ChartPoint(10f, 318f, "今天"),
        ),
    ),
    ChartSeries(
        name = "上周",
        items = listOf(
            ChartPoint(1f, 105f, "7/18"), ChartPoint(2f, 134f, "7/19"),
            ChartPoint(3f, 146f, "7/20"), ChartPoint(4f, 171f, "7/21"),
            ChartPoint(5f, 196f, "7/22"), ChartPoint(6f, 189f, "7/23"),
            ChartPoint(7f, 221f, "7/24"), ChartPoint(8f, 214f, "7/25"),
            ChartPoint(9f, 242f, "7/26"), ChartPoint(10f, 257f, "今天"),
        ),
    ),
)

private fun initialCategoryData(): List<ChartSeries<BarEntry>> = listOf(
    ChartSeries(
        name = "本月",
        items = listOf(
            BarEntry("搜索", 186f), BarEntry("推荐", 248f), BarEntry("活动", 164f),
            BarEntry("直播", 292f), BarEntry("社交", 221f),
        ),
    ),
    ChartSeries(
        name = "上月",
        items = listOf(
            BarEntry("搜索", 159f), BarEntry("推荐", 214f), BarEntry("活动", 181f),
            BarEntry("直播", 238f), BarEntry("社交", 196f),
        ),
    ),
)

private fun alternateTrendData(): List<ChartSeries<ChartPoint>> {
    val labels = listOf("7/28", "7/29", "7/30", "7/31", "8/01", "8/02", "8/03", "8/04", "8/05", "今天")
    return initialTrendData().mapIndexed { seriesIndex, series ->
        series.copy(items = series.items.mapIndexed { index, point ->
            point.copy(
                y = (point.y * if (seriesIndex == 0) 0.9f else 1.08f) + index * 3f,
                label = labels[index],
            )
        })
    }
}

private fun alternateCategoryData(): List<ChartSeries<BarEntry>> {
    val labels = listOf("短视频", "私域", "广告", "联盟", "线下")
    return initialCategoryData().mapIndexed { seriesIndex, series ->
        series.copy(items = series.items.mapIndexed { index, entry ->
            entry.copy(
                label = labels[index],
                value = entry.value * if (seriesIndex == 0) 0.92f else 1.07f,
            )
        })
    }
}

private fun initialRadarData(): List<ChartSeries<RadarEntry>> = listOf(
    ChartSeries(
        "当前表现",
        listOf(
            RadarEntry("响应", 86f), RadarEntry("解决", 72f), RadarEntry("满意度", 91f),
            RadarEntry("覆盖", 68f), RadarEntry("成本", 76f),
        ),
    ),
    ChartSeries(
        "目标水平",
        listOf(
            RadarEntry("响应", 80f), RadarEntry("解决", 82f), RadarEntry("满意度", 88f),
            RadarEntry("覆盖", 84f), RadarEntry("成本", 72f),
        ),
    ),
)

private fun initialAreaData(): List<ChartSeries<ChartPoint>> = listOf(
    ChartSeries(
        name = "成交金额",
        items = listOf(
            ChartPoint(1f, 82f, "7/20"), ChartPoint(2f, 108f, "7/21"),
            ChartPoint(3f, 96f, "7/22"), ChartPoint(4f, 142f, "7/23"),
            ChartPoint(5f, 168f, "7/24"), ChartPoint(6f, 154f, "7/25"),
            ChartPoint(7f, 201f, "7/26"), ChartPoint(8f, 226f, "今天"),
        ),
    ),
)

private fun formatPointCount(pointCount: Int): String = when (pointCount) {
    1_000 -> "1,000"
    5_000 -> "5,000"
    else -> pointCount.toString()
}

private fun ViewContainer<*, *>.ShowcaseChartControls(
    accent: Color,
    elevatedBackground: Color,
    primaryText: Color,
    onEntranceRefresh: () -> Unit,
    onDataUpdate: () -> Unit,
) {
    View {
        attr {
            flexDirectionRow()
            marginBottom(10f)
        }
        Button {
            attr {
                size(88f, 32f)
                borderRadius(16f)
                backgroundColor(elevatedBackground)
                marginRight(8f)
                titleAttr {
                    text("入场刷新")
                    fontSize(12f)
                    color(primaryText)
                }
            }
            event { click { onEntranceRefresh() } }
        }
        Button {
            attr {
                size(88f, 32f)
                borderRadius(16f)
                backgroundColor(accent)
                titleAttr {
                    text("实时更新")
                    fontSize(12f)
                    color(Color.WHITE)
                }
            }
            event { click { onDataUpdate() } }
        }
    }
}

private fun ViewContainer<*, *>.PieChartControls(
    accent: Color,
    elevatedBackground: Color,
    primaryText: Color,
    onEntranceAnimation: () -> Unit,
    onSwitchData: () -> Unit,
) {
    View {
        attr {
            flexDirectionRow()
            marginBottom(10f)
        }
        Button {
            attr {
                size(96f, 32f)
                borderRadius(16f)
                backgroundColor(elevatedBackground)
                marginRight(8f)
                titleAttr {
                    text("入场动画")
                    fontSize(12f)
                    color(primaryText)
                }
            }
            event { click { onEntranceAnimation() } }
        }
        Button {
            attr {
                size(96f, 32f)
                borderRadius(16f)
                backgroundColor(elevatedBackground)
                marginRight(8f)
                titleAttr {
                    text("切换数据")
                    fontSize(12f)
                    color(primaryText)
                }
            }
            event { click { onSwitchData() } }
        }
    }
}

private fun ViewContainer<*, *>.RealtimeChartControls(
    accent: Color,
    elevatedBackground: Color,
    primaryText: Color,
    onEntranceRefresh: () -> Unit,
    onRealtimeUpdate: () -> Unit,
) {
    View {
        attr {
            flexDirectionRow()
            marginBottom(10f)
        }
        Button {
            attr {
                size(88f, 32f)
                borderRadius(16f)
                backgroundColor(elevatedBackground)
                marginRight(8f)
                titleAttr {
                    text("入场刷新")
                    fontSize(12f)
                    color(primaryText)
                }
            }
            event { click { onEntranceRefresh() } }
        }
        Button {
            attr {
                size(88f, 32f)
                borderRadius(16f)
                backgroundColor(accent)
                titleAttr {
                    text("实时更新")
                    fontSize(12f)
                    color(Color.WHITE)
                }
            }
            event { click { onRealtimeUpdate() } }
        }
    }
}

private fun ViewContainer<*, *>.ShowcaseChartEntrance(
    page: ChartShowcasePage,
    chartKey: String,
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        content()
    }
}

@Page(CHART_SHOWCASE_PAGE, supportInLocal = true)
internal class ChartShowcasePage : BasePager() {
    private val performanceDataSets = listOf(100, 1_000, 5_000).associateWith(::buildPerformancePoints)
    private val performanceSampleCounts = performanceDataSets.mapValues { (_, points) ->
        LineSampler.sample(points.withIndex().toList(), maxPointCount = 240).size
    }

    private var themeMode by observable(ShowcaseThemeMode.LIGHT)
    private val trendDataSets = listOf(initialTrendData(), alternateTrendData())
    private val categoryDataSets = listOf(initialCategoryData(), alternateCategoryData())
    private var trendDataSetIndex by observable(0)
    private var categoryDataSetIndex by observable(0)
    private var trendData by observable(trendDataSets.first())
    private var categoryData by observable(categoryDataSets.first())
    private var radarData by observable(initialRadarData())
    private var areaData by observable(initialAreaData())
    private var trendFeedbackTitle by observable("等待趋势交互")
    private var trendFeedbackText by observable("单指左右拖动浏览日期；双指捏合缩放；双击复位。")
    private var areaFeedbackTitle by observable("等待面积图交互")
    private var areaFeedbackText by observable("点击或长按面积边界，查看每日成交金额。")
    private var pieFeedbackTitle by observable("等待饼环图交互")
    private var pieFeedbackText by observable("点击任意扇区查看渠道订单量与占比。")
    private var heatmapFeedbackTitle by observable("等待热力图交互")
    private var heatmapFeedbackText by observable("点击任意时段单元格查看原始热度值。")
    private var radarFeedbackTitle by observable("等待雷达图交互")
    private var radarFeedbackText by observable("点击能力维度顶点，比较当前表现与目标。")
    private var mixedFeedbackTitle by observable("等待组合图交互")
    private var mixedFeedbackText by observable("点击柱体或折线点，比较实际收入与目标收入。")
    private var sparklineFeedbackTitle by observable("等待紧凑趋势交互")
    private var sparklineFeedbackText by observable("点击趋势线附近，查看对应日期的支付成功率。")
    private var performancePointCount by observable(100)
    private var performanceFeedbackTitle by observable("100 点 · 原样绘制")
    private var performanceFeedbackText by observable("点击折线可核对采样前的原始数据索引。")
    private var categoryFeedbackTitle by observable("等待分类交互")
    private var categoryFeedbackText by observable("点击柱体查看本月与上月的原始订单量。")
    private var heatmapScenarioIndex by observable(0)
    private val pieDataSets = listOf(
        listOf(
            PieEntry("推荐", 420f),
            PieEntry("搜索", 260f),
            PieEntry("直播", 190f),
            PieEntry("其他", 130f),
        ),
        listOf(
            PieEntry("推荐", 280f),
            PieEntry("搜索", 220f),
            PieEntry("直播", 160f),
            PieEntry("社群", 145f),
            PieEntry("门店", 110f),
            PieEntry("其他", 85f),
        ),
        listOf(
            PieEntry("线上", 510f),
            PieEntry("线下", 280f),
            PieEntry("合作渠道", 190f),
        ),
    )
    private var pieDataSetIndex by observable(0)
    private var pieEntries by observable(pieDataSets.first())

    private var chartControlRevision by observable(0)
    private var chartDataRevision by observable(0)
    private val chartDataVersions = mutableMapOf<String, Int>()
    private val chartDataSeeds = mutableMapOf<String, Int>()
    private val chartEntranceProgress = mutableMapOf<String, Float>()
    private val chartEntranceTimers = mutableMapOf<String, Timer>()
    private val chartRealtimeTimers = mutableMapOf<String, Timer>()

    private val isH5Showcase: Boolean
        get() = pageData.params.optString("is_H5") == "1"

    override fun created() {
        super.created()
        if (isH5Showcase) {
            themeMode = ShowcaseThemeMode.DARK
        }
    }

    internal fun chartEntranceProgress(chartKey: String): Float {
        chartControlRevision
        return chartEntranceProgress[chartKey] ?: 1f
    }

    private fun refreshChartEntrance(chartKey: String) {
        chartEntranceTimers.remove(chartKey)?.cancel()
        chartEntranceProgress[chartKey] = 0f
        chartControlRevision += 1
        var frame = 0
        val totalFrames = 16
        val timer = Timer()
        chartEntranceTimers[chartKey] = timer
        timer.schedule(delay = 16, period = 16) {
            frame += 1
            val linear = (frame.toFloat() / totalFrames).coerceIn(0f, 1f)
            chartEntranceProgress[chartKey] = 1f - (1f - linear) * (1f - linear)
            chartControlRevision += 1
            if (linear >= 1f) {
                timer.cancel()
                if (chartEntranceTimers[chartKey] === timer) chartEntranceTimers.remove(chartKey)
            }
        }
    }

    private fun refreshChartData(chartKey: String) {
        when (chartKey) {
            "trend", "bar", "radar", "area" -> runRealtimeChartUpdate(chartKey)
            else -> updateChartData(chartKey)
        }
    }

    private fun switchChartData(chartKey: String) {
        chartRealtimeTimers.remove(chartKey)?.cancel()
        when (chartKey) {
            "trend" -> {
                trendDataSetIndex = (trendDataSetIndex + 1) % trendDataSets.size
                trendData = trendDataSets[trendDataSetIndex]
                trendFeedbackTitle = "趋势数据已切换"
                trendFeedbackText = "已直接切换到新的日期区间和数据快照。"
            }
            "bar" -> {
                categoryDataSetIndex = (categoryDataSetIndex + 1) % categoryDataSets.size
                categoryData = categoryDataSets[categoryDataSetIndex]
                categoryFeedbackTitle = "分类数据已切换"
                categoryFeedbackText = "已直接切换到新的渠道分类和数据快照。"
            }
            else -> updateChartData(chartKey)
        }
    }

    private fun runRealtimeChartUpdate(chartKey: String) {
        chartRealtimeTimers.remove(chartKey)?.cancel()
        val frameCount = 26
        var frame = 0
        val timer = Timer()
        chartRealtimeTimers[chartKey] = timer
        when (chartKey) {
            "trend" -> {
                val start = trendData
                val target = start.map { series ->
                    series.copy(items = series.items.map { point ->
                        point.copy(y = nextUpdatedValue(point.y, minimum = 80f, maximum = 330f))
                    })
                }
                trendFeedbackTitle = "趋势正在实时更新"
                trendFeedbackText = "日期位置保持不变，点位与连线正在平滑过渡。"
                timer.schedule(delay = 16, period = 16) {
                    frame += 1
                    val linear = (frame.toFloat() / frameCount).coerceIn(0f, 1f)
                    trendData = interpolateTrendData(start, target, easeOut(linear))
                    if (linear >= 1f) {
                        timer.cancel()
                        if (chartRealtimeTimers[chartKey] === timer) chartRealtimeTimers.remove(chartKey)
                        trendFeedbackTitle = "趋势实时更新完成"
                        trendFeedbackText = "本周与上周数据已更新。"
                    }
                }
            }
            "bar" -> {
                val start = categoryData
                val target = start.map { series ->
                    series.copy(items = series.items.map { entry ->
                        entry.copy(value = nextUpdatedValue(entry.value, minimum = 120f, maximum = 320f))
                    })
                }
                categoryFeedbackTitle = "分类正在实时更新"
                categoryFeedbackText = "分类位置保持不变，两组柱体正在连续伸缩。"
                timer.schedule(delay = 16, period = 16) {
                    frame += 1
                    val linear = (frame.toFloat() / frameCount).coerceIn(0f, 1f)
                    categoryData = interpolateCategoryData(start, target, easeOut(linear))
                    if (linear >= 1f) {
                        timer.cancel()
                        if (chartRealtimeTimers[chartKey] === timer) chartRealtimeTimers.remove(chartKey)
                        categoryFeedbackTitle = "分类实时更新完成"
                        categoryFeedbackText = "本月与上月订单数据已更新。"
                    }
                }
            }
            "radar" -> {
                val start = radarData
                val target = start.map { series ->
                    series.copy(items = series.items.map { entry ->
                        entry.copy(value = nextUpdatedValue(entry.value, minimum = 45f, maximum = 100f))
                    })
                }
                radarFeedbackTitle = "能力对照正在实时更新"
                radarFeedbackText = "能力维度保持不变，顶点与覆盖区域正在平滑变化。"
                timer.schedule(delay = 16, period = 16) {
                    frame += 1
                    val linear = (frame.toFloat() / frameCount).coerceIn(0f, 1f)
                    radarData = interpolateRadarData(start, target, easeOut(linear))
                    if (linear >= 1f) {
                        timer.cancel()
                        if (chartRealtimeTimers[chartKey] === timer) chartRealtimeTimers.remove(chartKey)
                        radarFeedbackTitle = "能力对照实时更新完成"
                        radarFeedbackText = "当前表现与目标水平已更新。"
                    }
                }
            }
            "area" -> {
                val start = areaData
                val target = start.map { series ->
                    series.copy(items = series.items.map { point ->
                        point.copy(y = nextUpdatedValue(point.y, minimum = 70f, maximum = 250f))
                    })
                }
                areaFeedbackTitle = "累计趋势正在实时更新"
                areaFeedbackText = "日期位置保持不变，折线与面积正在平滑过渡。"
                timer.schedule(delay = 16, period = 16) {
                    frame += 1
                    val linear = (frame.toFloat() / frameCount).coerceIn(0f, 1f)
                    areaData = interpolateTrendData(start, target, easeOut(linear))
                    if (linear >= 1f) {
                        timer.cancel()
                        if (chartRealtimeTimers[chartKey] === timer) chartRealtimeTimers.remove(chartKey)
                        areaFeedbackTitle = "累计趋势实时更新完成"
                        areaFeedbackText = "近 8 日成交金额已更新。"
                    }
                }
            }
        }
    }

    private fun interpolateTrendData(
        start: List<ChartSeries<ChartPoint>>,
        target: List<ChartSeries<ChartPoint>>,
        progress: Float,
    ): List<ChartSeries<ChartPoint>> = target.mapIndexed { seriesIndex, targetSeries ->
        val startSeries = start[seriesIndex]
        targetSeries.copy(items = targetSeries.items.mapIndexed { itemIndex, targetPoint ->
            val startPoint = startSeries.items[itemIndex]
            targetPoint.copy(y = interpolateValue(startPoint.y, targetPoint.y, progress))
        })
    }

    private fun interpolateCategoryData(
        start: List<ChartSeries<BarEntry>>,
        target: List<ChartSeries<BarEntry>>,
        progress: Float,
    ): List<ChartSeries<BarEntry>> = target.mapIndexed { seriesIndex, targetSeries ->
        val startSeries = start[seriesIndex]
        targetSeries.copy(items = targetSeries.items.mapIndexed { itemIndex, targetEntry ->
            val startEntry = startSeries.items[itemIndex]
            targetEntry.copy(value = interpolateValue(startEntry.value, targetEntry.value, progress))
        })
    }

    private fun interpolateRadarData(
        start: List<ChartSeries<RadarEntry>>,
        target: List<ChartSeries<RadarEntry>>,
        progress: Float,
    ): List<ChartSeries<RadarEntry>> = target.mapIndexed { seriesIndex, targetSeries ->
        val startSeries = start[seriesIndex]
        targetSeries.copy(items = targetSeries.items.mapIndexed { itemIndex, targetEntry ->
            val startEntry = startSeries.items[itemIndex]
            targetEntry.copy(value = interpolateValue(startEntry.value, targetEntry.value, progress))
        })
    }

    private fun nextUpdatedValue(value: Float, minimum: Float, maximum: Float): Float {
        var next = Random.nextInt(minimum.toInt(), maximum.toInt() + 1).toFloat()
        if (kotlin.math.abs(next - value) < 18f) {
            next = if (next <= (minimum + maximum) / 2f) {
                (next + 36f).coerceAtMost(maximum)
            } else {
                (next - 36f).coerceAtLeast(minimum)
            }
        }
        return next
    }

    private fun easeOut(progress: Float): Float = 1f - (1f - progress) * (1f - progress)

    private fun interpolateValue(from: Float, to: Float, progress: Float): Float =
        (from.toDouble() + (to.toDouble() - from.toDouble()) * progress.toDouble()).toFloat()

    private fun updateChartData(chartKey: String) {
        chartDataVersions[chartKey] = (chartDataVersions[chartKey] ?: 0) + 1
        chartDataSeeds[chartKey] = Random.nextInt()
        when (chartKey) {
            "trend" -> {
                trendFeedbackTitle = "趋势正在原位更新"
                trendFeedbackText = "日期位置保持不变，本周与上周的点位和连线正以 420ms 缓出节奏平滑过渡。"
            }
            "bar" -> {
                categoryFeedbackTitle = "分类数据正在原位更新"
                categoryFeedbackText = "分类与分组位置保持不变，本月和上月柱体正从基线连续伸缩。"
            }
            "heatmap" -> {
                heatmapScenarioIndex = (heatmapScenarioIndex + 1) % heatmapShowcaseScenarios.size
                syncHeatmapFeedbackWithScenario()
                // A new heatmap snapshot should reveal with the same staggered
                // cell entrance used for its initial render.
                refreshChartEntrance(chartKey)
            }
        }
        // Recompose with a new immutable snapshot. Heatmap updates additionally
        // restart their staggered cell entrance above.
        chartDataRevision += 1
        chartControlRevision += 1
    }

    private fun switchPieData() {
        pieDataSetIndex = (pieDataSetIndex + 1) % pieDataSets.size
        pieEntries = pieDataSets[pieDataSetIndex]
        refreshChartEntrance("pie")
        pieFeedbackTitle = "已切换渠道数据"
        pieFeedbackText = "第 ${pieDataSetIndex + 1} 组共 ${pieEntries.size} 个扇区，正在重放入场动画。"
    }

    private fun currentHeatmapScenario(): HeatmapShowcaseScenario {
        return heatmapShowcaseScenarios[heatmapScenarioIndex]
    }

    private fun syncHeatmapFeedbackWithScenario() {
        val scenario = currentHeatmapScenario()
        heatmapFeedbackTitle = "已切换 ${scenario.seriesName} 示例"
        heatmapFeedbackText =
            "${scenario.summary} · ${scenario.columnCount} 列 × ${scenario.rowCount} 行，共 ${scenario.entries.size} 个单元格"
    }

    private fun refreshedChartValue(chartKey: String, base: Float, index: Int): Float {
        val dataRevision = chartDataRevision
        if (!base.isFinite()) return base
        if (dataRevision == 0 || (chartDataVersions[chartKey] ?: 0) == 0) return base
        val seed = chartDataSeeds[chartKey] ?: 0
        var mixed = seed xor (index * 0x45D9F3B)
        mixed = mixed xor (mixed ushr 16)
        mixed *= 0x45D9F3B
        mixed = mixed xor (mixed ushr 16)
        val fraction = ((mixed ushr 8) and 0xFFFF).toFloat() / 65535f
        val (minimum, maximum) = when (chartKey) {
            "performance" -> 120f to 260f
            "invalid-line" -> 20f to 80f
            "sparkline" -> 92f to 100f
            "mixed" -> 60f to 180f
            "pie" -> 90f to 440f
            "heatmap" -> 20f to 100f
            "radar" -> 45f to 100f
            "area" -> 70f to 250f
            "trend" -> 80f to 330f
            "bar" -> 120f to 320f
            else -> 0f to base.coerceAtLeast(1f)
        }
        return minimum + (maximum - minimum) * fraction
    }

    private fun pieTotal(): Float = pieEntries.sumOf { it.value.toDouble() }.toFloat()

    override fun viewDestroyed() {
        chartEntranceTimers.values.forEach { it.cancel() }
        chartEntranceTimers.clear()
        chartRealtimeTimers.values.forEach { it.cancel() }
        chartRealtimeTimers.clear()
        super.viewDestroyed()
    }

    override fun body(): ViewBuilder {
        val page = this
        val isH5 = isH5Showcase
        val activeThemeMode = if (isH5 && themeMode == ShowcaseThemeMode.LIGHT) {
            ShowcaseThemeMode.DARK
        } else {
            themeMode
        }
        val isDark = activeThemeMode == ShowcaseThemeMode.DARK
        val pageBackground = if (isDark) {
            if (isH5) Color(0xFF071423) else Color(0xFF0B1220)
        } else {
            Color(0xFFF3F6FB)
        }
        val cardBackground = if (isDark) Color(0xFF111C2E) else Color.WHITE
        val elevatedBackground = if (isDark) Color(0xFF17263D) else Color(0xFFEAF2FF)
        val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF172033)
        val secondaryText = if (isDark) Color(0xFFAAB7C9) else Color(0xFF687386)
        val accent = when (activeThemeMode) {
            ShowcaseThemeMode.LIGHT -> Color(0xFF2563EB)
            ShowcaseThemeMode.DARK -> if (isH5) Color(0xFF2DD4BF) else Color(0xFF60A5FA)
            ShowcaseThemeMode.OCEAN -> Color(0xFF0284C7)
            ShowcaseThemeMode.SUNSET -> Color(0xFFF97316)
        }
        val accentSoft = when (activeThemeMode) {
            ShowcaseThemeMode.LIGHT -> Color(0xFFE8F0FF)
            ShowcaseThemeMode.DARK -> if (isH5) Color(0xFF123745) else Color(0xFF203452)
            ShowcaseThemeMode.OCEAN -> Color(0xFFE0F5FE)
            ShowcaseThemeMode.SUNSET -> Color(0xFFFFEDE3)
        }
        val chartTheme = when (activeThemeMode) {
            ShowcaseThemeMode.LIGHT -> ChartTheme.light()
            ShowcaseThemeMode.DARK -> ChartTheme.dark()
            ShowcaseThemeMode.OCEAN -> ChartTheme.ocean()
            ShowcaseThemeMode.SUNSET -> ChartTheme.sunset()
        }
        val heatmapColorScale = defaultHeatmapColorScale(chartTheme)
        return {
            attr { backgroundColor(pageBackground) }
            if (!isH5) {
                RouterNavBar {
                    attr { title = "KuiklyChartKit Showcase" }
                }
            }
            Scroller {
                attr {
                    flex(1f)
                    padding(16f)
                }

                if (isH5) {
                    View {
                        attr {
                            borderRadius(22f)
                            padding(22f)
                            marginBottom(14f)
                            backgroundLinearGradient(
                                Direction.TO_BOTTOM,
                                ColorStop(Color(0xFF12364A), 0f),
                                ColorStop(Color(0xFF0D1E34), 0.58f),
                                ColorStop(Color(0xFF101A2C), 1f),
                            )
                        }
                        Text {
                            attr {
                                text("Kuikly ChartKit / 数据工作台")
                                fontSize(25f)
                                fontWeightBold()
                                color(Color(0xFFF0FDFA))
                            }
                        }
                        Text {
                            attr {
                                text("真实 Kotlin DSL、Canvas 渲染与交互回调，在同一份 H5 演示中完成趋势、性能与决策视图验证。")
                                fontSize(13f)
                                color(Color(0xFFB9D4DE))
                                marginTop(8f)
                            }
                        }
                        View {
                            attr {
                                flexDirectionRow()
                                marginTop(20f)
                            }
                            View {
                                attr {
                                    flex(1f)
                                    backgroundColor(Color(0x2A38BDF8))
                                    borderRadius(12f)
                                    padding(10f)
                                    marginRight(6f)
                                }
                                Text { attr { text("9"); fontSize(21f); fontWeightBold(); color(Color(0xFF7DD3FC)) } }
                                Text { attr { text("真实演示"); fontSize(10f); color(Color(0xFFB7D3E4)); marginTop(2f) } }
                            }
                            View {
                                attr {
                                    flex(1f)
                                    backgroundColor(Color(0x2A2DD4BF))
                                    borderRadius(12f)
                                    padding(10f)
                                    marginRight(6f)
                                }
                                Text { attr { text("3"); fontSize(21f); fontWeightBold(); color(Color(0xFF5EEAD4)) } }
                                Text { attr { text("目标平台"); fontSize(10f); color(Color(0xFFB7D3E4)); marginTop(2f) } }
                            }
                            View {
                                attr {
                                    flex(1f)
                                    backgroundColor(Color(0x2AFBBF24))
                                    borderRadius(12f)
                                    padding(10f)
                                    marginRight(6f)
                                }
                                Text { attr { text("4"); fontSize(21f); fontWeightBold(); color(Color(0xFFFDE68A)) } }
                                Text { attr { text("视觉主题"); fontSize(10f); color(Color(0xFFB7D3E4)); marginTop(2f) } }
                            }
                            View {
                                attr {
                                    flex(1f)
                                    backgroundColor(Color(0x2AA78BFA))
                                    borderRadius(12f)
                                    padding(10f)
                                }
                                Text { attr { text("P2"); fontSize(21f); fontWeightBold(); color(Color(0xFFC4B5FD)) } }
                                Text { attr { text("当前阶段"); fontSize(10f); color(Color(0xFFB7D3E4)); marginTop(2f) } }
                            }
                        }
                        View {
                            attr {
                                flexDirectionRow()
                                marginTop(16f)
                            }
                            View {
                                attr {
                                    backgroundColor(Color(0x332DD4BF))
                                    borderRadius(10f)
                                    padding(7f)
                                    marginRight(7f)
                                }
                                Text { attr { text("Canvas Runtime"); fontSize(10f); color(Color(0xFFCCFBF1)) } }
                            }
                            View {
                                attr {
                                    backgroundColor(Color(0x3338BDF8))
                                    borderRadius(10f)
                                    padding(7f)
                                    marginRight(7f)
                                }
                                Text { attr { text("交互可验证"); fontSize(10f); color(Color(0xFFE0F2FE)) } }
                            }
                            View {
                                attr {
                                    backgroundColor(Color(0x33FBBF24))
                                    borderRadius(10f)
                                    padding(7f)
                                }
                                Text { attr { text("P0 - P2"); fontSize(10f); color(Color(0xFFFEF3C7)) } }
                            }
                        }
                    }
                }

                // Product introduction and the primary entry point for the rest of the page.
                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(20f)
                        padding(20f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("把数据故事带到每一个 Kuikly 端")
                            fontSize(24f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("一个基于 Canvas 的跨端图表组件库，用声明式 Kotlin DSL 构建一致、可交互的数据体验。")
                            fontSize(14f)
                            color(secondaryText)
                            marginTop(8f)
                        }
                    }
                    View {
                        attr {
                            backgroundColor(accentSoft)
                            borderRadius(12f)
                            padding(12f)
                            marginTop(16f)
                        }
                        Text {
                            attr {
                            text("当前展台包含多系列折线、分组柱状、柱线组合、雷达、Sparkline、面积、环形、热力与高密度折线图；所有示例均由真实 DSL 渲染。")
                                fontSize(12f)
                                color(accent)
                            }
                        }
                    }
                }

                val performanceSectionStart = childrenSize()
                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("高密度折线图与边界验证"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("100 / 1,000 / 5,000 点采样实验与非法数据断线验证。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
                }
                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("高密度折线图")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            val pointCount = page.performancePointCount
                            val sampledPointCount = page.performanceSampleCounts.getValue(pointCount)
                            val samplingState = if (sampledPointCount < pointCount) {
                                "min/max 峰谷采样"
                            } else {
                                "无需采样"
                            }
                            text(
                                "输入 ${formatPointCount(pointCount)} 点 · " +
                                    "绘制 $sampledPointCount 点 · $samplingState · 长按拖动可框选原始点区间",
                            )
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(4f)
                        }
                    }
                    View {
                        attr {
                            flexDirectionRow()
                            marginTop(12f)
                            marginBottom(10f)
                        }
                        Button {
                            attr {
                                size(76f, 36f)
                                borderRadius(18f)
                                marginRight(8f)
                                backgroundColor(if (page.performancePointCount == 100) accent else elevatedBackground)
                                titleAttr {
                                    text("100 点")
                                    fontSize(12f)
                                    color(if (page.performancePointCount == 100) Color.WHITE else primaryText)
                                }
                            }
                            event { click { page.selectPerformancePointCount(100) } }
                        }
                        Button {
                            attr {
                                size(82f, 36f)
                                borderRadius(18f)
                                marginRight(8f)
                                backgroundColor(if (page.performancePointCount == 1_000) accent else elevatedBackground)
                                titleAttr {
                                    text("1,000 点")
                                    fontSize(12f)
                                    color(if (page.performancePointCount == 1_000) Color.WHITE else primaryText)
                                }
                            }
                            event { click { page.selectPerformancePointCount(1_000) } }
                        }
                        Button {
                            attr {
                                size(82f, 36f)
                                borderRadius(18f)
                                backgroundColor(if (page.performancePointCount == 5_000) accent else elevatedBackground)
                                titleAttr {
                                    text("5,000 点")
                                    fontSize(12f)
                                    color(if (page.performancePointCount == 5_000) Color.WHITE else primaryText)
                                }
                            }
                            event { click { page.selectPerformancePointCount(5_000) } }
                        }
                    }
                    View {
                        attr {
                            backgroundColor(accentSoft)
                            borderRadius(12f)
                            padding(10f)
                            marginBottom(8f)
                        }
                        Text {
                            attr {
                                text(page.performanceFeedbackTitle)
                                fontSize(12f)
                                fontWeightBold()
                                color(accent)
                            }
                        }
                        Text {
                            attr {
                                text(page.performanceFeedbackText)
                                fontSize(11f)
                                color(if (isDark) Color(0xFFD5E6FF) else Color(0xFF23416B))
                                marginTop(3f)
                            }
                        }
                    }
                    ShowcaseChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("performance") },
                        onDataUpdate = { page.refreshChartData("performance") },
                    )
                    ShowcaseChartEntrance(page, "performance") {
                    LineChart {
                        attr {
                            height(220f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("performance") }
                            data(
                                ChartSeries(
                                    "密集趋势",
                                    page.performanceDataSets.getValue(page.performancePointCount).mapIndexed { index, point ->
                                        point.copy(y = page.refreshedChartValue("performance", point.y, index))
                                    },
                                ),
                            )
                            xAxis { visible = false }
                            yAxis { visible = true; tickCount = 4; includeZero = false; min = 100f; max = 280f }
                            legend { visible = false }
                            line { smooth = false; showPoints = false; lineWidth = 1.5f }
                            tooltip { enabled = true }
                            interaction { maxRenderPointCount = 240 }
                            brush { enabled = true }
                            dataTransition { enabled = true; durationMs = 420 }
                        }
                        event {
                            onItemSelected {
                                page.performanceFeedbackTitle = "命中原始点 #${it.itemIndex + 1}"
                                page.performanceFeedbackText =
                                    "${it.item.label} · 数值 ${it.item.y} · 回调索引 ${it.itemIndex}"
                            }
                            onBrushChanged { selection ->
                                if (selection == null) {
                                    page.performanceFeedbackTitle = "框选已清除"
                                    page.performanceFeedbackText = "双击图表可清除当前区间。"
                                } else {
                                    page.performanceFeedbackTitle = "已框选原始点区间"
                                    page.performanceFeedbackText =
                                        "第 ${selection.startIndex + 1} 至第 ${selection.endIndex + 1} 个输入点。"
                                }
                            }
                        }
                    }
                    }
                }

                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("异常值折线图")
                            fontSize(16f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("输入 7 点 · 有效 5 点 · NaN / Infinity 形成断线且不参与刻度。")
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(4f)
                            marginBottom(8f)
                        }
                    }
                    ShowcaseChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("invalid-line") },
                        onDataUpdate = { page.refreshChartData("invalid-line") },
                    )
                    ShowcaseChartEntrance(page, "invalid-line") {
                    LineChart {
                        attr {
                            height(180f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("invalid-line") }
                            data(
                                ChartSeries(
                                    name = "边界数据",
                                    items = listOf(
                                        ChartPoint(1f, 32f, "A"),
                                        ChartPoint(2f, 48f, "B"),
                                        ChartPoint(3f, Float.NaN, "NaN"),
                                        ChartPoint(4f, 41f, "C"),
                                        ChartPoint(5f, Float.POSITIVE_INFINITY, "Infinity"),
                                        ChartPoint(6f, 56f, "D"),
                                        ChartPoint(7f, 63f, "E"),
                                    ).mapIndexed { index, point ->
                                        point.copy(y = page.refreshedChartValue("invalid-line", point.y, index))
                                    },
                                ),
                            )
                            yAxis { tickCount = 4; includeZero = false }
                            legend { visible = false }
                            line { smooth = false; showPoints = true }
                            tooltip { enabled = false }
                            dataTransition { enabled = true; durationMs = 420 }
                        }
                    }
                    }
                }

                val sparklineSectionStart = childrenSize()
                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("迷你折线图（Sparkline）"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("P1 Sparkline 默认隐藏轴、网格和图例，适合 KPI 卡片。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
                }
                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("支付成功率")
                            fontSize(15f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("98.6%")
                            fontSize(28f)
                            fontWeightBold()
                            color(accent)
                            marginTop(4f)
                        }
                    }
                    Text {
                        attr {
                            text("近 7 日 · 较上周 +1.2%")
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(2f)
                            marginBottom(8f)
                        }
                    }
                    View {
                        attr {
                            backgroundColor(accentSoft)
                            borderRadius(12f)
                            padding(10f)
                            marginBottom(6f)
                        }
                        Text {
                            attr {
                                text(page.sparklineFeedbackTitle)
                                fontSize(12f)
                                fontWeightBold()
                                color(accent)
                            }
                        }
                        Text {
                            attr {
                                text(page.sparklineFeedbackText)
                                fontSize(11f)
                                color(if (isDark) Color(0xFFD5E6FF) else Color(0xFF23416B))
                                marginTop(3f)
                            }
                        }
                    }
                    ShowcaseChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("sparkline") },
                        onDataUpdate = { page.refreshChartData("sparkline") },
                    )
                    SparklineChart {
                        attr {
                            height(116f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("sparkline") }
                            selectable = true
                            data(
                                ChartSeries(
                                    name = "支付成功率",
                                    items = listOf(
                                        ChartPoint(1f, 96.8f, "周一"),
                                        ChartPoint(2f, 97.1f, "周二"),
                                        ChartPoint(3f, 96.9f, "周三"),
                                        ChartPoint(4f, 97.8f, "周四"),
                                        ChartPoint(5f, 98.1f, "周五"),
                                        ChartPoint(6f, 98.0f, "周六"),
                                        ChartPoint(7f, 98.6f, "今天"),
                                    ).mapIndexed { index, point ->
                                        point.copy(y = page.refreshedChartValue("sparkline", point.y, index))
                                    },
                                ),
                            )
                            line { smooth = true; showPoints = false; lineWidth = 2.5f }
                            yAxis { min = 90f; max = 100f }
                            dataTransition { enabled = true; durationMs = 420 }
                            tooltip {
                                enabled = true
                                valueFormatter = { value -> "$value%" }
                            }
                        }
                        event {
                            onItemSelected {
                                page.sparklineFeedbackTitle = "已选择 ${it.item.label}"
                                page.sparklineFeedbackText = "支付成功率 ${it.item.y}%"
                            }
                        }
                    }
                }

                val mixedSectionStart = childrenSize()
                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("柱线组合图"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("P1 组合图让分类柱和折线共享坐标、图例与 Tooltip。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
                }
                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("本周收入与目标")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("柱体展示实际收入，折线展示同一分类槽中的目标值。")
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(4f)
                            marginBottom(8f)
                        }
                    }
                    View {
                        attr {
                            backgroundColor(accentSoft)
                            borderRadius(14f)
                            padding(12f)
                            marginBottom(10f)
                        }
                        Text {
                            attr {
                                text(page.mixedFeedbackTitle)
                                fontSize(13f)
                                fontWeightBold()
                                color(accent)
                            }
                        }
                        Text {
                            attr {
                                text(page.mixedFeedbackText)
                                fontSize(12f)
                                color(if (isDark) Color(0xFFD5E6FF) else Color(0xFF23416B))
                                marginTop(4f)
                            }
                        }
                    }
                    ShowcaseChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("mixed") },
                        onDataUpdate = { page.refreshChartData("mixed") },
                    )
                    MixedChart {
                        attr {
                            height(286f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("mixed") }
                            barData(
                                ChartSeries(
                                    name = "实际收入",
                                    items = listOf(
                                        BarEntry("周一", 86f),
                                        BarEntry("周二", 112f),
                                        BarEntry("周三", 104f),
                                        BarEntry("周四", 138f),
                                        BarEntry("周五", 151f),
                                    ).mapIndexed { index, entry ->
                                        entry.copy(value = page.refreshedChartValue("mixed", entry.value, index))
                                    },
                                ),
                            )
                            lineData(
                                ChartSeries(
                                    name = "目标收入",
                                    items = listOf(
                                        BarEntry("周一", 96f),
                                        BarEntry("周二", 105f),
                                        BarEntry("周三", 116f),
                                        BarEntry("周四", 128f),
                                        BarEntry("周五", 142f),
                                    ).mapIndexed { index, entry ->
                                        entry.copy(value = page.refreshedChartValue("mixed", entry.value, index + 5))
                                    },
                                ),
                            )
                            yAxis { tickCount = 5; includeZero = true; min = 0f; max = 200f }
                            dataTransition { enabled = true; durationMs = 420 }
                            bars { showValueLabels = false; cornerRadius = 5f }
                            line { smooth = true; showPoints = true; lineWidth = 2.5f }
                            tooltip { valueFormatter = { value -> "¥${value.toInt()}K" } }
                        }
                        event {
                            onItemSelected {
                                val kind = when (it.seriesType) {
                                    MixedSeriesType.BAR -> "柱系列"
                                    MixedSeriesType.LINE -> "线系列"
                                }
                                page.mixedFeedbackTitle = "已选择${it.seriesName}"
                                page.mixedFeedbackText =
                                    "$kind · ${it.item.label}：¥${it.item.value.toInt()}K"
                            }
                        }
                    }
                }

                val pieSectionStart = childrenSize()
                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("环形图"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("P1 饼环图使用独立极坐标布局，不暴露无意义的坐标轴配置。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
                }
                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("渠道订单占比")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("扇区面积表达订单占比；中心展示有效数据总量。")
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(4f)
                            marginBottom(8f)
                        }
                    }
                    View {
                        attr {
                            backgroundColor(accentSoft)
                            borderRadius(14f)
                            padding(12f)
                            marginBottom(10f)
                        }
                        Text {
                            attr {
                                text(page.pieFeedbackTitle)
                                fontSize(13f)
                                fontWeightBold()
                                color(accent)
                            }
                        }
                        Text {
                            attr {
                                text(page.pieFeedbackText)
                                fontSize(12f)
                                color(if (isDark) Color(0xFFD5E6FF) else Color(0xFF23416B))
                                marginTop(4f)
                            }
                        }
                    }
                    PieChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceAnimation = { page.refreshChartEntrance("pie") },
                        onSwitchData = { page.switchPieData() },
                    )
                    PieChart {
                        attr {
                            height(292f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("pie") }
                            seriesName = "渠道订单"
                            data(*page.pieEntries.toTypedArray())
                            pie {
                                innerRadiusRatio = 0.58f
                                startAngleDegrees = -90f
                                gapAngleDegrees = 2f
                                showValueLabels = true
                                centerLabel = "订单总量"
                            }
                            tooltip {
                                valueFormatter = { value -> "${value.toInt()} 单" }
                            }
                        }
                        event {
                            onItemSelected {
                                val total = page.pieTotal()
                                val percentage = (it.item.value / total * 100f).toInt()
                                page.pieFeedbackTitle = "已选择 ${it.item.label}"
                                page.pieFeedbackText = "${it.item.value.toInt()} 单 · 占全部渠道 $percentage%"
                            }
                        }
                    }
                }

                val heatmapSectionStart = childrenSize()
                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("热力图"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("P2 热力图使用颜色强度表达二维数据分布。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
                }
                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("${page.currentHeatmapScenario().seriesName}热度分布")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            page.currentHeatmapScenario().let { scenario ->
                                text("${scenario.summary}：${scenario.columnCount} 列 × ${scenario.rowCount} 行，共 ${scenario.entries.size} 个单元格。数据更新可切换示例。")
                            }
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(4f)
                            marginBottom(8f)
                        }
                    }
                    View {
                        attr {
                            backgroundColor(accentSoft)
                            borderRadius(14f)
                            padding(12f)
                            marginBottom(10f)
                        }
                        Text {
                            attr {
                                text(page.heatmapFeedbackTitle)
                                fontSize(13f)
                                fontWeightBold()
                                color(accent)
                            }
                        }
                        Text {
                            attr {
                                text(page.heatmapFeedbackText)
                                fontSize(12f)
                                color(if (isDark) Color(0xFFD5E6FF) else Color(0xFF23416B))
                                marginTop(4f)
                            }
                        }
                    }
                    ShowcaseChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("heatmap") },
                        onDataUpdate = { page.refreshChartData("heatmap") },
                    )
                    HeatmapChart {
                        attr {
                            height(246f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("heatmap") }
                            page.currentHeatmapScenario().let { scenario ->
                                seriesName = scenario.seriesName
                                data(*scenario.entries.toTypedArray())
                            }
                            dataTransition { enabled = true; durationMs = 420 }
                            heatmap {
                                cellGap = 4f
                                showValueLabels = false
                                colorScale = heatmapColorScale
                            }
                            tooltip { valueFormatter = { value -> "${value.toInt()} 热度" } }
                        }
                        event {
                            onItemSelected {
                                page.heatmapFeedbackTitle = "已选择 ${it.item.xLabel} ${it.item.yLabel}"
                                page.heatmapFeedbackText = "${it.seriesName}：热度 ${it.item.value.toInt()} · 原始索引 ${it.itemIndex}"
                            }
                        }
                    }
                }
                val radarSectionStart = childrenSize()
                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("雷达图"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("P2 雷达图在极坐标网格中展示多维指标对比。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
                }
                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("服务能力对照")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("同一组能力维度以极坐标网格展开；半透明填充仅强化系列范围，不表达必要信息。")
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(4f)
                            marginBottom(8f)
                        }
                    }
                    View {
                        attr {
                            backgroundColor(accentSoft)
                            borderRadius(14f)
                            padding(12f)
                            marginBottom(10f)
                        }
                        Text {
                            attr {
                                text(page.radarFeedbackTitle)
                                fontSize(13f)
                                fontWeightBold()
                                color(accent)
                            }
                        }
                        Text {
                            attr {
                                text(page.radarFeedbackText)
                                fontSize(12f)
                                color(if (isDark) Color(0xFFD5E6FF) else Color(0xFF23416B))
                                marginTop(4f)
                            }
                        }
                    }
                    ShowcaseChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("radar") },
                        onDataUpdate = { page.refreshChartData("radar") },
                    )
                    RadarChart {
                        attr {
                            height(286f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("radar") }
                            data(*page.radarData.toTypedArray())
                            dataTransition { enabled = false }
                            radar {
                                gridCount = 5
                                maxValue = 100f
                                showPoints = true
                                trackerEnabled = true
                                fillColors = listOf(Color(0x332563EB), Color(0x330D9488))
                            }
                            tooltip { valueFormatter = { value -> "${value.toInt()} 分" } }
                        }
                        event {
                            onTrackerChanged { tracker ->
                                if (tracker == null) {
                                    page.radarFeedbackTitle = "Radar tracking released"
                                    page.radarFeedbackText = "The preview vertex and filled area returned to the source data."
                                } else {
                                    page.radarFeedbackTitle = "Tracking ${tracker.selection.item.label}"
                                    page.radarFeedbackText = "${tracker.selection.seriesName}: ${tracker.previewValue.toInt()} - filled area updates live"
                                }
                            }
                            onItemSelected {
                                page.radarFeedbackTitle = "已选择 ${it.item.label}"
                                page.radarFeedbackText = "${it.seriesName}：${it.item.value.toInt()} 分 · 原始索引 ${it.itemIndex}"
                            }
                        }
                    }
                }

                val areaSectionStart = childrenSize()
                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("面积图"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("P1 面积图复用折线坐标、Tooltip 与长按追踪协议。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
                }
                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("近 8 日成交金额")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("半透明面积强调趋势规模；点击或长按仍返回原始数据项。")
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(4f)
                            marginBottom(8f)
                        }
                    }
                    View {
                        attr {
                            backgroundColor(accentSoft)
                            borderRadius(14f)
                            padding(12f)
                            marginBottom(10f)
                        }
                        Text {
                            attr {
                                text(page.areaFeedbackTitle)
                                fontSize(13f)
                                fontWeightBold()
                                color(accent)
                            }
                        }
                        Text {
                            attr {
                                text(page.areaFeedbackText)
                                fontSize(12f)
                                color(if (isDark) Color(0xFFD5E6FF) else Color(0xFF23416B))
                                marginTop(4f)
                            }
                        }
                    }
                    ShowcaseChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("area") },
                        onDataUpdate = { page.refreshChartData("area") },
                    )
                    AreaChart {
                        attr {
                            height(264f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("area") }
                            data(*page.areaData.toTypedArray())
                            yAxis { tickCount = 5; includeZero = true; min = 0f; max = 260f }
                            line { smooth = true; showPoints = false; lineWidth = 2.5f }
                            dataTransition { enabled = false }
                            area {
                                fillColors = listOf(Color(0x332563EB))
                            }
                            tooltip {
                                trackerEnabled = true
                                keepTrackerOnRelease = false
                                valueFormatter = { value -> "¥${value.toInt()}K" }
                            }
                            crosshair { enabled = true }
                        }
                        event {
                            onItemSelected {
                                page.areaFeedbackTitle = "已选择成交数据"
                                page.areaFeedbackText = "${it.item.label}：¥${it.item.y.toInt()}K"
                            }
                            onTrackerChanged { tracker ->
                                if (tracker == null) {
                                    page.areaFeedbackTitle = "面积追踪已结束"
                                    page.areaFeedbackText = "长按面积边界可重新查看成交金额。"
                                } else {
                                    val selection = tracker.selections.first()
                                    page.areaFeedbackTitle = "正在追踪 ${selection.item.label}"
                                    page.areaFeedbackText = "${selection.seriesName}：¥${selection.item.y.toInt()}K"
                                }
                            }
                        }
                    }
                }

                val trendSectionStart = childrenSize()
                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("多系列折线图"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("多系列平滑折线、原位数据过渡、单指平移、双指捏合缩放与双击复位。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
                }
                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("近 10 日活跃用户")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("实时更新时日期位置不变，点位与连线沿数值方向平滑过渡。")
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(4f)
                            marginBottom(8f)
                        }
                    }
                    View {
                        attr {
                            backgroundColor(accentSoft)
                            borderRadius(14f)
                            padding(12f)
                            marginBottom(10f)
                        }
                        Text {
                            attr {
                                text(page.trendFeedbackTitle)
                                fontSize(13f)
                                fontWeightBold()
                                color(accent)
                            }
                        }
                        Text {
                            attr {
                                text(page.trendFeedbackText)
                                fontSize(12f)
                                color(if (isDark) Color(0xFFD5E6FF) else Color(0xFF23416B))
                                marginTop(4f)
                            }
                        }
                    }
                    RealtimeChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("trend") },
                        onRealtimeUpdate = { page.refreshChartData("trend") },
                    )
                    LineChart {
                        attr {
                            height(292f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("trend") }
                            data(*page.trendData.toTypedArray())
                            yAxis { tickCount = 5; includeZero = false; min = 60f; max = 340f }
                            dataTransition { enabled = false }
                            line { smooth = true; showPoints = true }
                            tooltip {
                                trackerEnabled = true
                                keepTrackerOnRelease = false
                            }
                            interaction {
                                enablePan = true
                                enableZoom = true
                                visibleItemCount = 5
                                minVisibleItemCount = 3
                                maxVisibleItemCount = 8
                            }
                        }
                        event {
                            onItemSelected {
                                page.trendFeedbackTitle = "已选择趋势数据"
                                page.trendFeedbackText = "${it.seriesName} · ${it.item.label}：${it.item.y.toInt()} 活跃用户"
                            }
                            onTrackerChanged { tracker ->
                                if (tracker == null) {
                                    page.trendFeedbackTitle = "追踪已结束"
                                    page.trendFeedbackText = "长按任意日期可重新比较所有系列。"
                                } else {
                                    page.trendFeedbackTitle = "正在追踪 ${tracker.selections.first().item.label}"
                                    page.trendFeedbackText = tracker.selections.joinToString(" · ") {
                                        "${it.seriesName} ${it.item.y.toInt()}"
                                    }
                                }
                            }
                            onViewportChanged { viewport ->
                                page.trendFeedbackTitle = "可视窗口已更新"
                                page.trendFeedbackText = "当前浏览第 ${viewport.startIndex + 1} 至第 ${viewport.endIndex + 1} 个数据点。"
                            }
                        }
                    }
                }

                val categorySectionStart = childrenSize()
                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("分组柱状图"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("分组柱状图、原位数据过渡、数值标签与点击命中，适合渠道和业务指标比较。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
                }
                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("渠道订单量对比")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("实时更新时分类位置不变，两组柱体从基线连续伸缩；点击柱体查看原始数据。")
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(4f)
                            marginBottom(8f)
                        }
                    }
                    View {
                        attr {
                            backgroundColor(accentSoft)
                            borderRadius(14f)
                            padding(12f)
                            marginBottom(10f)
                        }
                        Text {
                            attr {
                                text(page.categoryFeedbackTitle)
                                fontSize(13f)
                                fontWeightBold()
                                color(accent)
                            }
                        }
                        Text {
                            attr {
                                text(page.categoryFeedbackText)
                                fontSize(12f)
                                color(if (isDark) Color(0xFFD5E6FF) else Color(0xFF23416B))
                                marginTop(4f)
                            }
                        }
                    }
                    RealtimeChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("bar") },
                        onRealtimeUpdate = { page.refreshChartData("bar") },
                    )
                    BarChart {
                        attr {
                            height(276f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("bar") }
                            data(*page.categoryData.toTypedArray())
                            yAxis { tickCount = 5; min = 0f; max = 340f }
                            dataTransition { enabled = false }
                            bars { showValueLabels = true; cornerRadius = 5f }
                        }
                        event {
                            onItemSelected {
                                page.categoryFeedbackTitle = "已选择分类数据"
                                page.categoryFeedbackText = "${it.item.label} · ${it.seriesName}：${it.item.value.toInt()} 单"
                            }
                        }
                    }
                }

                val categorySectionEnd = childrenSize()
                val performanceSection = (performanceSectionStart until sparklineSectionStart).map(::getChild)
                val sparklineSection = (sparklineSectionStart until mixedSectionStart).map(::getChild)
                val mixedSection = (mixedSectionStart until pieSectionStart).map(::getChild)
                val pieSection = (pieSectionStart until heatmapSectionStart).map(::getChild)
                val heatmapSection = (heatmapSectionStart until radarSectionStart).map(::getChild)
                val radarSection = (radarSectionStart until areaSectionStart).map(::getChild)
                val areaSection = (areaSectionStart until trendSectionStart).map(::getChild)
                val trendSection = (trendSectionStart until categorySectionStart).map(::getChild)
                val categorySection = (categorySectionStart until categorySectionEnd).map(::getChild)
                listOf(
                    performanceSection,
                    heatmapSection,
                    pieSection,
                    areaSection,
                    sparklineSection,
                    radarSection,
                    mixedSection,
                    categorySection,
                    trendSection,
                ).forEach { section ->
                    section.asReversed().forEach { child ->
                        move(templateChildren().indexOf(child), performanceSectionStart, 1)
                    }
                }

                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("设计与数据约定"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("示例页面也说明组件在产品中如何处理状态与边界。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
                }
                View {
                    attr {
                        backgroundColor(elevatedBackground)
                        borderRadius(16f)
                        padding(14f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("可预期的渲染行为")
                            fontSize(15f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("• 空数据不伪造刻度，并显示空状态\n• NaN 与 Infinity 不参与绘制\n• 选中项失效时自动清理，回调始终对应原始输入索引\n• 主题、网格、坐标轴和 Tooltip 共享同一组视觉令牌")
                            fontSize(12f)
                            color(secondaryText)
                            marginTop(8f)
                        }
                    }
                }

            }
        }
    }

    private fun selectPerformancePointCount(pointCount: Int) {
        performancePointCount = pointCount
        val sampledPointCount = performanceSampleCounts.getValue(pointCount)
        performanceFeedbackTitle = "${formatPointCount(pointCount)} 点数据已加载"
        performanceFeedbackText = if (sampledPointCount < pointCount) {
            "min/max 峰谷采样后绘制 $sampledPointCount 点，点击仍返回原始索引。"
        } else {
            "数据量未超过 240 点预算，保持全部点位。"
        }
    }

}
