# DSL 规范

## 1. 契约目标

DSL 是组件最重要的兼容面：调用方只需声明数据与外观，无需关心 Canvas、像素坐标或平台差异。它遵循 Kuikly 官方的 ComposeView 习惯：扩展函数创建组件，`attr {}` 管理状态和视觉配置，`event {}` 管理外部回调。以下是首版的公共契约；实现时应以此命名并补充 KDoc，变更须走语义化版本升级。

## 2. 通用结构

```kotlin
LineChart {
    attr {
        data(ChartSeries("活跃用户", weeklyPoints, Color(0xFF2F80ED)))
        xAxis { labelFormatter { point, _ -> point.label.orEmpty() } }
        yAxis { tickCount = 5; includeZero = false }
        grid { visible = true; color = Color(0xFFE9EDF3) }
        line { smooth = true; showPoints = true; showValueLabels = false }
        tooltip {
            enabled = true
            trackerEnabled = true // 实验性，默认 false
            keepTrackerOnRelease = false
        }
        interaction {
            enablePan = true // 实验性，默认 false
            visibleItemCount = 20
            maxRenderPointCount = 240 // 0 按绘图区宽度自动计算
        }
    }
    event { onItemSelected { selection -> onPointSelected(selection) } }
}
```

`BarChart` 与 `AreaChart` 复用笛卡尔坐标、网格、Tooltip 与主题。`MixedChart` 使用 `barData` 和 `lineData` 接收分类系列，两类系列按输入索引共享分类槽和 Y 轴；`PieChart` 只复用主题、Tooltip、事件与格式化器，不伪造坐标轴 API。`HeatmapChart` 使用 `HeatmapEntry(xLabel, yLabel, value)` 构建二维分类网格；`RadarChart` 使用 `ChartSeries<RadarEntry>`，所有非空系列必须共享至少三个、顺序一致且唯一的维度标签。

## 3. 数据模型

| 类型 | 必填字段 | 语义 |
| --- | --- | --- |
| `ChartPoint` | `x: Double`、`y: Double` | 折线数据点；`label` 可选，用作 X 轴显示。 |
| `BarEntry` | `value: Double`、`label: String` | 单组柱的分类数据。 |
| `PieEntry` | `label: String`、`value: Double`、`color?` | 饼环扇区；非有限值、零和负值不参与布局。 |
| `HeatmapEntry` | `xLabel: String`、`yLabel: String`、`value: Float`、`color?` | 二维分类单元格；相同坐标组合在 DSL 中拒绝，非有限值在渲染时跳过。 |
| `RadarEntry` | `label: String`、`value: Float` | 雷达维度值；负值和非有限值成为断点，保留其他顶点的原始索引。 |
| `ChartSeries<T>` | `name`、`items` | 同图多系列的数据与图例标识。 |
| `ChartSelection<T>` | `seriesIndex`、`itemIndex`、`item` | 点击命中后的稳定回调载荷。 |
| `MixedChartSelection` | `seriesType`、`seriesIndex`、`itemIndex`、`item` | 组合图回调；系列索引分别属于柱或线系列列表。 |
| `ChartViewport` | `startIndex`、`endIndex`、`scale` | 候选密集数据浏览的可恢复状态；尚未成为 P0 契约。 |

折线图支持多系列；柱状图 P0 支持单系列，后续可显式支持 `GROUPED` 与 `STACKED`。模式切换是显式属性，不能静默改变柱宽或数值语义。

组合图的柱系列和线系列均使用 `ChartSeries<BarEntry>`：`BarEntry.label` 定义分类标签，输入索引定义共享 X 槽，`value` 进入同一个 Y 轴数据域。线点命中优先于其下方柱体；离开线点命中半径后再回落到柱命中。

`SparklineChart` 复用 `ChartSeries<ChartPoint>`、折线布局和 `LineChartEvent`，但只接受零或一个系列。默认隐藏坐标轴、网格、图例、数据点和 Tooltip，并启用平滑线；`selectable=true` 后才处理点击选择。

## 4. 默认值与校验

| 配置 | 默认值 | 规则 |
| --- | --- | --- |
| `xAxis.visible` / `yAxis.visible` | `true` | 轴隐藏时仍保留必要边距，除非显式关闭。 |
| `grid.visible` | `true` | 默认随 Y 轴刻度绘制水平网格。 |
| `yAxis.tickCount` | `5` | 最小为 2，最终数量由漂亮刻度算法决定。 |
| `showValueLabels` | 折线 `false`；柱状 `true` | 标签过密时可截断或跳过，但不能越界。 |
| `interaction.enabled` | `true` | 空数据时不触发回调。 |
| `tooltip.enabled` | `true` | P0 为点击提示。 |
| `tooltip.trackerEnabled` | `false` | P1 折线/面积长按追踪；已完成 Showcase 验收，仍需显式开启。 |
| `tooltip.keepTrackerOnRelease` | `false` | 仅在追踪开启时生效，控制 `end` 后是否保留最后选中槽。 |
| `interaction.enablePan` | `false` | P1 折线/面积水平平移；已完成 Showcase 验收，仍需显式开启。 |
| `interaction.visibleItemCount` | `0` | `0` 显示全部数据；非零值必须至少为 2，初始窗口默认对齐末尾数据。 |
| `interaction.maxRenderPointCount` | `0` | 折线/面积/Sparkline 每个可见系列的最大绘制点数；`0` 按绘图区宽度自动计算，显式值必须至少为 4。超限时用 min/max 桶采样保留首尾、峰谷、坏点分段和原始索引。 |
| `pie.innerRadiusRatio` | `0.58` | `0` 为饼图，`(0, 0.85]` 为环图。 |
| `pie.startAngleDegrees` | `-90` | 默认从十二点方向开始，必须为有限值。 |
| `pie.gapAngleDegrees` | `1.5` | 扇区间隙范围为 `[0, 10]`；单扇区自动取消间隙。 |
| `heatmap.cellGap` | `3` | 必须大于等于 0；实际间距会被单元格尺寸限制，避免负尺寸。 |
| `heatmap.colorScale` | 空列表 | 空值使用 GitHub 风格的绿色深浅离散色阶；`ChartTheme.dark()` 使用深色适配绿阶。有值时按归一化热度选择颜色桶。 |
| `radar.gridCount` | `5` | 范围为 `[2, 10]`；所有系列共享同一从零开始的漂亮刻度域。 |
| `radar.fillColors` | 空列表 | 默认只描边；如启用填充，应提供半透明 ARGB，避免遮挡重叠系列。 |
| `SparklineChart.selectable` | `false` | 默认不处理点击；开启后回调 `ChartSelection<ChartPoint>`。 |
| `theme` | `ChartTheme.light()` | 内置 Light、Dark、Ocean、Sunset；系列显式颜色优先。 |

数据项出现 `NaN` 或无穷值时跳过该项；若一个系列没有有效项，则按空数据处理。`tickCount < 2`、负边距、无效颜色等开发者配置应在构建 DSL 时失败。

## 5. 兼容性规则

- 新增公共字段时必须提供保持旧行为的默认值。
- 不得复用已发布字段名称表达相反含义。
- 回调索引必须对应输入序列中的原始项；若因无效值过滤发生偏移，需保留原始索引。
- 主题只提供默认值，系列级显式配置优先级最高。
- 格式化器和渲染器优先级固定为：调用方 `slot/renderer` > 系列配置 > 主题 > 内置默认实现。
