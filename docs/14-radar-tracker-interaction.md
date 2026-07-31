# 雷达图长按追踪

## 状态

已实现，默认关闭。该能力在 `radar { trackerEnabled = true }` 后生效。

## 交互契约

1. 长按必须从一个可见的雷达顶点开始；空白区域不会进入追踪。
2. 拖动坐标会投影到被锁定顶点所属的放射轴，垂直于该轴的位移不会改变预览值。
3. 拖动期间只替换当前顶点的临时几何：折线、半透明填充面积、数值标签和追踪提示同步更新；`RadarEntry` 和原始数据列表保持不变。
4. 松手后以 160ms ease-out 回到原始值与原始多边形，随后触发 `onTrackerChanged(null)`。
5. 新的长按开始或视图销毁会取消尚未完成的回弹。

## DSL

```kotlin
RadarChart {
    attr {
        data {
            series("当前") { metric("响应", 86f); metric("解决", 72f); metric("满意度", 91f) }
            series("目标") { metric("响应", 80f); metric("解决", 82f); metric("满意度", 88f) }
        }
        radar {
            trackerEnabled = true
            fillColors = listOf(currentFill, targetFill)
        }
    }
    event {
        onTrackerChanged { tracker ->
            if (tracker == null) return@onTrackerChanged
            showPreview(tracker.selection.item.label, tracker.previewValue)
        }
    }
}
```

`RadarTracker.selection` 始终指向原始系列和原始 `RadarEntry`；`previewValue` 仅表示手势期间的临时值。

## 验收

- 单一顶点沿任意一条放射轴可连续拖动，预览值被图表比例尺钳制。
- 有填充的系列在拖动时只发生局部面积变化；其他顶点和其他系列不移动。
- 释放后 160ms 内回到初始几何，回调最终收到 `null`。
- 图表点击选择和 `onItemSelected` 的既有语义不变。
