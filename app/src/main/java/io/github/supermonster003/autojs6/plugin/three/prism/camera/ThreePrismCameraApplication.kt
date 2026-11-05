package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraExecutors
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraSession

/**
 * The application. It registers the notification channel of the camera foreground service (roadmap D25) and removes
 * the picture and frame files a killed process left behind.
 * zh-CN: 应用类. 注册相机前台服务的通知渠道 (路线图 D25), 并删除被杀进程遗留的照片与帧文件.
 */
class ThreePrismCameraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        CameraExecutors.io.execute { runCatching { CameraSession.cleanStaleFiles(this) } }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // The camera channel carries the ongoing notification of an open camera session; it stays silent and low
        // importance because the notification only states that the camera is in use.
        // zh-CN: 相机渠道承载相机会话打开期间的常驻通知; 通知只说明相机正在使用, 因此渠道静音且重要性低.
        val camera = NotificationChannel(CHANNEL_CAMERA, getString(R.string.notification_channel_camera_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = getString(R.string.notification_channel_camera_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(camera)
    }

    internal companion object {
        const val CHANNEL_CAMERA = "camera"
    }

}
