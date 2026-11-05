package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import org.autojs.plugin.camera.api.CameraLimits
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

/**
 * The frame ring state machine and the row packing of roadmap P2.2 (protocol "Frame transport").
 * zh-CN: 路线图 P2.2 的帧环状态机与行紧排 (协议 "Frame transport" 一节).
 */
class FrameRingTest {

    private fun FrameRing.block(): FrameRing.Slot.Block = acquire() as FrameRing.Slot.Block

    private fun FrameRing.deliver(): FrameRing.Slot.Block = block().also(::publish)

    @Test
    fun `three blocks rotate with increasing seq starting at 1`() {
        val ring = FrameRing()
        assertEquals(CameraLimits.FRAME_BUFFER_COUNT, ring.size)
        val first = ring.deliver()
        val second = ring.deliver()
        val third = ring.deliver()
        assertEquals(listOf(0 to 1L, 1 to 2L, 2 to 3L), listOf(first, second, third).map { it.index to it.seq })
        assertEquals(3L, ring.lastSeq)
        assertEquals(3, ring.heldCount())
    }

    @Test
    fun `a full ring drops and reports every thirtieth drop`() {
        val ring = FrameRing()
        repeat(3) { ring.deliver() }
        val drops = (1..CameraLimits.DROPPED_REPORT_INTERVAL).map { ring.acquire() as FrameRing.Slot.Dropped }
        assertEquals((1L..CameraLimits.DROPPED_REPORT_INTERVAL.toLong()).toList(), drops.map { it.droppedCount })
        assertEquals(listOf(CameraLimits.DROPPED_REPORT_INTERVAL.toLong()), drops.filter { it.report }.map { it.droppedCount })
        assertEquals(CameraLimits.DROPPED_REPORT_INTERVAL.toLong(), ring.droppedCount)
    }

    @Test
    fun `a released block is reused and the seq keeps counting`() {
        val ring = FrameRing()
        val first = ring.deliver()
        ring.deliver()
        ring.deliver()
        assertTrue(ring.release(first.index, first.seq))
        val reused = ring.deliver()
        assertEquals(first.index, reused.index)
        assertEquals(4L, reused.seq)
    }

    @Test
    fun `round robin starts after the block written last`() {
        val ring = FrameRing()
        val a = ring.deliver()
        assertTrue(ring.release(a.index, a.seq))
        // Block 0 is free again, but the next frame goes to block 1.
        // zh-CN: 块 0 已重新空闲, 但下一帧写入块 1.
        assertEquals(1, ring.deliver().index)
        assertEquals(2, ring.deliver().index)
        assertEquals(0, ring.deliver().index)
    }

    @Test
    fun `late, repeated and foreign releases are ignored`() {
        val ring = FrameRing()
        val first = ring.deliver()
        assertFalse(ring.release(first.index, first.seq + 1))
        assertFalse(ring.release(5, first.seq))
        assertFalse(ring.release(first.index, 0L))
        assertTrue(ring.release(first.index, first.seq))
        assertFalse("a repeated release", ring.release(first.index, first.seq))
        assertEquals(0, ring.heldCount())
    }

    @Test
    fun `an aborted write keeps the block free and the seq unused`() {
        val ring = FrameRing()
        val block = ring.block()
        ring.abort(block)
        val again = ring.block()
        assertEquals(block, again)
        ring.publish(again)
        assertEquals(1L, ring.lastSeq)
    }

    @Test(expected = IllegalStateException::class)
    fun `only one block is written at a time`() {
        val ring = FrameRing()
        ring.acquire()
        ring.acquire()
    }

    @Test
    fun `padded rows are packed to width times four`() {
        val width = 3
        val height = 2
        val rowStride = 16
        val source = ByteBuffer.allocate(rowStride + width * 4)
        for (row in 0 until height) for (byte in 0 until width * 4) source.put(row * rowStride + byte, (row * 100 + byte).toByte())
        val target = ByteBuffer.allocate(width * 4 * height)
        RgbaPacker.pack(source, rowStride, width, height, target)
        val expected = ByteArray(width * 4 * height) { (it / (width * 4) * 100 + it % (width * 4)).toByte() }
        assertArrayEquals(expected, target.array())
        assertEquals("the source position stays", 0, source.position())
        assertEquals("the target position stays", 0, target.position())
    }

    @Test
    fun `packed rows are copied in one go from the source position`() {
        val source = ByteBuffer.allocate(4 + 8)
        for (i in 0 until 12) source.put(i, i.toByte())
        source.position(4)
        val target = ByteBuffer.allocate(8)
        RgbaPacker.pack(source, 4, 1, 2, target)
        assertArrayEquals(ByteArray(8) { (it + 4).toByte() }, target.array())
        assertEquals(4, source.position())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a short plane is refused`() {
        RgbaPacker.pack(ByteBuffer.allocate(10), 4, 1, 3, ByteBuffer.allocate(12))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a stride below the row is refused`() {
        RgbaPacker.pack(ByteBuffer.allocate(64), 8, 3, 2, ByteBuffer.allocate(24))
    }

}
