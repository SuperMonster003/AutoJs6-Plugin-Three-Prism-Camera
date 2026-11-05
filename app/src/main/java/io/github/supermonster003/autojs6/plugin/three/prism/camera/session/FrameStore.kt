package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.os.SharedMemory
import android.system.OsConstants
import android.util.Log
import androidx.annotation.RequiresApi
import org.autojs.plugin.camera.api.CameraLimits
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * The frame blocks of one analysis (protocol "Frame transport", "Resource ownership"): three `SharedMemory` blocks
 * named `prism-frame-<i>` on API 27+, three files `cacheDir/frames/<i>.rgba` with one read-only descriptor per frame
 * below. Every block holds `width * height * 4` bytes. Written on the analyzer thread only; closed after the analyzer
 * stopped.
 * zh-CN: 一次分析的帧块 (协议 "Frame transport", "Resource ownership" 两节): API 27+ 为三块名为 `prism-frame-<i>` 的 `SharedMemory`, 以下为三个
 * 文件 `cacheDir/frames/<i>.rgba`, 每帧附一个只读描述符. 每块 `width * height * 4` 字节. 只在分析器线程写入; 分析器停止后关闭.
 */
internal interface FrameStore : Closeable {

    val width: Int
    val height: Int

    /** The `buffers` of the `startAnalysis` answer, null below API 27. zh-CN: `startAnalysis` 应答的 `buffers`, API 27 以下为 null. */
    fun buffers(): Array<Parcelable>?

    /** Writes block [index]: [write] gets a buffer at position 0 with `width * height * 4` bytes remaining. zh-CN: 写入块 [index]. */
    fun write(index: Int, write: (ByteBuffer) -> Unit)

    /** The `frameFd` of block [index] once written, null with shared memory. zh-CN: 块 [index] 写入后的 `frameFd`, 共享内存时为 null. */
    fun frameFd(index: Int): ParcelFileDescriptor?

    companion object {
        fun create(context: Context, width: Int, height: Int): FrameStore =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) SharedMemoryFrameStore(width, height) else FileFrameStore(context, width, height)

        fun byteCount(width: Int, height: Int): Int = width * height * CameraLimits.BYTES_PER_PIXEL
    }

}

@RequiresApi(Build.VERSION_CODES.O_MR1)
internal class SharedMemoryFrameStore(override val width: Int, override val height: Int) : FrameStore {

    private val bytes = FrameStore.byteCount(width, height)
    private val memories = ArrayList<SharedMemory>(CameraLimits.FRAME_BUFFER_COUNT)
    private val mappings = ArrayList<ByteBuffer>(CameraLimits.FRAME_BUFFER_COUNT)

    init {
        try {
            repeat(CameraLimits.FRAME_BUFFER_COUNT) { index ->
                val memory = SharedMemory.create("prism-frame-$index", bytes)
                memories += memory
                mappings += memory.mapReadWrite()
                // The plugin keeps its writable mapping; the host can only map the block read-only.
                // zh-CN: 插件保留自己的可写映射; 宿主只能以只读方式映射该块.
                runCatching { memory.setProtect(OsConstants.PROT_READ) }.onFailure { Log.w(TAG, "setProtect failed: $it") }
            }
        } catch (throwable: Throwable) {
            close()
            throw throwable
        }
    }

    override fun buffers(): Array<Parcelable> = memories.toTypedArray()

    override fun write(index: Int, write: (ByteBuffer) -> Unit) {
        val buffer = mappings[index]
        buffer.clear()
        write(buffer)
    }

    override fun frameFd(index: Int): ParcelFileDescriptor? = null

    override fun close() {
        mappings.forEach { runCatching { SharedMemory.unmap(it) } }
        mappings.clear()
        memories.forEach { runCatching { it.close() } }
        memories.clear()
    }

    private companion object {
        const val TAG = "PrismFrames"
    }

}

internal class FileFrameStore(context: Context, override val width: Int, override val height: Int) : FrameStore {

    private val bytes = FrameStore.byteCount(width, height)
    private val directory = File(context.cacheDir, DIRECTORY)
    private val files = List(CameraLimits.FRAME_BUFFER_COUNT) { File(directory, "$it.rgba") }
    private val channels = ArrayList<FileChannel>(CameraLimits.FRAME_BUFFER_COUNT)
    private val staging: ByteBuffer = ByteBuffer.allocateDirect(bytes)

    init {
        try {
            directory.mkdirs()
            files.forEach { file ->
                val access = RandomAccessFile(file, "rw")
                access.setLength(bytes.toLong())
                channels += access.channel
            }
        } catch (throwable: Throwable) {
            close()
            throw throwable
        }
    }

    override fun buffers(): Array<Parcelable>? = null

    override fun write(index: Int, write: (ByteBuffer) -> Unit) {
        staging.clear()
        write(staging)
        staging.clear()
        val channel = channels[index]
        var position = 0L
        while (staging.hasRemaining()) position += channel.write(staging, position)
    }

    override fun frameFd(index: Int): ParcelFileDescriptor = ParcelFileDescriptor.open(files[index], ParcelFileDescriptor.MODE_READ_ONLY)

    override fun close() {
        channels.forEach { runCatching { it.close() } }
        channels.clear()
        files.forEach { it.delete() }
    }

    companion object {
        const val DIRECTORY = "frames"
    }

}
