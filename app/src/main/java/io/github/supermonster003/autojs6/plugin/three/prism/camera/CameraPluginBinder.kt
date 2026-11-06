package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.camera.core.CameraInfo
import androidx.camera.lifecycle.ProcessCameraProvider
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.BundleOptions
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraEngine
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraExclusivity
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraFailure
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraRequests
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraSession
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraSessionBinder
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CaptureUiRegistry
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.MainThread
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.OpenRequest
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.answer
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.guarded
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.ThreePrismCameraCaptureActivity
import org.autojs.plugin.camera.api.CameraCapabilityKeys
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraResultKeys
import org.autojs.plugin.camera.api.ICameraCaptureCallback
import org.autojs.plugin.camera.api.ICameraPlugin
import org.autojs.plugin.camera.api.ICameraSessionCallback
import org.autojs.plugin.common.api.PluginInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The `ICameraPlugin` Binder of the capability service (protocol "Binder interfaces", "Per method", roadmap P2.1).
 * Every method answers a Bundle stamped with the request's contract version, an error answer when the call fails; the
 * CameraX part runs on the main thread, the waits (camera provider, foreground service) on the calling Binder thread.
 * zh-CN: 能力服务的 `ICameraPlugin` Binder (协议 "Binder interfaces", "Per method" 两节, 路线图 P2.1). 每个方法都返回带请求契约版本戳的 Bundle,
 * 调用失败时为错误应答; CameraX 部分在主线程运行, 等待 (相机提供者, 前台服务) 在调用方的 Binder 线程上进行.
 */
internal class CameraPluginBinder(context: Context) : ICameraPlugin.Stub() {

    private val appContext = context.applicationContext
    private val exclusivity = CameraExclusivity.shared
    private val version = ThreePrismCameraContract.CONTRACT_VERSION

    override fun getInfo(): PluginInfo = appContext.pluginRuntimeFields().toPluginInfo()

    /** The advertised capabilities plus the live state (protocol "Discovery and binding"). zh-CN: 声明的能力加实时状态. */
    override fun getCapabilities(): Bundle = guarded(version, TAG) {
        val permission = CameraEngine.hasPermission(appContext)
        val cameraCount = if (permission) runCatching { cameraInfos().size }.getOrDefault(0) else 0
        answer(version) {
            putAll(appContext.pluginRuntimeFields().toPluginInfo().capabilities ?: Bundle())
            putBoolean(CameraCapabilityKeys.PERMISSION_GRANTED, permission)
            putString(CameraCapabilityKeys.ENGINE_VERSION, CameraEngine.VERSION)
            putInt(CameraCapabilityKeys.CAMERA_COUNT, cameraCount)
            putBoolean(CameraCapabilityKeys.SHARED_MEMORY, Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
        }
    }

    override fun listCameras(): Bundle = guarded(version, TAG) {
        requirePermission()
        val provider = CameraEngine.provider(appContext)
        val cameras = MainThread.call { CameraEngine.cameras(provider).map(CameraEngine::describe) }
        answer(version) { putParcelableArray(CameraResultKeys.CAMERAS, cameras.toTypedArray<Parcelable>()) }
    }

    /**
     * Opens the one session of the plugin (protocol "Per method", "The viewfinder and `CAMERA_BUSY`"): checks the
     * permission, the policy and the holder, resolves the camera, starts the session and its foreground service, and
     * answers the session Binder with the state keys (`ready`).
     * zh-CN: 打开插件唯一的会话 (协议 "Per method", "The viewfinder and `CAMERA_BUSY`" 两节): 检查权限, 策略与持有者, 确定镜头, 启动会话及其前台服务,
     * 返回会话 Binder 与状态键 (`ready`).
     */
    override fun openSession(request: Bundle?, callback: ICameraSessionCallback?): Bundle = guarded(request, TAG) { requestVersion ->
        callback ?: throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, "the session callback is null")
        val open = CameraRequests.open(BundleOptions(request))
        requirePermission()
        if (CameraEngine.disabledByPolicy(appContext)) throw CameraFailure(CameraErrorCodes.CAMERA_DISABLED, "a device policy disabled the camera")
        val holder = CameraExclusivity.Holder(open.holderLabel, viewfinder = false)
        exclusivity.acquire(holder)?.let { busy ->
            throw CameraFailure(CameraErrorCodes.CAMERA_BUSY, "the camera is held by ${busy.busyLabel}", holder = busy.busyLabel)
        }
        val foreground = try {
            val provider = CameraEngine.provider(appContext)
            val info = MainThread.call { CameraEngine.select(provider, open.cameraId, open.facing).also { validate(it, open) } }
            createSession(provider, info, open, callback, requestVersion, holder)
        } catch (throwable: Throwable) {
            exclusivity.release(holder)
            throw throwable
        }
        val session = foreground.session
        holdForeground(foreground)
        val binder = CameraSessionBinder(session)
        val state = MainThread.call { session.stateBundle() }
        answer(requestVersion) {
            putAll(state)
            putBinder(CameraResultKeys.SESSION, binder)
        }
    }

    private fun createSession(
        provider: ProcessCameraProvider,
        info: CameraInfo,
        open: OpenRequest,
        callback: ICameraSessionCallback,
        requestVersion: Int,
        holder: CameraExclusivity.Holder,
    ): ForegroundHold {
        lateinit var foreground: ForegroundHold
        val session = CameraSession(appContext, provider, info, open, callback, requestVersion, holder) {
            exclusivity.release(holder)
            ThreePrismCameraForegroundService.release(foreground)
        }
        foreground = ForegroundHold(session)
        holder.onPreempted = { session.close(CameraContract.CLOSE_REASON_PLUGIN_UI, null, null, notify = true) }
        MainThread.call { session.start() }
        return foreground
    }

    /**
     * Starts the camera foreground service for [session] and waits for its outcome (roadmap D10, D25): a refusal closes
     * the session and fails the call with `CAMERA_DISABLED`; a refusal after the answer only reports `onError`.
     * zh-CN: 为 [session] 启动相机前台服务并等待结果 (路线图 D10, D25): 被拒时关闭会话并以 `CAMERA_DISABLED` 结束调用; 应答之后的拒绝只报告 `onError`.
     */
    private fun holdForeground(foreground: ForegroundHold) {
        val session = foreground.session
        ThreePrismCameraForegroundService.acquire(appContext, session.holder.label, foreground)
        if (!foreground.latch.await(FOREGROUND_WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
            Log.w(TAG, "The camera foreground service did not start within $FOREGROUND_WAIT_MILLIS ms; the session goes on")
        }
        foreground.answer()?.let { reason ->
            session.close(CameraContract.CLOSE_REASON_ERROR, null, null, notify = false)
            throw CameraFailure(CameraErrorCodes.CAMERA_DISABLED, "the camera foreground service was refused: $reason")
        }
    }

    /**
     * The foreground service listener of one session. [onStarted] may come more than once (a later session restarts the
     * notification), so it only opens the latch. A refusal before [answer] fails `openSession`, a later one is reported.
     * zh-CN: 一个会话的前台服务监听器. [onStarted] 可能不止一次 (后续会话刷新通知), 因此只打开闩锁. [answer] 之前的拒绝使 `openSession` 失败, 之后的拒绝被报告.
     */
    private class ForegroundHold(val session: CameraSession) : ThreePrismCameraForegroundService.Listener {
        val latch = CountDownLatch(1)
        private var answered = false
        private var refusal: String? = null

        override fun onStarted() = latch.countDown()

        override fun onRefused(reason: String) {
            val late = synchronized(this) {
                if (!answered) refusal = reason
                answered
            }
            latch.countDown()
            if (late) session.reportError(CameraFailure(CameraErrorCodes.CAMERA_DISABLED, "the camera foreground service was refused: $reason"))
        }

        /** Marks the call answered and returns an earlier refusal. zh-CN: 标记调用已应答并返回此前的拒绝. */
        fun answer(): String? = synchronized(this) {
            answered = true
            refusal
        }
    }

    /**
     * Starts the viewfinder for one picture (protocol "Capture", roadmap D15, D31, P3.1). The result arrives through
     * [CaptureUiRegistry]; a start the system drops (AutoJs6 not visible) expires after
     * [CaptureUiRegistry.ATTACH_TIMEOUT_MILLIS] with `UI_REQUIRES_FOREGROUND`.
     * zh-CN: 为一张照片启动取景页 (协议 "Capture" 一节, 路线图 D15, D31, P3.1). 结果经 [CaptureUiRegistry] 送达; 被系统丢弃的启动 (AutoJs6 不可见)
     * 在 [CaptureUiRegistry.ATTACH_TIMEOUT_MILLIS] 后以 `UI_REQUIRES_FOREGROUND` 过期.
     */
    override fun openCaptureUi(request: Bundle?, callback: ICameraCaptureCallback?): Bundle = guarded(request, TAG) { requestVersion ->
        callback ?: throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, "the capture callback is null")
        val captureUi = CameraRequests.captureUi(BundleOptions(request))
        requirePermission()
        if (CaptureUiRegistry.hasPending() || exclusivity.holder()?.viewfinder == true) {
            throw CameraFailure(CameraErrorCodes.CAMERA_BUSY, "the viewfinder is open already", holder = CameraContract.HOLDER_PLUGIN_UI)
        }
        val provider = CameraEngine.provider(appContext)
        MainThread.call { CameraEngine.select(provider, captureUi.cameraId, captureUi.facing) }
        val pending = CaptureUiRegistry.register(callback, requestVersion, captureUi)
        try {
            appContext.startActivity(
                Intent(appContext, ThreePrismCameraCaptureActivity::class.java)
                    .putExtra(CaptureUiRegistry.EXTRA_TOKEN, pending.token)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            CaptureUiRegistry.fail(pending.token, CameraFailure(CameraErrorCodes.UI_REQUIRES_FOREGROUND, "the viewfinder could not start"))
            throw CameraFailure(CameraErrorCodes.UI_REQUIRES_FOREGROUND, "the viewfinder could not start: $e")
        }
        val token = pending.token
        MainThread.handler.postDelayed({ CaptureUiRegistry.expireUnattached(token) }, CaptureUiRegistry.ATTACH_TIMEOUT_MILLIS)
        answer(requestVersion)
    }

    /**
     * Opens the permission screen when `android.permission.CAMERA` is missing and answers at once (protocol "Per method").
     * zh-CN: 缺少 `android.permission.CAMERA` 时打开权限页并立即返回 (协议 "Per method" 一节).
     */
    override fun requestPermission(request: Bundle?): Bundle = guarded(request, TAG) { requestVersion ->
        val launched = if (CameraEngine.hasPermission(appContext)) {
            false
        } else {
            try {
                appContext.startActivity(ThreePrismCameraPermissionActivity.intent(appContext))
                true
            } catch (e: Exception) {
                throw CameraFailure(CameraErrorCodes.UI_REQUIRES_FOREGROUND, "the permission screen could not start: $e")
            }
        }
        answer(requestVersion) { putBoolean(CameraResultKeys.LAUNCHED, launched) }
    }

    private fun requirePermission() {
        if (!CameraEngine.hasPermission(appContext)) {
            throw CameraFailure(CameraErrorCodes.PERMISSION_DENIED, "the plugin lacks ${Manifest.permission.CAMERA}")
        }
    }

    private fun cameraInfos(): List<CameraInfo> {
        val provider = CameraEngine.provider(appContext)
        return MainThread.call { CameraEngine.cameras(provider) }
    }

    /** Main thread: the open options the camera cannot honor. zh-CN: 主线程: 镜头无法满足的打开选项. */
    private fun validate(info: CameraInfo, open: OpenRequest) {
        val flashMode = open.flashMode
        if (flashMode != null && flashMode != CameraContract.FLASH_OFF && !info.hasFlashUnit()) {
            throw CameraFailure(CameraErrorCodes.UNSUPPORTED, "the camera has no flash unit for flash mode $flashMode")
        }
        val index = open.exposureIndex ?: return
        val exposure = info.exposureState
        if (!exposure.isExposureCompensationSupported) {
            if (index != 0) throw CameraFailure(CameraErrorCodes.UNSUPPORTED, "the camera has no exposure compensation")
            return
        }
        val range = exposure.exposureCompensationRange
        if (index < range.lower) throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, "exposure index $index is below ${range.lower}", limit = range.lower.toLong())
        if (index > range.upper) throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, "exposure index $index is above ${range.upper}", limit = range.upper.toLong())
    }

    private companion object {
        const val TAG = "PrismPlugin"

        /** How long `openSession` waits for the foreground service (the host allows 15 s). zh-CN: `openSession` 等待前台服务的时长 (宿主允许 15 秒). */
        const val FOREGROUND_WAIT_MILLIS = 5_000L
    }

}
