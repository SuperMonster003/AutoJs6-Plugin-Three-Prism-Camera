package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Camera foreground service (roadmap D10, D25, `foregroundServiceType="camera"`, not exported): while a camera session
 * is open the plugin holds it with the ongoing notification of [ThreePrismCameraApplication.CHANNEL_CAMERA], so the
 * camera keeps working after AutoJs6 leaves the foreground or the screen turns off (the capability the host binding
 * passes on ends with the host's own). It must start while the plugin still has the camera capability it inherits from
 * the visible host, so [acquire] runs when a session opens. Android may refuse the start (API 31+ background starts,
 * API 34+ while-in-use checks): [Listener.onRefused] tells the session, which then reports `CAMERA_DISABLED`. A start
 * Android drops silently (`startForegroundService` answers null, or `onStartCommand` does not come within
 * [ForegroundStartWatch.TIMEOUT_MILLIS]) is a refusal too, so the next session starts the service again (appendix D
 * Q64 = (a)). The service stops with the last session, also when the host dies (roadmap D30). The same pattern as the
 * microphone service of 3-Macaw Speech.
 * zh-CN: 相机前台服务 (路线图 D10, D25, `foregroundServiceType="camera"`, 不导出): 相机会话打开期间, 插件以 [ThreePrismCameraApplication.CHANNEL_CAMERA]
 * 的常驻通知持有它, 使 AutoJs6 离开前台或熄屏后相机仍可使用 (宿主绑定传递的能力随宿主自身的能力结束). 它必须在插件仍继承可见宿主的相机能力时启动,
 * 因此会话打开时即调用 [acquire]. Android 可能拒绝启动 (API 31+ 的后台启动, API 34+ 的使用期间检查): [Listener.onRefused] 通知会话, 会话随后报告
 * `CAMERA_DISABLED`. 被 Android 静默丢弃的启动 (`startForegroundService` 返回 null, 或 [ForegroundStartWatch.TIMEOUT_MILLIS] 内没有 `onStartCommand`)
 * 同样按被拒处理, 下一个会话会重新启动服务 (附录 D Q64 = (a)). 最后一个会话结束时服务停止, 宿主死亡时同样如此 (路线图 D30). 与 3-Macaw Speech 的麦克风服务同构.
 */
class ThreePrismCameraForegroundService : Service() {

    /** Hears the outcome of a start (main thread). zh-CN: 接收启动结果 (主线程). */
    interface Listener {
        fun onStarted()
        fun onRefused(reason: String)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        start.arrived()
        Log.i(TAG, "onStartCommand: sessions=${users.size}")
        try {
            // startForeground must follow startForegroundService, even when the last session already ended.
            // zh-CN: 即使最后一个会话已经结束, startForegroundService 之后也必须调用 startForeground.
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), foregroundType())
            running = this
            state = if (users.isNotEmpty()) STATE_FOREGROUND else STATE_IDLE
        } catch (e: Exception) {
            // SecurityException or ForegroundServiceStartNotAllowedException (API 31+, while-in-use rules of API 34+).
            // zh-CN: SecurityException 或 ForegroundServiceStartNotAllowedException (API 31+, API 34+ 的使用期间规则).
            Log.w(TAG, "Camera foreground service refused: $e")
            refused(e.toString())
            stopSelf()
            return START_NOT_STICKY
        } finally {
            finishStarter()
        }
        if (users.isEmpty()) stop() else started()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (running === this) running = null
        if (state == STATE_FOREGROUND) state = STATE_IDLE
        super.onDestroy()
    }

    private fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        if (running === this) running = null
        state = STATE_IDLE
    }

    /** Shows the holder of the newest session (one session at a time, roadmap D17). zh-CN: 显示最新会话的占用者 (同一时刻只有一个会话). */
    private fun refreshNotification() {
        runCatching { ContextCompat.getSystemService(this, android.app.NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification()) }
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, ThreePrismCameraApplication.CHANNEL_CAMERA)
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .setContentTitle(getString(R.string.notification_camera_in_use_title))
        .setContentText(getString(R.string.notification_camera_in_use_text))
        .apply { users.values.lastOrNull()?.takeIf { it.isNotBlank() }?.let { setSubText(it) } }
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build()

    private fun foregroundType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0

    companion object {
        private const val TAG = "PrismCameraService"
        private const val NOTIFICATION_ID = 0x5052

        const val STATE_IDLE = "idle"
        const val STATE_STARTING = "starting"
        const val STATE_FOREGROUND = "foreground"
        const val STATE_REFUSED = "refused"

        private val main = Handler(Looper.getMainLooper())

        /** How long the start activity may take to appear before the service is started without it. zh-CN: 启动 Activity 出现的等待上限. */
        private const val STARTER_TIMEOUT_MILLIS = 1500L

        /** The start activity closes after this at the latest. zh-CN: 启动 Activity 最迟在此后关闭. */
        private const val STARTER_LIFETIME_MILLIS = 3000L

        // Touched on the main thread only. zh-CN: 只在主线程访问.
        private val users = LinkedHashMap<Listener, String>()
        private var running: ThreePrismCameraForegroundService? = null
        private val start = ForegroundStartWatch({ delayMillis, action -> main.postDelayed(action, delayMillis) })
        private var starter: ThreePrismCameraStartActivity? = null
        private var starterSeen = false

        /** Last known state, for diagnostics and device tests. zh-CN: 最近的状态, 供诊断与设备测试. */
        @Volatile
        var state: String = STATE_IDLE
            private set

        /**
         * A camera session opened: start (or keep) the foreground service; [listener] hears whether it runs. [label]
         * (the holder of the session) is the sub text of the notification.
         * zh-CN: 一个相机会话已打开: 启动 (或保持) 前台服务; [listener] 得知其是否运行. [label] (会话的占用者) 是通知的副文本.
         */
        fun acquire(context: Context, label: String, listener: Listener) {
            val application = context.applicationContext
            onMain {
                users[listener] = label
                val service = running
                if (service != null) {
                    service.refreshNotification()
                    listener.onStarted()
                    return@onMain
                }
                // A start under way answers this listener too. zh-CN: 进行中的启动同样答复此监听器.
                if (!start.begin()) return@onMain
                state = STATE_STARTING
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || !launchStarter(application)) {
                    startService(application)?.let(::refused)
                }
            }
        }

        /**
         * API 34+: a camera foreground service started while the plugin is only bound by the visible AutoJs6 gets no
         * camera access, and `startForeground` with type camera throws. A transparent activity of the plugin makes it the
         * top app for a moment and starts the service from there; AutoJs6 binds the plugin with
         * `BIND_ALLOW_ACTIVITY_STARTS` (API 34+), which allows this background activity start while AutoJs6 is visible.
         * When the activity does not come up (AutoJs6 is not visible either), the service is started without it and
         * Android refuses it as before.
         * zh-CN: API 34+: 插件仅被可见的 AutoJs6 绑定时启动的前台服务没有相机权限, 以 camera 类型调用 `startForeground` 会抛出异常. 插件的透明 Activity
         * 让插件短暂成为顶层应用并从那里启动服务; AutoJs6 以 `BIND_ALLOW_ACTIVITY_STARTS` (API 34+) 绑定插件, 因此 AutoJs6 可见时允许这次后台启动
         * Activity. Activity 没有出现时 (AutoJs6 也不可见), 不经它直接启动服务, Android 照旧拒绝.
         */
        private fun launchStarter(context: Context): Boolean {
            starterSeen = false
            val attempt = start.attempt
            return try {
                context.startActivity(
                    Intent(context, ThreePrismCameraStartActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                    ),
                )
                main.postDelayed({
                    if (start.isCurrent(attempt) && !starterSeen) {
                        Log.w(TAG, "The camera start activity did not appear; starting the service without it")
                        startService(context)?.let(::refused)
                    }
                }, STARTER_TIMEOUT_MILLIS)
                true
            } catch (e: Exception) {
                Log.w(TAG, "The camera start activity was refused: $e")
                false
            }
        }

        /** Called by [ThreePrismCameraStartActivity] once it is created (main thread). zh-CN: 由 [ThreePrismCameraStartActivity] 在创建后调用 (主线程). */
        internal fun onStarterCreated(activity: ThreePrismCameraStartActivity) {
            starterSeen = true
            starter = activity
            main.postDelayed(::finishStarter, STARTER_LIFETIME_MILLIS)
            if (!start.starting || running != null) {
                finishStarter()
                return
            }
            startService(activity)?.let(::refused)
        }

        private fun finishStarter() {
            val activity = starter ?: return
            starter = null
            if (!activity.isFinishing) activity.finish()
        }

        /**
         * Starts the service; answers the reason of a refusal. `ContextCompat.startForegroundService` drops the answer of
         * `Context.startForegroundService`, which is null when Android drops the start silently (appendix D Q64 = (a)), so
         * the platform call is made here; a start that is accepted but never reaches `onStartCommand` is refused later,
         * through [refused], by [ForegroundStartWatch].
         * zh-CN: 启动服务; 被拒时返回原因. `ContextCompat.startForegroundService` 丢弃 `Context.startForegroundService` 的返回值, 而 Android 静默丢弃启动时
         * 它为 null (附录 D Q64 = (a)), 因此这里直接调用平台方法; 被接受却始终没有到达 `onStartCommand` 的启动, 由 [ForegroundStartWatch] 稍后经 [refused] 拒绝.
         */
        private fun startService(context: Context): String? {
            val intent = Intent(context, ThreePrismCameraForegroundService::class.java)
            val component = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
            } catch (e: Exception) {
                // ForegroundServiceStartNotAllowedException (API 31+, for example ColorOS with the host in the background) or
                // SecurityException. zh-CN: ForegroundServiceStartNotAllowedException (API 31+, 例如宿主在后台时的 ColorOS) 或 SecurityException.
                start.abandon()
                finishStarter()
                Log.w(TAG, "Camera foreground service not started: $e")
                return e.toString()
            }
            val reason = start.requested(component != null) { late ->
                Log.w(TAG, "Camera foreground service not started: $late")
                finishStarter()
                refused(late)
            } ?: return null
            finishStarter()
            Log.w(TAG, "Camera foreground service not started: $reason")
            return reason
        }

        /** A camera session ended (any thread). zh-CN: 一个相机会话已结束 (任意线程). */
        fun release(listener: Listener) {
            main.post {
                if (users.remove(listener) == null) return@post
                if (users.isEmpty()) running?.stop() else running?.refreshNotification()
            }
        }

        private fun started() {
            users.keys.toList().forEach { runCatching { it.onStarted() } }
        }

        private fun refused(reason: String) {
            state = STATE_REFUSED
            val listeners = users.keys.toList()
            users.clear()
            listeners.forEach { runCatching { it.onRefused(reason) } }
        }

        /** Runs [block] on the main thread and waits for it (at most two seconds). zh-CN: 在主线程运行 [block] 并等待 (至多两秒). */
        private fun onMain(block: () -> Unit) {
            if (Looper.myLooper() == Looper.getMainLooper()) return block()
            val latch = CountDownLatch(1)
            main.post {
                try {
                    block()
                } finally {
                    latch.countDown()
                }
            }
            latch.await(2, TimeUnit.SECONDS)
        }
    }

}
