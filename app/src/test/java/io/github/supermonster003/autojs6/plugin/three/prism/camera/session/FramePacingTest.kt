package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The analysis frame rate plan and the software pacing of roadmap D18.
 * zh-CN: 路线图 D18 的分析帧率方案与软件限速.
 */
class FramePacingTest {

    private val ranges = listOf(15..15, 7..30, 30..30, 15..30, 60..60)

    @Test
    fun `no requested rate keeps the camera's choice`() {
        assertEquals(FpsPlan(null, null, 60), FpsPlanner.plan(null, ranges))
        assertEquals(FpsPlan(null, null, FpsPlanner.DEFAULT_FPS), FpsPlanner.plan(null, emptyList()))
    }

    @Test
    fun `a range ending at the rate wins, the steadiest first`() {
        assertEquals(FpsPlan(30..30, null, 30), FpsPlanner.plan(30, ranges))
        assertEquals(FpsPlan(15..15, null, 15), FpsPlanner.plan(15, ranges))
    }

    @Test
    fun `otherwise the lowest range above with software pacing`() {
        assertEquals(FpsPlan(15..15, 10, 10), FpsPlanner.plan(10, ranges))
        assertEquals(FpsPlan(30..30, 24, 24), FpsPlanner.plan(24, ranges))
        assertEquals(FpsPlan(60..60, 45, 45), FpsPlanner.plan(45, ranges))
    }

    @Test
    fun `a rate above every range gets the fastest one`() {
        assertEquals(FpsPlan(30..30, null, 30), FpsPlanner.plan(60, listOf(7..30, 30..30)))
    }

    @Test
    fun `invalid ranges are ignored and no range paces in software`() {
        assertEquals(FpsPlan(null, 20, 20), FpsPlanner.plan(20, listOf(0..30, 30..15)))
        assertEquals(FpsPlan(null, null, 30), FpsPlanner.plan(null, listOf(0..30)))
    }

    @Test
    fun `no rate passes every frame`() {
        val pacer = FramePacer(null)
        assertTrue((0 until 10).all { pacer.admit(it * 1_000L) })
        assertTrue(FramePacer(0).admit(0L))
    }

    @Test
    fun `frames are thinned to the rate with a tenth of jitter tolerance`() {
        val interval = 100_000_000L
        val pacer = FramePacer(10)
        // A 30 fps camera paced to 10 fps passes every third frame.
        // zh-CN: 30 fps 的相机限速到 10 fps 时每三帧放行一帧.
        val admitted = (0 until 30).filter { pacer.admit(it * interval / 3) }
        assertEquals(listOf(0, 3, 6, 9, 12, 15, 18, 21, 24, 27), admitted)
        val steady = FramePacer(10)
        assertTrue(steady.admit(0L))
        assertTrue("slightly early", steady.admit(interval - interval / 20))
        assertFalse("too early", steady.admit(interval + interval / 2))
        assertTrue(steady.admit(2 * interval))
    }

    @Test
    fun `a stall restarts the schedule instead of letting a burst through`() {
        val interval = 100_000_000L
        val pacer = FramePacer(10)
        assertTrue(pacer.admit(0L))
        assertTrue(pacer.admit(10 * interval))
        assertFalse(pacer.admit(10 * interval + interval / 2))
        assertTrue(pacer.admit(11 * interval))
    }

}
