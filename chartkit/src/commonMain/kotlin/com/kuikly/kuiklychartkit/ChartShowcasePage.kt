package com.kuikly.kuiklychartkit

import com.kuikly.kuiklychartkit.base.BasePager
import com.kuikly.kuiklychartkit.chart.BarChart
import com.kuikly.kuiklychartkit.chart.BarEntry
import com.kuikly.kuiklychartkit.chart.ChartPoint
import com.kuikly.kuiklychartkit.chart.ChartSeries
import com.kuikly.kuiklychartkit.chart.ChartTheme
import com.kuikly.kuiklychartkit.chart.LineChart
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

private const val CHART_SHOWCASE_PAGE = "chart_showcase"

@Page(CHART_SHOWCASE_PAGE, supportInLocal = true)
internal class ChartShowcasePage : BasePager() {
    private var darkMode by observable(false)
    private var selectionText by observable("点击数据点或柱体查看详情")

    override fun body(): ViewBuilder {
        val page = this
        val pageBackground = if (darkMode) Color(0xFF0B1220) else Color(0xFFF2F5F9)
        val cardBackground = if (darkMode) Color(0xFF111827) else Color.WHITE
        val primaryText = if (darkMode) Color(0xFFF8FAFC) else Color(0xFF172033)
        val secondaryText = if (darkMode) Color(0xFF9CA3AF) else Color(0xFF687386)
        val chartTheme = if (darkMode) ChartTheme.dark() else ChartTheme.light()
        return {
            attr { backgroundColor(pageBackground) }
            RouterNavBar {
                attr { title = "KuiklyChartKit · P0 Showcase" }
            }
            Scroller {
                attr {
                    flex(1f)
                    padding(16f)
                }

                View {
                    attr {
                        backgroundColor(cardBackground)
                        borderRadius(16f)
                        padding(18f)
                        marginBottom(14f)
                    }
                    Text {
                        attr {
                            text("数据洞察面板")
                            fontSize(22f)
                            fontWeightBold()
                            color(primaryText)
                        }
                    }
                    Text {
                        attr {
                            text("跨端 Canvas · 官方风格 DSL · 点击交互")
                            fontSize(13f)
                            color(secondaryText)
                            marginTop(6f)
                        }
                    }
                    View {
                        attr {
                            marginTop(14f)
                            paddingTop(9f)
                            paddingBottom(9f)
                            paddingLeft(14f)
                            paddingRight(14f)
                            borderRadius(18f)
                            backgroundColor(if (page.darkMode) Color(0xFF263449) else Color(0xFFE8F0FF))
                            alignSelfFlexStart()
                        }
                        Text {
                            attr {
                                text(if (page.darkMode) "切换到浅色主题" else "切换到深色主题")
                                color(if (page.darkMode) Color(0xFFBFDBFE) else Color(0xFF1D4ED8))
                                fontSize(13f)
                                fontWeightMedium()
                            }
                        }
                        event { click { page.darkMode = !page.darkMode } }
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
                            text("近 7 日活跃趋势")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                            marginBottom(4f)
                        }
                    }
                    Text {
                        attr {
                            text("平滑多系列折线 · 自动刻度 · 防重叠标签")
                            fontSize(12f)
                            color(secondaryText)
                            marginBottom(8f)
                        }
                    }
                    LineChart {
                        attr {
                            height(280f)
                            theme = chartTheme
                            data(
                                ChartSeries(
                                    name = "本周",
                                    items = listOf(
                                        ChartPoint(1f, 128f, "周一"),
                                        ChartPoint(2f, 166f, "周二"),
                                        ChartPoint(3f, 151f, "周三"),
                                        ChartPoint(4f, 207f, "周四"),
                                        ChartPoint(5f, 232f, "周五"),
                                        ChartPoint(6f, 218f, "周六"),
                                        ChartPoint(7f, 276f, "周日"),
                                    ),
                                ),
                                ChartSeries(
                                    name = "上周",
                                    items = listOf(
                                        ChartPoint(1f, 105f, "周一"),
                                        ChartPoint(2f, 134f, "周二"),
                                        ChartPoint(3f, 146f, "周三"),
                                        ChartPoint(4f, 171f, "周四"),
                                        ChartPoint(5f, 196f, "周五"),
                                        ChartPoint(6f, 189f, "周六"),
                                        ChartPoint(7f, 221f, "周日"),
                                    ),
                                ),
                            )
                            yAxis { tickCount = 5; includeZero = false }
                            line { smooth = true; showPoints = true }
                        }
                        event {
                            onItemSelected {
                                page.selectionText = "${it.seriesName} · ${it.item.label}: ${it.item.y.toInt()}"
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
                            text("渠道订单对比")
                            fontSize(17f)
                            fontWeightBold()
                            color(primaryText)
                            marginBottom(4f)
                        }
                    }
                    Text {
                        attr {
                            text("圆角柱体 · 数值标签 · 点击命中")
                            fontSize(12f)
                            color(secondaryText)
                            marginBottom(8f)
                        }
                    }
                    BarChart {
                        attr {
                            height(270f)
                            theme = chartTheme
                            data(
                                ChartSeries(
                                    name = "订单量",
                                    items = listOf(
                                        BarEntry("搜索", 186f),
                                        BarEntry("推荐", 248f),
                                        BarEntry("活动", 164f),
                                        BarEntry("直播", 292f),
                                        BarEntry("社交", 221f),
                                    ),
                                    color = Color(0xFF0D9488),
                                )
                            )
                            yAxis { tickCount = 5 }
                            bars { showValueLabels = true; cornerRadius = 5f }
                        }
                        event {
                            onItemSelected {
                                page.selectionText = "${it.item.label} · ${it.seriesName}: ${it.item.value.toInt()}"
                            }
                        }
                    }
                }

                View {
                    attr {
                        backgroundColor(if (page.darkMode) Color(0xFF1B2940) else Color(0xFFEAF2FF))
                        borderRadius(12f)
                        padding(14f)
                        marginBottom(28f)
                    }
                    Text {
                        attr {
                            text(page.selectionText)
                            color(if (page.darkMode) Color(0xFFBFDBFE) else Color(0xFF1D4ED8))
                            fontSize(13f)
                        }
                    }
                }
            }
        }
    }
}
