# KuiklyChartKit

面向金融行情场景的 Kuikly 跨端图表组件库。P0 / M0 已实现折线图、柱状图、Kuikly 官方风格声明式 DSL、点击选择与基础 Tooltip，并用同一份 `commonMain` 页面完成 Android APK 和 H5 production 站点验证。

> 对应任务：[Tencent-TDS/KuiklyUI#1477](https://github.com/Tencent-TDS/KuiklyUI/issues/1477)  
> 当前基线：Kuikly `2.7.0` · Kotlin `2.1.21` · Android + H5

## 当前完成度

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| `LineChart` | P0 已完成 | 多系列、直线/平滑、点、轴、网格、图例、空态、点击选择和基础 Tooltip。 |
| `BarChart` | P0 已完成 | 分类柱、并列多系列、正负值零基线、值标签、圆角、点击选择和基础 Tooltip。 |
| Kuikly DSL | P0 已完成 | `ViewContainer` 扩展 + `ComposeView<Attr, Event>`；错误配置给出字段级异常。 |
| Android Showcase | P0 已完成 | `chart_showcase` 页面已接入路由，Debug APK 构建通过。 |
| H5 Showcase | P0 已完成 | 官方 Web Render 真宿主，业务包自动嵌入，浏览器实际挂载 2 个 Canvas。 |
| Tracker / Pan / Zoom | M1 计划 | 完成双端手势能力探针后公开 API。 |
| K 线 / 成交量 | M2 计划 | 复用笛卡尔内核，增加 OHLC、共享视口和行情边界处理。 |
| Kuikly AI 股票 Demo | M3 计划 | 组件门禁通过后接入真实业务页面。 |

“已完成”和“计划”能力的逐项证据见 [M0 实现与验收记录](docs/11-m0-implementation-status.md)。

## 30 秒构建

在 Windows PowerShell 的仓库根目录执行：

```powershell
# 自动化测试
.\gradlew :chartkit:testDebugUnitTest

# Android Debug APK
.\gradlew :androidApp:assembleDebug

# H5 production 站点
.\gradlew :h5App:publishChartShowcase
```

产物：

```text
androidApp/build/outputs/apk/debug/androidApp-debug.apk
h5App/build/dist/js/productionExecutable/
```

H5 本地运行：

```powershell
.\gradlew :h5App:jsBrowserProductionRun
```

浏览器访问 `http://localhost:8080/`。

## DSL 示例

```kotlin
LineChart {
    attr {
        data(
            ChartSeries(
                name = "收盘价",
                items = listOf(
                    ChartPoint(1f, 12.4f, "周一"),
                    ChartPoint(2f, 13.1f, "周二"),
                    ChartPoint(3f, 12.8f, "周三"),
                ),
            ),
        )
        theme = ChartTheme.ocean()
        yAxis {
            tickCount = 5
            includeZero = false
        }
        line {
            smooth = true
            showPoints = true
        }
        tooltip { enabled = true }
    }
    event {
        onItemSelected { selection ->
            // selection.item 是原始 ChartPoint；
            // seriesIndex/itemIndex 保持调用方输入索引。
        }
    }
}
```

```kotlin
BarChart {
    attr {
        data(
            ChartSeries(
                name = "成交量",
                items = listOf(
                    BarEntry("周一", 120f),
                    BarEntry("周二", -36f),
                ),
            ),
        )
        bars {
            mode = BarMode.GROUPED
            showValueLabels = true
            cornerRadius = 4f
        }
    }
    event { onItemSelected { selection -> /* 原始 BarEntry */ } }
}
```

## 设计要点

- 所有公共数据、主题、布局语义和 DSL 位于 `commonMain`，不暴露 Android 或浏览器原生类型。
- 图表使用 Kuikly `Canvas` 绘制，Android 与 H5 共享组件、页面和确定性样本。
- `NaN` / 无穷值不会导致崩溃：折线非法点形成断线，柱状非法值跳过。
- 空数据、单值、等值、跨零数据使用安全 domain；轴刻度和命中测试为纯 Kotlin，可自动化回归。
- H5 遵循“`nativevue2.js` 先注册页面，`h5App.js` 后挂载 Web Render”的官方加载顺序。
- Kotlin 2.1 的 Android D8/R8 版本已按 [Google 官方兼容矩阵](https://developer.android.com/build/kotlin-support) 固定为 8.6.17。

## 文档导航

- [完整文档索引](docs/README.md)
- [需求与验收](docs/01-requirements-and-acceptance.md)
- [总体技术设计](docs/02-architecture.md)
- [DSL 规范](docs/03-dsl-specification.md)
- [公共 API](docs/06-public-api-reference.md)
- [接入与双端运行](docs/07-integration-and-examples.md)
- [测试计划](docs/08-test-plan.md)
- [竞争性交付方案](docs/10-competitive-delivery-plan.md)
- [M0 实现与验收记录](docs/11-m0-implementation-status.md)

## P0 验证摘要

- 12 个自动化测试，0 failure；
- Android Debug APK 构建通过；
- Kotlin/JS 与 H5 production webpack 构建通过；
- 浏览器 1280×720 首屏实际检测到 2 个 Canvas；
- 浅/深主题切换通过；
- 折线末点点击回调与页面反馈通过；
- 浏览器控制台无运行时 error。

后续里程碑不会把规划能力包装为已完成能力。Tracker、平移/缩放、K 线/成交量和真实股票 Demo 的设计目标保留在规格文档中，并分别按 M1–M3 门禁推进。
