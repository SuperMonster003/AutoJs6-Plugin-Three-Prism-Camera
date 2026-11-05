package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import org.autojs.plugin.camera.api.CameraContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The exclusivity state machine of roadmap P2.1 (D17, appendix D Q7 = (a)): one holder in the whole plugin,
 * `CAMERA_BUSY` with the holder, the viewfinder taking over with close reason `pluginUi`.
 * zh-CN: 路线图 P2.1 的独占状态机 (D17, 附录 D Q7 = (a)): 整个插件只有一个持有者, `CAMERA_BUSY` 带持有者, 取景页以关闭原因 `pluginUi` 接管.
 */
class CameraExclusivityTest {

    private val exclusivity = CameraExclusivity()

    @Test
    fun `a second session is answered with the first holder`() {
        val first = CameraExclusivity.Holder("/sdcard/Scripts/first.js", viewfinder = false)
        val second = CameraExclusivity.Holder("/sdcard/Scripts/second.js", viewfinder = false)
        assertNull(exclusivity.acquire(first))
        val busy = exclusivity.acquire(second)
        assertSame(first, busy)
        assertEquals("/sdcard/Scripts/first.js", busy?.busyLabel)
        assertSame(first, exclusivity.holder())
    }

    @Test
    fun `acquiring again as the same holder is not busy`() {
        val holder = CameraExclusivity.Holder("a", viewfinder = false)
        assertNull(exclusivity.acquire(holder))
        assertNull(exclusivity.acquire(holder))
    }

    @Test
    fun `only the holder releases the camera`() {
        val first = CameraExclusivity.Holder("a", viewfinder = false)
        val other = CameraExclusivity.Holder("b", viewfinder = false)
        exclusivity.acquire(first)
        assertFalse(exclusivity.release(other))
        assertSame(first, exclusivity.holder())
        assertTrue(exclusivity.release(first))
        assertFalse("a repeated release", exclusivity.release(first))
        assertNull(exclusivity.holder())
        assertNull(exclusivity.acquire(other))
    }

    @Test
    fun `the viewfinder takes over and the script session is told to close`() {
        val script = CameraExclusivity.Holder("a", viewfinder = false)
        var preempted = 0
        script.onPreempted = { preempted++ }
        exclusivity.acquire(script)
        val viewfinder = CameraExclusivity.Holder("", viewfinder = true)
        assertSame(script, exclusivity.preemptForViewfinder(viewfinder))
        assertEquals(1, preempted)
        assertSame(viewfinder, exclusivity.holder())
        assertFalse("the closing session no longer owns the camera", exclusivity.release(script))
        val next = CameraExclusivity.Holder("b", viewfinder = false)
        assertEquals(CameraContract.HOLDER_PLUGIN_UI, exclusivity.acquire(next)?.busyLabel)
        assertTrue(exclusivity.release(viewfinder))
    }

    @Test
    fun `a viewfinder on a free camera preempts nobody`() {
        val viewfinder = CameraExclusivity.Holder("", viewfinder = true)
        assertNull(exclusivity.preemptForViewfinder(viewfinder))
        assertNull("the same viewfinder again", exclusivity.preemptForViewfinder(viewfinder))
    }

    @Test
    fun `a failing preemption callback still hands the camera over`() {
        val script = CameraExclusivity.Holder("a", viewfinder = false)
        script.onPreempted = { throw IllegalStateException("closing failed") }
        exclusivity.acquire(script)
        val viewfinder = CameraExclusivity.Holder("", viewfinder = true)
        assertSame(script, exclusivity.preemptForViewfinder(viewfinder))
        assertSame(viewfinder, exclusivity.holder())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `only the viewfinder preempts`() {
        exclusivity.preemptForViewfinder(CameraExclusivity.Holder("a", viewfinder = false))
    }

}
