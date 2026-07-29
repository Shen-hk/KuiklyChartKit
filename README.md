# KuiklyChartKit

KuiklyChartKit 是面向通用数据可视化场景的 Kuikly 跨端图表组件库，对应 [KuiklyUI Issue #1477](https://github.com/Tencent-TDS/KuiklyUI/issues/1477)。总体目标是交付可复用、可扩展、可验证的图表基础能力，而不是面向某个行业或业务 Demo 的专用图表库。

当前能力按“已验证 / 实现中 / 候选”分级展示，并把代码、测试和平台证据分开陈述。P0/P1 已覆盖折线、柱状、面积、饼环、组合与 Sparkline；P2 已提供热力、雷达和一次性图片导出封装。图片导出仍是实验 API，P3 手势也尚未取得 Android、iOS、OpenHarmony 的完整真机证据，因此不宣称三端正式可用。

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
| P3-1 手势视口 | 实现中 | `ChartGestureController` 作为纯 Kotlin 内核处理 pan、pinch focal 和双击复位；`commonTest` 覆盖钳制、焦点、复位、tracker release 和非有限值。仍缺 Android 真机、APK 与 iOS/OpenHarmony 证据。详见 [P3 路线](docs/15-p3-interaction-data-roadmap.md)。 |
| P3-2 Crosshair / Brush | 候选，默认关闭 | 已明确为不抢占点击、追踪和平移的独立状态机；先完成 P3-1 三端验收，再交付可选十字准星、区间框选、缩放至选区和清除。详见 [P3 路线](docs/15-p3-interaction-data-roadmap.md)。 |
| 数据更新动画 | 已实现，补强测试 | 兼容快照仅对 Y 值插值；删除/重排 series 或非有限值立即切换，极值插值以 `Double` 中间值避免溢出。 |
| 证据与限制 | 部分已验证 | Performance Lab 覆盖 100/1,000/5,000 点；自动化测试、Android 单测和 H5 发布构建有记录。正式三端视觉、手势和导出证据仍待补，详见 [验收记录](docs/12-p1-implementation-status.md) 与 [P2 状态](docs/13-p2-implementation-status.md)。 |

### 能力等级与证据

- **已验证**：至少有公共 DSL、示例、自动化测试和对应构建记录；不等同于所有正式平台均已验收。
- **实现中**：代码与 commonTest 已进入仓库，但关键平台证据或 API 稳定性尚未收口。
- **候选**：已有规格与验收条件，尚未作为可承诺功能发布。

证据索引见 [M0 实现与验收记录](docs/11-m0-implementation-status.md)、[P1 实现与验收记录](docs/12-p1-implementation-status.md)、[P2 实现与验收记录](docs/13-p2-implementation-status.md)、[P3 路线](docs/15-p3-interaction-data-roadmap.md) 和 [测试计划](docs/08-test-plan.md)。

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
