package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Camera foreground service (roadmap D25, `foregroundServiceType="camera"`, not exported). The skeleton only declares it so
 * the Manifest contract is frozen from roadmap P0 on; roadmap P2.1 makes it call `startForeground` with the notification of
 * [ThreePrismCameraApplication.CHANNEL_CAMERA] when a session opens and stop when the session closes or the host dies.
 * Nothing starts it yet, so it stops itself if started.
 * zh-CN: 相机前台服务 (路线图 D25, `foregroundServiceType="camera"`, 不导出). 骨架只声明它, 使 Manifest 契约从路线图 P0 起固定;
 * 路线图 P2.1 让它在会话打开时以 [ThreePrismCameraApplication.CHANNEL_CAMERA] 的通知调用 `startForeground`, 会话关闭或宿主
 * 死亡时停止. 目前没有任何组件启动它, 被启动时立即自行停止.
 */
class ThreePrismCameraForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

}
