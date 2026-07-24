# 接入与示例

## Showcase：以股票行情为主线

组件位于 `chartkit`，Android 入口位于 `androidApp`。本轮必须新增可运行的 H5 宿主并复用同一份 `commonMain` 图表 DSL；不维护 Android 与 H5 两套业务图表代码。

完整路线的 `Chart Showcase` 至少包含以下页面。当前 M0 已交付第 2 项中的折线/柱状与主题、点击 Tooltip；其余页面分别随 M1–M3 落地：

1. **行情概览**：股票名称、最新价、涨跌信息、带十字光标的价格折线和成交量柱。
2. **趋势与比较**：折线与柱状图，展示声明式主题、格式化器和基础 Tooltip。
3. **K 线详情**：OHLC K 线与成交量组合，支持时间槽 Tracker、平移和缩放。
4. **实时更新**：模拟新增、修正、截断和替换行情快照，展示选中态与视口恢复策略。
5. **边界实验室**：空数据、单根 K 线、重复时间、非法 OHLC、极值、长标签和密集数据。
6. **性能与交互**：记录可视点数、采样状态、首帧/更新时间，以及 Android/H5 的实际手势表现。

## 折线接入示例

```kotlin
LineChart {
    attr {
        data(ChartSeries("收盘价", dailyPoints, Color(0xFF2F80ED)))
        xAxis { labelFormatter { point, _ -> point.label.orEmpty() } }
        yAxis { tickCount = 5; includeZero = false }
        line { smooth = false; showPoints = false }
        tooltip { mode = TooltipMode.TRACKER }
        interaction { enableTracker = true; enablePan = true; enableZoom = true }
    }
    event {
        onTrackerChanged { tracker -> quotePanel.render(tracker) }
        onViewportChanged { viewport -> quoteStore.saveViewport(viewport) }
    }
}
```

## K 线与成交量接入示例

```kotlin
KLineChart {
    attr {
        data(kLineEntries)
        kLine {
            upColor = Color(0xFFEB5757)
            downColor = Color(0xFF27AE60)
            showLastPrice = true
        }
        volume { visible = true; heightRatio = 0.24f; colorByPriceDirection = true }
        tooltip { mode = TooltipMode.TRACKER; keepOnRelease = false }
        interaction {
            enableTracker = true
            enablePan = true
            enableZoom = true
            minZoom = 1f
            maxZoom = 8f
        }
    }
    event { onTrackerChanged { tracker -> quotePanel.render(tracker) } }
}
```

示例中的数据由业务层转换为不可变 `ChartPoint` 或 `KLineEntry` 列表；网络加载、空/错误/重试 UI 和价格格式本地化应留在业务层。代码片段在实现完成前是目标形态，落地后必须纳入编译测试。

## Android 与 H5 运行要求

Android 的 P0 页面为 `chart_showcase`，`RouterPage` 已提供跳转入口。模块级验证命令为：

```powershell
.\gradlew :chartkit:compileDebugKotlinAndroid
.\gradlew :chartkit:testDebugUnitTest
```

完整 Android APK 已通过下列命令构建：

```powershell
.\gradlew :androidApp:assembleDebug
```

产物为 `androidApp/build/outputs/apk/debug/androidApp-debug.apk`。项目保留官方 Kuikly 使用的 AGP 7.4.2，并在根构建脚本覆盖 D8/R8 为 Google 对 Kotlin 2.1 要求的 8.6.17，解决旧 D8 的 Kotlin metadata 解析失败；依据和哈希见 [11-M0 实现与验收记录](11-m0-implementation-status.md)。

H5 宿主位于 `h5App`，使用官方 `KuiklyRenderViewDelegator`。下列单一任务会先构建 `chartkit` 的 `nativevue2.js`，将业务包和资源嵌入宿主，再生成 production 站点：

```powershell
.\gradlew :h5App:publishChartShowcase
```

产物目录：

```text
h5App/build/dist/js/productionExecutable/
├── index.html
├── h5App.js
├── page/nativevue2.js
└── assets/
```

本地交互调试：

```powershell
.\gradlew :h5App:jsBrowserProductionRun
```

访问 `http://localhost:8080/`，默认加载 `chart_showcase`；也可用 `?page_name=chart_showcase` 显式指定页面。2026-07-24 已在 1280×720 浏览器视口实测：2 个 Canvas 成功挂载、主题切换成功、折线点击命中返回原始系列与条目，控制台无运行时错误。

两端示例复用 `commonMain/ChartShowcasePage.kt` 中同一组确定性样本。十字光标、平移/缩放、K 线成交量组合和数据更新录屏属于 M1–M3 交付门槛，不计入 M0 已完成范围。

## Kuikly AI 股票行情 Demo 接入

仅在组件通过 M0–M2 验收后接入业务 Demo，接入顺序如下：

1. 在 Demo 的数据层将股票行情响应转换为 `ChartPoint` 与 `KLineEntry`，保留时间戳/业务 ID，不把网络对象传入图表。
2. 用 `ChartViewport` 保存用户浏览位置；仅在业务明确要求时用 `followLatest` 跟随最新报价。
3. 将加载、空态、错误、停牌或无成交量提示放在图表容器外部；图表接收合法快照后只负责绘制与交互。
4. 复用组件的 Tracker 回调驱动行情详情面板，并用真实页面录屏验证选择、更新和边界处理。
5. 在 Demo 文档中记录使用的组件版本、API 适配层、Android/H5 验证环境与已知限制。

## 接入检查清单

- [ ] 图表处于有确定尺寸的 Kuikly 容器；零尺寸不应产生可见图形。
- [ ] 每个 K 线时间戳唯一且 OHLC 已在业务/数据层校验。
- [ ] 数据更新使用新列表快照，不原地修改图表内部状态。
- [ ] Tracker、平移和缩放按平台能力开关启用，并处理降级。
- [ ] API/KDoc、Android/H5 示例、自动化测试和 Demo 录屏与当前实现一致。
