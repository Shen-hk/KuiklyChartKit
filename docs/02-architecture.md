# 总体技术设计

## 1. 设计原则

图表内核只依赖 Kuikly 公共 Canvas/Compose 能力，数据计算、视口状态和 `RenderPlan` 均位于 `chartkit/src/commonMain`。Android 与 H5 宿主只负责页面启动与运行环境，不承载金融行情逻辑。组件表面保持 Kuikly 的 `ComposeView<Attr, Event>` 模式。

```text
声明式 DSL / 行情快照
    -> ChartSpec + ChartViewport
    -> 数据规范化（排序、校验、稳定 ID）
    -> LayoutEngine（价格区、成交量区、坐标轴、标签）
    -> RenderPlan（线/柱/K线/十字线/命中区域）
    -> Kuikly Canvas + Compose Tooltip 覆盖层
```

事件沿反方向流动：`click / longPress / pan / zoom` -> `GestureCoordinator` -> `HitTestEngine` -> `Selection / ChartViewport` -> 重建 `RenderPlan` -> callback。命中、十字线和 Tooltip 必须都读取同一份 `RenderPlan`，不得分别按不同坐标公式计算。

## 2. 推荐目录与职责

```text
chartkit/src/commonMain/kotlin/.../chart/
  model/          ChartPoint、BarEntry、KLineEntry、Viewport、Theme、ChartSpec
  dsl/            LineChart、BarChart、KLineChart 及配置构建器
  data/           排序、OHLC 校验、更新合并、采样与原始索引映射
  layout/          坐标域、刻度、价格/成交量分区、标签与可绘制区域
  render/          RenderPlan、Line/Bar/Candle/Volume/Axis Renderer
  interaction/     GestureCoordinator、HitTestEngine、Tracker、ViewportController
  components/      ComposeView、Attr、Event 与 Canvas/Tooltip 覆盖层
  demo/            Showcase 与股票行情 Demo 的数据适配边界
```

`model`、`data`、`layout`、`interaction` 尽量保持纯 Kotlin，便于 `commonTest` 对价格计算、视口与边界进行确定性验证。`render` 只消费已计算的计划，不修改 DSL 输入；`components` 负责观察数据和尺寸变化、分发事件并发出回调。

## 3. 核心数据与更新流

1. DSL 接收不可变数据快照；`KLineEntry` 必须以 `timestamp` 或显式 `id` 建立稳定身份。
2. 规范化步骤排序时间序列、过滤非有限数值、验证 `low <= min(open, close) <= max(open, close) <= high`，并保留原始索引和诊断信息。
3. `ViewportController` 将数据总范围与当前 `start/end/scale` 相交，得到当前可视窗口。拖动和缩放只能更新视口，不得改写调用方数据。
4. `LayoutEngine` 计算 `pricePlotRect`、`volumePlotRect`、共享 X 轴、各自 Y 轴和标签。K 线与成交量的每个可视槽必须对应同一个稳定数据项。
5. `RenderPlan` 同时产出绘制原语、每个数据项的屏幕位置/包围盒和 X 槽索引。Tracker 在最近 X 槽查找价格、成交量和 Tooltip 内容。
6. 新快照到达时，优先以稳定 ID 恢复选中项和视口；找不到时清除选中或按配置回到尾部，绝不把旧下标静默映射到新数据。

## 4. 交互状态机

```text
Idle --tap--> Selected
Idle --long press--> Tracking --move--> Tracking
Tracking --end--> Idle 或 Selected（由 keepOnRelease 决定）
Idle --horizontal pan--> Panning --end/cancel--> Idle
Idle/Panning --zoom--> Zooming --end/cancel--> Idle
```

Tracker 激活时，滑动优先更新吸附到 X 槽的选中项；未激活 Tracker 的横向移动才进入平移。缩放以手势中心为锚点，并被 `minZoom`、`maxZoom` 与数据边界钳制。若当前平台未支持缩放，公共开关必须无效化并有可观察的降级说明，不能伪造缩放结果。

## 5. 平台与性能边界

- 公共层只暴露数据模型、颜色值、配置和业务回调；手势原始对象、DOM/Android 对象和 Canvas 引用不外泄。
- Tooltip 由 Compose 覆盖层布局，十字线、选中点和 K 线高亮由 Canvas 绘制，保证坐标一致并可避免越界。
- 当可视 K 线/点超过可绘制像素容量时，使用像素桶保留峰谷；K 线不可被错误合成为不存在的 OHLC，必要时按时间桶聚合并在 API 中显式标明。
- 只在尺寸、数据、主题、视口或交互状态变化时重算。文本测量、刻度与坐标转换在一次帧内缓存；批量 Canvas 命令仅在双端探针通过后使用。
