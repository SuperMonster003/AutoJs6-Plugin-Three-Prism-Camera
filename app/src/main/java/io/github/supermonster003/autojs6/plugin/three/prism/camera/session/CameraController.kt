package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import android.content.Context
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

/**
 * The CameraX entry of the plugin (roadmap P0.2 shell). It obtains the process camera provider and reports how many cameras
 * CameraX can use; roadmap P2.1 grows it into the session owner (bind / unbind the use cases, single session, D30 release).
 * Enumerating cameras needs no `CAMERA` permission, so the probe also works before the user grants it.
 * zh-CN: 插件的 CameraX 入口 (路线图 P0.2 外壳). 它获取进程级相机提供者并报告 CameraX 可用的镜头数; 路线图 P2.1 把它扩展为会话
 * 持有者 (绑定 / 解绑用例, 单会话, D30 释放). 枚举镜头不需要 `CAMERA` 权限, 因此在用户授权之前探测同样可用.
 */
internal class CameraController(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Result of one probe: [cameraCount] is `availableCameraInfos.size`, or `null` with [error] when CameraX failed to
     * initialize (no camera service, a vendor HAL failure).
     * zh-CN: 一次探测的结果: [cameraCount] 为 `availableCameraInfos.size`; CameraX 初始化失败 (无相机服务, 厂商 HAL 故障) 时为
     * `null`, 原因在 [error].
     */
    data class Probe(val cameraCount: Int?, val error: String?)

    /**
     * Initializes CameraX through `ProcessCameraProvider.getInstance` and delivers the probe on the main thread; the result
     * is also logged under [TAG] as `availableCameraInfos=<count>` for the device scripts.
     * zh-CN: 经 `ProcessCameraProvider.getInstance` 初始化 CameraX, 在主线程交付探测结果; 结果同时以 `availableCameraInfos=<数量>`
     * 记入 [TAG] 日志, 供设备脚本读取.
     */
    fun probe(onResult: (Probe) -> Unit) {
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener({
            val probe = runCatching { future.get().availableCameraInfos.size }.fold(
                onSuccess = { Probe(it, null) },
                onFailure = { Probe(null, it.cause?.toString() ?: it.toString()) },
            )
            if (probe.cameraCount != null) {
                Log.i(TAG, "availableCameraInfos=${probe.cameraCount}")
            } else {
                Log.w(TAG, "availableCameraInfos=error ${probe.error}")
            }
            onResult(probe)
        }, ContextCompat.getMainExecutor(appContext))
    }

    internal companion object {
        const val TAG = "PrismCamera"
    }

}
