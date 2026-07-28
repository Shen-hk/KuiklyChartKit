package com.kuikly.kuiklychartkit.chart.interaction

import com.tencent.kuikly.core.timer.Timer

/**
 * 由图表视图持有的帧动画控制器，用于选中、追踪等交互反馈。
 *
 * 启动新动画会自动取消旧动画；视图在 `viewDestroyed` 中调用 [cancel]，
 * 避免已销毁的图表仍被计时器更新。
 */
internal class ChartFrameAnimation {
    private var timer: Timer? = null

    fun start(
        frameCount: Int,
        onFrame: (linearProgress: Float) -> Unit,
        onFinished: () -> Unit = {},
    ) {
        cancel()
        val totalFrames = frameCount.coerceAtLeast(1)
        var frame = 0
        val activeTimer = Timer()
        timer = activeTimer
        activeTimer.schedule(delay = FRAME_INTERVAL_MS, period = FRAME_INTERVAL_MS) {
            frame += 1
            val progress = (frame.toFloat() / totalFrames).coerceIn(0f, 1f)
            onFrame(progress)
            if (progress >= 1f) {
                activeTimer.cancel()
                if (timer === activeTimer) {
                    timer = null
                    onFinished()
                }
            }
        }
    }

    fun cancel() {
        timer?.cancel()
        timer = null
    }

    private companion object {
        const val FRAME_INTERVAL_MS = 16
    }
}
