package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Camera capability service (`org.autojs.plugin.CAMERA`, category `camera`, roadmap D23): answers the `ICameraPlugin`
 * Binder of the staged host contract (roadmap P2.1, [CameraPluginBinder]); the plugin copies no AIDL.
 * zh-CN: 相机能力服务 (`org.autojs.plugin.CAMERA`, category `camera`, 路线图 D23): 返回暂存宿主契约的 `ICameraPlugin` Binder (路线图 P2.1,
 * [CameraPluginBinder]); 插件不复制 AIDL.
 */
class ThreePrismCameraService : Service() {

    private val binder by lazy { CameraPluginBinder(this) }

    override fun onBind(intent: Intent?): IBinder = binder

}
