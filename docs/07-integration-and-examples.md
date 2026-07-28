# 接入与示例

## Showcase 而非零散测试页

组件位于 `chartkit`，Android、iOS、OpenHarmony 宿主分别位于 `androidApp`、`iosApp`、`ohosApp`。所有正式 Demo 页面均放在 `chartkit/src/commonMain`，确保多端运行同一份图表 DSL。H5 Showcase 可复用该页面用于工程验证，但不改变正式支持范围。

最终提供一个 `Chart Showcase` 入口，至少包含：

1. **Overview Dashboard**：亮/暗主题、KPI 趋势、环比柱图与 Sparkline；体现产品级布局。
2. **Data Explorer**：多系列平滑折线、点击 Tooltip、标签格式化与密集数据策略。
3. **Comparison Lab**：单组、分组、堆叠、正负值柱图，以及可点击图例。
4. **Chart Gallery**：面积、饼环、组合图，以统一主题与格式化器展示扩展性。
5. **Live Update**：模拟新增、替换和异常数据，验证重绘、动画与选中态清理。
6. **Performance Lab**：100、1,000、5,000 点切换，展示点数、采样状态和测量结果。

## 折线图示例

下列代码是目标 API 形态；实现后必须编译运行并与 KDoc 一致。

```kotlin
LineChart {
    attr {
        data(ChartSeries("访问量", listOf(
            ChartPoint(1.0, 120.0, "周一"),
            ChartPoint(2.0, 168.0, "周二"),
            ChartPoint(3.0, 142.0, "周三"),
        ), Color(0xFF2F80ED)))
        xAxis { labelFormatter { point, _ -> point.label.orEmpty() } }
        yAxis { tickCount = 4; includeZero = false }
        line { smooth = true; showPoints = true }
        tooltip { enabled = true }
    }
    event { onItemSelected { selection -> println(selection.item) } }
}
```

## 柱状图示例

```kotlin
BarChart {
    attr {
        data(ChartSeries("转化", listOf(
            BarEntry(86.0, "A"), BarEntry(132.0, "B"), BarEntry(109.0, "C")
        ), Color(0xFF27AE60)))
        yAxis { includeZero = true; tickCount = 5 }
        bars { mode = BarMode.GROUPED; showValueLabels = true }
    }
    event { onItemSelected { selection -> onBarChosen(selection.item) } }
}
```

## 面积图示例

`AreaChart` 是 P1 首批扩展，使用与折线图相同的数据、Tooltip 和事件协议，并默认将零纳入 Y 轴范围。

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
    event { onItemSelected { selection -> onAmountSelected(selection.item) } }
}
```

## 饼环图示例

`PieChart` 不提供坐标轴配置。`innerRadiusRatio=0` 时渲染饼图，大于零时渲染环图；点击回调保留过滤前的原始数据索引。

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
            startAngleDegrees = -90f
            gapAngleDegrees = 2f
            centerLabel = "订单总量"
        }
        tooltip { valueFormatter = { value -> "${value.toInt()} 单" } }
    }
    event { onItemSelected { selection -> onChannelSelected(selection.item) } }
}
```

## 组合图示例

`MixedChart` 使用同一个分类槽和 Y 轴绘制柱系列与线系列。两类数据都使用 `ChartSeries<BarEntry>`，选择回调通过 `seriesType` 区分渲染来源。

```kotlin
MixedChart {
    attr {
        barData(
            ChartSeries(
                "实际收入",
                listOf(BarEntry("周一", 86f), BarEntry("周二", 112f)),
            ),
        )
        lineData(
            ChartSeries(
                "目标收入",
                listOf(BarEntry("周一", 96f), BarEntry("周二", 105f)),
            ),
        )
        bars { showValueLabels = false }
        line { smooth = true; showPoints = true }
        tooltip { valueFormatter = { value -> "¥${value.toInt()}K" } }
    }
    event {
        onItemSelected { selection ->
            when (selection.seriesType) {
                MixedSeriesType.BAR -> onActualSelected(selection.item)
                MixedSeriesType.LINE -> onTargetSelected(selection.item)
            }
        }
    }
}
```

## Sparkline 示例

`SparklineChart` 复用折线数据和事件类型，但默认是单系列、无轴、无网格、无图例且不可点击的紧凑趋势。

```kotlin
SparklineChart {
    attr {
        data(
            ChartSeries(
                "支付成功率",
                listOf(
                    ChartPoint(1f, 96.8f, "周一"),
                    ChartPoint(2f, 97.1f, "周二"),
                    ChartPoint(3f, 98.6f, "今天"),
                ),
            ),
        )
        selectable = true
        tooltip {
            enabled = true
            valueFormatter = { value -> "$value%" }
        }
    }
    event { onItemSelected { selection -> onRateSelected(selection.item) } }
}
```

## 热力图示例

`HeatmapChart` 使用平面分类网格。`xLabel/yLabel` 组合在一次 `data(...)` 调用中必须唯一；非有限值不绘制且不会改变其他单元格的原始索引。

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
        heatmap {
            cellGap = 4f
            colorScale = listOf(
                Color(0xFF9BE9A8), Color(0xFF40C463),
                Color(0xFF30A14E), Color(0xFF216E39),
            )
        }
        tooltip { valueFormatter = { value -> "${value.toInt()} 次" } }
    }
    event { onItemSelected { selection -> onTimeSlotSelected(selection.item) } }
}
```

## 雷达图示例

`RadarChart` 的每个非空系列必须有至少三个维度，且维度标签顺序完全一致。负值和非有限值会成为断点而不导致渲染失败。

```kotlin
RadarChart {
    attr {
        data(
            ChartSeries("当前", listOf(
                RadarEntry("响应", 86f), RadarEntry("解决", 72f), RadarEntry("满意度", 91f),
                RadarEntry("覆盖", 68f), RadarEntry("成本", 76f),
            )),
            ChartSeries("目标", listOf(
                RadarEntry("响应", 80f), RadarEntry("解决", 82f), RadarEntry("满意度", 88f),
                RadarEntry("覆盖", 84f), RadarEntry("成本", 72f),
            )),
        )
        radar {
            gridCount = 5
            fillColors = listOf(Color(0x332563EB), Color(0x330D9488))
        }
    }
    event { onItemSelected { selection -> onMetricSelected(selection.item) } }
}
```

## 实验性导出

图片导出尚未通过 Android、iOS、OpenHarmony 的正式验证，调用方必须显式 opt-in。默认 `DATA_URI` 不创建缓存型输出，回调内消费结果即可；若选择 `FILE`，由宿主负责文件清理与分享。

```kotlin
@OptIn(ExperimentalChartImageExportApi::class)
fun shareChart(chart: LineChartView) {
    chart.exportImage { result ->
        if (result.isSuccess) shareDataUri(result.data.orEmpty())
        else showExportError(result.message.orEmpty())
    }
}
```

## 运行与接入检查清单

- 依赖 `chartkit` 模块或已发布的同版本制品。
- 把图表放入有确定尺寸的 Kuikly 容器；零尺寸容器不会产生可见图形。
- 网络加载、错误和重试由业务层处理，再向图表传入不可变数据列表。
- 每个正式平台的示例 README 附截图、交互录屏和最低支持版本；不得只展示随机数据或空白 Canvas。
- 若使用 H5 Showcase，明确它是实验性验证，并记录构建命令、浏览器版本和已知差异。
- 图片导出在完成 Kuikly 升级、三端截图/录屏和内存回归前保持实验性，不能写入正式支持矩阵。
