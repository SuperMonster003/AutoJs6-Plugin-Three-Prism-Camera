package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Camera capability service (`org.autojs.plugin.CAMERA`, category `camera`). Until the host contract of roadmap P1.1 is
 * staged (roadmap P2.1) it answers with a placeholder Binder that only carries the `ICameraPlugin` descriptor and rejects
 * every transaction (the 3-Macaw Speech P0 approach), so a host bind succeeds but no call reaches camera code; the plugin
 * copies no AIDL.
 * zh-CN: 相机能力服务 (`org.autojs.plugin.CAMERA`, category `camera`). 在路线图 P1.1 的宿主契约暂存 (路线图 P2.1) 之前,
 * 它返回只携带 `ICameraPlugin` 描述符的占位 Binder, 拒绝全部事务 (3-Macaw Speech P0 的做法), 宿主可以绑定成功, 但任何调用
 * 都不会进入相机代码; 插件不复制 AIDL.
 */
class ThreePrismCameraService : Service() {

    private val binder = Binder().apply { attachInterface(null, ThreePrismCameraContract.CAPABILITY_DESCRIPTOR) }

    override fun onBind(intent: Intent?): IBinder = binder

}
