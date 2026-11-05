package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import org.autojs.plugin.camera.api.CameraLimits
import java.nio.ByteBuffer

/**
 * The ring of frame blocks of one analysis (protocol "Frame transport", roadmap D9, D26): a block is free, being written
 * by the analyzer, or held by the host with the `seq` of the frame in it until `releaseFrame(bufferIndex, seq)` returns
 * it. No free block means a drop; drops are reported every [CameraLimits.DROPPED_REPORT_INTERVAL]. `seq` starts at 1
 * for every analysis. Thread safe: the analyzer acquires and publishes, Binder threads release.
 * zh-CN: 一次分析的帧块环 (协议 "Frame transport" 一节, 路线图 D9, D26): 块处于空闲, 正被分析器写入, 或由宿主持有 (记着其中帧的 `seq`), 直到
 * `releaseFrame(bufferIndex, seq)` 归还. 没有空闲块即丢帧; 每 [CameraLimits.DROPPED_REPORT_INTERVAL] 次丢帧报告一次. 每次分析的 `seq` 从 1 开始.
 * 线程安全: 分析器取块与发布, Binder 线程归还.
 */
internal class FrameRing(val size: Int = CameraLimits.FRAME_BUFFER_COUNT) {

    sealed interface Slot {
        /** A block to write the frame [seq] into. zh-CN: 写入帧 [seq] 的块. */
        data class Block(val index: Int, val seq: Long) : Slot

        /** No block was free; [droppedCount] counts from the start, [report] asks for `onDropped`. zh-CN: 没有空闲块; [report] 表示应发送 `onDropped`. */
        data class Dropped(val droppedCount: Long, val report: Boolean) : Slot
    }

    init {
        require(size > 0) { "size must be positive" }
    }

    // The seq of the frame the host holds per block, 0 when free.
    // zh-CN: 每块中宿主持有的帧的 seq, 空闲时为 0.
    private val held = LongArray(size)
    private var writing = -1
    private var nextSeq = 1L
    private var cursor = 0

    var droppedCount = 0L
        @Synchronized get
        private set

    /** The seq of the last published frame, 0 before the first. zh-CN: 最后发布的帧的 seq, 首帧前为 0. */
    var lastSeq = 0L
        @Synchronized get
        private set

    @Synchronized
    fun acquire(): Slot {
        check(writing < 0) { "a block is being written already" }
        // Round robin from the block after the last one written, so the host gets the most time with every block.
        // zh-CN: 从上次写入的下一块开始轮询, 让宿主持有每块的时间尽量长.
        for (step in 0 until size) {
            val index = (cursor + step) % size
            if (held[index] == 0L) {
                writing = index
                return Slot.Block(index, nextSeq)
            }
        }
        droppedCount++
        return Slot.Dropped(droppedCount, droppedCount % CameraLimits.DROPPED_REPORT_INTERVAL == 0L)
    }

    /** The frame of [block] goes to the host now. zh-CN: [block] 中的帧即将交给宿主. */
    @Synchronized
    fun publish(block: Slot.Block) {
        check(writing == block.index && block.seq == nextSeq) { "not the block being written" }
        held[block.index] = block.seq
        writing = -1
        lastSeq = block.seq
        nextSeq++
        cursor = (block.index + 1) % size
    }

    /** Writing [block] failed; it stays free and its seq unused. zh-CN: 写入 [block] 失败; 它保持空闲, seq 不消耗. */
    @Synchronized
    fun abort(block: Slot.Block) {
        if (writing == block.index) writing = -1
    }

    /**
     * The host is done with frame [seq] of block [index]; a release that does not match (a late or a repeated one) is
     * ignored and answers false.
     * zh-CN: 宿主用完了块 [index] 中的帧 [seq]; 不匹配的归还 (迟到或重复) 被忽略并返回 false.
     */
    @Synchronized
    fun release(index: Int, seq: Long): Boolean {
        if (index !in 0 until size || seq <= 0L || held[index] != seq) return false
        held[index] = 0L
        return true
    }

    /** The number of blocks the host holds. zh-CN: 宿主持有的块数. */
    @Synchronized
    fun heldCount(): Int = held.count { it != 0L }

}

/**
 * Copies an RGBA_8888 plane into a frame block with packed rows (`rowStride = width * 4`, protocol "Frame transport"
 * step 3), dropping the row padding of the source.
 * zh-CN: 把 RGBA_8888 平面拷入帧块并紧排各行 (`rowStride = width * 4`, 协议 "Frame transport" 第 3 步), 去掉源的行填充.
 */
internal object RgbaPacker {

    /**
     * Writes [height] rows of `width * 4` bytes from [source] (rows [rowStride] bytes apart, from its position) to
     * [target] at its position. The positions of both buffers stay unchanged; the last source row may lack its padding.
     * zh-CN: 从 [source] (自其 position 起, 行间距 [rowStride] 字节) 写 [height] 行, 每行 `width * 4` 字节, 到 [target] 的 position 处.
     * 两个缓冲的 position 均不变; 源的最后一行可以没有填充.
     */
    fun pack(source: ByteBuffer, rowStride: Int, width: Int, height: Int, target: ByteBuffer) {
        require(width > 0 && height > 0) { "the size must be positive" }
        val rowBytes = width * CameraLimits.BYTES_PER_PIXEL
        require(rowStride >= rowBytes) { "rowStride $rowStride is below ${rowBytes}" }
        val base = source.position()
        val needed = (height - 1).toLong() * rowStride + rowBytes
        require(source.limit() - base >= needed) { "the plane holds ${source.limit() - base} bytes, $needed needed" }
        require(target.remaining() >= rowBytes.toLong() * height) { "the block holds ${target.remaining()} bytes, ${rowBytes.toLong() * height} needed" }
        val from = source.duplicate()
        val to = target.duplicate()
        if (rowStride == rowBytes) {
            from.limit(base + rowBytes * height)
            from.position(base)
            to.put(from)
            return
        }
        for (row in 0 until height) {
            val start = base + row * rowStride
            // Raise the limit first: the new position lies beyond the old limit.
            // zh-CN: 先提高 limit: 新的 position 在旧 limit 之后.
            from.limit(start + rowBytes)
            from.position(start)
            to.put(from)
        }
    }

}
