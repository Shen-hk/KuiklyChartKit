# 公共 API 参考

> 本文档定义拟公开 API。代码完成后，每个公共符号必须拥有等价 KDoc，示例名称必须与实际代码一致。候选扩展不构成已发布承诺。

## 图表入口

所有入口都是 Kuikly `ViewContainer` 扩展函数，创建对应 `ComposeView<Attr, Event>`。调用方式固定为 `Chart { attr { ... } event { ... } }`。

| API | 说明 |
| --- | --- |
| `ViewContainer<*, *>.LineChart(init)` | P0 折线；支持多系列、直线/平滑、点和图例。 |
| `ViewContainer<*, *>.BarChart(init)` | P0 柱图；可扩展分组、堆叠和负值。 |
| `ViewContainer<*, *>.AreaChart(init)` | P1 面积图；复用折线笛卡尔、选择、追踪和平移内核。 |
| `ViewContainer<*, *>.MixedChart(init)` | P1 组合图；分类柱与分类折线共享坐标、图例和 Tooltip。 |
| `ViewContainer<*, *>.PieChart(init)` | P1 饼环图；独立极坐标布局，支持扇区点击与 Tooltip。 |
| `ViewContainer<*, *>.SparklineChart(init)` | P1 紧凑趋势；默认单系列、无轴、无网格、无图例且不响应点击。 |
| `ChartTheme.light()` | 默认浅色主题；另提供 `dark()`、`ocean()`、`sunset()`。 |

图表尺寸由 Kuikly 布局属性决定；不提供与布局系统冲突的像素尺寸构造参数。

## 数据、格式化与配置

| API | 字段/签名 | 说明 |
| --- | --- | --- |
| `ChartPoint` | `x`、`y`、`label?` | 折线或面积的数据项。 |
| `BarEntry` | `value`、`label` | 分类柱数据项。 |
| `PieEntry` | `label`、`value`、`color?` | 饼环扇区；仅有限且大于零的值参与布局。 |
| `ChartSeries<T>` | `name`、`items`、`color?` | 一个系列及其稳定标识。 |
| `ChartSelection<T>` | `seriesIndex`、`itemIndex`、`item` | 命中后回调的原始项与索引。 |
| `ChartTracker<T>` | `x`、`selections` | 长按追踪结果；`x` 为数据坐标，选择项保留原始索引。 |
| `ChartViewport` | `startIndex`、`endIndex` | 折线/面积窗口的闭区间，保留原始数据索引。 |
| `MixedChartSelection` | `seriesType`、`seriesIndex`、`itemIndex`、`seriesName`、`item` | 组合图命中结果；系列索引分别在柱/线系列列表内计数。 |
| `AxisLabelFormatter<T>` | `(item: T, index: Int) -> String` | 格式化分类标签。 |
| `ValueFormatter` | `(value: Double) -> String` | 格式化轴、标签和 Tooltip。 |

| 配置块 | 关键属性 | 说明 |
| --- | --- | --- |
| `xAxis {}` / `yAxis {}` | `visible`、`tickCount`、`includeZero`、formatter | 坐标范围、漂亮刻度和标签。 |
| `grid {}` | `visible`、`color`、`lineWidth` | 网格外观。 |
| `line {}` | `smooth`、`showPoints`、`showValueLabels` | 折线表现。 |
| `area {}` | `fillColors` | 面积填充色，按系列索引循环使用；建议使用含透明度的 ARGB。 |
| `pie {}` | `innerRadiusRatio`、`startAngleDegrees`、`gapAngleDegrees`、`showValueLabels`、`centerLabel` | `innerRadiusRatio=0` 为饼图，大于零为环图。 |
| `barData(...)` / `lineData(...)` | `ChartSeries<BarEntry>` | 组合图的柱/线系列；输入顺序共同定义分类槽。 |
| `SparklineChart.selectable` | `false` | 显式开启后复用折线点击选择与 `LineChartEvent`；Tooltip 仍独立控制。 |
| `bars {}` | `mode`、`barWidthRatio`、`showValueLabels` | 单组、分组或堆叠柱。 |
| `legend {}` | `visible`、`position`、`toggleSeriesOnTap` | 图例和系列可见性。 |
| `tooltip {}` | `enabled`、`trackerEnabled`、`keepTrackerOnRelease`、`formatter` | 点击提示；折线/面积长按追踪和释放策略。 |
| `interaction {}` | `enablePan`、`visibleItemCount`、`maxRenderPointCount` | 折线/面积水平平移、可视窗口与每系列采样上限；采样保留峰谷、坏点分段和原始索引。 |
| `animation {}` | `enabled`、`durationMs`、`style` | 渐显或裁剪揭示，默认关闭。 |

## 回调与扩展优先级

`onItemSelected` 接收 `ChartSelection<T>`。`LineChartEvent.onTrackerChanged` 接收 `ChartTracker<ChartPoint>?`；`null` 表示非持久追踪已释放或清除。`onViewportChanged` 返回平移后已钳制的 `ChartViewport`。`AreaChart` 复用同一事件类型。缩放尚未定稿。平台原生对象、Canvas 引用和手势原始坐标不暴露给使用方。

格式化与渲染优先级固定为：调用方 `slot/renderer` > 系列显式配置 > `ChartTheme` > 内置默认实现。公共 API 不依赖 `internal` 的 Renderer、布局矩形或 Canvas 命令；新增字段必须有保留旧行为的默认值。
