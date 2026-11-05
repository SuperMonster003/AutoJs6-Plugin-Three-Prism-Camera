package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import android.os.Bundle
import android.util.Log
import android.view.Surface
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraLimits
import org.autojs.plugin.camera.api.CameraResultKeys
import org.autojs.plugin.camera.api.ICameraCaptureCallback
import org.autojs.plugin.camera.api.ICameraFrameCallback
import org.autojs.plugin.camera.api.ICameraSession
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * The `ICameraSession` Binder of one [CameraSession] (protocol "Binder interfaces", "Per method"). Calls of one session
 * are serialized by [callLock] and run their CameraX part on the main thread; `releaseFrame` (oneway, every frame) and
 * `close` (immediate) skip the lock. Every answer is a Bundle, an error answer when the call fails.
 * zh-CN: 一个 [CameraSession] 的 `ICameraSession` Binder (协议 "Binder interfaces", "Per method" 两节). 同一会话的调用由 [callLock] 串行化, 其
 * CameraX 部分在主线程运行; `releaseFrame` (oneway, 每帧一次) 与 `close` (立即返回) 不取锁. 每个应答都是 Bundle, 调用失败时为错误应答.
 */
internal class CameraSessionBinder(private val session: CameraSession) : ICameraSession.Stub() {

    private val callLock = Any()
    private val version: Int get() = session.version

    private inline fun locked(crossinline block: () -> Bundle): Bundle = synchronized(callLock) { block() }

    override fun startPreview(surface: Surface?, request: Bundle?): Bundle = guarded(request, TAG) { _ ->
        surface ?: throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, "the surface is null")
        val preview = try {
            CameraRequests.preview(BundleOptions(request))
        } catch (failure: CameraFailure) {
            runCatching { surface.release() }
            throw failure
        }
        locked { MainThread.call { session.startPreview(surface, preview) } }
    }

    override fun stopPreview(): Bundle = guarded(version, TAG) {
        locked { MainThread.call { session.stopPreview() } }
    }

    override fun startAnalysis(request: Bundle?, callback: ICameraFrameCallback?): Bundle = guarded(request, TAG) { _ ->
        callback ?: throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, "the frame callback is null")
        val analysis = CameraRequests.analysis(BundleOptions(request), session.open)
        locked {
            if (session.isClosed) throw CameraFailure(CameraErrorCodes.SESSION_CLOSED, "the camera session is closed")
            // A new analysis replaces the running one (its final `onDropped` goes to the old callback).
            // zh-CN: 新的分析替换进行中的分析 (其最终 `onDropped` 发给旧回调).
            session.stopAnalysisFromBinder(report = true)
            MainThread.call { session.startAnalysis(analysis, callback) }
        }
    }

    override fun stopAnalysis(): Bundle = guarded(version, TAG) {
        locked {
            if (session.isClosed) throw CameraFailure(CameraErrorCodes.SESSION_CLOSED, "the camera session is closed")
            session.stopAnalysisFromBinder(report = true)
            MainThread.call { session.stateBundle() }
        }
    }

    override fun releaseFrame(bufferIndex: Int, seq: Long) {
        try {
            session.releaseFrame(bufferIndex, seq)
        } catch (throwable: Throwable) {
            Log.w(TAG, "releaseFrame failed", throwable)
        }
    }

    override fun capture(request: Bundle?, callback: ICameraCaptureCallback?): Bundle = guarded(request, TAG) { _ ->
        callback ?: throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, "the capture callback is null")
        val capture = CameraRequests.capture(BundleOptions(request))
        locked { MainThread.call { session.capture(capture, callback) } }
    }

    override fun setTorch(enabled: Boolean): Bundle = guarded(version, TAG) {
        locked { MainThread.call { session.setTorch(enabled) } }
    }

    override fun setFlashMode(mode: String?): Bundle = guarded(version, TAG) {
        val flashMode = CameraRequests.flashMode(mode)
        locked { MainThread.call { session.setFlashMode(flashMode) } }
    }

    override fun setZoom(request: Bundle?): Bundle = guarded(request, TAG) { _ ->
        val zoom = CameraRequests.zoom(BundleOptions(request))
        locked { MainThread.call { session.setZoom(zoom) } }
    }

    override fun setExposure(index: Int): Bundle = guarded(version, TAG) {
        locked { MainThread.call { session.setExposure(index) } }
    }

    /**
     * Waits for the focus on this Binder thread, outside the call lock, so a `close` or `getState` meanwhile is not held up.
     * zh-CN: 在本 Binder 线程上等待对焦, 不持有调用锁, 期间的 `close` 或 `getState` 不受阻.
     */
    override fun focusAt(request: Bundle?): Bundle = guarded(request, TAG) { requestVersion ->
        val focus = CameraRequests.focus(BundleOptions(request))
        val future = synchronized(callLock) { MainThread.call { session.startFocus(focus) } }
        val focused = future?.let {
            try {
                it.get(CameraLimits.FOCUS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).isFocusSuccessful
            } catch (e: TimeoutException) {
                false
            } catch (e: ExecutionException) {
                // A newer focus request or the closing camera cancels this one.
                // zh-CN: 较新的对焦请求或正在关闭的镜头会取消本次请求.
                Log.d(TAG, "focusAt: ${e.cause ?: e}")
                false
            } catch (e: java.util.concurrent.CancellationException) {
                false
            }
        } ?: false
        answer(requestVersion) { putBoolean(CameraResultKeys.FOCUSED, focused) }
    }

    override fun getState(): Bundle = guarded(version, TAG) {
        MainThread.call { session.stateBundle() }
    }

    override fun close(): Bundle = guarded(version, TAG) {
        session.close(CameraContract.CLOSE_REASON_SCRIPT, null, null, notify = true)
        answer(version)
    }

    private companion object {
        const val TAG = "PrismSession"
    }

}
