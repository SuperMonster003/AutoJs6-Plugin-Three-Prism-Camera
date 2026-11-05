package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraLimits
import org.autojs.plugin.camera.api.CameraResultKeys
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * CameraX access of the capability service: the process camera provider, the permission and policy checks, the camera
 * Bundles of `listCameras` and the camera of an `openSession` request.
 * zh-CN: 能力服务对 CameraX 的访问: 进程级相机提供者, 权限与策略检查, `listCameras` 的镜头 Bundle, 以及 `openSession` 请求对应的镜头.
 */
internal object CameraEngine {

    /** The CameraX version the plugin is built with (`cameraEngineVersion`). zh-CN: 插件构建所用的 CameraX 版本 (`cameraEngineVersion`). */
    const val VERSION = "1.6.2"

    private const val PROVIDER_TIMEOUT_MILLIS = 10_000L

    @Volatile
    private var provider: ProcessCameraProvider? = null

    /**
     * The process camera provider, initialized on first use (a Binder thread may wait for it, never the main thread).
     * zh-CN: 进程级相机提供者, 首次使用时初始化 (可由 Binder 线程等待, 从不在主线程等待).
     */
    fun provider(context: Context): ProcessCameraProvider {
        provider?.let { return it }
        check(!MainThread.isCurrent()) { "the camera provider is not awaited on the main thread" }
        val future = ProcessCameraProvider.getInstance(context.applicationContext)
        return try {
            future.get(PROVIDER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).also { provider = it }
        } catch (e: TimeoutException) {
            throw CameraFailure(CameraErrorCodes.INTERNAL, "CameraX did not initialize within $PROVIDER_TIMEOUT_MILLIS ms")
        } catch (e: ExecutionException) {
            throw CameraFailure(CameraErrorCodes.CAMERA_UNAVAILABLE, "CameraX failed to initialize: ${ErrorText.describe(e.cause ?: e)}", cause = e)
        }
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    /** A device policy disables every camera (`DevicePolicyManager.getCameraDisabled`). zh-CN: 设备策略禁用了全部相机. */
    fun disabledByPolicy(context: Context): Boolean =
        runCatching { context.getSystemService(DevicePolicyManager::class.java)?.getCameraDisabled(null) == true }.getOrDefault(false)

    fun facingOf(info: CameraInfo): String = when (info.lensFacing) {
        CameraSelector.LENS_FACING_BACK -> CameraContract.FACING_BACK
        CameraSelector.LENS_FACING_FRONT -> CameraContract.FACING_FRONT
        else -> CameraContract.FACING_EXTERNAL
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun idOf(info: CameraInfo): String = Camera2CameraInfo.from(info).cameraId

    /** The cameras CameraX can use, at most [CameraLimits.MAX_CAMERAS]. zh-CN: CameraX 可用的镜头, 至多 [CameraLimits.MAX_CAMERAS] 个. */
    fun cameras(provider: ProcessCameraProvider): List<CameraInfo> = provider.availableCameraInfos.take(CameraLimits.MAX_CAMERAS)

    /**
     * The camera of a request: [cameraId] when given, else the first camera facing [facing] (back by default).
     * zh-CN: 请求对应的镜头: 给出 [cameraId] 时按它查找, 否则取第一个朝向 [facing] 的镜头 (默认后置).
     */
    fun select(provider: ProcessCameraProvider, cameraId: String?, facing: String?): CameraInfo {
        val cameras = provider.availableCameraInfos
        if (cameraId != null) {
            return cameras.firstOrNull { runCatching { idOf(it) }.getOrNull() == cameraId }
                ?: throw CameraFailure(CameraErrorCodes.CAMERA_UNAVAILABLE, "no camera has the id $cameraId", cameraId = cameraId)
        }
        val wanted = facing ?: CameraContract.FACING_BACK
        return cameras.firstOrNull { facingOf(it) == wanted }
            ?: throw CameraFailure(CameraErrorCodes.CAMERA_UNAVAILABLE, "no camera faces $wanted")
    }

    /** The camera Bundle of `listCameras`. zh-CN: `listCameras` 的镜头 Bundle. */
    @OptIn(ExperimentalCamera2Interop::class)
    fun describe(info: CameraInfo): Bundle = Bundle().apply {
        val camera2 = Camera2CameraInfo.from(info)
        putString(CameraResultKeys.ID, camera2.cameraId)
        putString(CameraResultKeys.FACING, facingOf(info))
        putBoolean(CameraResultKeys.HAS_FLASH, info.hasFlashUnit())
        putInt(CameraResultKeys.SENSOR_ROTATION, info.sensorRotationDegrees)
        val zoom = info.zoomState.value
        putFloat(CameraResultKeys.ZOOM_MIN, zoom?.minZoomRatio ?: 1f)
        putFloat(CameraResultKeys.ZOOM_MAX, zoom?.maxZoomRatio ?: 1f)
        val exposure = info.exposureState
        val supported = exposure.isExposureCompensationSupported
        putInt(CameraResultKeys.EXPOSURE_MIN, if (supported) exposure.exposureCompensationRange.lower else 0)
        putInt(CameraResultKeys.EXPOSURE_MAX, if (supported) exposure.exposureCompensationRange.upper else 0)
        putFloat(CameraResultKeys.EXPOSURE_STEP, if (supported) exposure.exposureCompensationStep.toFloat() else 0f)
        val map = runCatching { camera2.getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) }.getOrNull()
        val analysis = map?.getOutputSizes(ImageFormat.YUV_420_888).orEmpty().filter(::isAnalysisSize)
        val capture = map?.getOutputSizes(ImageFormat.JPEG).orEmpty().toList()
        putStringArray(CameraResultKeys.ANALYSIS_RESOLUTIONS, resolutionList(analysis))
        putStringArray(CameraResultKeys.CAPTURE_RESOLUTIONS, resolutionList(capture))
    }

    /**
     * A buffer size within the analysis limits as the host checks them (width 160 - 1920, height 120 - 1080; buffers
     * are in sensor orientation, landscape on phones).
     * zh-CN: 按宿主的检查方式处于分析限制内的缓冲尺寸 (宽 160 - 1920, 高 120 - 1080; 缓冲为传感器方向, 手机上为横向).
     */
    fun isAnalysisSize(size: Size): Boolean = isAnalysisSize(size.width, size.height)

    fun isAnalysisSize(width: Int, height: Int): Boolean =
        width in CameraLimits.MIN_ANALYSIS_WIDTH..CameraLimits.MAX_ANALYSIS_WIDTH &&
            height in CameraLimits.MIN_ANALYSIS_HEIGHT..CameraLimits.MAX_ANALYSIS_HEIGHT

    private fun resolutionList(sizes: List<Size>): Array<String> = sizes
        .distinctBy { it.width to it.height }
        .sortedWith(compareByDescending<Size> { it.width.toLong() * it.height }.thenByDescending { it.width })
        .take(CameraLimits.MAX_RESOLUTIONS)
        .map { "${it.width}x${it.height}" }
        .toTypedArray()

}
