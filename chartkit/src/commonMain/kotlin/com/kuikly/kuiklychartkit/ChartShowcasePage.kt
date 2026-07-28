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

private fun buildPerformancePoints(pointCount: Int): List<ChartPoint> = List(pointCount) { index ->
    ChartPoint(
        x = index.toFloat(),
        y = 180f + ((index % 37) - 18) * 0.8f + index * 0.01f,
        label = "#${index + 1}",
    )
}

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
                    text("数据更新")
                    fontSize(12f)
                    color(Color.WHITE)
                }
            }
            event { click { onDataUpdate() } }
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
    private var trendFeedbackTitle by observable("等待趋势交互")
    private var trendFeedbackText by observable("点击数据点、长按追踪折线，或拖动趋势图浏览更多日期。")
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

    private var chartControlRevision by observable(0)
    private val chartDataVersions = mutableMapOf<String, Int>()
    private val chartDataSeeds = mutableMapOf<String, Int>()
    private val chartEntranceProgress = mutableMapOf<String, Float>()
    private val chartEntranceTimers = mutableMapOf<String, Timer>()

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
        chartDataVersions[chartKey] = (chartDataVersions[chartKey] ?: 0) + 1
        chartDataSeeds[chartKey] = Random.nextInt()
        refreshChartEntrance(chartKey)
    }

    private fun refreshedChartValue(chartKey: String, base: Float, index: Int): Float {
        if (!base.isFinite()) return base
        if ((chartDataVersions[chartKey] ?: 0) == 0) return base
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

    private fun pieTotal(): Float =
        refreshedChartValue("pie", 420f, 0) +
            refreshedChartValue("pie", 260f, 1) +
            refreshedChartValue("pie", 190f, 2) +
            refreshedChartValue("pie", 130f, 3)

    override fun viewDestroyed() {
        chartEntranceTimers.values.forEach { it.cancel() }
        chartEntranceTimers.clear()
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
                            text("当前展台包含 Sparkline、折线、面积、饼环、热力、雷达、组合、分类对比和密集数据浏览；所有示例均由真实 DSL 渲染。")
                                fontSize(12f)
                                color(accent)
                            }
                        }
                    }
                }

                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("性能与边界"); fontSize(18f); fontWeightBold(); color(primaryText) } }
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
                            text("Performance Lab")
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
                                    "绘制 $sampledPointCount 点 · $samplingState",
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
                            yAxis { visible = true; tickCount = 4; includeZero = false }
                            legend { visible = false }
                            line { smooth = false; showPoints = false; lineWidth = 1.5f }
                            tooltip { enabled = true }
                            interaction { maxRenderPointCount = 240 }
                        }
                        event {
                            onItemSelected {
                                page.performanceFeedbackTitle = "命中原始点 #${it.itemIndex + 1}"
                                page.performanceFeedbackText =
                                    "${it.item.label} · 数值 ${it.item.y} · 回调索引 ${it.itemIndex}"
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
                            text("异常数据回归")
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
                        }
                    }
                    }
                }

                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("紧凑指标"); fontSize(18f); fontWeightBold(); color(primaryText) } }
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

                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("目标对照"); fontSize(18f); fontWeightBold(); color(primaryText) } }
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
                            yAxis { tickCount = 5; includeZero = true }
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

                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("占比洞察"); fontSize(18f); fontWeightBold(); color(primaryText) } }
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
                    ShowcaseChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("pie") },
                        onDataUpdate = { page.refreshChartData("pie") },
                    )
                    PieChart {
                        attr {
                            height(292f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("pie") }
                            seriesName = "渠道订单"
                            data(
                                PieEntry("推荐", page.refreshedChartValue("pie", 420f, 0)),
                                PieEntry("搜索", page.refreshedChartValue("pie", 260f, 1)),
                                PieEntry("直播", page.refreshedChartValue("pie", 190f, 2)),
                                PieEntry("其他", page.refreshedChartValue("pie", 130f, 3)),
                            )
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

                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("多维洞察"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("P2 热力图与雷达图复用 Theme、Tooltip 和原始索引回调。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
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
                            text("客服时段热度")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("GitHub 贡献图式绿阶表达日期与时段的相对咨询热度；点击单元格可查看实际值。")
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
                            seriesName = "客服咨询"
                            data(
                                *listOf(
                                HeatmapEntry("周一", "09:00", 42f), HeatmapEntry("周二", "09:00", 58f),
                                HeatmapEntry("周三", "09:00", 35f), HeatmapEntry("周四", "09:00", 74f),
                                HeatmapEntry("周五", "09:00", 66f),
                                HeatmapEntry("周一", "12:00", 81f), HeatmapEntry("周二", "12:00", 94f),
                                HeatmapEntry("周三", "12:00", 72f), HeatmapEntry("周四", "12:00", 88f),
                                HeatmapEntry("周五", "12:00", 97f),
                                HeatmapEntry("周一", "15:00", 63f), HeatmapEntry("周二", "15:00", 76f),
                                HeatmapEntry("周三", "15:00", 69f), HeatmapEntry("周四", "15:00", 83f),
                                HeatmapEntry("周五", "15:00", 91f),
                                HeatmapEntry("周一", "18:00", 39f), HeatmapEntry("周二", "18:00", 52f),
                                HeatmapEntry("周三", "18:00", 47f), HeatmapEntry("周四", "18:00", 61f),
                                HeatmapEntry("周五", "18:00", 56f),
                                ).mapIndexed { index, entry ->
                                    entry.copy(value = page.refreshedChartValue("heatmap", entry.value, index))
                                }.toTypedArray(),
                            )
                            heatmap {
                                cellGap = 4f
                                showValueLabels = false
                                colorScale = heatmapColorScale
                            }
                            tooltip { valueFormatter = { value -> "${value.toInt()} 次" } }
                        }
                        event {
                            onItemSelected {
                                page.heatmapFeedbackTitle = "已选择 ${it.item.xLabel} ${it.item.yLabel}"
                                page.heatmapFeedbackText = "${it.seriesName}：${it.item.value.toInt()} 次 · 原始索引 ${it.itemIndex}"
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
                            data(
                                ChartSeries(
                                    "当前表现",
                                    listOf(
                                        RadarEntry("响应", 86f), RadarEntry("解决", 72f),
                                        RadarEntry("满意度", 91f), RadarEntry("覆盖", 68f),
                                        RadarEntry("成本", 76f),
                                    ).mapIndexed { index, entry ->
                                        entry.copy(value = page.refreshedChartValue("radar", entry.value, index))
                                    },
                                ),
                                ChartSeries(
                                    "目标水平",
                                    listOf(
                                        RadarEntry("响应", 80f), RadarEntry("解决", 82f),
                                        RadarEntry("满意度", 88f), RadarEntry("覆盖", 84f),
                                        RadarEntry("成本", 72f),
                                    ).mapIndexed { index, entry ->
                                        entry.copy(value = page.refreshedChartValue("radar", entry.value, index + 5))
                                    },
                                ),
                            )
                            radar {
                                gridCount = 5
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

                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("累计趋势"); fontSize(18f); fontWeightBold(); color(primaryText) } }
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
                            data(
                                ChartSeries(
                                    name = "成交金额",
                                    items = listOf(
                                        ChartPoint(1f, 82f, "7/20"), ChartPoint(2f, 108f, "7/21"),
                                        ChartPoint(3f, 96f, "7/22"), ChartPoint(4f, 142f, "7/23"),
                                        ChartPoint(5f, 168f, "7/24"), ChartPoint(6f, 154f, "7/25"),
                                        ChartPoint(7f, 201f, "7/26"), ChartPoint(8f, 226f, "今天"),
                                    ).mapIndexed { index, point ->
                                        point.copy(y = page.refreshedChartValue("area", point.y, index))
                                    },
                                ),
                            )
                            line { smooth = true; showPoints = false; lineWidth = 2.5f }
                            area {
                                fillColors = listOf(Color(0x332563EB))
                            }
                            tooltip {
                                trackerEnabled = true
                                keepTrackerOnRelease = false
                                valueFormatter = { value -> "¥${value.toInt()}K" }
                            }
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

                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("趋势分析"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("多系列平滑折线、点击 Tooltip、长按追踪与横向平移。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
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
                            text("长按图表比较“本周”和“上周”；左右拖动可切换 5 天可视窗口。")
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
                    ShowcaseChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("trend") },
                        onDataUpdate = { page.refreshChartData("trend") },
                    )
                    LineChart {
                        attr {
                            height(292f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("trend") }
                            data(
                                ChartSeries(
                                    name = "本周",
                                    items = listOf(
                                        ChartPoint(1f, 128f, "7/18"), ChartPoint(2f, 166f, "7/19"),
                                        ChartPoint(3f, 151f, "7/20"), ChartPoint(4f, 207f, "7/21"),
                                        ChartPoint(5f, 232f, "7/22"), ChartPoint(6f, 218f, "7/23"),
                                        ChartPoint(7f, 276f, "7/24"), ChartPoint(8f, 245f, "7/25"),
                                        ChartPoint(9f, 294f, "7/26"), ChartPoint(10f, 318f, "今天"),
                                    ).mapIndexed { index, point ->
                                        point.copy(y = page.refreshedChartValue("trend", point.y, index))
                                    },
                                ),
                                ChartSeries(
                                    name = "上周",
                                    items = listOf(
                                        ChartPoint(1f, 105f, "7/18"), ChartPoint(2f, 134f, "7/19"),
                                        ChartPoint(3f, 146f, "7/20"), ChartPoint(4f, 171f, "7/21"),
                                        ChartPoint(5f, 196f, "7/22"), ChartPoint(6f, 189f, "7/23"),
                                        ChartPoint(7f, 221f, "7/24"), ChartPoint(8f, 214f, "7/25"),
                                        ChartPoint(9f, 242f, "7/26"), ChartPoint(10f, 257f, "今天"),
                                    ).mapIndexed { index, point ->
                                        point.copy(y = page.refreshedChartValue("trend", point.y, index + 10))
                                    },
                                ),
                            )
                            yAxis { tickCount = 5; includeZero = false }
                            line { smooth = true; showPoints = true }
                            tooltip {
                                trackerEnabled = true
                                keepTrackerOnRelease = false
                            }
                            interaction {
                                enablePan = true
                                visibleItemCount = 5
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

                View {
                    attr { marginTop(8f); marginBottom(9f) }
                    Text { attr { text("分类对比"); fontSize(18f); fontWeightBold(); color(primaryText) } }
                    Text { attr { text("分组柱状图、数值标签与点击命中，适合渠道和业务指标比较。"); fontSize(12f); color(secondaryText); marginTop(3f) } }
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
                            text("每个分类显示本月与上月两组柱；点击柱体查看原始数据。")
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
                    ShowcaseChartControls(
                        accent = accent,
                        elevatedBackground = elevatedBackground,
                        primaryText = primaryText,
                        onEntranceRefresh = { page.refreshChartEntrance("bar") },
                        onDataUpdate = { page.refreshChartData("bar") },
                    )
                    BarChart {
                        attr {
                            height(276f)
                            theme = chartTheme
                            entrance { progress = page.chartEntranceProgress("bar") }
                            data(
                                ChartSeries(
                                    name = "本月",
                                    items = listOf(
                                        BarEntry("搜索", 186f), BarEntry("推荐", 248f), BarEntry("活动", 164f),
                                        BarEntry("直播", 292f), BarEntry("社交", 221f),
                                    ).mapIndexed { index, entry ->
                                        entry.copy(value = page.refreshedChartValue("bar", entry.value, index))
                                    },
                                ),
                                ChartSeries(
                                    name = "上月",
                                    items = listOf(
                                        BarEntry("搜索", 159f), BarEntry("推荐", 214f), BarEntry("活动", 181f),
                                        BarEntry("直播", 238f), BarEntry("社交", 196f),
                                    ).mapIndexed { index, entry ->
                                        entry.copy(value = page.refreshedChartValue("bar", entry.value, index + 5))
                                    },
                                ),
                            )
                            yAxis { tickCount = 5 }
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
