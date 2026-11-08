package io.github.supermonster003.autojs6.plugin.three.prism.camera

/**
 * The start bookkeeping of [ThreePrismCameraForegroundService] (appendix D Q64 = (a)), free of Android types for the JVM
 * tests; main thread only. A start is under way from [begin] until the service's `onStartCommand` ([arrived]) or a
 * refusal. Android may drop a start without an exception: `Context.startForegroundService` answers null (HyperOS, API 35,
 * when the plugin is restricted in the background and AutoJs6 is not visible), or it answers the component and
 * `onStartCommand` never comes. [requested] turns both into a refusal (the reason at once, or through `onTimeout` after
 * [timeoutMillis]), so the next session starts the service again instead of waiting for a start that never comes.
 * zh-CN: [ThreePrismCameraForegroundService] 的启动簿记 (附录 D Q64 = (a)), 不含 Android 类型, 便于 JVM 测试; 只在主线程使用. 从 [begin] 起启动进行中,
 * 直到服务的 `onStartCommand` ([arrived]) 或被拒. Android 可能不抛异常就丢弃启动: `Context.startForegroundService` 返回 null (HyperOS, API 35,
 * 插件受后台限制且 AutoJs6 不可见时), 或返回组件但 `onStartCommand` 始终不来. [requested] 把两者都变成被拒 (立即返回原因, 或在 [timeoutMillis]
 * 后经 `onTimeout` 告知), 使下一个会话重新启动服务, 而不是一直等待不会到来的启动.
 */
internal class ForegroundStartWatch(
    private val postDelayed: (delayMillis: Long, action: () -> Unit) -> Unit,
    private val timeoutMillis: Long = TIMEOUT_MILLIS,
) {

    /** A start is under way. zh-CN: 启动进行中. */
    var starting = false
        private set

    /** Counts the starts, so a late timer of an earlier start leaves a newer one alone. zh-CN: 启动计数, 使较早启动的迟到计时器不影响较新的启动. */
    var attempt = 0
        private set

    /** Begins a start; false when one is under way already. zh-CN: 开始一次启动; 已有启动进行中时返回 false. */
    fun begin(): Boolean {
        if (starting) return false
        starting = true
        attempt++
        return true
    }

    /** Whether start [attempt] is still under way. zh-CN: 第 [attempt] 次启动是否仍在进行. */
    fun isCurrent(attempt: Int): Boolean = starting && this.attempt == attempt

    /** The service's `onStartCommand` came: the start is over, whatever it answers. zh-CN: 服务的 `onStartCommand` 已到: 启动结束, 无论其结果如何. */
    fun arrived() {
        starting = false
    }

    /** The start call threw: the start is over as a refusal. zh-CN: 启动调用抛出异常: 启动以被拒结束. */
    fun abandon() {
        starting = false
    }

    /**
     * `Context.startForegroundService` returned; [accepted] is false when it answered null. Answers the reason of a
     * dropped start. An accepted start arms the timeout: without [arrived] within [timeoutMillis] the start ends and
     * [onTimeout] hears the reason. A call while no start is under way does nothing.
     * zh-CN: `Context.startForegroundService` 已返回; 返回 null 时 [accepted] 为 false. 被丢弃的启动返回原因. 被接受的启动开始计时: [timeoutMillis]
     * 内没有 [arrived] 时启动结束, 并把原因告知 [onTimeout]. 没有启动进行中时调用不做任何事.
     */
    fun requested(accepted: Boolean, onTimeout: (reason: String) -> Unit): String? {
        if (!starting) return null
        if (!accepted) {
            starting = false
            return REASON_DROPPED
        }
        val mine = attempt
        postDelayed(timeoutMillis) {
            if (isCurrent(mine)) {
                starting = false
                onTimeout("the camera foreground service did not start within $timeoutMillis ms (no onStartCommand)")
            }
        }
        return null
    }

    companion object {

        /** How long an accepted start may take to reach `onStartCommand`. zh-CN: 被接受的启动到达 `onStartCommand` 的等待上限. */
        const val TIMEOUT_MILLIS = 5_000L

        const val REASON_DROPPED = "the system dropped the start of the camera foreground service (startForegroundService answered null)"
    }

}
