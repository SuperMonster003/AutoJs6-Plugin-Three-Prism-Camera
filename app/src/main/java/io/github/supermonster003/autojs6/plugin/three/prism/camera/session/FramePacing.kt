package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

/**
 * The plan for an analysis frame rate (roadmap D18): CameraX 1.6 has no target frame rate for `ImageAnalysis`, so the
 * plugin picks an auto exposure target range of the camera (`CONTROL_AE_TARGET_FPS_RANGE` through `Camera2Interop`)
 * and paces in software when no range ends at the requested rate.
 * zh-CN: 分析帧率的方案 (路线图 D18): CameraX 1.6 的 `ImageAnalysis` 没有目标帧率, 因此插件从相机的自动曝光目标范围中挑选一个 (经 `Camera2Interop`
 * 设置 `CONTROL_AE_TARGET_FPS_RANGE`), 没有范围恰好止于所请求的帧率时再用软件限速.
 *
 * @property range the AE target range to request, or null to leave the camera's own choice.
 * @property throttleFps the software pacing rate, or null for none.
 * @property actualFps the rate answered as `actualFps`.
 */
internal data class FpsPlan(val range: IntRange?, val throttleFps: Int?, val actualFps: Int)

internal object FpsPlanner {

    /** The rate answered when neither the request nor the camera names one. zh-CN: 请求与相机均未给出帧率时的应答值. */
    const val DEFAULT_FPS = 30

    /**
     * [fps] null keeps the camera's choice. Otherwise: a range ending at [fps] (the highest lower bound, the steadiest);
     * else the lowest range ending above it with software pacing; else the fastest range the camera has.
     * zh-CN: [fps] 为 null 时沿用相机的选择. 否则: 止于 [fps] 的范围 (下限最高者, 最平稳); 再否则止于其上的最低范围并软件限速; 再否则相机最快的范围.
     */
    fun plan(fps: Int?, ranges: Collection<IntRange>): FpsPlan {
        val valid = ranges.filter { it.first in 1..it.last }
        if (fps == null) return FpsPlan(null, null, valid.maxOfOrNull { it.last } ?: DEFAULT_FPS)
        if (valid.isEmpty()) return FpsPlan(null, fps, fps)
        valid.filter { it.last == fps }.maxByOrNull { it.first }?.let { return FpsPlan(it, null, fps) }
        val above = valid.filter { it.last > fps }
        if (above.isNotEmpty()) {
            val upper = above.minOf { it.last }
            return FpsPlan(above.filter { it.last == upper }.maxBy { it.first }, fps, fps)
        }
        val upper = valid.maxOf { it.last }
        return FpsPlan(valid.filter { it.last == upper }.maxBy { it.first }, null, upper)
    }

}

/**
 * Software pacing of the analysis frames to [fps] by their sensor timestamps; null or a non-positive rate passes every
 * frame. A frame up to a tenth of the interval early still passes, so a camera running at the requested rate is not
 * thinned by timestamp jitter. Used on the analyzer thread only.
 * zh-CN: 按传感器时间戳把分析帧限速到 [fps]; null 或非正数时全部放行. 提前不超过十分之一间隔的帧仍放行, 使恰以所请求帧率运行的相机不因时间戳抖动而被
 * 抽稀. 只在分析器线程使用.
 */
internal class FramePacer(fps: Int?) {

    private val intervalNanos: Long = if (fps != null && fps > 0) 1_000_000_000L / fps else 0L
    private var dueNanos = 0L
    private var started = false

    fun admit(timestampNanos: Long): Boolean {
        if (intervalNanos == 0L) return true
        if (!started) {
            started = true
            dueNanos = timestampNanos + intervalNanos
            return true
        }
        if (timestampNanos < dueNanos - intervalNanos / 10) return false
        // A frame late by less than an interval keeps the schedule (the average rate holds); after a longer stall the
        // schedule restarts from this frame instead of letting a burst through.
        // zh-CN: 迟到不足一个间隔的帧保持原排期 (平均帧率不变); 更长的停顿之后从本帧重新排期, 而不是放过一串积压的帧.
        dueNanos = if (timestampNanos - dueNanos >= intervalNanos) timestampNanos + intervalNanos else dueNanos + intervalNanos
        return true
    }

}
