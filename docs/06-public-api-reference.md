# 公共 API 参考

> 本文档描述当前公共 API。每个公共符号均应有等价 KDoc，示例名称必须与实际代码一致；图片导出、Crosshair、Brush 和未完成三端验证的手势仍会明确标注为实验性或实现中。

## 图表入口

所有入口都是 Kuikly `ViewContainer` 扩展函数，创建对应 `ComposeView<Attr, Event>`。调用方式固定为 `Chart { attr { ... } event { ... } }`。

| API | 说明 |
| --- | --- |
| `ViewContainer<*, *>.LineChart(init)` | P0 折线；支持多系列、直线/平滑、点和图例。 |
| `ViewContainer<*, *>.BarChart(init)` | P0 柱图；可扩展分组、堆叠和负值。 |
| `ViewContainer<*, *>.AreaChart(init)` | P1 面积图；复用折线笛卡尔、选择、追踪和平移内核。 |
| `ViewContainer<*, *>.MixedChart(init)` | P1 组合图；分类柱与分类折线共享坐标、图例和 Tooltip。 |
| `ViewContainer<*, *>.PieChart(init)` | P1 饼环图；独立极坐标布局，支持扇区点击与 Tooltip。 |
| `ViewContainer<*, *>.HeatmapChart(init)` | P2 热力图；二维分类网格、离散颜色桶和单元格选择。 |
| `ViewContainer<*, *>.RadarChart(init)` | P2 雷达图；多系列共享维度、极坐标网格和顶点选择。 |
| `ViewContainer<*, *>.SparklineChart(init)` | P1 紧凑趋势；默认单系列、无轴、无网格、无图例且不响应点击。 |
| `ComposeView<*, *>.exportImage(request, onResult)` | P2 实验性一次性导出；默认 `DATA_URI`，尚未构成三端支持承诺。 |
| `ChartTheme.light()` | 默认浅色主题；另提供 `dark()`、`ocean()`、`sunset()`。 |

图表尺寸由 Kuikly 布局属性决定；不提供与布局系统冲突的像素尺寸构造参数。

## 数据、格式化与配置

| API | 字段/签名 | 说明 |
| --- | --- | --- |
| `ChartPoint` | `x`、`y`、`label?` | 折线或面积的数据项。 |
| `BarEntry` | `value`、`label` | 分类柱数据项。 |
| `PieEntry` | `label`、`value`、`color?` | 饼环扇区；仅有限且大于零的值参与布局。 |
| `HeatmapEntry` | `xLabel`、`yLabel`、`value`、`color?` | 热力图单元格；同一 `xLabel/yLabel` 坐标只能出现一次，非有限值跳过。 |
| `RadarEntry` | `label`、`value` | 雷达维度值；每个非空系列必须使用同一顺序且唯一的维度标签，负值和非有限值作为断点。 |
| `ChartSeries<T>` | `name`、`items`、`color?` | 一个系列及其稳定标识。 |
| `PointDataScope` / `PointSeriesScope` | `series(name) { point(...) }` | Line、Area、Sparkline 的推荐嵌套数据 DSL；`point(label, value)` 自动分配 X。 |
| `BarDataScope` / `BarSeriesScope` | `series(name) { item(...) }` | Bar 与 Mixed 的推荐分类数据 DSL。 |
| `PieDataScope` | `slice(label, value, color?)` | Pie/Donut 的推荐扇区 DSL。 |
| `HeatmapDataScope` | `cell(xLabel, yLabel, value, color?)` | Heatmap 的推荐单元格 DSL。 |
| `RadarDataScope` / `RadarSeriesScope` | `series(name) { metric(...) }` | Radar 的推荐维度 DSL。 |
| `ChartSelection<T>` | `seriesIndex`、`itemIndex`、`item` | 命中后回调的原始项与索引。 |
| `ChartTracker<T>` | `x`、`selections` | 长按追踪结果；`x` 为数据坐标，选择项保留原始索引。 |
| `ChartViewport` | `startIndex`、`endIndex` | 折线/面积窗口的闭区间，保留原始数据索引。 |
| `MixedChartSelection` | `seriesType`、`seriesIndex`、`itemIndex`、`seriesName`、`item` | 组合图命中结果；系列索引分别在柱/线系列列表内计数。 |
| `AxisOptions.labelFormatter` | `(value: Float) -> String` | 格式化数值坐标轴标签。 |
| `TooltipOptions.valueFormatter` | `(value: Float) -> String` | 格式化 Tooltip 数值。 |

| 配置块 | 关键属性 | 说明 |
| --- | --- | --- |
| `xAxis {}` / `yAxis {}` | `visible`、`tickCount`、`includeZero`、formatter | 坐标范围、漂亮刻度和标签。 |
| `grid {}` | `visible`、`color`、`lineWidth` | 网格外观。 |
| `line {}` | `smooth`、`showPoints`、`showValueLabels` | 折线表现。 |
| `area {}` | `fillColors` | 面积填充色，按系列索引循环使用；建议使用含透明度的 ARGB。 |
| `pie {}` | `innerRadiusRatio`、`startAngleDegrees`、`gapAngleDegrees`、`showValueLabels`、`centerLabel` | `innerRadiusRatio=0` 为饼图，大于零为环图。 |
| `heatmap {}` | `cellGap`、`showValueLabels`、`colorScale` | 二维网格的单元格间距、可选值文本和离散颜色桶；空 `colorScale` 使用 GitHub 风格绿色深浅色阶。 |
| `radar {}` | `gridCount`、`maxValue`、`showPoints`、`showValueLabels`、`lineWidth`、`fillColors` | 雷达网格与系列样式；`maxValue` 可固定径向比例尺以便实时数据更新，填充色应为半透明 ARGB，空列表不填充。 |
| `barData(...)` / `lineData(...)` | `ChartSeries<BarEntry>` | 组合图的柱/线系列；输入顺序共同定义分类槽。 |
| `data { ... }` | 各图表对应的 `*DataScope` | 推荐的简洁数据入口；与 `data(ChartSeries(...))` 或 `data(Entry(...))` 等价，且保留后者兼容性。 |
| `SparklineChart.selectable` | `false` | 显式开启后复用折线点击选择与 `LineChartEvent`；Tooltip 仍独立控制。 |
| `bars {}` | `mode`、`barWidthRatio`、`showValueLabels` | 单组、分组或堆叠柱。 |
| `legend {}` | `visible`、`position`、`toggleSeriesOnTap` | 图例和系列可见性。 |
| `tooltip {}` | `enabled`、`trackerEnabled`、`keepTrackerOnRelease`、`formatter` | 点击提示；折线/面积长按追踪和释放策略。 |
| `crosshair {}` | `enabled`、`showHorizontalGuide` | 折线/面积 tracker 的可选水平参考线；默认关闭。 |
| `brush {}` | `enabled`、`zoomToSelectionOnRelease` | 折线/面积图的实验性长按框选；默认关闭，长按时优先于 tracker。 |
| `interaction {}` | `enablePan`、`enableZoom`、`visibleItemCount`、`minVisibleItemCount`、`maxVisibleItemCount`、`viewport`、`maxRenderPointCount` | 折线/面积图的单指平移、双指缩放、受控可视窗口与每系列采样上限；采样保留峰谷、坏点分段和原始索引。 |
| `dataTransition {}` | `enabled`、`durationMs` | 所有现有图表的兼容快照值更新动画，默认关闭。身份结构变化、新增/删除/重排以及非有限值均直接切换，避免跨语义插值。 |
| `animation {}` | `enabled`、`durationMs`、`style` | 渐显或裁剪揭示，默认关闭。 |

## 回调与扩展优先级

`onItemSelected` 接收 `ChartSelection<T>`。`HeatmapChart` 将选择回调到原始 `HeatmapEntry`，`RadarChart` 回调到原始 `RadarEntry`；二者的 `itemIndex` 均不因坏值过滤而重排。`LineChartEvent.onTrackerChanged` 接收 `ChartTracker<ChartPoint>?`；`null` 表示非持久追踪已释放或清除。`onBrushChanged` 接收 `ChartBrushSelection?`，`null` 表示双击或销毁时清除。`onViewportChanged` 在平移、缩放、Brush 缩放和双击复位后返回已钳制的 `ChartViewport`；面积图复用 `LineChartEvent`。平台原生对象、Canvas 引用和手势原始坐标不暴露给使用方。

格式化与渲染优先级固定为：调用方 `slot/renderer` > 系列显式配置 > `ChartTheme` > 内置默认实现。公共 API 不依赖 `internal` 的 Renderer、布局矩形或 Canvas 命令；新增字段必须有保留旧行为的默认值。

## 实验性图片导出

`exportImage` 接收 `ChartImageExportRequest`，其中 `output` 默认是 `DATA_URI`、`sampleSize` 必须大于等于 1；回调返回 `ChartImageExportResult`。默认输出不创建可长期保留的原生缓存 key，ChartKit 不保留回调或导出数据。`FILE` 输出的文件生命周期由宿主负责。

该 API 标注为 `ExperimentalChartImageExportApi`。当前 Kuikly `2.7.0` 源码可编译 `toImage` 入口，但没有 Android、iOS、OpenHarmony 三端升级、回归、内存和真机证据，因此不得作为正式跨端导出能力发布。
