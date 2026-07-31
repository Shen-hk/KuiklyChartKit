# 总体技术设计

## 1. 设计原则

图表内核只依赖 Kuikly 的公共 Canvas/Compose 能力，所有数据计算与绘制计划放在 `chartkit/src/commonMain`。平台宿主只负责启动 Kuikly 页面与提供运行环境，不承载图表业务逻辑。组件表面严格采用官方的 `ComposeView<Attr, Event>` 模式。

```text
开发者 DSL（嵌套数据构建器或直接不可变模型）
    -> 不可变 ChartSpec
    -> 数据校验与规范化
    -> LayoutEngine（坐标域、刻度、标签、绘制区域）
    -> RenderPlan（线、柱、文本、网格、命中区域）
    -> Kuikly Canvas
    -> 平台渲染器
```

交互事件沿相反方向流动：Canvas 事件 -> `HitTestEngine` -> `Selection` -> tooltip/highlight/callback。命中测试必须复用 `RenderPlan` 的坐标，不得按另一套公式重新计算。

```kotlin
fun ViewContainer<*, *>.LineChart(init: LineChartView.() -> Unit) {
    addChild(LineChartView(), init)
}

class LineChartView : ComposeView<LineChartAttr, LineChartEvent>() {
    override fun createAttr() = LineChartAttr()
    override fun createEvent() = LineChartEvent()
    override fun body(): ViewBuilder = { /* Canvas + Tooltip overlay */ }
}
```

这使调用方沿用 Kuikly 熟悉的 `attr {}` 配置和 `event {}` 回调；公开 `View` 只负责组合，算法与绘制不泄漏到调用方。

## 2. 目录与职责

```text
chartkit/src/commonMain/kotlin/com/kuikly/kuiklychartkit/chart/
  ChartModels.kt       公共数据模型和 Kuikly DSL 配置对象
  ChartDataDsl.kt      Point/Bar/Pie/Heatmap/Radar 的嵌套数据构建器
  ChartLayout.kt       纯 Kotlin 标尺、布局和采样算法
  render/              不可变 ResolvedChartSpec 与 RenderPlan
  interaction/         命中测试与选中状态转换
  ChartComponents.kt   对接 Kuikly Compose/Canvas 的公开视图与临时绘制适配
  P2Chart*.kt          热力图、雷达图；后续按相同边界逐步迁移
  accessibility/   图表摘要、选中项语义与替代文本
```

`ChartModels`、`ChartLayout`、`render` 与 `interaction` 均保持纯 Kotlin，以便用 `commonTest` 进行确定性测试。`render` 只接收不可变配置快照并生成 `RenderPlan`，不改变业务数据。`ChartComponents` 负责监听数据/尺寸变化、触发绘制和事件分发；历史 Canvas 绘制代码会按图表类型渐进迁移，避免改变公开 DSL。

## 3. 核心数据流

1. DSL 用嵌套构建器或直接模型构建不可变数据快照，并在提交给组件时检查必填项与范围。
2. 规范化步骤过滤无效值、计算数据域；所有值相等时扩展一个安全范围，防止除零。
3. `LayoutEngine` 根据视图宽高、边距和文字测量结果计算 `plotRect`、刻度与标签位置。
4. Renderer 将布局转换为 Canvas 原语，并同时保留数据项的屏幕包围盒/点位以供命中测试；复杂图在 Canvas 回调中开启批量绘制（仅能力探针通过时）。
5. 选中态或数据变化时，重新生成计划；绘制过程不修改 DSL 输入。

## 4. 平台抽象边界

- 公共层：数据模型、布局、颜色值、事件回调、Canvas 绘制命令。
- 平台层：仅由 Kuikly 处理 Canvas 实现、字体度量和触摸事件接入。
- 若多端 Canvas API 有差异，优先在一个内部 `CanvasScope` 适配层消化，禁止把条件判断散落到 Renderer。
- Tooltip 优先作为 Compose 覆盖层而非绘制在 Canvas 内：它能使用正常的圆角、阴影、文字布局和无障碍属性；选中高亮仍由 Canvas 绘制以保证坐标一致。
- 线性渐变、路径裁剪和文本测量是跨端基线；不得以平台实验性能力表达必要信息。

## 5. 错误与性能策略

- 对空数据绘制占位文案或空状态，不抛异常。
- 对开发者配置错误（例如空系列名、负柱宽）在 DSL 构建期 `require` 并给出字段名；对外部数据错误采取过滤与诊断。
- 只在尺寸、数据、样式或选中态变化时重算布局；文本测量与坐标变换应在一次渲染帧中缓存。
- 数据量超过可视像素密度时，先对每个 X 像素桶保留 min/max 点，保证峰谷可见；原始数据与索引始终用于回调。
- 先保证静态渲染正确，再引入动画；动画必须可选，不能改变最终几何结果。
