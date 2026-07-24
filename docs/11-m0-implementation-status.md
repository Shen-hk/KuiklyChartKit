# M0 实现与验收记录

> 基线日期：2026-07-24  
> 适用范围：P0 / M0 基础组件  
> 结论：Line、Bar、Kuikly DSL、点击选择与基础 Tooltip、Android/H5 共用 Showcase、算法测试、Android Debug APK 和 H5 production 站点已落地。Tracker、平移/缩放、K 线/成交量和真实股票 Demo 仍属于 M1–M3，未宣称完成。

## 1. M0 范围冻结

M0 对应需求文档中的 FR-01、FR-02、FR-03，以及点击选择和基础 Tooltip。为了让评审可以快速判断完成度，本阶段只按下列五项验收：

1. `LineChart` 可用 Kuikly DSL 声明多系列、直线/平滑线、点、坐标轴、网格、图例、空态和主题。
2. `BarChart` 可声明分类柱、正负值零基线、数值标签、圆角、主题、空态和点击选择。
3. 点击结果映射回原始 `seriesIndex`、`itemIndex`、系列名和数据项；Tooltip 不越过画布边界。
4. Android 与 H5 复用同一个 `commonMain` 页面和确定性数据，不维护两套业务 UI。
5. 核心刻度、布局、格式化和命中算法有自动化测试；双端至少完成模块编译或实际浏览器运行验证。

以下能力不在 M0 完成声明内：

- M1：长按/滑动 Tracker、十字光标生命周期、横向平移和缩放；
- M2：K 线、成交量组合、视口和动态行情恢复；
- M3：完整 API/视觉证据包及 Kuikly AI 股票行情 Demo 的真实接入。

## 2. 已实现功能

| 能力 | 实现位置 | M0 行为 |
| --- | --- | --- |
| 公共数据与主题 | `chart/ChartModels.kt` | `ChartPoint`、`BarEntry`、`ChartSeries<T>`、`ChartSelection<T>`、4 套主题和各 DSL options。 |
| 确定性刻度与布局 | `chart/ChartLayout.kt` | 空/单值/等值/跨零/非法值安全域、nice ticks、Line/Bar 坐标映射和命中结构。 |
| 折线组件 | `chart/ChartComponents.kt` | `ComposeView<LineChartAttr, LineChartEvent>`；多系列、折线/平滑曲线、坏点断线、图例、点击选点、选择引导线和 Tooltip。 |
| 柱状组件 | `chart/ChartComponents.kt` | `ComposeView<BarChartAttr, BarChartEvent>`；多系列并列绘制、正负柱、零基线、值标签、点击命中和 Tooltip；`SINGLE` 模式拒绝多系列以避免静默丢数据。 |
| 官方风格 DSL | `chart/ChartComponents.kt` | `ViewContainer<*, *>.LineChart/BarChart` + `attr {}` + `event {}`；非法 tick、线宽、柱宽比例等通过 `require` 给出字段级错误。 |
| Android/H5 共用页 | `ChartShowcasePage.kt` | 页面名 `chart_showcase`；双 Canvas、浅/深主题、折线和柱状点击结果区。 |
| Android 导航 | `RouterPage.kt` | 可从现有路由页进入 P0 Showcase。 |
| H5 真宿主 | `h5App` | 官方 Web Render 2.7.0-2.1.21、业务 JS 自动嵌入、production webpack 和浏览器生命周期。 |
| 自动化测试 | `chart/ChartLayoutTest.kt` | nice scale、空/等值/跨零/非法数据、布局、Line/Bar 命中和紧凑数值格式。 |

## 3. DSL 最小示例

```kotlin
LineChart {
    attr {
        data(
            ChartSeries(
                name = "收盘价",
                items = listOf(
                    ChartPoint(1f, 12.4f, "周一"),
                    ChartPoint(2f, 13.1f, "周二"),
                ),
            ),
        )
        theme = ChartTheme.ocean()
        yAxis { tickCount = 5; includeZero = false }
        line { smooth = true; showPoints = true }
        tooltip { enabled = true }
    }
    event {
        onItemSelected { selection ->
            // selection.item 是原始 ChartPoint
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
            barWidthRatio = 0.68f
            showValueLabels = true
            cornerRadius = 4f
        }
    }
    event { onItemSelected { selection -> /* 原始 BarEntry */ } }
}
```

## 4. H5 架构与一键复现

H5 遵循 Kuikly 官方宿主拆分：

```text
ChartShowcasePage / LineChart / BarChart (commonMain)
                 │
                 ▼
chartkit nativevue2.js       注册 Kuikly 页面
                 │ 先加载
                 ▼
h5App.js                     KuiklyRenderViewDelegator 挂载 root
                 │
                 ▼
浏览器 DOM + 2 个 Canvas
```

生产构建：

```powershell
.\gradlew :h5App:publishChartShowcase
```

任务依赖链：

```text
:chartkit:jsBrowserDistribution
        → :h5App:syncChartBundle
        → :h5App:jsBrowserDistribution
        → :h5App:publishChartShowcase
```

完整产物：

```text
h5App/build/dist/js/productionExecutable/
├── index.html
├── h5App.js
├── h5App.js.map
├── page/nativevue2.js
└── assets/image_adapter/sample.png
```

本地浏览器运行：

```powershell
.\gradlew :h5App:jsBrowserProductionRun
```

打开 `http://localhost:8080/`。默认页面为 `chart_showcase`，可使用查询参数 `?page_name=chart_showcase` 显式指定。

## 5. 验收证据

### 5.1 已通过

| 命令/检查 | 结果 | 说明 |
| --- | --- | --- |
| `.\gradlew :chartkit:testDebugUnitTest` | 通过 | 12 个 commonTest 在 Android unit-test 目标执行，0 failure。 |
| `.\gradlew :chartkit:compileDebugKotlinAndroid` | 通过 | P0 公共代码、Kuikly DSL 和 Android 目标编译通过。 |
| `.\gradlew :chartkit:compileKotlinJs` | 通过 | 同一 `commonMain` 组件编译到 Kotlin/JS。 |
| `.\gradlew :h5App:compileKotlinJs` | 通过 | 官方 Web Render API 与 2.7.0-2.1.21 依赖匹配。 |
| `.\gradlew :h5App:publishChartShowcase` | 通过 | production 站点和业务包自动组装完成。 |
| `.\gradlew :androidApp:assembleDebug` | 通过 | Debug APK 完整经过资源、D8/Dex 和打包链路。 |
| 浏览器首屏 | 通过 | 1280×720 下 `root` 成功挂载，DOM 检测到 2 个 Canvas。 |
| 主题交互 | 通过 | 点击“切换到深色主题”后控件变为“切换到浅色主题”。 |
| 折线点击 | 通过 | 点击末端数据点，页面反馈 `本周 · 周日: 276`。 |
| 浏览器控制台 | 通过 | 无运行时 error；仅有 webpack 体积建议 warning。 |

### 5.2 Android D8/R8 基线修复

首次执行 `.\gradlew :androidApp:assembleDebug` 时，既有 AGP 7.4.2 自带的旧 D8 在 `mergeExtDexDebug` 解析 Kotlin 2.1 依赖时报告 `com.android.tools.r8.kotlin.H`。Google 的 Kotlin/D8/R8 兼容矩阵要求 Kotlin 2.1 使用 R8 8.6.17；根 `build.gradle.kts` 因此显式加入：

```kotlin
classpath("com.android.tools:r8:8.6.17")
```

该修复只覆盖 D8/R8，不改变 Kuikly 2.7.0、Kotlin 2.1.21、Gradle 8.5、AGP 7.4.2 或组件 API。修复后 `:androidApp:assembleDebug` 完整通过：

- APK：`androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- 大小：4,267,769 bytes
- SHA-256：`B5C24678240FB30AD63D02AD854714BC598E31186416C34B637161E407F98C20`
- 依据：[Google 官方 Kotlin/AGP/D8/R8 兼容矩阵](https://developer.android.com/build/kotlin-support)

### 5.3 剩余非门禁问题

`.\gradlew :chartkit:compileCommonMainKotlinMetadata` 会被仓库既有 KSP 生成文件 `KuiklyCoreEntry.kt` 的 metadata 依赖解析问题阻断。Android 与 JS 实际目标、Android APK、H5 production 站点均已通过，因此 M0 使用实际平台任务而非这个聚合 metadata 任务作为门禁。

## 6. 官方实现对齐

本实现对齐 Kuikly 官方仓库中的以下模式：

- 页面：`@Page` + `Pager` + `body(): ViewBuilder`；
- 组件：`ComposeView<Attr, Event>` + `ViewContainer` 扩展；
- 绘制：Kuikly `Canvas` 与 `CanvasContext`，不在 `commonMain` 泄漏 Android/H5 类型；
- H5：业务包先注册页面，宿主再由 `KuiklyRenderViewDelegator` 调用 `onAttach/onResume/onPause/onDetach`；
- 版本：业务核心、KSP、Android Renderer 和 Web Renderer统一匹配 `2.7.0-2.1.21`。

参考：

- [KuiklyUI 官方仓库](https://github.com/Tencent-TDS/KuiklyUI)
- [Issue #1477](https://github.com/Tencent-TDS/KuiklyUI/issues/1477)
- [官方 Web Render base 构建](https://github.com/Tencent-TDS/KuiklyUI/blob/main/core-render-web/base/build.gradle.kts)
- [官方 Web Render h5 构建](https://github.com/Tencent-TDS/KuiklyUI/blob/main/core-render-web/h5/build.gradle.kts)
- [官方 h5App 入口](https://github.com/Tencent-TDS/KuiklyUI/blob/main/h5App/src/jsMain/kotlin/Main.kt)

## 7. 进入 M1 的条件

M1 开始前必须保持以下基线持续为绿：

- `LineChart` / `BarChart` DSL 不破坏；
- 点击选择的原始索引与数据项语义不改变；
- Android 模块编译、commonTest、JS 编译和 H5 production 构建持续通过；
- Tracker 手势先完成 Android/H5 能力探针，再公开 API；
- 平移/缩放必须定义与外层滚动的手势仲裁，不以平台私有回调污染 `commonMain`。
