package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.app.Activity
import android.os.Bundle

/**
 * Transparent start entry of the camera foreground service (roadmap D25): on API 34+ a camera foreground service may only
 * start while the plugin is visible, so roadmap P2.1 starts the service from here and finishes. The skeleton finishes at
 * once without any user interface.
 * zh-CN: 相机前台服务的透明启动入口 (路线图 D25): API 34+ 只允许插件在可见状态启动相机前台服务, 路线图 P2.1 在这里启动服务后
 * 结束. 骨架立即结束, 不显示任何界面.
 */
class ThreePrismCameraStartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }

}
