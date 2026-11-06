package io.github.supermonster003.autojs6.plugin.three.prism.camera.ui

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.ExperimentalMirrorMode
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.TorchState
import androidx.camera.core.UseCase
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Observer
import io.github.supermonster003.autojs6.plugin.three.prism.camera.R
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraController
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraEngine
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraExecutors
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraExclusivity
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraFailure
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraRequests
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CaptureRequest
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CaptureUiRegistry
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CaptureWriter
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.DeviceRotation
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.ErrorText
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.ExifOrientation
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.MainThread
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.ZoomMath
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.EmptyAction
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.PrismScreen
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.TopBarAction
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.settings.SettingsActivity
import kotlinx.coroutines.delay
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraLimits
import org.autojs.plugin.camera.api.CameraResultKeys
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The viewfinder, launcher target of the plugin (roadmap D7, D31, P3.1): `CameraXViewfinder` of `camera-compose`
 * (appendix D Q3 = (b)) with lens switch, flashlight, zoom slider and pinch, tap to focus, shutter and close.
 *
 * - Opened from the launcher (normal mode) it shows the photo and its size after the shutter and saves nothing; the
 *   lens is the default of the settings screen (appendix D Q41 = (a)).
 * - Started by `openCaptureUi` (script mode) it carries [CaptureUiRegistry.EXTRA_TOKEN]: the photo goes to the script
 *   through the registry and the screen closes; closing it without a photo answers `CANCELLED` (protocol "Capture").
 * - The photo takes the in-memory path of a session capture (appendix D Q13 = (b)); the front preview is mirrored and the
 *   photo is not, unless the script asked for `mirror` (appendix D Q6 = (b)).
 * - The screen holds the plugin's one camera ([CameraExclusivity]) only while it is visible. When a script session holds
 *   it, the screen names the script and offers to stop it (appendix D Q7 = (a)): the session closes with reason
 *   `pluginUi` and the viewfinder takes the camera. Its own use cases are unbound in [onStop], so a script session that
 *   opens while the screen is hidden is never suspended by the Activity lifecycle.
 *
 * zh-CN: 取景页, 插件的启动器目标 (路线图 D7, D31, P3.1): `camera-compose` 的 `CameraXViewfinder` (附录 D Q3 = (b)), 支持切换镜头, 手电筒,
 * 变焦滑条与捏合, 点按对焦, 快门与关闭.
 *
 * - 从启动器打开 (普通模式) 时, 拍照后显示照片与尺寸, 不保存任何内容; 镜头取设置页的默认值 (附录 D Q41 = (a)).
 * - 由 `openCaptureUi` 启动 (脚本模式) 时携带 [CaptureUiRegistry.EXTRA_TOKEN]: 照片经登记表交给脚本, 随后关闭本页; 未拍照就关闭时应答
 *   `CANCELLED` (协议 "Capture" 一节).
 * - 照片沿用会话拍照的内存路径 (附录 D Q13 = (b)); 前置预览镜像, 照片不镜像, 除非脚本要求 `mirror` (附录 D Q6 = (b)).
 * - 本页只在可见时持有插件唯一的相机 ([CameraExclusivity]). 脚本会话持有相机时, 本页显示该脚本并提供停止按钮 (附录 D Q7 = (a)): 会话以原因
 *   `pluginUi` 关闭, 取景页接过相机. 本页自己的用例在 [onStop] 中解绑, 因此本页隐藏期间打开的脚本会话不会被 Activity 生命周期挂起.
 */
class ThreePrismCameraCaptureActivity : StandaloneActivity() {

    private enum class Phase { LOADING, NO_PERMISSION, POLICY, NO_CAMERA, FAILED, BUSY, READY }

    /** A photo of the normal mode, scaled for the screen, and its full upright size. zh-CN: 普通模式的照片 (按屏幕缩放) 及其转正后的完整尺寸. */
    private class Photo(val bitmap: Bitmap, val width: Int, val height: Int)

    /** One press of the shutter. zh-CN: 一次快门. */
    private class Shot(
        val generation: Int,
        val facing: String,
        val mirror: Boolean,
        val expectedRotation: Int,
        val pending: CaptureUiRegistry.Pending?,
    )

    private val exclusivity = CameraExclusivity.shared
    private val holder = CameraExclusivity.Holder(CameraContract.HOLDER_PLUGIN_UI, viewfinder = true)
    private val normalCapture: CaptureRequest by lazy { CameraRequests.capture(null) }

    private var captureToken: String? = null

    // Compose state, main thread. zh-CN: Compose 状态, 主线程.
    private var phase by mutableStateOf(Phase.LOADING)
    private var busyLabel by mutableStateOf("")
    private var permissionBlocked by mutableStateOf(false)
    private var scriptMode by mutableStateOf(false)
    private var surfaceRequest by mutableStateOf<SurfaceRequest?>(null)
    private var cameraCount by mutableIntStateOf(0)
    private var lensFacing by mutableStateOf<String?>(null)
    private var hasFlash by mutableStateOf(false)
    private var torchOn by mutableStateOf(false)
    private var zoomRatio by mutableFloatStateOf(1f)
    private var zoomLinear by mutableFloatStateOf(0f)
    private var zoomMin by mutableFloatStateOf(1f)
    private var zoomMax by mutableFloatStateOf(1f)
    private var shooting by mutableStateOf(false)
    private var photo by mutableStateOf<Photo?>(null)
    private var message by mutableStateOf<String?>(null)

    // CameraX state, main thread. zh-CN: CameraX 状态, 主线程.
    private var provider: ProcessCameraProvider? = null
    private var providerPending = false
    private var cameras: List<CameraInfo> = emptyList()
    private var selectedId: String? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var started = false
    private var deviceRotation = Surface.ROTATION_0
    private var watchdog: Runnable? = null

    /** Any thread: claims the result of a shot exactly once. zh-CN: 任意线程: 恰好一次认领一次快门的结果. */
    private val shotGeneration = AtomicInteger()

    private val zoomObserver = Observer<ZoomState> { state ->
        zoomRatio = state.zoomRatio
        zoomLinear = state.linearZoom
        zoomMin = state.minZoomRatio
        zoomMax = state.maxZoomRatio
    }

    private val torchObserver = Observer<Int> { torchOn = it == TorchState.ON }

    private val busyPoll = Runnable { if (started && phase == Phase.BUSY) startCamera() }

    private val orientationListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = DeviceRotation.surfaceRotationOf(orientation, deviceRotation)
                if (rotation == deviceRotation) return
                deviceRotation = rotation
                imageCapture?.targetRotation = rotation
            }
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            // The Activity handles rotations itself (manifest `configChanges`), so the preview follows the display here.
            // zh-CN: Activity 自行处理旋转 (清单 `configChanges`), 因此预览在这里跟随显示方向.
            if (displayId == Display.DEFAULT_DISPLAY) preview?.targetRotation = displayRotation()
        }
    }

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            permissionBlocked = false
            startCamera()
        } else {
            // No rationale after a refusal means "don't ask again": only the app settings can grant it now.
            // zh-CN: 拒绝后不再需要说明, 表示 "不再询问": 此后只能在应用设置中授予.
            permissionBlocked = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureToken = intent?.getStringExtra(CaptureUiRegistry.EXTRA_TOKEN)
        scriptMode = CaptureUiRegistry.attach(captureToken) != null
        // The device smoke test reads the probe log `availableCameraInfos=<count>` (.python/device_smoke.py).
        // zh-CN: 设备冒烟测试读取探测日志 `availableCameraInfos=<数量>` (.python/device_smoke.py).
        CameraController(this).probe {}
    }

    /**
     * The activity is `singleTask`: an `openCaptureUi` while the screen is open in the background (from the launcher or an
     * earlier call) arrives here, and its token replaces the earlier one, which is cancelled if it is still pending.
     * zh-CN: 本 Activity 为 `singleTask`: 界面在后台打开 (来自启动器或之前的调用) 时的 `openCaptureUi` 到达此处, 其令牌取代之前的令牌,
     * 之前的令牌若仍待返回则取消.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val token = intent.getStringExtra(CaptureUiRegistry.EXTRA_TOKEN) ?: return
        captureToken?.takeIf { it != token }?.let(CaptureUiRegistry::cancel)
        captureToken = token
        scriptMode = CaptureUiRegistry.attach(token) != null
        photo = null
        selectedId = null
        if (started && camera != null) provider?.let(::bind)
    }

    override fun onStart() {
        super.onStart()
        started = true
        deviceRotation = displayRotation()
        if (orientationListener.canDetectOrientation()) orientationListener.enable()
        getSystemService(DisplayManager::class.java)?.registerDisplayListener(displayListener, MainThread.handler)
        startCamera()
    }

    override fun onResume() {
        super.onResume()
        scriptMode = CaptureUiRegistry.peek(captureToken) != null
    }

    override fun onPause() {
        // Only a real close cancels the pending result. The answer goes out here because on API 24 `onDestroy` of a
        // finished activity can wait about 10 seconds for the next activity to idle.
        // zh-CN: 只有真正关闭时才取消待返回的结果. 应答在此发出, 因为在 API 24 上已结束的 Activity 的 `onDestroy` 可能要等下一个 Activity 空闲约 10 秒.
        if (isFinishing) {
            releaseCamera()
            captureToken?.let(CaptureUiRegistry::cancel)
        }
        super.onPause()
    }

    override fun onStop() {
        started = false
        MainThread.handler.removeCallbacks(busyPoll)
        orientationListener.disable()
        getSystemService(DisplayManager::class.java)?.unregisterDisplayListener(displayListener)
        unbindOwn()
        exclusivity.release(holder)
        super.onStop()
    }

    override fun onDestroy() {
        // A fallback for a close that skipped `onPause`; a token that was already answered is ignored.
        // zh-CN: 作为未经过 `onPause` 的关闭的兜底; 已应答的令牌会被忽略.
        if (isFinishing) captureToken?.let(CaptureUiRegistry::cancel)
        watchdog?.let(MainThread.handler::removeCallbacks)
        super.onDestroy()
    }

    /** No recreation for an appearance change while a photo is on its way. zh-CN: 照片处理期间不因外观变化重建. */
    override fun holdsAppearance(): Boolean = shooting

    // ------------------------------------------------------------------ camera

    private fun startCamera() {
        if (!started) return
        MainThread.handler.removeCallbacks(busyPoll)
        if (!CameraEngine.hasPermission(this)) {
            phase = Phase.NO_PERMISSION
            return
        }
        if (CameraEngine.disabledByPolicy(this)) {
            phase = Phase.POLICY
            return
        }
        val busy = exclusivity.acquire(holder)
        if (busy != null) {
            // Polled, so the screen takes the camera by itself once the script closes its session.
            // zh-CN: 轮询, 脚本关闭会话后本页自行接过相机.
            busyLabel = busy.label
            phase = Phase.BUSY
            MainThread.handler.postDelayed(busyPoll, BUSY_POLL_MILLIS)
            return
        }
        if (camera == null) phase = Phase.LOADING
        withProvider(::bind)
    }

    /** The "stop this script" button (appendix D Q7 = (a)). zh-CN: "停止该脚本" 按钮 (附录 D Q7 = (a)). */
    private fun stopHolder() {
        if (!started) return
        MainThread.handler.removeCallbacks(busyPoll)
        val previous = exclusivity.preemptForViewfinder(holder)
        Log.i(TAG, "The viewfinder took the camera from ${previous?.label ?: "nobody"}")
        phase = Phase.LOADING
        withProvider(::bind)
    }

    private fun withProvider(action: (ProcessCameraProvider) -> Unit) {
        provider?.let {
            action(it)
            return
        }
        if (providerPending) return
        providerPending = true
        val future = ProcessCameraProvider.getInstance(applicationContext)
        future.addListener({
            providerPending = false
            val result = runCatching { future.get() }
            result.exceptionOrNull()?.let { Log.w(TAG, "CameraX failed to initialize: ${ErrorText.describe(it.cause ?: it)}") }
            provider = result.getOrNull()
            if (!started || exclusivity.holder() !== holder) return@addListener
            val ready = provider
            if (ready == null) {
                exclusivity.release(holder)
                phase = Phase.FAILED
            } else {
                action(ready)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalMirrorMode::class)
    private fun bind(provider: ProcessCameraProvider) {
        if (!started || exclusivity.holder() !== holder) return
        val list = CameraEngine.cameras(provider).sortedBy { facingRank(CameraEngine.facingOf(it)) }
        cameras = list
        cameraCount = list.size
        if (list.isEmpty()) {
            unbindOwn()
            exclusivity.release(holder)
            phase = Phase.NO_CAMERA
            return
        }
        val info = selectedId?.let { id -> list.firstOrNull { CameraEngine.idOf(it) == id } } ?: firstCamera(provider, list) ?: return
        selectedId = CameraEngine.idOf(info)
        unbindOwn()
        val previewUseCase = Preview.Builder()
            .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
            .setTargetRotation(displayRotation())
            .build()
        previewUseCase.setSurfaceProvider { request -> surfaceRequest = request }
        val captureUseCase = captureUseCase(CaptureUiRegistry.peek(captureToken)?.request?.capture ?: normalCapture, info)
        val bound = try {
            provider.bindToLifecycle(this, info.cameraSelector, previewUseCase, captureUseCase)
        } catch (e: Exception) {
            Log.w(TAG, "Binding the viewfinder failed: ${ErrorText.describe(e)}")
            runCatching { provider.unbind(previewUseCase, captureUseCase) }
            exclusivity.release(holder)
            phase = Phase.FAILED
            return
        }
        preview = previewUseCase
        imageCapture = captureUseCase
        camera = bound
        lensFacing = CameraEngine.facingOf(info)
        hasFlash = bound.cameraInfo.hasFlashUnit()
        bound.cameraInfo.zoomState.observeForever(zoomObserver)
        bound.cameraInfo.torchState.observeForever(torchObserver)
        phase = Phase.READY
        Log.i(TAG, "Viewfinder bound camera $selectedId (${lensFacing}, ${if (scriptMode) "script" else "normal"} mode)")
    }

    /**
     * The first camera: the one of the script request, else the default lens of the settings screen. A script camera
     * that vanished since `openCaptureUi` checked it fails the request and closes the screen.
     * zh-CN: 首个镜头: 脚本请求的镜头, 否则为设置页的默认镜头. 脚本镜头在 `openCaptureUi` 检查之后消失时, 请求失败并关闭本页.
     */
    private fun firstCamera(provider: ProcessCameraProvider, list: List<CameraInfo>): CameraInfo? {
        val request = CaptureUiRegistry.peek(captureToken)?.request
        if (request != null) {
            return try {
                CameraEngine.select(provider, request.cameraId, request.facing)
            } catch (failure: CameraFailure) {
                captureToken?.let { CaptureUiRegistry.fail(it, failure) }
                finish()
                null
            }
        }
        val facing = CameraPreferences.defaultFacing(this)
        return list.firstOrNull { CameraEngine.facingOf(it) == facing } ?: list.first()
    }

    /** The capture use case as a session builds it (appendix D Q13 = (b)). zh-CN: 与会话相同方式构建的拍照用例 (附录 D Q13 = (b)). */
    private fun captureUseCase(request: CaptureRequest, info: CameraInfo): ImageCapture {
        val strategy = if (request.width != null && request.height != null) {
            ResolutionStrategy(Size(max(request.width, request.height), min(request.width, request.height)), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
        } else {
            ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
        }
        val flash = when {
            !info.hasFlashUnit() -> ImageCapture.FLASH_MODE_OFF
            request.flashMode == CameraContract.FLASH_AUTO -> ImageCapture.FLASH_MODE_AUTO
            request.flashMode == CameraContract.FLASH_ON -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_OFF
        }
        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(request.quality)
            .setFlashMode(flash)
            .setTargetRotation(deviceRotation)
            .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(strategy).build())
            .build()
    }

    /** Unbinds only this screen's use cases, never `unbindAll`. zh-CN: 只解绑本页的用例, 不使用 `unbindAll`. */
    /**
     * Unbinds and gives the camera back at once; used before a script is answered, since `onStop` comes too late for a
     * script that opens the viewfinder again right away.
     * zh-CN: 立即解绑并归还相机; 在应答脚本之前使用, 因为对紧接着再次打开取景页的脚本而言 `onStop` 来得太晚.
     */
    private fun releaseCamera() {
        MainThread.handler.removeCallbacks(busyPoll)
        unbindOwn()
        exclusivity.release(holder)
    }

    private fun unbindOwn() {
        camera?.cameraInfo?.let { info ->
            info.zoomState.removeObserver(zoomObserver)
            info.torchState.removeObserver(torchObserver)
        }
        val useCases = listOfNotNull<UseCase>(preview, imageCapture)
        if (useCases.isNotEmpty()) provider?.let { p -> runCatching { p.unbind(*useCases.toTypedArray()) } }
        preview = null
        imageCapture = null
        camera = null
        surfaceRequest = null
        torchOn = false
    }

    private fun switchLens() {
        val ready = provider ?: return
        if (cameras.size < 2 || shooting || phase != Phase.READY) return
        val index = cameras.indexOfFirst { CameraEngine.idOf(it) == selectedId }
        selectedId = CameraEngine.idOf(cameras[(index + 1) % cameras.size])
        bind(ready)
    }

    private fun toggleTorch() {
        camera?.cameraControl?.enableTorch(!torchOn)
    }

    private fun setLinearZoom(value: Float) {
        zoomLinear = value
        camera?.cameraControl?.setLinearZoom(value)
    }

    private fun pinch(factor: Float) {
        val control = camera?.cameraControl ?: return
        control.setZoomRatio(ZoomMath.clampRatio(zoomRatio * factor, zoomMin, zoomMax))
    }

    /** Tap to focus; false when the tap is outside the picture. zh-CN: 点按对焦; 点按位置不在画面内时为 false. */
    private fun focusAt(offset: Offset, transformer: MutableCoordinateTransformer, request: SurfaceRequest): Boolean {
        val control = camera?.cameraControl ?: return false
        val point = with(transformer) { offset.transform() }
        val width = request.resolution.width.toFloat()
        val height = request.resolution.height.toFloat()
        if (point.x !in 0f..width || point.y !in 0f..height) return false
        val meteringPoint = SurfaceOrientedMeteringPointFactory(width, height).createPoint(point.x, point.y)
        control.startFocusAndMetering(
            FocusMeteringAction.Builder(meteringPoint).setAutoCancelDuration(FOCUS_AUTO_CANCEL_SECONDS, TimeUnit.SECONDS).build(),
        )
        return true
    }

    // ------------------------------------------------------------------ shutter

    private fun shoot() {
        val useCase = imageCapture ?: return
        val info = camera?.cameraInfo ?: return
        if (shooting) return
        val pending = CaptureUiRegistry.peek(captureToken)
        val request = pending?.request?.capture ?: normalCapture
        useCase.targetRotation = deviceRotation
        // The turn the picture needs to stand upright as the phone is held (appendix D Q14).
        // zh-CN: 照片按手持方向转正所需的旋转 (附录 D Q14).
        val shot = Shot(shotGeneration.incrementAndGet(), CameraEngine.facingOf(info), request.mirror, info.getSensorRotationDegrees(deviceRotation), pending)
        shooting = true
        val timeout = Runnable {
            if (claim(shot)) shotFailed("no picture within ${CameraLimits.CAPTURE_TIMEOUT_MILLIS} ms")
        }
        watchdog = timeout
        MainThread.handler.postDelayed(timeout, CameraLimits.CAPTURE_TIMEOUT_MILLIS)
        useCase.takePicture(CameraExecutors.io, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) = captured(shot, image)
            override fun onError(exception: ImageCaptureException) {
                if (claim(shot)) MainThread.post { shotFailed("ImageCapture error ${exception.imageCaptureError}: ${ErrorText.describe(exception)}") }
            }
        })
    }

    private fun claim(shot: Shot): Boolean = shotGeneration.compareAndSet(shot.generation, shot.generation + 1)

    /**
     * IO thread: writes the JPEG with its EXIF orientation settled (as a session capture does), then hands it to the
     * script or decodes it for the screen.
     * zh-CN: IO 线程: 写入 JPEG 并确定 EXIF 方向 (与会话拍照相同), 然后交给脚本或解码后显示.
     */
    private fun captured(shot: Shot, image: ImageProxy) {
        val file = try {
            CaptureWriter.newFile(applicationContext).also { file ->
                try {
                    CaptureWriter.write(image, file, shot.mirror, shot.expectedRotation)
                } catch (throwable: Throwable) {
                    file.delete()
                    throw throwable
                }
            }
        } catch (throwable: Throwable) {
            if (claim(shot)) MainThread.post { shotFailed(ErrorText.describe(throwable)) }
            return
        } finally {
            image.close()
        }
        val pending = shot.pending
        if (pending != null) {
            val sealed = try {
                CaptureWriter.seal(file, shot.facing, pending.version)
            } catch (throwable: Throwable) {
                if (claim(shot)) MainThread.post { shotFailed(ErrorText.describe(throwable)) }
                return
            }
            if (!claim(shot)) {
                runCatching { sealed.descriptor.close() }
                return
            }
            // The camera is given back before the script is answered: a script that opens the viewfinder again right away
            // must not find the closing one still holding it (CAMERA_BUSY).
            // zh-CN: 先归还相机再应答脚本: 脚本紧接着再次打开取景页时, 不应发现正在关闭的取景页仍持有相机 (CAMERA_BUSY).
            runCatching {
                MainThread.call {
                    endShot()
                    releaseCamera()
                }
            }.onFailure { Log.w(TAG, "The viewfinder did not give the camera back before answering", it) }
            val sent = CaptureUiRegistry.deliver(pending.token, sealed)
            Log.i(TAG, "Viewfinder photo ${sealed.meta.getInt(CameraResultKeys.WIDTH)}x${sealed.meta.getInt(CameraResultKeys.HEIGHT)} delivered=$sent")
            MainThread.post { finish() }
        } else {
            val decoded = try {
                decodePhoto(file)
            } catch (throwable: Throwable) {
                if (claim(shot)) MainThread.post { shotFailed(ErrorText.describe(throwable)) }
                return
            } finally {
                file.delete()
            }
            if (!claim(shot)) return
            Log.i(TAG, "Viewfinder photo ${decoded.width}x${decoded.height} shown")
            MainThread.post {
                endShot()
                // The viewfinder leaves the composition with its surface, so the preview is rebound for the next photo.
                // zh-CN: 取景器连同其 Surface 离开组合, 因此下一张照片前重新绑定预览.
                unbindOwn()
                photo = decoded
            }
        }
    }

    /** "Take another": back to the viewfinder. zh-CN: "再拍一张": 回到取景器. */
    private fun takeAnother() {
        photo = null
        startCamera()
    }

    private fun endShot() {
        shooting = false
        watchdog?.let(MainThread.handler::removeCallbacks)
        watchdog = null
    }

    private fun shotFailed(detail: String) {
        Log.w(TAG, "Viewfinder capture failed: $detail")
        endShot()
        message = getString(R.string.capture_failed)
    }

    /**
     * Any thread: the photo upright and scaled to at most [PHOTO_MAX_SIDE] pixels, with the full upright size.
     * zh-CN: 任意线程: 转正并缩放到至多 [PHOTO_MAX_SIDE] 像素的照片, 以及转正后的完整尺寸.
     */
    private fun decodePhoto(file: File): Photo {
        val orientation = runCatching {
            ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val transform = ExifOrientation.transformOf(orientation)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PHOTO_MAX_SIDE) sample *= 2
        val raw = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalStateException("the picture could not be decoded")
        val matrix = Matrix().apply {
            postRotate(transform.rotationDegrees.toFloat())
            if (transform.mirrored) postScale(-1f, 1f)
        }
        val upright = if (matrix.isIdentity) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also { if (it !== raw) raw.recycle() }
        val quarter = transform.rotationDegrees % 180 != 0
        return Photo(upright, if (quarter) bounds.outHeight else bounds.outWidth, if (quarter) bounds.outWidth else bounds.outHeight)
    }

    // ------------------------------------------------------------------ navigation

    private fun requestPermission() = permissionRequest.launch(Manifest.permission.CAMERA)

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        if (runCatching { startActivity(intent) }.isFailure) message = getString(R.string.settings_open_failed)
    }

    private fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    private fun displayRotation(): Int =
        getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0

    // ------------------------------------------------------------------ screen

    @Composable
    override fun Content() {
        val snackbar = remember { SnackbarHostState() }
        val text = message
        LaunchedEffect(text) {
            if (text != null) {
                snackbar.showSnackbar(text)
                message = null
            }
        }
        val shown = photo
        val title = when {
            shown != null && phase != Phase.BUSY -> stringResource(R.string.capture_title_result)
            scriptMode -> stringResource(R.string.capture_title_script)
            else -> stringResource(R.string.plugin_name)
        }
        PrismScreen(
            title = title,
            onBack = ::finish,
            snackbarHostState = snackbar,
            actions = {
                if (phase == Phase.READY && shown == null && hasFlash) {
                    TopBarAction(
                        icon = if (torchOn) R.drawable.ic_flashlight_off_24 else R.drawable.ic_flashlight_on_24,
                        description = stringResource(if (torchOn) R.string.capture_action_torch_off else R.string.capture_action_torch_on),
                        tag = "prism.capture.torch",
                        onClick = ::toggleTorch,
                    )
                }
                if (!scriptMode) {
                    TopBarAction(R.drawable.ic_settings_24, stringResource(R.string.capture_action_settings), "prism.capture.settings", ::openSettings)
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (phase) {
                    Phase.LOADING -> if (shown != null) PhotoPanel(shown) else CircularProgressIndicator(Modifier.align(Alignment.Center))
                    Phase.READY -> if (shown != null) PhotoPanel(shown) else ViewfinderPanel()
                    Phase.NO_PERMISSION -> StateMessage(
                        title = stringResource(R.string.capture_permission_title),
                        body = stringResource(R.string.capture_permission_body),
                        icon = R.drawable.ic_photo_camera_24,
                        primary = if (permissionBlocked) {
                            EmptyAction(stringResource(R.string.capture_permission_settings), ::openAppSettings, "prism.capture.appSettings")
                        } else {
                            EmptyAction(stringResource(R.string.capture_permission_grant), ::requestPermission, "prism.capture.grant")
                        },
                        secondary = if (permissionBlocked) {
                            emptyList()
                        } else {
                            listOf(EmptyAction(stringResource(R.string.capture_permission_settings), ::openAppSettings, "prism.capture.appSettings"))
                        },
                    )
                    Phase.POLICY -> StateMessage(stringResource(R.string.capture_disabled_policy), icon = R.drawable.ic_warning_24)
                    Phase.NO_CAMERA -> StateMessage(stringResource(R.string.capture_no_camera), icon = R.drawable.ic_warning_24)
                    Phase.FAILED -> StateMessage(
                        title = stringResource(R.string.capture_camera_unavailable),
                        icon = R.drawable.ic_warning_24,
                        primary = EmptyAction(stringResource(R.string.capture_retry), ::startCamera, "prism.capture.retry"),
                    )
                    Phase.BUSY -> StateMessage(
                        title = stringResource(R.string.capture_busy_title, busyLabel),
                        body = stringResource(R.string.capture_busy_body),
                        icon = R.drawable.ic_photo_camera_24,
                        primary = EmptyAction(stringResource(R.string.capture_busy_stop), ::stopHolder, "prism.capture.stopScript"),
                        secondary = listOf(EmptyAction(stringResource(R.string.capture_action_close), ::finish, "prism.capture.busyClose")),
                    )
                }
            }
        }
    }

    @Composable
    private fun StateMessage(
        title: String,
        body: String? = null,
        icon: Int? = null,
        primary: EmptyAction? = null,
        secondary: List<EmptyAction> = emptyList(),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = title,
                body = body,
                icon = icon,
                primary = primary,
                secondary = secondary,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }

    /**
     * The viewfinder keeps one call site in both orientations: moving `CameraXViewfinder` to another place of the
     * composition would drop the surface of the bound preview, and a surface request is answered only once.
     * zh-CN: 取景器在两种方向下保持同一调用位置: 把 `CameraXViewfinder` 移到组合中的其他位置会丢弃已绑定预览的 Surface, 而 Surface 请求只能应答一次.
     */
    @Composable
    private fun ViewfinderPanel() {
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        Box(Modifier.fillMaxSize()) {
            ViewfinderArea(Modifier.fillMaxSize().padding(end = if (landscape) CONTROL_BAR else 0.dp, bottom = if (landscape) 0.dp else CONTROL_BAR))
            if (landscape) {
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(CONTROL_BAR).padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { ControlButtons() }
            } else {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(CONTROL_BAR).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) { ControlButtons() }
            }
        }
    }

    @Composable
    private fun ViewfinderArea(modifier: Modifier) {
        val transformer = remember { MutableCoordinateTransformer() }
        var focus by remember { mutableStateOf<Offset?>(null) }
        var focusSerial by remember { mutableIntStateOf(0) }
        LaunchedEffect(focusSerial) {
            if (focus != null) {
                delay(FOCUS_RING_MILLIS)
                focus = null
            }
        }
        Box(modifier.background(Color.Black).clipToBounds()) {
            val request = surfaceRequest
            if (request != null) {
                CameraXViewfinder(
                    surfaceRequest = request,
                    coordinateTransformer = transformer,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(request) {
                            detectTapGestures { offset ->
                                if (focusAt(offset, transformer, request)) {
                                    focus = offset
                                    focusSerial++
                                }
                            }
                        }
                        .pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> if (zoom != 1f) pinch(zoom) } }
                        .testTag("prism.capture.viewfinder"),
                )
            } else {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }
            focus?.let { FocusRing(it) }
            LensChip(Modifier.align(Alignment.TopStart).padding(12.dp))
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                if (scriptMode) {
                    Text(
                        text = stringResource(R.string.capture_script_note),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().background(OVERLAY).padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                if (zoomMax > zoomMin) ZoomRow()
            }
        }
    }

    @Composable
    private fun FocusRing(point: Offset) {
        val half = with(LocalDensity.current) { (FOCUS_RING / 2).toPx() }
        Box(
            Modifier
                .offset { IntOffset((point.x - half).roundToInt(), (point.y - half).roundToInt()) }
                .size(FOCUS_RING)
                .border(2.dp, Color.White, CircleShape),
        )
    }

    @Composable
    private fun LensChip(modifier: Modifier) {
        val lens = when (lensFacing) {
            CameraContract.FACING_FRONT -> stringResource(R.string.capture_lens_front)
            CameraContract.FACING_EXTERNAL -> stringResource(R.string.capture_lens_external)
            null -> return
            else -> stringResource(R.string.capture_lens_back)
        }
        Row(
            modifier = modifier
                .background(OVERLAY, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("prism.capture.lens"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(lens, color = Color.White, style = MaterialTheme.typography.labelMedium)
            Text(String.format(Locale.ROOT, "%.1fx", zoomRatio), color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }

    @Composable
    private fun ZoomRow() {
        Row(
            modifier = Modifier.fillMaxWidth().background(OVERLAY).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.capture_zoom), color = Color.White, style = MaterialTheme.typography.labelLarge)
            Slider(
                value = zoomLinear,
                onValueChange = ::setLinearZoom,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).testTag("prism.capture.zoom"),
            )
            Text(String.format(Locale.ROOT, "%.1fx", zoomRatio), color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }

    @Composable
    private fun ControlButtons() {
        val ready = phase == Phase.READY && !shooting
        if (cameraCount >= 2) {
            IconButton(onClick = ::switchLens, enabled = ready, modifier = Modifier.size(SIDE_BUTTON).testTag("prism.capture.switch")) {
                Icon(painterResource(R.drawable.ic_cameraswitch_24), contentDescription = stringResource(R.string.capture_action_switch))
            }
        } else {
            Spacer(Modifier.size(SIDE_BUTTON))
        }
        Box(Modifier.size(SHUTTER).border(4.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(8.dp)) {
            FilledIconButton(onClick = ::shoot, enabled = ready, shape = CircleShape, modifier = Modifier.fillMaxSize().testTag("prism.capture.shutter")) {
                if (shooting) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(painterResource(R.drawable.ic_photo_camera_24), contentDescription = stringResource(R.string.capture_action_shutter))
                }
            }
        }
        if (scriptMode) {
            IconButton(onClick = ::finish, modifier = Modifier.size(SIDE_BUTTON).testTag("prism.capture.close")) {
                Icon(painterResource(R.drawable.ic_close_24), contentDescription = stringResource(R.string.capture_action_close))
            }
        } else {
            Spacer(Modifier.size(SIDE_BUTTON))
        }
    }

    @Composable
    private fun PhotoPanel(shown: Photo) {
        val image = remember(shown) { shown.bitmap.asImageBitmap() }
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val picture = @Composable { modifier: Modifier ->
            Box(modifier.background(Color.Black)) {
                Image(
                    bitmap = image,
                    contentDescription = stringResource(R.string.capture_title_result),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().testTag("prism.capture.photo"),
                )
            }
        }
        val details = @Composable { modifier: Modifier ->
            Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    text = stringResource(R.string.capture_result_size, shown.width, shown.height),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("prism.capture.photoSize"),
                )
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.capture_result_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = ::takeAnother,
                    modifier = Modifier.align(Alignment.End).defaultMinSize(minHeight = 48.dp).testTag("prism.capture.again"),
                ) { Text(stringResource(R.string.capture_again)) }
            }
        }
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                picture(Modifier.weight(1f).fillMaxHeight())
                details(Modifier.width(PHOTO_DETAILS_WIDTH).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                picture(Modifier.weight(1f).fillMaxWidth())
                details(Modifier.fillMaxWidth())
            }
        }
    }

    private companion object {
        const val TAG = "PrismViewfinder"
        const val BUSY_POLL_MILLIS = 1_000L
        const val FOCUS_RING_MILLIS = 1_500L
        const val FOCUS_AUTO_CANCEL_SECONDS = 3L
        const val PHOTO_MAX_SIDE = 2_048
        val CONTROL_BAR = 112.dp
        val SIDE_BUTTON = 56.dp
        val SHUTTER = 76.dp
        val FOCUS_RING = 64.dp
        val PHOTO_DETAILS_WIDTH = 280.dp
        val OVERLAY = Color.Black.copy(alpha = 0.45f)

        fun facingRank(facing: String): Int = when (facing) {
            CameraContract.FACING_BACK -> 0
            CameraContract.FACING_FRONT -> 1
            else -> 2
        }
    }

}
