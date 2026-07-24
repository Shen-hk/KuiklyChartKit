# KuiklyChartKit 开发文档

本目录是 KuiklyChartKit 的实现与验收依据。文档已按 2026-07-24 的补充计划更新：组件围绕**金融行情**场景交付，先稳定完成折线图、柱状图和 Kuikly 风格声明式 DSL，再补齐行情交互、K 线与成交量组合、Android/H5 示例、测试、API 文档，以及 Kuikly AI 股票行情 Demo 的真实接入。

文档描述的是本轮目标与验收口径；除明确标注“已验证”的内容外，均不得视作已实现或已发布能力。

| 阶段 | 文档 | 作用 |
| --- | --- | --- |
| 调研与约束 | [00-调研依据](00-research-notes.md) | 记录 Kuikly 能力、H5 缺口与必须完成的能力探针。 |
| 范围与验收 | [01-需求与验收](01-requirements-and-acceptance.md) | 定义金融行情目标、优先级、平台门槛与完成条件。 |
| 架构 | [02-总体技术设计](02-architecture.md) | 约束行情数据、视口、交互状态与渲染链路。 |
| DSL | [03-DSL 规范](03-dsl-specification.md) | 定义折线、柱状、K 线/成交量及交互的声明式契约。 |
| 视觉与交互 | [04-视觉与交互规范](04-visual-and-interaction-specification.md) | 定义十字光标、Tooltip、滑动选点、拖动和缩放语义。 |
| 跨端支持 | [05-平台兼容与适配矩阵](05-platform-compatibility.md) | 将 Android 与 H5 设为本轮可运行示例及验收目标。 |
| API | [06-公共 API](06-public-api-reference.md) | 集中维护公共模型、DSL 配置与回调。 |
| 接入 | [07-接入与示例](07-integration-and-examples.md) | 说明 Android/H5 Showcase 以及 Kuikly AI 股票行情 Demo 接入。 |
| 测试 | [08-测试计划](08-test-plan.md) | 覆盖动态行情更新、边界数据、交互和视觉回归。 |
| 发布 | [09-发布与贡献](09-release-and-contribution.md) | 统一构建、版本、变更与交付证据要求。 |
| 交付计划 | [10-竞争性交付方案](10-competitive-delivery-plan.md) | 给出按依赖排序的里程碑、质量门槛和评审证据。 |
| 实现状态 | [11-M0 实现与验收记录](11-m0-implementation-status.md) | 区分已实现 P0/M0 与后续规划，记录双端构建、浏览器实测和已知基线问题。 |

## 维护规则

- 需求、DSL、交互或平台范围变化时，先同步相应规格，再修改实现、示例和 KDoc。
- 公共 API 采用 Kuikly `ComposeView + Attr + Event + ViewContainer` 模式；平台原生类型不得出现在 `commonMain` 公共契约中。
- Android 与 H5 示例、API/KDoc、自动化测试和 Kuikly AI 股票行情 Demo 接入证据必须共同更新；不能只更新其中一项就宣称能力已完成。
- 任何缩放、手势、Canvas 或 H5 行为都要先通过能力探针。未验证的平台只能标为“待验证”，不能标为“支持”。
