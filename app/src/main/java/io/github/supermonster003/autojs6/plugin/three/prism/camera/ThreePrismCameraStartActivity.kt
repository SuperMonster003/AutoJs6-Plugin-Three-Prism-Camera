package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.app.Activity
import android.os.Bundle

/**
 * Transparent, internal activity that starts [ThreePrismCameraForegroundService] while the plugin is the top app (API
 * 34+, roadmap D25): only a camera foreground service started from a visible state gets camera access since Android 14.
 * It shows nothing and closes as soon as the service reports (three seconds at the latest).
 * zh-CN: 透明的内部 Activity, 在插件为顶层应用时启动 [ThreePrismCameraForegroundService] (API 34+, 路线图 D25): 自 Android 14 起, 只有在可见状态下
 * 启动的相机前台服务才能访问相机. 它不显示任何内容, 服务一有结果即关闭 (最迟三秒).
 */
class ThreePrismCameraStartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            finish()
            return
        }
        ThreePrismCameraForegroundService.onStarterCreated(this)
    }

}
