# DSL 规范

## 1. 契约目标

DSL 是调用方唯一需要面对的图表配置面。它采用 Kuikly 的 `ViewContainer` 扩展 + `ComposeView<Attr, Event>` 结构：`attr {}` 配置不可变数据、样式和交互，`event {}` 注册业务回调。以下名称是本轮目标公共契约；代码落地后必须与 KDoc、示例和 API 参考完全一致。

## 2. 折线行情示例

```kotlin
LineChart {
    attr {
        data(ChartSeries("收盘价", dailyPoints, Color(0xFF2F80ED)))
        xAxis { labelFormatter { point, _ -> point.label.orEmpty() } }
        yAxis { tickCount = 5; includeZero = false }
        line { smooth = false; showPoints = false }
        tooltip { mode = TooltipMode.TRACKER; keepOnRelease = false }
        interaction {
            enableTracker = true
            enablePan = true
            enableZoom = true
            minZoom = 1f
            maxZoom = 8f
        }
    }
    event {
        onTrackerChanged { tracker -> updateQuotePanel(tracker) }
        onViewportChanged { viewport -> saveViewport(viewport) }
    }
}
```

Tracker 吸附到最近可见 X 槽；多系列折线在同一槽位返回一个 `ChartTracker`，而不是各自寻找最接近的不同 X 值。

## 3. K 线与成交量示例

```kotlin
KLineChart {
    attr {
        data(kLines)
        kLine {
            upColor = Color(0xFFEB5757)
            downColor = Color(0xFF27AE60)
            showLastPrice = true
        }
        volume {
            visible = true
            heightRatio = 0.24f
            colorByPriceDirection = true
        }
        tooltip { mode = TooltipMode.TRACKER }
        interaction { enableTracker = true; enablePan = true; enableZoom = true }
    }
    event { onTrackerChanged { tracker -> showOhlcv(tracker) } }
}
```

`KLineChart` 固定组合价格区和可选成交量区。调用方不能分别给两个区域传入不同的时间序列；每根成交量柱必须与同一 `KLineEntry` 的时间戳对应。

## 4. 数据模型

| 类型 | 必填字段 | 语义 |
| --- | --- | --- |
| `ChartPoint` | `x`、`y` | 折线数据点；`label` 可选。 |
| `BarEntry` | `value`、`label` | 柱状图分类或时间槽数据。 |
| `KLineEntry` | `timestamp`、`open`、`high`、`low`、`close` | 一根按时间排序的 OHLC K 线；`volume?`、`label?`、`id?` 可选。 |
| `ChartSeries<T>` | `name`、`items`、`color?` | 折线/柱图系列及稳定标识。 |
| `ChartSelection<T>` | `seriesIndex`、`itemIndex`、`item`、`stableId` | 单项选择回调；`itemIndex` 指向原始输入。 |
| `ChartTracker` | `slotIndex`、`timestamp?`、`selections` | 十字光标所在 X 槽及该槽全部可见数据。 |
| `ChartViewport` | `startIndex`、`endIndex`、`scale` | 可恢复的可视数据范围；范围为闭区间并会被数据边界钳制。 |

## 5. 配置块和默认值

| 配置 | 关键属性 | 默认与规则 |
| --- | --- | --- |
| `line {}` | `smooth`、`showPoints`、`showValueLabels` | 默认直线、无点、无值标签；无效值导致断线。 |
| `bars {}` | `mode`、`barWidthRatio`、`showValueLabels` | P0 单系列；分组/堆叠是后续显式模式，不静默改变语义。 |
| `kLine {}` | `upColor`、`downColor`、`showLastPrice` | 涨跌颜色可配置；实体与影线都按同一方向颜色绘制。 |
| `volume {}` | `visible`、`heightRatio`、`colorByPriceDirection` | 默认可见，价格区/成交量区之间保留主题定义的间隙。 |
| `tooltip {}` | `mode`、`formatter`、`keepOnRelease` | 默认 `TAP`；`TRACKER` 需 `enableTracker=true`。 |
| `interaction {}` | `enableTracker`、`enablePan`、`enableZoom`、`minZoom`、`maxZoom` | 手势默认关闭；不支持的平台必须降级，不得回调虚假的视口。 |
| `xAxis {}` / `yAxis {}` | `visible`、`tickCount`、formatter | K 线价格与成交量分别配置 Y 轴，但共用 X 轴和视口。 |

## 6. 校验与兼容规则

- `KLineEntry` 的 OHLC 必须为有限数值，且满足 `low <= open/close <= high`；不符合的外部数据被过滤并可通过诊断回调观察。
- `timestamp` 必须在一个 `KLineChart` 中唯一。重复时间戳由数据层明确合并或拒绝，Renderer 不擅自覆盖。
- `minZoom >= 1f` 且 `maxZoom >= minZoom`；负边距、空系列名和非法颜色等开发者错误在 DSL 构建时失败。
- 新增公共字段必须提供保持旧行为的默认值；回调索引必须始终对应原始输入，而非采样后的显示索引。
