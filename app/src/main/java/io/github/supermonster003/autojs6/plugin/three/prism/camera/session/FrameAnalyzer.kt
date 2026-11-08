package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import android.graphics.Matrix
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.autojs.plugin.camera.api.CameraFrameKeys
import org.autojs.plugin.camera.api.ICameraFrameCallback
import java.util.concurrent.atomic.AtomicLong

/**
 * Frames and drops of a session over all its analyses (state keys `frames`, `dropped`).
 * zh-CN: 一个会话在全部分析中的帧数与丢帧数 (状态键 `frames`, `dropped`).
 */
internal class FrameCounters {
    val frames = AtomicLong()
    val dropped = AtomicLong()
}

/**
 * The `ImageAnalysis` analyzer of one analysis (protocol "Frame transport" steps 3 and 6): paces the frames, takes a
 * free block of the [ring] (or counts a drop), packs the RGBA plane into the block and sends `onFrame`; drops go out as
 * `onDropped` every 30. [finish] waits for the frame in progress, so nothing reaches [callback] after it returns. With
 * [outputUpright] the use case turns the frames upright ([UprightFrames], roadmap appendix D Q66 (a)): the header names
 * the turn applied and the size of the turned frame, [expectedTurn] is the camera's rotation for the use case's target
 * rotation.
 * zh-CN: 一次分析的 `ImageAnalysis` 分析器 (协议 "Frame transport" 第 3, 6 步): 对帧限速, 从 [ring] 取空闲块 (或计一次丢帧), 把 RGBA 平面紧排写入块并
 * 发送 `onFrame`; 丢帧每 30 次以 `onDropped` 报告. [finish] 等待进行中的帧, 因此它返回后不再有调用到达 [callback]. 带 [outputUpright] 时由用例旋正帧
 * ([UprightFrames], 路线图附录 D Q66 (a)): 帧头写明已施加的旋转与旋正后的尺寸, [expectedTurn] 为用例目标旋转对应的相机旋转.
 */
internal class FrameAnalyzer(
    private val store: FrameStore,
    private val ring: FrameRing,
    private val callback: ICameraFrameCallback,
    private val version: Int,
    private val facing: String,
    private val pacer: FramePacer,
    private val counters: FrameCounters,
    private val outputUpright: Boolean = false,
    private val expectedTurn: () -> Int = { 0 },
) : ImageAnalysis.Analyzer {

    private val lock = Any()
    private var active = true
    private var warned = false
    private val matrixValues = FloatArray(9)

    override fun analyze(image: ImageProxy) {
        try {
            synchronized(lock) {
                if (active) handle(image)
            }
        } catch (throwable: Throwable) {
            Log.w(TAG, "Frame failed", throwable)
        } finally {
            image.close()
        }
    }

    private fun handle(image: ImageProxy) {
        val plane = image.planes.firstOrNull()
        val info = image.imageInfo
        val shape = if (image.format == PixelFormat.RGBA_8888 && plane != null && plane.pixelStride == 4) {
            val matrixTurn = if (outputUpright) matrixTurnOf(info.sensorToBufferTransformMatrix) else null
            UprightFrames.shape(store.width, store.height, image.width, image.height, outputUpright, matrixTurn, if (outputUpright) expectedTurn() else 0)
        } else {
            null
        }
        if (shape == null || plane == null) {
            if (!warned) {
                warned = true
                Log.w(TAG, "Unexpected frame ${image.width}x${image.height} format ${image.format}; expected ${store.width}x${store.height} RGBA_8888 (outputUpright $outputUpright)")
            }
            return
        }
        if (!pacer.admit(info.timestamp)) return
        when (val slot = ring.acquire()) {
            is FrameRing.Slot.Dropped -> {
                counters.dropped.incrementAndGet()
                if (slot.report) send { callback.onDropped(droppedBundle()) }
            }
            is FrameRing.Slot.Block -> {
                val fd = try {
                    store.write(slot.index) { target -> RgbaPacker.pack(plane.buffer, plane.rowStride, shape.width, shape.height, target) }
                    store.frameFd(slot.index)
                } catch (throwable: Throwable) {
                    ring.abort(slot)
                    throw throwable
                }
                ring.publish(slot)
                val frame = answer(version) {
                    putLong(CameraFrameKeys.SEQ, slot.seq)
                    putInt(CameraFrameKeys.BUFFER_INDEX, slot.index)
                    putInt(CameraFrameKeys.WIDTH, shape.width)
                    putInt(CameraFrameKeys.HEIGHT, shape.height)
                    putInt(CameraFrameKeys.ROTATION_DEGREES, info.rotationDegrees)
                    shape.appliedRotationDegrees?.let { putInt(CameraFrameKeys.APPLIED_ROTATION_DEGREES, it) }
                    putLong(CameraFrameKeys.TIMESTAMP_NANOS, info.timestamp)
                    putString(CameraFrameKeys.FACING, facing)
                    fd?.let { putParcelable(CameraFrameKeys.FRAME_FD, it) }
                }
                try {
                    if (send { callback.onFrame(frame) }) counters.frames.incrementAndGet()
                } finally {
                    // The transaction carries its own copy of the descriptor.
                    // zh-CN: 事务携带描述符的副本.
                    fd?.let { runCatching { it.close() } }
                }
            }
        }
    }

    private fun matrixTurnOf(matrix: Matrix): Int? {
        matrix.getValues(matrixValues)
        return UprightFrames.quarterTurnOf(matrixValues)
    }

    private fun droppedBundle(): Bundle = answer(version) {
        putLong(CameraFrameKeys.DROPPED_COUNT, ring.droppedCount)
        putLong(CameraFrameKeys.LAST_SEQ, ring.lastSeq)
    }

    private inline fun send(call: () -> Unit): Boolean = try {
        call()
        true
    } catch (e: RemoteException) {
        // The host is gone or cleared the relay: stop calling it; the session's death link closes the rest.
        // zh-CN: 宿主已退出或清除了中转: 停止调用; 其余由会话的死亡监听关闭.
        Log.w(TAG, "Frame callback failed: $e")
        active = false
        false
    }

    /**
     * Stops the analyzer (waiting for the frame in progress) and sends the final `onDropped` (protocol: at
     * `stopAnalysis`). Idempotent; [report] false skips the callback (the host is gone).
     * zh-CN: 停止分析器 (等待进行中的帧) 并发送最终的 `onDropped` (协议: 在 `stopAnalysis` 时). 幂等; [report] 为 false 时不调用回调 (宿主已退出).
     */
    fun finish(report: Boolean) {
        val wasActive = synchronized(lock) { active.also { active = false } }
        if (wasActive && report) {
            try {
                callback.onDropped(droppedBundle())
            } catch (e: RemoteException) {
                Log.w(TAG, "Final onDropped failed: $e")
            }
        }
    }

    private companion object {
        const val TAG = "PrismFrames"
    }

}
