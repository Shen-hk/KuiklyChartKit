package com.kuikly.kuiklychartkit

import com.kuikly.kuiklychartkit.base.BasePager
import com.kuikly.kuiklychartkit.chart.AreaChart
import com.kuikly.kuiklychartkit.chart.BarChart
import com.kuikly.kuiklychartkit.chart.BarEntry
import com.kuikly.kuiklychartkit.chart.ChartPoint
import com.kuikly.kuiklychartkit.chart.ChartSeries
import com.kuikly.kuiklychartkit.chart.ChartTheme
import com.kuikly.kuiklychartkit.chart.LineChart
import com.kuikly.kuiklychartkit.chart.LineSampler
import com.kuikly.kuiklychartkit.chart.MixedChart
import com.kuikly.kuiklychartkit.chart.MixedSeriesType
import com.kuikly.kuiklychartkit.chart.PieChart
import com.kuikly.kuiklychartkit.chart.PieEntry
import com.kuikly.kuiklychartkit.chart.SparklineChart
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button

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
    private var mixedFeedbackTitle by observable("等待组合图交互")
    private var mixedFeedbackText by observable("点击柱体或折线点，比较实际收入与目标收入。")
    private var sparklineFeedbackTitle by observable("等待紧凑趋势交互")
    private var sparklineFeedbackText by observable("点击趋势线附近，查看对应日期的支付成功率。")
    private var performancePointCount by observable(100)
    private var performanceFeedbackTitle by observable("100 点 · 原样绘制")
    private var performanceFeedbackText by observable("点击折线可核对采样前的原始数据索引。")
    private var categoryFeedbackTitle by observable("等待分类交互")
    private var categoryFeedbackText by observable("点击柱体查看本月与上月的原始订单量。")

    override fun body(): ViewBuilder {
        val page = this
        val isDark = themeMode == ShowcaseThemeMode.DARK
        val pageBackground = if (isDark) Color(0xFF0B1220) else Color(0xFFF3F6FB)
        val cardBackground = if (isDark) Color(0xFF111C2E) else Color.WHITE
        val elevatedBackground = if (isDark) Color(0xFF17263D) else Color(0xFFEAF2FF)
        val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF172033)
        val secondaryText = if (isDark) Color(0xFFAAB7C9) else Color(0xFF687386)
        val accent = when (themeMode) {
            ShowcaseThemeMode.LIGHT -> Color(0xFF2563EB)
            ShowcaseThemeMode.DARK -> Color(0xFF60A5FA)
            ShowcaseThemeMode.OCEAN -> Color(0xFF0284C7)
            ShowcaseThemeMode.SUNSET -> Color(0xFFF97316)
        }
        val accentSoft = when (themeMode) {
            ShowcaseThemeMode.LIGHT -> Color(0xFFE8F0FF)
            ShowcaseThemeMode.DARK -> Color(0xFF203452)
            ShowcaseThemeMode.OCEAN -> Color(0xFFE0F5FE)
            ShowcaseThemeMode.SUNSET -> Color(0xFFFFEDE3)
        }
        val chartTheme = when (themeMode) {
            ShowcaseThemeMode.LIGHT -> ChartTheme.light()
            ShowcaseThemeMode.DARK -> ChartTheme.dark()
            ShowcaseThemeMode.OCEAN -> ChartTheme.ocean()
            ShowcaseThemeMode.SUNSET -> ChartTheme.sunset()
        }
        return {
            attr { backgroundColor(pageBackground) }
            RouterNavBar {
                attr { title = "KuiklyChartKit Showcase" }
            }
            Scroller {
                attr {
                    flex(1f)
                    padding(16f)
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
                                text("当前展台包含 Sparkline、折线、面积、饼环、组合、分类对比和密集数据浏览；所有示例均由真实 DSL 渲染。")
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
                    LineChart {
                        attr {
                            height(220f)
                            theme = chartTheme
                            data(
                                ChartSeries(
                                    "密集趋势",
                                    page.performanceDataSets.getValue(page.performancePointCount),
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
                    LineChart {
                        attr {
                            height(180f)
                            theme = chartTheme
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
                                    ),
                                ),
                            )
                            yAxis { tickCount = 4; includeZero = false }
                            legend { visible = false }
                            line { smooth = false; showPoints = true }
                            tooltip { enabled = false }
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
                    SparklineChart {
                        attr {
                            height(116f)
                            theme = chartTheme
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
                                    ),
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
                    MixedChart {
                        attr {
                            height(286f)
                            theme = chartTheme
                            barData(
                                ChartSeries(
                                    name = "实际收入",
                                    items = listOf(
                                        BarEntry("周一", 86f),
                                        BarEntry("周二", 112f),
                                        BarEntry("周三", 104f),
                                        BarEntry("周四", 138f),
                                        BarEntry("周五", 151f),
                                    ),
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
                                    ),
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
                    PieChart {
                        attr {
                            height(292f)
                            theme = chartTheme
                            seriesName = "渠道订单"
                            data(
                                PieEntry("推荐", 420f),
                                PieEntry("搜索", 260f),
                                PieEntry("直播", 190f),
                                PieEntry("其他", 130f),
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
                                val total = 1_000f
                                val percentage = (it.item.value / total * 100f).toInt()
                                page.pieFeedbackTitle = "已选择 ${it.item.label}"
                                page.pieFeedbackText = "${it.item.value.toInt()} 单 · 占全部渠道 $percentage%"
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
                    AreaChart {
                        attr {
                            height(264f)
                            theme = chartTheme
                            data(
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
                    LineChart {
                        attr {
                            height(292f)
                            theme = chartTheme
                            data(
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
                    BarChart {
                        attr {
                            height(276f)
                            theme = chartTheme
                            data(
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
