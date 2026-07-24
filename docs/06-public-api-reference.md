# 公共 API 参考

> 本文档同时记录 M0 已实现 API 与 M1–M2 目标 API。表中“已实现”签名以当前源码和 KDoc 为准；“计划”项不能被当作当前已发布接口。若签名调整，应同时更新本文档、示例与兼容性说明。

## 图表入口

所有入口均为 Kuikly `ViewContainer` 扩展，创建对应的 `ComposeView<Attr, Event>`，调用形态固定为 `Chart { attr { ... } event { ... } }`。

| API | 状态 | 说明 |
| --- | --- | --- |
| `ViewContainer<*, *>.LineChart(init)` | M0 已实现 | 多系列、直线/平滑、坐标轴、网格、图例、空态、点击与基础 Tooltip；Tracker 属于 M1。 |
| `ViewContainer<*, *>.BarChart(init)` | M0 已实现 | 分类柱、并列多系列、颜色、标签、正负值零基线、点击与基础 Tooltip。 |
| `ChartTheme.light()` / `dark()` / `ocean()` / `sunset()` | M0 已实现 | 主题默认值；系列显式颜色优先。 |
| `ViewContainer<*, *>.KLineChart(init)` | M2 计划 | OHLC K 线与可选成交量组合；共享 X 轴、视口和 Tracker。 |

## 数据与状态模型

| API | 状态 | 字段/签名 | 说明 |
| --- | --- | --- | --- |
| `ChartPoint` | M0 已实现 | `x: Float`、`y: Float`、`label: String` | 折线数据项；非法坐标形成断线，不导致崩溃。 |
| `BarEntry` | M0 已实现 | `label: String`、`value: Float` | 柱状数据项。 |
| `ChartSeries<T>` | M0 已实现 | `name`、`items`、`color?` | 多系列折线/柱图的数据与样式。 |
| `ChartSelection<T>` | M0 已实现 | `seriesIndex`、`itemIndex`、`seriesName`、`item` | 单项命中结果；索引和数据项对应原始输入。 |
| 轴/Tooltip formatter | M0 已实现 | `(Float) -> String` | 格式化坐标、柱值与基础 Tooltip。 |
| `KLineEntry` | M2 计划 | `timestamp`、`open`、`high`、`low`、`close`、`volume?`、`label?`、`id?` | 一根金融行情 OHLC 数据；时间戳或 `id` 作为稳定身份。 |
| `ChartTracker` | M1 计划 | `slotIndex`、`timestamp?`、`selections` | Tracker 的统一 X 槽及该槽所有可见选择。 |
| `ChartViewport` | M1 计划 | `startIndex`、`endIndex`、`scale` | 当前可视区间与缩放比例，可传入或通过回调保存。 |

## 配置块

| 配置块 | 状态 | 关键属性 | 说明 |
| --- | --- | --- | --- |
| `xAxis {}` / `yAxis {}` | M0 已实现 | `visible`、`tickCount`、`includeZero`、`labelFormatter` | 轴范围、刻度和标签；`tickCount` 支持 2..10。 |
| `grid {}` / `legend {}` | M0 已实现 | `visible`、`lineWidth` | 网格和图例显示。 |
| `line {}` | M0 已实现 | `smooth`、`showPoints`、`lineWidth`、`pointRadius` | 折线外观。 |
| `bars {}` | M0 已实现 | `mode`、`barWidthRatio`、`showValueLabels`、`cornerRadius` | 默认 `GROUPED` 并列多系列；显式 `SINGLE` 时拒绝多系列输入，避免静默丢数据。 |
| `tooltip {}` | M0 已实现 | `enabled`、`valueFormatter` | 点击选择的提示内容。 |
| `interaction {}` | M1 计划 | `enableTracker`、`enablePan`、`enableZoom`、`minZoom`、`maxZoom` | 选择、平移、缩放与边界。 |
| `viewport {}` | M1 计划 | `initial`、`followLatest` | 初始可视范围和数据更新时是否跟随最新数据。 |
| `kLine {}` | M2 计划 | `upColor`、`downColor`、`showLastPrice` | K 线涨跌颜色与末价标记。 |
| `volume {}` | M2 计划 | `visible`、`heightRatio`、`colorByPriceDirection` | 成交量区显示与样式。 |

## 回调与更新语义

| 回调 | 触发时机 | 契约 |
| --- | --- | --- |
| `onItemSelected`（M0 已实现） | 点击命中单个折线点或柱。 | 返回原始系列索引、条目索引、系列名和原始项。 |
| `onTrackerChanged`（M1 计划） | Tracker 开始、移动、结束或清除。 | 同一 X 槽的多系列/成交量数据必须一致。 |
| `onViewportChanged`（M1 计划） | 平移或缩放导致可视范围实际变化。 | 返回已钳制、可恢复的 `ChartViewport`。 |
| `onDataDiagnostic`（M2 计划） | 外部数据被过滤、重复或不完整。 | 不替代渲染；供业务记录或监控。 |

M0 通过再次设置 `data(...)` 提交新快照，绘制和点击回调始终基于最新列表。以稳定 ID 恢复选择和视口属于 M1/M2 数据更新契约，尚未公开。当前格式化与绘制优先级为：系列或图表显式配置 > `ChartTheme` > 内置默认值。
