package io.github.supermonster003.autojs6.plugin.three.prism.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The start bookkeeping of the camera foreground service (appendix D Q64 = (a)): a start Android drops silently, one
 * that never reaches `onStartCommand`, and a normal one. The main thread handler is a list of timers run by hand.
 * zh-CN: 相机前台服务的启动簿记 (附录 D Q64 = (a)): 被 Android 静默丢弃的启动, 始终没有到达 `onStartCommand` 的启动, 以及正常的启动. 主线程 Handler
 * 换成手动运行的计时器列表.
 */
class ForegroundStartWatchTest {

    private class Timers {
        val pending = ArrayList<Pair<Long, () -> Unit>>()
        fun post(delayMillis: Long, action: () -> Unit) {
            pending += delayMillis to action
        }

        fun runAll() {
            val due = pending.toList()
            pending.clear()
            due.forEach { it.second() }
        }
    }

    private val timers = Timers()
    private val watch = ForegroundStartWatch(timers::post)
    private val timeouts = ArrayList<String>()

    @Test
    fun `a start answered with null is refused at once and the next session starts again`() {
        assertTrue(watch.begin())
        assertTrue(watch.starting)
        val reason = watch.requested(accepted = false) { timeouts += it }
        assertEquals(ForegroundStartWatch.REASON_DROPPED, reason)
        assertFalse("the start is over, so acquire does not wait for it", watch.starting)
        assertTrue("no timer for a dropped start", timers.pending.isEmpty())
        // The bug of appendix D Q64: `starting` stayed true and every later acquire did nothing.
        // zh-CN: 附录 D Q64 的缺陷: `starting` 一直为 true, 之后的 acquire 什么都不做.
        assertTrue("a later session begins a new start", watch.begin())
        assertTrue(timeouts.isEmpty())
    }

    @Test
    fun `an accepted start without onStartCommand is refused after the timeout`() {
        assertTrue(watch.begin())
        assertNull(watch.requested(accepted = true) { timeouts += it })
        assertTrue(watch.starting)
        assertEquals(listOf(ForegroundStartWatch.TIMEOUT_MILLIS), timers.pending.map { it.first })
        assertEquals("about 5 seconds", 5_000L, ForegroundStartWatch.TIMEOUT_MILLIS)
        assertFalse("a start under way answers the next session too", watch.begin())
        timers.runAll()
        assertEquals(1, timeouts.size)
        assertTrue(timeouts.single(), "5000 ms" in timeouts.single() && "onStartCommand" in timeouts.single())
        assertFalse(watch.starting)
        assertTrue("a later session begins a new start", watch.begin())
    }

    @Test
    fun `a normal start ends with onStartCommand and its timer does nothing`() {
        assertTrue(watch.begin())
        assertNull(watch.requested(accepted = true) { timeouts += it })
        watch.arrived()
        assertFalse(watch.starting)
        timers.runAll()
        assertTrue("onStartCommand came in time", timeouts.isEmpty())
    }

    @Test
    fun `a late timer of an earlier start leaves a newer start alone`() {
        assertTrue(watch.begin())
        assertNull(watch.requested(accepted = true) { timeouts += "first: $it" })
        watch.arrived()
        // The next session begins before the first timer fires (the starter activity of API 34+ is still on its way).
        // zh-CN: 第一个计时器触发之前下一个会话已开始启动 (API 34+ 的启动 Activity 还在途中).
        assertTrue(watch.begin())
        val second = watch.attempt
        timers.runAll()
        assertTrue(timeouts.isEmpty())
        assertTrue(watch.isCurrent(second))
        assertNull(watch.requested(accepted = true) { timeouts += "second: $it" })
        timers.runAll()
        assertEquals(1, timeouts.size)
        assertTrue(timeouts.single(), timeouts.single().startsWith("second: "))
    }

    @Test
    fun `a start call that throws ends the start`() {
        // ColorOS throws ForegroundServiceStartNotAllowedException when the host is in the background (OPD2413).
        // zh-CN: 宿主在后台时 ColorOS 抛出 ForegroundServiceStartNotAllowedException (OPD2413).
        assertTrue(watch.begin())
        watch.abandon()
        assertFalse(watch.starting)
        assertTrue(watch.begin())
    }

    @Test
    fun `a second start call of the same attempt arms one more timer but refuses once`() {
        // The starter activity may come up after its own timeout started the service already (API 34+).
        // zh-CN: 启动 Activity 可能在其超时已启动服务之后才出现 (API 34+).
        assertTrue(watch.begin())
        assertNull(watch.requested(accepted = true) { timeouts += it })
        assertNull(watch.requested(accepted = true) { timeouts += it })
        assertEquals(2, timers.pending.size)
        timers.runAll()
        assertEquals(1, timeouts.size)
    }

    @Test
    fun `a start call while no start is under way does nothing`() {
        assertNull(watch.requested(accepted = false) { timeouts += it })
        assertNull(watch.requested(accepted = true) { timeouts += it })
        assertTrue(timers.pending.isEmpty())
        assertFalse(watch.starting)
    }

    @Test
    fun `isCurrent names the start under way only`() {
        assertFalse(watch.isCurrent(watch.attempt))
        assertTrue(watch.begin())
        val attempt = watch.attempt
        assertTrue(watch.isCurrent(attempt))
        assertFalse(watch.isCurrent(attempt - 1))
        watch.arrived()
        assertFalse(watch.isCurrent(attempt))
    }

}
