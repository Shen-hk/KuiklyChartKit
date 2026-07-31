# P3 交互与数据能力路线

> 当前数据 DSL 已通过 `data { series { point/item/metric } }`、`slice`、`cell` 覆盖现有图表。P3 仅可在不改变这些数据模型、校验规则和原始索引回调语义的前提下扩展交互。

> 状态：P3-1 已将手势从 Canvas 迁移到普通触摸层，并抽出纯 Kotlin `ChartGestureController`；P3-2 已以实验性 `Crosshair + Brush` 接入折线/面积图。`commonTest` 已覆盖 pan 钳制、pinch 焦点、双击复位、tracker release、坏值防护和 Brush 状态迁移。Android 真机手势、APK 构建与 iOS/OpenHarmony 证据待补，均未标记为正式三端能力。
> 目标：在既有笛卡尔渲染、追踪、平移、采样和事件协议之上，优先增强数据探索能力；不以堆叠图表类型替代交互质量。

## 1. 输入与范围

本阶段调研了 KuiklyUI Issue #1477 中已提交的图表仓库，以及 Vico、KoalaPlot、KLineChart 和 Apache ECharts 的公开设计。可借鉴的是交互、可扩展性和数据组织方式，不复用任何对标仓库的代码、名称、截图或设计资产。

P3 的正式候选仅包括通用交互、数据模型与可访问性。金融图表仅作为独立候选包，必须在通用视口内核验收后、并确认扩大项目范围后才可实施。

## 2. 优先级与依赖

| 优先级 | 任务 | 依赖 | 交付与验收 |
| --- | --- | --- | --- |
| P3-1 | 缩放视口内核 | 既有 `ChartViewport`、RenderPlan | 为折线和面积图增加触摸层的单指平移、双指缩放与双击复位；范围钳制并保留原始索引。组合图在该路径完成 Android 验收后再接入。 |
| P3-2 | Crosshair 与区间框选（Brush） | P3-1 内核 | 实验性实现中，默认关闭。Crosshair 复用 tracker 的 RenderPlan 命中坐标，可选水平参考线与聚合 Tooltip；Brush 仅能在绘图区长按拖拽启动，输出稳定的原始索引闭区间，支持“缩放至选区”和双击清除。两者不抢占点击、双指缩放或普通平移；Brush 显式优先于 tracker，并补充状态迁移和跨端录屏验证。 |
| P3-3 | 数据集与变换 | P3-1 的可视范围语义 | 在 `commonMain` 提供不可变 `ChartDataset` 和可组合的 `filter`、`sort`、`aggregate`、`rollingAverage`；变换结果保留来源索引/标识，失败有确定诊断；首批只实现纯 Kotlin 变换，不引入表达式执行器或服务端查询能力。 |
| P3-4 | 无障碍与可读性 | 主题和数据集模型 | 提供可选 `accessibility {}`：生成图表摘要、最大/最小/趋势文本、系列/选区摘要及数据表回退；主题支持非颜色区分（线型、点型或纹理）和最小对比度测试。H5 输出语义描述，原生端通过宿主可用能力落地；未验证平台不得宣称支持。 |
| P3-5 | 金融图表扩展包（候选） | P3-1、确认扩大范围 | 独立模块实现 `CandlestickChart + Volume`，共用笛卡尔视口、Tooltip、缩放和数据更新；先定义 `OhlcEntry`、窗口策略与非有限值规则，再评估 MA/EMA 等可注册指标。不得将行业模型混入现有通用 `ChartPoint` API。 |

## 2.1 P3-1 实现记录（2026-07-29）

- Android `core-render-android 2.7.0-2.1.21` 的 Canvas 直接继承 `android.view.View`，未接入 Kuikly 手势分发器，也没有 `pinch`。因此 P3-1 改为在 Canvas 外包一层普通 Kuikly `View`，使用其同步原始触摸事件读取多指坐标。
- `LineChart` 与 `AreaChart` 的触摸层以单指 X 位移连续平移图形层，松手后约 120ms 吸附到最近的整数视口；以两指距离计算缩放比例，并以两指中心附近的数据索引为锚点；双击在触摸层复位。所有范围保持原始索引。
- `ChartGestureController` 不依赖 `ComposeView`、Canvas 或平台事件；组件层只将触点、当前 RenderPlan 命中点和配置快照转交给它，并应用其返回的受钳制视口。非有限触点、距离、缩放和无效数据窗口不会写回视口状态。
- 已通过 `.\\gradlew :chartkit:testDebugUnitTest :chartkit:compileKotlinJs`；待补 Android Debug APK 构建、Android 真机录屏，以及完整 iOS/OpenHarmony 回归。P3-2 保持实验性，直至这些证据齐全。

## 3. 建议实现顺序

```text
能力探针（Canvas 与普通触摸层）
  -> P3-1 缩放视口
  -> P3-2 区间框选
  -> P3-3 数据集与变换
  -> P3-4 无障碍
  -> 范围确认后再评估 P3-5 金融扩展包
```

每项开始前必须先更新 DSL 与交互规格；每项完成时必须同步 KDoc、Showcase、`commonTest`、Android/iOS/OpenHarmony 的证据矩阵。P3-1 的多指和双击能力若无法在任一正式平台获得一致行为，应降级为显式控制器 API 或保持候选，不暴露为“跨端手势”。

## 3.1 P3-2 设计边界

- 默认关闭：不新增默认手势，不改变已接入页面的点击、追踪或平移行为。
- Crosshair 只消费已有 tracker 命中结果，不从原始数据重新推导屏幕坐标；多系列同 X 值聚合沿用 `ChartTracker` 的原始索引语义。
- Brush 独立保存“待开始 / 拖拽中 / 已选择”状态，开始、更新和取消为纯 Kotlin 可测试转移；组件只负责将最终原始索引区间回调给调用方。
- 手势仲裁顺序固定为：明确 Brush 模式 > 双指 pinch > 已激活 tracker > 单指 pan > click。P3-2 只有在该顺序及平台录屏通过后才从实验性实现升级。

## 4. 暂缓项

- 不以“22 种图表”作为目标；散点、气泡、漏斗、桑基、仪表、树图等须由明确业务场景驱动，再单独排期。
- MA/EMA 之外的金融指标、画线工具、联网行情与投资分析不进入当前范围。
- 图片导出在 P2 的三端升级、内存和真机证据完成前继续保持实验性，不与 P3 绑定发布。

## 5. P3 完成定义

P3-1 至 P3-4 中任一项只有在公共 API/KDoc、示例、纯逻辑测试及正式平台证据均齐全后才能标记完成。P3-5 必须额外获得范围确认，并以独立模块、独立示例和明确的非投资用途说明交付。
