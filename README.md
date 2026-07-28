# KuiklyChartKit

KuiklyChartKit 是面向通用数据可视化场景的 Kuikly 跨端图表组件库，对应 [KuiklyUI Issue #1477](https://github.com/Tencent-TDS/KuiklyUI/issues/1477)。总体目标是交付可复用、可扩展、可验证的图表基础能力，而不是面向某个行业或业务 Demo 的专用图表库。

当前已完成 P0/M0 的折线图、柱状图、Kuikly 官方风格声明式 DSL、点击选择和基础 Tooltip；长按追踪、水平平移、面积图、饼环图、组合图和 Sparkline 已完成，P1 密集数据采样、异常数据回归、本机自动化证据及人工验收也已收口。P2 首批已接入热力图、雷达图和一次性图片导出封装；导出仍为实验 API，成为发布候选前仍需完成 Kuikly 升级，以及 Android、iOS、OpenHarmony 的正式截图/录屏、内存检查与设备记录。

> 当前基线：Kuikly `2.7.0` · Kotlin `2.1.21`

## 当前完成度

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| `LineChart` | P0 已完成 | 多系列、直线/平滑、点、轴、网格、图例、空态、点击选择和基础 Tooltip。 |
| `AreaChart` | P1 首批已完成 | 复用折线坐标、点击、长按追踪和平移协议；支持平滑边界、零基线和分系列半透明填充。 |
| `PieChart` | P1 第二批已完成 | 独立极坐标布局；支持饼/环切换、扇区间隙、百分比标签、中心汇总和点击选择。 |
| `HeatmapChart` | P2 首批已完成 | 二维分类网格、按值离散颜色桶、长标签截断、点击单元格和原始索引回调。 |
| `RadarChart` | P2 首批已完成 | 多系列共享维度极坐标网格；支持可选半透明填充、顶点点击和非法值断点。 |
| `MixedChart` | P1 第三批已完成 | 分类柱与分类折线共享坐标、图例和 Tooltip；选择结果明确区分柱/线来源。 |
| `SparklineChart` | P1 第四批已完成 | 单系列紧凑趋势；默认隐藏轴、网格、图例、数据点和 Tooltip，可选点击选择。 |
| `BarChart` | P0 已完成 | 分类柱、并列多系列、正负值零基线、值标签、圆角、点击选择和基础 Tooltip。 |
| Kuikly DSL | P0 已完成 | `ViewContainer` 扩展 + `ComposeView<Attr, Event>`；错误配置给出字段级异常。 |
| Android Showcase | 已验证 | `chart_showcase` 页面已接入路由，Debug APK 构建通过。 |
| H5 Showcase | 实验性验证 | 使用同一份 `commonMain` 页面完成浏览器验证；不替代正式平台支持矩阵。 |
| 折线长按追踪 | P1 已验收 | 默认关闭；按最近 X 槽聚合同槽多系列，并保留原始数据索引。 |
| 折线水平平移 | P1 已验收 | 默认关闭；可配置可视点窗口，范围变化返回钳制后的原始索引。 |
| 密集数据采样 | P1 已完成 | 折线、面积与 Sparkline 共用 min/max 桶采样；保留峰谷、坏点分段和原始索引，可配置每系列绘制上限。 |
| 图片导出 | P2 实验性 | `exportImage` 默认返回一次性 `DATA_URI`，不保留缓存型图像；当前基线尚无三端真机验证，不能宣称跨端导出支持。 |
| 缩放 | 候选扩展 | 需通过能力探针后再进入公共 API。 |
| Showcase/性能证据 | P1/P2 已验证 | Performance Lab 覆盖 100/1,000/5,000 点；38 项测试、Android 单测与包含 P2 卡片的 H5 发布构建通过。正式三端视觉证据待补。 |

已实现功能与验证记录见 [M0 实现与验收记录](docs/11-m0-implementation-status.md) 和 [P1 实现与验收记录](docs/12-p1-implementation-status.md)；完整范围和路线见 [开发文档索引](docs/README.md)。

## 构建

在 Windows PowerShell 的仓库根目录执行：

```powershell
# 自动化测试
.\gradlew :chartkit:testDebugUnitTest

# Android Debug APK
.\gradlew :androidApp:assembleDebug

# 实验性 H5 Showcase
.\gradlew :h5App:publishChartShowcase
```

Android APK 位于：

```text
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

## DSL 示例

```kotlin
LineChart {
    attr {
        data(
            ChartSeries(
                name = "访问量",
                items = listOf(
                    ChartPoint(1f, 120f, "周一"),
                    ChartPoint(2f, 168f, "周二"),
                    ChartPoint(3f, 142f, "周三"),
                ),
            ),
        )
        theme = ChartTheme.ocean()
        yAxis { tickCount = 5; includeZero = false }
        line { smooth = true; showPoints = true }
        tooltip { enabled = true }
    }
    event { onItemSelected { selection -> onPointSelected(selection.item) } }
}
```

```kotlin
BarChart {
    attr {
        data(
            ChartSeries(
                name = "转化",
                items = listOf(
                    BarEntry("A", 86f),
                    BarEntry("B", 132f),
                    BarEntry("C", 109f),
                ),
            ),
        )
        bars { barWidthRatio = 0.68f; showValueLabels = true }
    }
    event { onItemSelected { selection -> onBarSelected(selection.item) } }
}
```

```kotlin
AreaChart {
    attr {
        data(
            ChartSeries(
                name = "成交金额",
                items = listOf(
                    ChartPoint(1f, 82f, "周一"),
                    ChartPoint(2f, 108f, "周二"),
                    ChartPoint(3f, 142f, "周三"),
                ),
            ),
        )
        line { smooth = true; showPoints = false }
        area { fillColors = listOf(Color(0x332563EB)) }
        tooltip { trackerEnabled = true }
    }
    event { onItemSelected { selection -> onPointSelected(selection.item) } }
}
```

```kotlin
PieChart {
    attr {
        seriesName = "渠道订单"
        data(
            PieEntry("推荐", 420f),
            PieEntry("搜索", 260f),
            PieEntry("直播", 190f),
            PieEntry("其他", 130f),
        )
        pie {
            innerRadiusRatio = 0.58f
            gapAngleDegrees = 2f
            centerLabel = "订单总量"
        }
    }
    event { onItemSelected { selection -> onSliceSelected(selection.item) } }
}
```

```kotlin
MixedChart {
    attr {
        barData(ChartSeries("实际收入", actualRevenue))
        lineData(ChartSeries("目标收入", targetRevenue))
        bars { showValueLabels = false }
        line { smooth = true; showPoints = true }
        tooltip { valueFormatter = { value -> "¥${value.toInt()}K" } }
    }
    event { onItemSelected { selection -> onMixedSelected(selection) } }
}
```

```kotlin
SparklineChart {
    attr {
        data(ChartSeries("支付成功率", successRatePoints))
        selectable = true
        tooltip { enabled = true; valueFormatter = { value -> "$value%" } }
    }
    event { onItemSelected { selection -> onRateSelected(selection.item) } }
}
```

```kotlin
HeatmapChart {
    attr {
        seriesName = "客服咨询"
        data(
            HeatmapEntry("周一", "09:00", 42f),
            HeatmapEntry("周一", "12:00", 81f),
            HeatmapEntry("周二", "09:00", 58f),
            HeatmapEntry("周二", "12:00", 94f),
        )
        heatmap { cellGap = 4f }
    }
    event { onItemSelected { selection -> onCellSelected(selection.item) } }
}
```

```kotlin
RadarChart {
    attr {
        data(
            ChartSeries("当前", listOf(
                RadarEntry("响应", 86f), RadarEntry("解决", 72f), RadarEntry("满意度", 91f),
            )),
            ChartSeries("目标", listOf(
                RadarEntry("响应", 80f), RadarEntry("解决", 82f), RadarEntry("满意度", 88f),
            )),
        )
        radar { gridCount = 5; fillColors = listOf(Color(0x332563EB), Color(0x330D9488)) }
    }
    event { onItemSelected { selection -> onMetricSelected(selection.item) } }
}
```

## 项目约束

- 公共 API 使用 `commonMain` Kotlin 模型，不能暴露平台原生类型。
- 相同输入应保持一致的数据域、刻度、命中和回调语义；平台字体与抗锯齿差异可接受。
- 任何未来扩展都必须同步公共 API/KDoc、示例、自动化测试和平台验证证据。
- 图片导出仅可通过标注为实验性的 `exportImage` 调用；在三端升级、回归和内存验证完成前，不得将其标记为正式跨端能力。
