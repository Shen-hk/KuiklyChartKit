# 调研依据与技术结论

本文件记录图表组件方案的证据与约束，避免把“看起来不错”的想法误当成 Kuikly 已验证能力。调研日期：2026-07-27。

## 官方 Kuikly 结论

1. [ComposeView 文档](https://kuikly.tds.qq.com/DevGuide/compose-view.html) 将可复用组件定义为 `ComposeView<Attr, Event>`：`body()` 组装 UI，`Attr` 暴露配置，`Event` 暴露回调；对外通过 `ViewContainer` 扩展函数 `addChild` 提供 DSL。因此公开入口采用 `LineChart { attr { } event { } }`，而非另起一套 DSL。
2. [Canvas API](https://github.com/Tencent-TDS/KuiklyUI/blob/main/docs/API/components/canvas.md) 覆盖路径、贝塞尔曲线、弧、虚线、线性渐变、文本测量、裁剪和变换，足以在 `commonMain` 实现折线、柱、面积、饼环和 Tooltip；径向渐变是 iOS 实验性能力，首版不依赖它。
3. 官方基础事件支持 `click`、`longPress(start/move/end)` 和 `pan`。长按追踪与水平平移是可验证的候选交互方向，但只有通过当前版本的能力探针后才能成为承诺能力。
4. 官方 Canvas 源码提供 `batchDraw` 命令批量发送机制；高密度数据渲染先确认当前依赖版本可用，再启用。当前工程固定 Kuikly `2.7.0-2.1.21`，不能直接承诺使用较新版本的 API。
5. 基础 `toImage` 能力在官方文档中标注为 Android/iOS/OpenHarmony 的 Kuikly `2.17+` 能力。图表导出图片是可选增强项，必须先完成升级兼容性验证，不能作为 P0 依赖。

## 官方组件范例的可借鉴点

[KuiklyChatUI](https://github.com/Kuikly-contrib/KuiklyChatUI) 是官方 Issue 给出的工程范例。其公开做法值得继承：

- 顶层 `ViewContainer` 扩展函数加 Config/DSL，调用方无需接触内部渲染细节；
- 主题有完整默认值，且支持集中替换；
- 扩展点有清晰优先级（调用方 Slot/Formatter > 可替换工厂 > 内置默认实现）。

ChartKit 将采用同一理念：`ValueFormatter`、`AxisFormatter`、`TooltipRenderer` 和 `SeriesRenderer` 可替换；显式配置优先于 `ChartTheme`，主题优先于内置默认值。

## 公开竞争对标

Issue 评论中，`Lfan-ke` 已于 2026-07-08 提交 [KuiklyChartView](https://github.com/Lfan-ke/KuiklyChartView)。其 README 宣称 22 种图表、4 套主题、点击 Tooltip、动画和多端适配；公开源码将这些图表集中在一个约 144 KB 的 `ChartView.kt` 中，仓库文件清单未见图表测试目录。此处仅用于识别公开基线，不复用其代码或视觉资产。

我们的取胜策略不是短期堆叠图表数量，而是交付稳定的 P0、统一内核、可验证交互、密集数据策略、完整测试和跨端证据。候选行业能力只能作为架构扩展性参考，不能改变通用组件库的产品目标。

## 仍需完成的能力探针

| 探针 | 通过标准 | 失败时的处理 |
| --- | --- | --- |
| `longPress` 与 `pan` 在当前依赖中编译且三端回调一致 | 当前已确认 Android/JS 可编译；三端均收到准确坐标与状态。 | Tracker/平移保持实验性；降级为点击 Tooltip，并评估最小安全升级。 |
| `CanvasContext.batchDraw` 在当前依赖可用 | 大数据页面无逐命令跨端瓶颈。 | 使用布局/标签缓存与采样；不引用不可用 API。 |
| Canvas 文本测量 | 中英文和数字标签宽度可预测。 | 以实际 `measureText` 为唯一布局依据。 |
| `toImage` | 升级后 Android/iOS/OpenHarmony 均成功导出。 | 放在 P2，不影响核心发布。 |
