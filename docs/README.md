# KuiklyChartKit 开发文档

本目录是图表组件的设计、实现与验收依据。文档以 [KuiklyUI Issue #1477](https://github.com/Tencent-TDS/KuiklyUI/issues/1477) 为需求来源，并以仓库的 Kotlin Multiplatform、Kuikly 与 Canvas 配置为实现基线。

总体目标是构建通用、可维护的跨端图表组件库：交付折线图、柱状图和符合 Kuikly 习惯的 Kotlin DSL，并在同一数据契约上扩展面积、饼环、组合、Sparkline、热力和雷达图。公共数据 DSL 现已支持 `data { series { point/item/metric } }`、`slice(...)` 与 `cell(...)` 的简洁写法，同时保留直接传入 `ChartSeries`/Entry 模型的写法，便于接入已有状态层数据。行业专用图表、业务 Demo 或平台范围扩张不属于既定目标，需单独确认后才可纳入计划。

| 阶段 | 文档 | 目的 |
| --- | --- | --- |
| 调研 | [00-调研依据](00-research-notes.md) | 记录官方能力、对标基线与版本约束。 |
| 编码前 | [01-需求与验收](01-requirements-and-acceptance.md) | 固化范围、优先级与完成定义。 |
| 编码前 | [02-总体技术设计](02-architecture.md) | 约束模块划分、渲染链路和扩展边界。 |
| 编码中 | [03-DSL 规范](03-dsl-specification.md) | 定义稳定的开发者契约和推荐的嵌套数据 DSL。 |
| 编码前 | [04-视觉与交互规范](04-visual-and-interaction-specification.md) | 明确绘制、命中与交互细节。 |
| 编码前 | [05-平台兼容与适配矩阵](05-platform-compatibility.md) | 约束正式支持和验证范围。 |
| 实现中 | [06-公共 API](06-public-api-reference.md) | 面向使用者的 API 参考。 |
| 实现中 | [07-接入与示例](07-integration-and-examples.md) | 提供可复用的接入方式。 |
| 实现中 | [08-测试计划](08-test-plan.md) | 覆盖功能、渲染、交互和多端验收。 |
| 发布前 | [09-发布与贡献](09-release-and-contribution.md) | 统一构建、发布和贡献规则。 |
| 交付总览 | [10-竞争性交付方案](10-competitive-delivery-plan.md) | 明确优先级、Demo 效果与评审证据。 |
| 实现记录 | [11-M0 实现与验收记录](11-m0-implementation-status.md) | 记录已实现的基础能力与已验证环境。 |
| 实现记录 | [12-P1 实现与验收记录](12-p1-implementation-status.md) | 记录追踪/平移、扩展图表、密集数据采样、构建产物与正式平台待补证据。 |
| 实现记录 | [13-P2 实现与验收记录](13-p2-implementation-status.md) | 记录热力图、雷达图、实验性图片导出封装，以及升级与三端验证门槛。 |
| 后续规划 | [15-P3 交互与数据能力路线](15-p3-interaction-data-roadmap.md) | 规划缩放、区间框选、数据集变换、无障碍与经范围确认后的金融扩展包。 |

## 使用规则

- 需求、DSL 或视觉行为改变时，先更新对应规格，再改代码和示例。
- 标记为“拟定”或“候选”的 API/阈值是实现前的默认决策；代码落地后必须更新为已实现行为，不能让文档停留在提案状态。
- 公共组件必须遵循 Kuikly 的 `ComposeView + Attr + Event + ViewContainer` 模式；不得为图表引入与页面割裂的状态框架。
- Issue 未明确的体验取舍，以本目录的通用设计为准；如需扩大到行业方案、专用 Demo、缩放或新的正式平台，先完成能力探针并更新验收表。
