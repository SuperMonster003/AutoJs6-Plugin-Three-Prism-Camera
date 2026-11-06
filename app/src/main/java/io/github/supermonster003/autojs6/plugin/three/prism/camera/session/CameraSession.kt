package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest as Camera2Request
import android.hardware.camera2.TotalCaptureResult
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Display
import android.view.OrientationEventListener
import android.view.Surface
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraState
import androidx.camera.core.ExperimentalMirrorMode
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import com.google.common.util.concurrent.ListenableFuture
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraLimits
import org.autojs.plugin.camera.api.CameraResultKeys
import org.autojs.plugin.camera.api.ICameraCaptureCallback
import org.autojs.plugin.camera.api.ICameraFrameCallback
import org.autojs.plugin.camera.api.ICameraSessionCallback
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The worker threads of the camera sessions: one analyzer thread and one thread for saved pictures and the closing
 * tail. They live as long as the process, so a late CameraX callback never meets a shut down executor.
 * zh-CN: 相机会话的工作线程: 一个分析线程, 一个处理已保存照片与关闭收尾的线程. 它们与进程同寿, 迟到的 CameraX 回调不会遇到已关闭的执行器.
 */
internal object CameraExecutors {
    val analysis: Executor = Executors.newSingleThreadExecutor { Thread(it, "prism-analysis").apply { isDaemon = true } }
    val io: Executor = Executors.newSingleThreadExecutor { Thread(it, "prism-io").apply { isDaemon = true } }
}

/**
 * One camera session of a host (protocol "Binder interfaces", plugin roadmap P2.1 - P2.5). The session is the
 * `LifecycleOwner` of its use cases: RESUMED from [start] on, DESTROYED at the close. "ready" means the camera is
 * resolved; CameraX opens the device with the first bound use case and closes it when none is bound. All CameraX state
 * lives on the main thread; [CameraSessionBinder] serializes the host calls and hands them over with [MainThread.call].
 * Torch, zoom and exposure are kept here and applied whenever a use case is bound and whenever the camera opens again.
 * zh-CN: 宿主的一个相机会话 (协议 "Binder interfaces" 一节, 插件路线图 P2.1 - P2.5). 会话是其用例的 `LifecycleOwner`: 自 [start] 起为 RESUMED, 关闭时为
 * DESTROYED. "ready" 表示镜头已确定; CameraX 在绑定第一个用例时打开设备, 没有用例时关闭. 全部 CameraX 状态位于主线程; [CameraSessionBinder] 串行化宿主的
 * 调用并经 [MainThread.call] 移交. 补光灯, 变焦与曝光保存在这里, 每次绑定用例以及镜头重新打开时重新应用.
 */
internal class CameraSession(
    context: Context,
    private val provider: ProcessCameraProvider,
    private val cameraInfo: CameraInfo,
    val open: OpenRequest,
    private val callback: ICameraSessionCallback,
    val version: Int,
    val holder: CameraExclusivity.Holder,
    private val onEnded: (CameraSession) -> Unit,
) : LifecycleOwner {

    private val appContext = context.applicationContext
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    val id: String = CameraEngine.idOf(cameraInfo)
    val facing: String = CameraEngine.facingOf(cameraInfo)
    private val hasFlash = cameraInfo.hasFlashUnit()
    private val selector = cameraInfo.cameraSelector

    private val closed = AtomicBoolean(false)
    private val callbackLock = Any()
    private var closedSent = false
    private val counters = FrameCounters()

    // Main thread only below, except where marked.
    // zh-CN: 以下字段只在主线程访问, 另有说明的除外.
    private var camera: Camera? = null
    private var torch = false
    private var flashMode = open.flashMode ?: CameraContract.FLASH_OFF
    private var zoomRatio: Float? = null
    private var zoomLinear: Float? = null
    private var exposureIndex = open.exposureIndex ?: 0
    private var previewMirrored = false
    private var preview: Preview? = null
    private var previewTarget: PreviewTarget? = null

    /** Read by `releaseFrame` on a Binder thread. zh-CN: 由 Binder 线程上的 `releaseFrame` 读取. */
    @Volatile
    private var analysis: AnalysisRun? = null
    private var capture: ImageCapture? = null
    private var captureConfig: CaptureConfig? = null
    private val captures = ArrayDeque<CaptureJob>()
    private var captureInFlight: CaptureJob? = null
    private var captureStartedAt = 0L
    private var reboundAt = 0L
    private var lastErrorCode: Int? = null
    private var lastStateType: CameraState.Type? = null
    private var displayRotation = Surface.ROTATION_0
    private var deviceRotation = Surface.ROTATION_0

    private val deathRecipient = IBinder.DeathRecipient {
        Log.i(TAG, "The host of session ${holder.label} died")
        close(CameraContract.CLOSE_REASON_HOST, null, null, notify = false)
    }

    private val stateObserver = Observer<CameraState> { onCameraState(it) }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY || closed.get()) return
            val rotation = currentDisplayRotation()
            if (rotation == displayRotation) return
            displayRotation = rotation
            analysis?.useCase?.targetRotation = rotation
            if (previewTarget?.explicitRotation == null) preview?.targetRotation = rotation
        }
    }

    private val orientationListener = object : OrientationEventListener(appContext) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = DeviceRotation.surfaceRotationOf(orientation, deviceRotation)
            if (rotation == deviceRotation) return
            deviceRotation = rotation
            capture?.targetRotation = rotation
        }
    }

    private class AnalysisRun(
        val useCase: ImageAnalysis,
        val analyzer: FrameAnalyzer,
        val store: FrameStore,
        val ring: FrameRing,
        val actualFps: Int,
    )

    /** The Surface of one `startPreview` and who still uses it. zh-CN: 一次 `startPreview` 的 Surface 及其使用状态. */
    private class PreviewTarget(val surface: Surface, val explicitRotation: Int?) {
        var provided = false
        var cameraDone = false
        var stopped = false

        /** Releases the parcelled copy once neither the session nor the camera uses it. zh-CN: 会话与镜头都不再使用时释放其副本. */
        fun releaseIfUnused() {
            if (stopped && (!provided || cameraDone)) runCatching { surface.release() }
        }
    }

    private data class CaptureConfig(val quality: Int, val width: Int?, val height: Int?)

    private class CaptureJob(val request: CaptureRequest, val callback: ICameraCaptureCallback) {
        val done = AtomicBoolean(false)
        var watchdog: Runnable? = null
    }

    // ------------------------------------------------------------------ start and state

    /** Main thread: links the host, resumes the lifecycle and starts watching the camera. zh-CN: 主线程: 关联宿主, 恢复生命周期, 开始观察镜头. */
    fun start() {
        check(MainThread.isCurrent())
        callback.asBinder().linkToDeath(deathRecipient, 0)
        registry.currentState = Lifecycle.State.RESUMED
        displayRotation = currentDisplayRotation()
        deviceRotation = displayRotation
        cameraInfo.cameraState.observeForever(stateObserver)
        appContext.getSystemService(DisplayManager::class.java)?.registerDisplayListener(displayListener, MainThread.handler)
        if (orientationListener.canDetectOrientation()) orientationListener.enable()
    }

    val isClosed: Boolean get() = closed.get()

    /** Main thread: the state keys (protocol "Per method", `previewMirrored` per appendix D Q11 = (b)). zh-CN: 主线程: 状态键 (含附录 D Q11 = (b) 的 `previewMirrored`). */
    fun stateBundle(): Bundle = answer(version) {
        putString(CameraResultKeys.STATE, if (closed.get()) CameraContract.STATE_CLOSED else CameraContract.STATE_READY)
        putString(CameraResultKeys.ID, id)
        putString(CameraResultKeys.FACING, facing)
        putBoolean(CameraResultKeys.PREVIEWING, preview != null)
        val run = analysis
        putBoolean(CameraResultKeys.ANALYZING, run != null)
        if (run != null) {
            putInt(CameraResultKeys.WIDTH, run.store.width)
            putInt(CameraResultKeys.HEIGHT, run.store.height)
            putInt(CameraResultKeys.ACTUAL_FPS, run.actualFps)
        }
        putBoolean(CameraResultKeys.TORCH, torch)
        putString(CameraResultKeys.FLASH_MODE, flashMode)
        putBoolean(CameraResultKeys.HAS_FLASH, hasFlash)
        val (minZoom, maxZoom) = zoomRange()
        val zoom = cameraInfo.zoomState.value
        val ratio = zoomRatio ?: zoom?.zoomRatio ?: minZoom
        putFloat(CameraResultKeys.ZOOM_RATIO, ratio)
        putFloat(CameraResultKeys.ZOOM_LINEAR, zoomLinear ?: zoom?.linearZoom ?: ZoomMath.linearOf(ratio, minZoom, maxZoom))
        putFloat(CameraResultKeys.ZOOM_MIN, minZoom)
        putFloat(CameraResultKeys.ZOOM_MAX, maxZoom)
        val exposure = cameraInfo.exposureState
        val supported = exposure.isExposureCompensationSupported
        putInt(CameraResultKeys.EXPOSURE_INDEX, if (supported) exposureIndex else 0)
        putInt(CameraResultKeys.EXPOSURE_MIN, if (supported) exposure.exposureCompensationRange.lower else 0)
        putInt(CameraResultKeys.EXPOSURE_MAX, if (supported) exposure.exposureCompensationRange.upper else 0)
        putFloat(CameraResultKeys.EXPOSURE_STEP, if (supported) exposure.exposureCompensationStep.toFloat() else 0f)
        putLong(CameraResultKeys.FRAMES, counters.frames.get())
        putLong(CameraResultKeys.DROPPED, counters.dropped.get())
        putString(CameraResultKeys.HOLDER, holder.label)
        putBoolean(CameraResultKeys.STATE_PREVIEW_MIRRORED, previewMirrored)
    }

    private fun zoomRange(): Pair<Float, Float> {
        val zoom = cameraInfo.zoomState.value
        val minZoom = zoom?.minZoomRatio ?: 1f
        return minZoom to max(minZoom, zoom?.maxZoomRatio ?: minZoom)
    }

    private fun currentDisplayRotation(): Int =
        appContext.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0

    private fun requireOpen() {
        if (closed.get()) throw CameraFailure(CameraErrorCodes.SESSION_CLOSED, "the camera session is closed")
    }

    // ------------------------------------------------------------------ binding

    /** Main thread: binds [useCase] next to the bound ones and reapplies the controls. zh-CN: 主线程: 在已绑定用例之外绑定 [useCase] 并重新应用控制. */
    private fun bind(useCase: UseCase) {
        reboundAt = SystemClock.elapsedRealtime()
        camera = try {
            provider.bindToLifecycle(this, selector, useCase)
        } catch (e: IllegalArgumentException) {
            throw CameraFailure(CameraErrorCodes.UNSUPPORTED, "the camera cannot combine the requested use cases: ${ErrorText.describe(e)}", cause = e)
        } catch (e: IllegalStateException) {
            throw CameraFailure(CameraErrorCodes.INTERNAL, "binding the use case failed: ${ErrorText.describe(e)}", cause = e)
        }
        applyControls()
    }

    private fun unbind(useCase: UseCase?) {
        useCase ?: return
        reboundAt = SystemClock.elapsedRealtime()
        runCatching { provider.unbind(useCase) }.onFailure { Log.w(TAG, "unbind failed: $it") }
        if (preview == null && analysis == null && capture == null) camera = null
    }

    /** Main thread: torch, zoom and exposure as the session keeps them. zh-CN: 主线程: 按会话保存的值应用补光灯, 变焦与曝光. */
    private fun applyControls() {
        val control = camera?.cameraControl ?: return
        if (hasFlash) control.enableTorch(torch).ignoreFailure("enableTorch")
        zoomRatio?.let { control.setZoomRatio(it).ignoreFailure("setZoomRatio") }
        if (cameraInfo.exposureState.isExposureCompensationSupported) {
            control.setExposureCompensationIndex(exposureIndex).ignoreFailure("setExposureCompensationIndex")
        }
    }

    private fun <T> ListenableFuture<T>.ignoreFailure(label: String) {
        addListener({
            try {
                get()
            } catch (e: Exception) {
                // A newer request or a closed camera cancels the older one.
                // zh-CN: 较新的请求或已关闭的镜头会取消较早的请求.
                Log.d(TAG, "$label: ${(e as? ExecutionException)?.cause ?: e}")
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun onCameraState(state: CameraState) {
        if (closed.get()) return
        val error = state.error
        if (error == null) {
            lastErrorCode = null
        } else if (error.code != lastErrorCode) {
            lastErrorCode = error.code
            val policyDisabled = error.code == CameraErrorPolicy.ERROR_CAMERA_DISABLED && CameraEngine.disabledByPolicy(appContext)
            when (val action = CameraErrorPolicy.actionOf(error.code, policyDisabled)) {
                is CameraErrorPolicy.Action.Report -> {
                    val target = previewTarget
                    if (error.code == CameraErrorPolicy.ERROR_STREAM_CONFIG && target != null && !target.surface.isValid) {
                        // The host's surface went away (protocol: `stopPreview` from `surfaceDestroyed` may come late): stop the
                        // preview so analysis and capture go on.
                        // zh-CN: 宿主的 Surface 已失效 (协议: 来自 `surfaceDestroyed` 的 `stopPreview` 可能较晚): 停止预览, 使分析与拍照继续.
                        stopPreviewOnMain()
                        sendState()
                    } else {
                        sendError(CameraFailure(action.code, action.message, cameraId = id.takeIf { action.code == CameraErrorCodes.CAMERA_UNAVAILABLE }))
                    }
                }
                is CameraErrorPolicy.Action.Close -> CameraExecutors.io.execute { close(action.reason, action.code, action.message, notify = true) }
                is CameraErrorPolicy.Action.FailAndClose -> {
                    // `onError` goes out before the close starts; both are one-way calls on the same callback Binder, so the host
                    // receives them in this order (appendix D Q57 = (a)).
                    // zh-CN: `onError` 在关闭开始之前发出; 两者都是同一个回调 Binder 上的单向调用, 宿主按此顺序收到 (附录 D Q57 = (a)).
                    Log.i(TAG, "CameraState error ${error.code}: ${action.code}, then close ${action.reason}")
                    sendError(CameraFailure(action.code, action.message, cameraId = id))
                    CameraExecutors.io.execute { close(action.reason, null, action.message, notify = true) }
                }
            }
        }
        if (state.type == CameraState.Type.OPEN && lastStateType != CameraState.Type.OPEN) applyControls()
        lastStateType = state.type
    }

    // ------------------------------------------------------------------ preview (P2.5)

    /**
     * Main thread. The front lens preview is mirrored (roadmap D19, appendix D Q6 = (b)); `setMirrorMode` of `Preview` is
     * still experimental in CameraX 1.6.
     * zh-CN: 主线程. 前置镜头的预览镜像 (路线图 D19, 附录 D Q6 = (b)); `Preview` 的 `setMirrorMode` 在 CameraX 1.6 中仍属实验 API.
     */
    @OptIn(ExperimentalMirrorMode::class, ExperimentalCamera2Interop::class)
    fun startPreview(surface: Surface, request: PreviewRequest): Bundle {
        requireOpen()
        if (!surface.isValid) {
            runCatching { surface.release() }
            throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, "the surface is not valid")
        }
        stopPreviewOnMain()
        val target = PreviewTarget(surface, request.surfaceRotation)
        val rotation = request.surfaceRotation ?: displayRotation
        val builder = Preview.Builder()
            .setTargetRotation(rotation)
            .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
            .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(aspectOf(request.surfaceWidth, request.surfaceHeight)).build())
        Camera2Interop.Extender(builder).setSessionCaptureCallback(PreviewLossWatch(target))
        val useCase = builder.build()
        useCase.setSurfaceProvider(ContextCompat.getMainExecutor(appContext)) { surfaceRequest -> provide(target, surfaceRequest) }
        previewTarget = target
        preview = useCase
        try {
            bind(useCase)
        } catch (failure: Throwable) {
            stopPreviewOnMain()
            throw failure
        }
        return stateBundle().also { sendState() }
    }

    private fun provide(target: PreviewTarget, request: SurfaceRequest) {
        if (target.stopped || previewTarget !== target || closed.get() || !target.surface.isValid) {
            request.willNotProvideSurface()
            return
        }
        request.setTransformationInfoListener(ContextCompat.getMainExecutor(appContext)) { info ->
            if (previewTarget === target && previewMirrored != info.isMirroring) {
                previewMirrored = info.isMirroring
                sendState()
            }
        }
        target.provided = true
        target.cameraDone = false
        request.provideSurface(target.surface, ContextCompat.getMainExecutor(appContext)) { result ->
            target.cameraDone = true
            if (result.resultCode == SurfaceRequest.Result.RESULT_INVALID_SURFACE && previewTarget === target && !closed.get()) {
                Log.w(TAG, "The preview surface is invalid; stopping the preview")
                stopPreviewOnMain()
                sendState()
            }
            target.releaseIfUnused()
        }
    }

    /**
     * Notices a preview consumer that went away while the host's `stopPreview` is late or never comes (protocol
     * "Lifetimes": the host calls it from `surfaceDestroyed`). `Surface.isValid` stays true on the plugin's parcelled
     * copy, so the camera tells it. A HAL3 device loses the preview buffer of every frame while the other streams go on:
     * [LOSS_BURST] losses within [LOSS_WINDOW_MILLIS] stop the preview. The legacy camera2 shim (LEGACY devices, the
     * API 24 - 26 emulators) loses no buffer through this callback; it drops the repeating request instead, so its
     * sequence ends and every stream stalls. A sequence that ends (or a single lost buffer) arms a check: no completed
     * frame for [STALL_MILLIS] stops the preview, so analysis and capture go on. A replaced repeating request (controls,
     * captures, rebinding) ends its sequence too, but frames follow; rebinding gets [REBIND_GRACE_MILLIS], a capture
     * [STALL_MILLIS] from its start, a camera that is not open is not judged, and the check gives up after
     * [STALL_GIVE_UP_MILLIS]. Camera threads; the checks and the stop run on the main thread.
     * zh-CN: 宿主的 `stopPreview` 迟到或不来时 (协议 "Lifetimes" 一节: 宿主在 `surfaceDestroyed` 中调用), 发现已失去消费者的预览. 插件持有的
     * Surface 副本 `isValid` 仍为 true, 因此由镜头告知. HAL3 设备每帧都丢失预览缓冲而其他流照常: [LOSS_WINDOW_MILLIS] 内丢失 [LOSS_BURST] 次即停止
     * 预览. 旧版 camera2 兼容层 (LEGACY 设备, API 24 - 26 模拟器) 不经此回调报告丢失, 而是丢弃重复请求, 其序列结束后所有流停滞. 序列结束 (或单次
     * 丢失) 后开始检查: [STALL_MILLIS] 内没有完成的帧即停止预览, 使分析与拍照继续. 替换重复请求 (控制, 拍照, 重新绑定) 也会结束序列, 但随后有帧;
     * 重新绑定另给 [REBIND_GRACE_MILLIS], 拍照从开始时起算 [STALL_MILLIS], 镜头未打开时不判断, 超过 [STALL_GIVE_UP_MILLIS] 放弃. 回调运行于镜头线程,
     * 检查与停止在主线程执行.
     */
    private inner class PreviewLossWatch(private val target: PreviewTarget) : CameraCaptureSession.CaptureCallback() {
        private val losses = ArrayDeque<Long>()

        @Volatile
        private var lastCompleted = 0L

        override fun onCaptureCompleted(session: CameraCaptureSession, request: Camera2Request, result: TotalCaptureResult) {
            lastCompleted = SystemClock.elapsedRealtime()
        }

        override fun onCaptureBufferLost(session: CameraCaptureSession, request: Camera2Request, surface: Surface, frameNumber: Long) {
            if (surface !== target.surface || target.stopped) return
            val now = SystemClock.elapsedRealtime()
            val burst = synchronized(losses) {
                losses.addLast(now)
                while (now - losses.first() > LOSS_WINDOW_MILLIS) losses.removeFirst()
                losses.size >= LOSS_BURST
            }
            if (burst) {
                MainThread.post { stop("$LOSS_BURST lost buffers within $LOSS_WINDOW_MILLIS ms") }
            } else {
                watch(now, "a lost buffer")
            }
        }

        override fun onCaptureSequenceCompleted(session: CameraCaptureSession, sequenceId: Int, frameNumber: Long) {
            if (!target.stopped) watch(SystemClock.elapsedRealtime(), "the end of a capture sequence")
        }

        override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
            if (!target.stopped) watch(SystemClock.elapsedRealtime(), "an aborted capture sequence")
        }

        private fun watch(since: Long, cause: String) {
            MainThread.handler.postDelayed({ checkStall(since, cause) }, STALL_MILLIS)
        }

        /** Main thread. zh-CN: 主线程. */
        private fun checkStall(since: Long, cause: String) {
            // The last frame of an ended sequence often completes in the same millisecond, so only a later one counts.
            // zh-CN: 已结束序列的最后一帧常在同一毫秒内完成, 因此只有更晚的帧才算.
            if (target.stopped || previewTarget !== target || closed.get() || lastCompleted > since) return
            val now = SystemClock.elapsedRealtime()
            val settled = lastStateType == CameraState.Type.OPEN &&
                now - reboundAt >= REBIND_GRACE_MILLIS &&
                (captureInFlight == null || now - captureStartedAt >= STALL_MILLIS)
            when {
                settled -> stop("no frame completed within ${now - since} ms of $cause")
                now - since < STALL_GIVE_UP_MILLIS -> MainThread.handler.postDelayed({ checkStall(since, cause) }, STALL_MILLIS)
            }
        }

        /** Main thread. zh-CN: 主线程. */
        private fun stop(reason: String) {
            if (target.stopped || previewTarget !== target || closed.get()) return
            Log.w(TAG, "The preview surface is gone ($reason); stopping the preview")
            stopPreviewOnMain()
            sendState()
        }
    }

    /** Main thread. zh-CN: 主线程. */
    fun stopPreview(): Bundle {
        requireOpen()
        val wasPreviewing = preview != null
        stopPreviewOnMain()
        return stateBundle().also { if (wasPreviewing) sendState() }
    }

    private fun stopPreviewOnMain() {
        val useCase = preview
        val target = previewTarget
        preview = null
        previewTarget = null
        previewMirrored = false
        useCase?.setSurfaceProvider(null)
        unbind(useCase)
        target?.let {
            it.stopped = true
            it.releaseIfUnused()
        }
    }

    private fun aspectOf(width: Int?, height: Int?): AspectRatioStrategy {
        if (width == null || height == null) return AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
        val ratio = max(width, height).toFloat() / min(width, height)
        return if (abs(ratio - 16f / 9f) < abs(ratio - 4f / 3f)) {
            AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        } else {
            AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
        }
    }

    // ------------------------------------------------------------------ analysis (P2.2)

    /** Main thread: binds the analysis and allocates its blocks (protocol "Frame transport" step 1). zh-CN: 主线程: 绑定分析并分配块. */
    @OptIn(ExperimentalCamera2Interop::class)
    fun startAnalysis(request: AnalysisRequest, frameCallback: ICameraFrameCallback): Bundle {
        requireOpen()
        val longSide = max(request.width, request.height)
        val shortSide = min(request.width, request.height)
        val plan = FpsPlanner.plan(request.fps, cameraInfo.supportedFrameRateRanges.map { it.lower..it.upper })
        val builder = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(displayRotation)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(aspectOf(longSide, shortSide))
                    .setResolutionStrategy(ResolutionStrategy(Size(longSide, shortSide), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                    .setResolutionFilter { sizes, _ -> sizes.filter(CameraEngine::isAnalysisSize) }
                    .build(),
            )
        plan.range?.let { Camera2Interop.Extender(builder).setCaptureRequestOption(Camera2Request.CONTROL_AE_TARGET_FPS_RANGE, Range(it.first, it.last)) }
        val useCase = builder.build()
        bind(useCase)
        val resolution = useCase.resolutionInfo?.resolution
        if (resolution == null || !CameraEngine.isAnalysisSize(resolution)) {
            unbind(useCase)
            throw CameraFailure(CameraErrorCodes.UNSUPPORTED, "the camera offers no analysis size within the limits (got $resolution)")
        }
        val store = try {
            FrameStore.create(appContext, resolution.width, resolution.height)
        } catch (e: Exception) {
            unbind(useCase)
            throw CameraFailure(CameraErrorCodes.INTERNAL, "the frame blocks could not be allocated: ${ErrorText.describe(e)}", cause = e)
        }
        val ring = FrameRing()
        val analyzer = FrameAnalyzer(store, ring, frameCallback, version, facing, FramePacer(plan.throttleFps), counters)
        analysis = AnalysisRun(useCase, analyzer, store, ring, plan.actualFps)
        useCase.setAnalyzer(CameraExecutors.analysis, analyzer)
        val answer = answer(version) {
            store.buffers()?.let { putParcelableArray(CameraResultKeys.BUFFERS, it) }
            putInt(CameraResultKeys.WIDTH, store.width)
            putInt(CameraResultKeys.HEIGHT, store.height)
            putInt(CameraResultKeys.ROW_STRIDE, store.width * CameraLimits.BYTES_PER_PIXEL)
            putString(CameraResultKeys.FORMAT, CameraContract.FORMAT_RGBA_8888)
            putInt(CameraResultKeys.ACTUAL_FPS, plan.actualFps)
        }
        sendState()
        return answer
    }

    /**
     * Main thread: unbinds the analysis and answers the run, whose analyzer the caller finishes off the main thread
     * ([finishAnalysis]).
     * zh-CN: 主线程: 解绑分析并返回该次分析, 调用方在主线程之外结束其分析器 ([finishAnalysis]).
     */
    private fun detachAnalysis(): AnalysisRun? {
        val run = analysis ?: return null
        analysis = null
        run.useCase.clearAnalyzer()
        unbind(run.useCase)
        return run
    }

    /** Any thread but main preferred: the final `onDropped`, then the blocks close. zh-CN: 最好不在主线程: 发送最终的 `onDropped`, 然后关闭块. */
    private fun finishAnalysis(run: AnalysisRun?, report: Boolean) {
        run ?: return
        run.analyzer.finish(report)
        runCatching { run.store.close() }
    }

    /** Binder thread: stops a running analysis (protocol: `onDropped` follows). zh-CN: Binder 线程: 停止进行中的分析. */
    fun stopAnalysisFromBinder(report: Boolean): Boolean {
        val run = MainThread.call { detachAnalysis() }
        finishAnalysis(run, report)
        if (run != null) MainThread.post { if (!closed.get()) sendState() }
        return run != null
    }

    /** Binder thread, oneway. zh-CN: Binder 线程, oneway. */
    fun releaseFrame(bufferIndex: Int, seq: Long) {
        analysis?.ring?.release(bufferIndex, seq)
    }

    // ------------------------------------------------------------------ capture (P2.3)

    /** Main thread: queues a capture (captures of one session run in call order). zh-CN: 主线程: 排队一次拍照 (同一会话的拍照按调用顺序执行). */
    fun capture(request: CaptureRequest, captureCallback: ICameraCaptureCallback): Bundle {
        requireOpen()
        val mode = request.flashMode ?: flashMode
        if (mode != CameraContract.FLASH_OFF && !hasFlash) throw CameraFailure(CameraErrorCodes.UNSUPPORTED, "the camera has no flash unit for flash mode $mode")
        captures.addLast(CaptureJob(request, captureCallback))
        pumpCaptures()
        return answer(version)
    }

    private fun pumpCaptures() {
        if (captureInFlight != null || closed.get()) return
        val job = captures.removeFirstOrNull() ?: return
        captureInFlight = job
        captureStartedAt = SystemClock.elapsedRealtime()
        try {
            val useCase = captureUseCase(CaptureConfig(job.request.quality, job.request.width, job.request.height))
            useCase.flashMode = flashModeOf(job.request.flashMode ?: flashMode)
            useCase.targetRotation = deviceRotation
            // The turn the picture needs to stand upright as the phone is held (appendix D Q14).
            // zh-CN: 照片按手持方向转正所需的旋转 (附录 D Q14).
            val expectedRotation = cameraInfo.getSensorRotationDegrees(deviceRotation)
            val watchdog = Runnable {
                finishCapture(job) {
                    job.callback.onError(CameraFailure(CameraErrorCodes.TIMEOUT, "the camera delivered no picture within ${CameraLimits.CAPTURE_TIMEOUT_MILLIS} ms").toBundle(version))
                }
            }
            job.watchdog = watchdog
            MainThread.handler.postDelayed(watchdog, CameraLimits.CAPTURE_TIMEOUT_MILLIS)
            useCase.takePicture(CameraExecutors.io, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) = written(job, image, expectedRotation)
                override fun onError(exception: ImageCaptureException) {
                    val failure = when {
                        exception.imageCaptureError == ImageCapture.ERROR_CAMERA_CLOSED && closed.get() ->
                            CameraFailure(CameraErrorCodes.SESSION_CLOSED, "the session closed during the capture")
                        exception.imageCaptureError == ImageCapture.ERROR_CAMERA_CLOSED ->
                            CameraFailure(CameraErrorCodes.CAMERA_UNAVAILABLE, "the camera closed during the capture", cameraId = id)
                        else -> CameraFailure(CameraErrorCodes.INTERNAL, "the capture failed (ImageCapture error ${exception.imageCaptureError}): ${ErrorText.describe(exception)}")
                    }
                    finishCapture(job) { job.callback.onError(failure.toBundle(version)) }
                }
            })
        } catch (throwable: Throwable) {
            val failure = throwable.asCameraFailure()
            finishCapture(job) { job.callback.onError(failure.toBundle(version)) }
        }
    }

    /**
     * IO thread: copies the in-memory JPEG to a file with its EXIF orientation settled (appendix D Q13 / Q14 = (b)) and
     * releases the image at once, then hands the file over.
     * zh-CN: IO 线程: 把内存中的 JPEG 复制到文件并确定 EXIF 方向 (附录 D Q13 / Q14 = (b)), 随即释放图像, 然后交出文件.
     */
    private fun written(job: CaptureJob, image: ImageProxy, expectedRotation: Int) {
        val file = try {
            if (job.done.get()) return
            CaptureWriter.newFile(appContext).also { file ->
                try {
                    CaptureWriter.write(image, file, job.request.mirror, expectedRotation)
                } catch (throwable: Throwable) {
                    file.delete()
                    throw throwable
                }
            }
        } catch (throwable: Throwable) {
            val failure = throwable.asCameraFailure()
            finishCapture(job) { job.callback.onError(failure.toBundle(version)) }
            return
        } finally {
            image.close()
        }
        delivered(job, file)
    }

    /** IO thread: reads the saved picture and hands it over (protocol "Capture"). zh-CN: IO 线程: 读取已保存的照片并交出. */
    private fun delivered(job: CaptureJob, file: File) {
        if (job.done.get()) {
            file.delete()
            return
        }
        val sealed = try {
            CaptureWriter.seal(file, facing, version)
        } catch (throwable: Throwable) {
            val failure = throwable.asCameraFailure()
            finishCapture(job) { job.callback.onError(failure.toBundle(version)) }
            return
        }
        try {
            finishCapture(job) { job.callback.onCaptured(sealed.descriptor, sealed.meta) }
        } finally {
            // The transaction carries its own copy of the descriptor.
            // zh-CN: 事务携带描述符的副本.
            runCatching { sealed.descriptor.close() }
        }
    }

    /** Exactly one result per capture, then the next one starts. zh-CN: 每次拍照恰好一个结果, 然后开始下一次. */
    private fun finishCapture(job: CaptureJob, send: () -> Unit) {
        if (!job.done.compareAndSet(false, true)) return
        try {
            send()
        } catch (e: RemoteException) {
            Log.w(TAG, "Capture callback failed: $e")
        } catch (e: RuntimeException) {
            Log.w(TAG, "Capture callback failed: $e")
        }
        MainThread.post {
            job.watchdog?.let { MainThread.handler.removeCallbacks(it) }
            if (captureInFlight === job) captureInFlight = null
            pumpCaptures()
        }
    }

    /** Main thread: the capture use case of [config], bound (rebinds when the quality or size differ). zh-CN: 主线程: 对应 [config] 的已绑定拍照用例. */
    private fun captureUseCase(config: CaptureConfig): ImageCapture {
        capture?.let { if (captureConfig == config) return it }
        unbind(capture)
        capture = null
        captureConfig = null
        val strategy = if (config.width != null && config.height != null) {
            ResolutionStrategy(Size(max(config.width, config.height), min(config.width, config.height)), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
        } else {
            ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
        }
        val useCase = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(config.quality)
            .setFlashMode(flashModeOf(flashMode))
            .setTargetRotation(deviceRotation)
            .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(strategy).build())
            .build()
        capture = useCase
        captureConfig = config
        try {
            bind(useCase)
        } catch (failure: Throwable) {
            capture = null
            captureConfig = null
            unbind(useCase)
            throw failure
        }
        return useCase
    }

    private fun flashModeOf(mode: String): Int = when (mode) {
        CameraContract.FLASH_AUTO -> ImageCapture.FLASH_MODE_AUTO
        CameraContract.FLASH_ON -> ImageCapture.FLASH_MODE_ON
        else -> ImageCapture.FLASH_MODE_OFF
    }

    // ------------------------------------------------------------------ controls (P2.4)

    /** Main thread. zh-CN: 主线程. */
    fun setTorch(enabled: Boolean): Bundle {
        requireOpen()
        if (!hasFlash) throw CameraFailure(CameraErrorCodes.UNSUPPORTED, "the camera has no flash unit")
        torch = enabled
        camera?.cameraControl?.enableTorch(enabled)?.ignoreFailure("enableTorch")
        return stateBundle().also { sendState() }
    }

    /** Main thread. zh-CN: 主线程. */
    fun setFlashMode(mode: String): Bundle {
        requireOpen()
        if (mode != CameraContract.FLASH_OFF && !hasFlash) throw CameraFailure(CameraErrorCodes.UNSUPPORTED, "the camera has no flash unit for flash mode $mode")
        flashMode = mode
        capture?.flashMode = flashModeOf(mode)
        return stateBundle().also { sendState() }
    }

    /** Main thread: the zoom applies asynchronously; the answer carries the clamped request. zh-CN: 主线程: 变焦异步生效; 应答携带夹取后的请求值. */
    fun setZoom(request: ZoomRequest): Bundle {
        requireOpen()
        val (minZoom, maxZoom) = zoomRange()
        val ratio: Float
        val linear: Float
        if (request.ratio != null) {
            ratio = ZoomMath.clampRatio(request.ratio, minZoom, maxZoom)
            linear = ZoomMath.linearOf(ratio, minZoom, maxZoom)
        } else {
            linear = (request.linear ?: 0f).coerceIn(0f, 1f)
            ratio = ZoomMath.ratioOf(linear, minZoom, maxZoom)
        }
        zoomRatio = ratio
        zoomLinear = linear
        camera?.cameraControl?.setZoomRatio(ratio)?.ignoreFailure("setZoomRatio")
        return stateBundle().also { sendState() }
    }

    /** Main thread. zh-CN: 主线程. */
    fun setExposure(index: Int): Bundle {
        requireOpen()
        val exposure = cameraInfo.exposureState
        if (!exposure.isExposureCompensationSupported) throw CameraFailure(CameraErrorCodes.UNSUPPORTED, "the camera has no exposure compensation")
        val range = exposure.exposureCompensationRange
        if (index < range.lower) throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, "exposure index $index is below ${range.lower}", limit = range.lower.toLong())
        if (index > range.upper) throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, "exposure index $index is above ${range.upper}", limit = range.upper.toLong())
        exposureIndex = index
        camera?.cameraControl?.setExposureCompensationIndex(index)?.ignoreFailure("setExposureCompensationIndex")
        return stateBundle().also { sendState() }
    }

    /**
     * Main thread: starts focus and metering at an upright point of the first bound use case (analysis, preview, then
     * capture); null when none is bound. The caller waits for the future off the main thread.
     * zh-CN: 主线程: 在第一个已绑定用例 (分析, 预览, 拍照) 的旋正坐标上开始对焦与测光; 没有已绑定用例时为 null. 调用方在主线程之外等待结果.
     */
    fun startFocus(request: FocusRequest): ListenableFuture<FocusMeteringResult>? {
        requireOpen()
        val control = camera?.cameraControl ?: return null
        val useCase: UseCase = analysis?.useCase ?: preview ?: capture ?: return null
        val targetRotation = when (useCase) {
            is ImageAnalysis -> useCase.targetRotation
            is Preview -> useCase.targetRotation
            is ImageCapture -> useCase.targetRotation
            else -> displayRotation
        }
        val (x, y) = FocusGeometry.bufferPoint(request.x, request.y, cameraInfo.getSensorRotationDegrees(targetRotation))
        val point = SurfaceOrientedMeteringPointFactory(1f, 1f, useCase).createPoint(x, y, request.size)
        val builder = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
        if (request.autoCancelMillis == 0L) builder.disableAutoCancel() else builder.setAutoCancelDuration(request.autoCancelMillis, TimeUnit.MILLISECONDS)
        val action = builder.build()
        if (!cameraInfo.isFocusMeteringSupported(action)) throw CameraFailure(CameraErrorCodes.UNSUPPORTED, "the camera has no AF / AE / AWB regions")
        return control.startFocusAndMetering(action)
    }

    // ------------------------------------------------------------------ callbacks

    private fun sendState() {
        if (closed.get()) return
        val state = stateBundle()
        send("onStateChanged") { callback.onStateChanged(state) }
    }

    /** Any thread: an `onError` of the session, for example a late foreground service refusal. zh-CN: 任意线程: 会话的 `onError`, 例如迟到的前台服务拒绝. */
    fun reportError(failure: CameraFailure) = sendError(failure)

    private fun sendError(failure: CameraFailure) {
        if (closed.get()) return
        val error = failure.toBundle(version)
        send("onError") { callback.onError(error) }
    }

    private inline fun send(label: String, call: () -> Unit) {
        synchronized(callbackLock) {
            if (closedSent) return
            try {
                call()
            } catch (e: RemoteException) {
                Log.w(TAG, "$label failed: $e")
            } catch (e: RuntimeException) {
                Log.w(TAG, "$label failed: $e")
            }
        }
    }

    // ------------------------------------------------------------------ close

    /**
     * Closes the session once (protocol "Callbacks", "Resource ownership"): unbinds every use case, closes the blocks,
     * fails pending captures with `SESSION_CLOSED`, stops the foreground service, frees the camera for the next holder
     * and, when [notify], sends `onClosed` last. Any thread; the CameraX part runs on the main thread.
     * zh-CN: 关闭会话一次 (协议 "Callbacks", "Resource ownership" 两节): 解绑全部用例, 关闭块, 以 `SESSION_CLOSED` 结束未完成的拍照, 停止前台服务, 把相机
     * 让给下一个持有者, [notify] 为 true 时最后发送 `onClosed`. 任意线程; CameraX 部分在主线程运行.
     */
    fun close(reason: String, errorCode: String?, errorMessage: String?, notify: Boolean) {
        if (!closed.compareAndSet(false, true)) return
        val pending = ArrayList<CaptureJob>()
        val teardown = {
            val run = detachAnalysis()
            stopPreviewOnMain()
            unbind(capture)
            capture = null
            captureConfig = null
            camera = null
            captureInFlight?.let { pending += it }
            pending += captures
            captures.clear()
            captureInFlight = null
            cameraInfo.cameraState.removeObserver(stateObserver)
            appContext.getSystemService(DisplayManager::class.java)?.unregisterDisplayListener(displayListener)
            orientationListener.disable()
            registry.currentState = Lifecycle.State.DESTROYED
            run
        }
        val tail = { run: AnalysisRun? ->
            finishAnalysis(run, report = false)
            pending.forEach { job ->
                if (notify) {
                    finishCapture(job) { job.callback.onError(CameraFailure(CameraErrorCodes.SESSION_CLOSED, "the camera session closed").toBundle(version)) }
                } else {
                    job.done.set(true)
                }
            }
            runCatching { callback.asBinder().unlinkToDeath(deathRecipient, 0) }
            // The camera is free before `onClosed`, so a host that opens again at once does not get `CAMERA_BUSY`.
            // zh-CN: 相机在 `onClosed` 之前已释放, 宿主立即重新打开时不会得到 `CAMERA_BUSY`.
            onEnded(this)
            if (notify) {
                val bundle = answer(version) {
                    putString(CameraResultKeys.REASON, reason)
                    putString(CameraResultKeys.HOLDER, holder.label)
                    if (errorCode != null) {
                        putString(CameraContract.KEY_ERROR_CODE, errorCode)
                        putString(CameraContract.KEY_ERROR_MESSAGE, ErrorText.truncateUtf8(errorMessage.orEmpty(), CameraLimits.MAX_ERROR_MESSAGE_BYTES))
                    }
                }
                synchronized(callbackLock) {
                    if (!closedSent) {
                        closedSent = true
                        try {
                            callback.onClosed(bundle)
                        } catch (e: RemoteException) {
                            Log.w(TAG, "onClosed failed: $e")
                        } catch (e: RuntimeException) {
                            Log.w(TAG, "onClosed failed: $e")
                        }
                    }
                }
            } else {
                synchronized(callbackLock) { closedSent = true }
            }
            Log.i(TAG, "Session closed: $reason")
        }
        if (MainThread.isCurrent()) {
            val run = teardown()
            CameraExecutors.io.execute { tail(run) }
        } else {
            val run = try {
                MainThread.call { teardown() }
            } catch (failure: CameraFailure) {
                Log.w(TAG, "Session teardown on the main thread failed: ${failure.message}")
                MainThread.post { runCatching { finishAnalysis(teardown(), report = false) } }
                null
            }
            tail(run)
        }
    }

    internal companion object {
        const val TAG = "PrismSession"

        /** [PreviewLossWatch]: lost preview buffers within [LOSS_WINDOW_MILLIS] that stop the preview. zh-CN: 停止预览所需的丢失次数. */
        const val LOSS_BURST = 3
        const val LOSS_WINDOW_MILLIS = 1000L

        /**
         * [PreviewLossWatch]: how long the frames may stall after a sequence end or a lost buffer. zh-CN: 序列结束或丢失缓冲后允许无帧的时长.
         */
        const val STALL_MILLIS = 1000L

        /** [PreviewLossWatch]: extra time after binding or unbinding a use case. zh-CN: 绑定或解绑用例后额外给出的时长. */
        const val REBIND_GRACE_MILLIS = 3000L

        /** [PreviewLossWatch]: a stall that does not settle by then is left alone. zh-CN: 到此仍未稳定的停滞不再处理. */
        const val STALL_GIVE_UP_MILLIS = 10_000L

        /** Removes pictures and frame files a killed process left behind. zh-CN: 删除被杀进程遗留的照片与帧文件. */
        fun cleanStaleFiles(context: Context) {
            CaptureWriter.cleanStale(context)
            File(context.cacheDir, FileFrameStore.DIRECTORY).listFiles()?.forEach { it.delete() }
        }
    }

}
