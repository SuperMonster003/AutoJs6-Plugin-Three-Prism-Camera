package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

/**
 * Transparent permission screen of `ICameraPlugin.requestPermission` (roadmap D25, P2.1): asks for `CAMERA` and, on API
 * 33+, `POST_NOTIFICATIONS` (the drawer shows the foreground service notification only with it, appendix D Q12), then
 * finishes. The host learns the result from `getCapabilities` (`cameraPermissionGranted`) or a later `openSession`.
 * zh-CN: `ICameraPlugin.requestPermission` 的透明授权页 (路线图 D25, P2.1): 申请 `CAMERA`, 在 API 33+ 另申请 `POST_NOTIFICATIONS` (有它通知栏才显示
 * 前台服务通知, 附录 D Q12), 然后结束. 宿主经 `getCapabilities` (`cameraPermissionGranted`) 或之后的 `openSession` 得知结果.
 */
class ThreePrismCameraPermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val missing = missingPermissions(this)
        if (missing.isEmpty()) {
            finish()
            return
        }
        requestPermissions(missing.toTypedArray(), REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) finish()
    }

    internal companion object {
        private const val REQUEST_CODE = 0x5043

        /** The permissions the screen asks for that are not held yet. zh-CN: 授权页要申请且尚未获得的权限. */
        fun missingPermissions(context: Context): List<String> = buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

        fun intent(context: Context): Intent = Intent(context, ThreePrismCameraPermissionActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }

}
