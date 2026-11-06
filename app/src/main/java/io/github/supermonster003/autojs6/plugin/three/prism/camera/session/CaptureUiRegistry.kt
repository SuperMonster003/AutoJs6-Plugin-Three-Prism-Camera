package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.ICameraCaptureCallback
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The pending `openCaptureUi` results (protocol "Capture", roadmap D15, D31): `openCaptureUi` registers the callback
 * under a token and starts the viewfinder with it ([EXTRA_TOKEN]); the viewfinder answers through [deliver] once, or
 * [cancel] when the user closes it (`CANCELLED`, which the host turns into a `null` result). A host that dies meanwhile
 * removes its entry, and a token no viewfinder picked up expires ([expireUnattached]). The viewfinder screen is
 * [io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.ThreePrismCameraCaptureActivity] (roadmap P3.1).
 * zh-CN: 待完成的 `openCaptureUi` 结果 (协议 "Capture" 一节, 路线图 D15, D31): `openCaptureUi` 以令牌登记回调并带着它 ([EXTRA_TOKEN]) 启动取景页;
 * 取景页经 [deliver] 应答一次, 用户关闭时经 [cancel] 应答 (`CANCELLED`, 宿主将其转为 `null` 结果). 期间死亡的宿主会移除自己的条目.
 * 没有取景页接手的令牌会过期 ([expireUnattached]). 取景页是 [io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.ThreePrismCameraCaptureActivity] (路线图 P3.1).
 */
internal object CaptureUiRegistry {

    const val EXTRA_TOKEN = "io.github.supermonster003.autojs6.plugin.three.prism.camera.extra.CAPTURE_UI_TOKEN"

    private const val TAG = "PrismCaptureUi"

    /**
     * How long the viewfinder may take to pick up a token. A start the system drops (a background activity start while
     * AutoJs6 is not visible) fails with `UI_REQUIRES_FOREGROUND` after it, so the script is not left waiting for ever.
     * zh-CN: 取景页接手令牌的时限. 系统丢弃的启动 (AutoJs6 不可见时的后台 Activity 启动) 超时后以 `UI_REQUIRES_FOREGROUND` 失败, 脚本不会一直等待.
     */
    const val ATTACH_TIMEOUT_MILLIS = 10_000L

    class Pending(val token: String, val callback: ICameraCaptureCallback, val version: Int, val request: CaptureUiRequest) {
        internal val done = AtomicBoolean(false)
        internal var deathRecipient: IBinder.DeathRecipient? = null

        /** The viewfinder picked up the token ([attach]). zh-CN: 取景页已接手令牌 ([attach]). */
        @Volatile
        internal var attached = false
    }

    private val pending = ConcurrentHashMap<String, Pending>()

    fun register(callback: ICameraCaptureCallback, version: Int, request: CaptureUiRequest): Pending {
        val entry = Pending(UUID.randomUUID().toString(), callback, version, request)
        val recipient = IBinder.DeathRecipient {
            Log.i(TAG, "The host of viewfinder ${entry.token} died")
            take(entry.token)
        }
        entry.deathRecipient = recipient
        try {
            callback.asBinder().linkToDeath(recipient, 0)
        } catch (e: RemoteException) {
            // The host is gone already; nothing waits for the result.
            // zh-CN: 宿主已经不在; 没有人等待结果.
            throw CameraFailure(CameraErrorCodes.INTERNAL, "the host of the viewfinder is gone")
        }
        pending[entry.token] = entry
        return entry
    }

    fun peek(token: String?): Pending? = token?.let { pending[it] }

    /** The viewfinder picked up [token] and answers it from now on. zh-CN: 取景页接手 [token], 此后由它应答. */
    fun attach(token: String?): Pending? = peek(token)?.also { it.attached = true }

    /**
     * Fails [token] with `UI_REQUIRES_FOREGROUND` when no viewfinder picked it up ([ATTACH_TIMEOUT_MILLIS]).
     * zh-CN: 没有取景页接手 [token] 时以 `UI_REQUIRES_FOREGROUND` 使其失败 ([ATTACH_TIMEOUT_MILLIS]).
     */
    fun expireUnattached(token: String): Boolean {
        val entry = pending[token] ?: return false
        if (entry.attached) return false
        Log.w(TAG, "The viewfinder of $token did not open within $ATTACH_TIMEOUT_MILLIS ms")
        return fail(token, CameraFailure(CameraErrorCodes.UI_REQUIRES_FOREGROUND, "the viewfinder did not open within $ATTACH_TIMEOUT_MILLIS ms"))
    }

    /** Whether a viewfinder result is still awaited. zh-CN: 是否仍有取景页结果待返回. */
    fun hasPending(): Boolean = pending.isNotEmpty()

    /**
     * Sends the picture of the viewfinder once; closes [sealed] in every case (the transaction carries its own copy).
     * zh-CN: 发送取景页的照片一次; 任何情况下都关闭 [sealed] (事务携带自己的副本).
     */
    fun deliver(token: String, sealed: CaptureWriter.Sealed): Boolean {
        try {
            val entry = take(token) ?: return false
            send(entry) { entry.callback.onCaptured(sealed.descriptor, sealed.meta) }
            return true
        } finally {
            runCatching { sealed.descriptor.close() }
        }
    }

    fun fail(token: String, failure: CameraFailure): Boolean {
        val entry = take(token) ?: return false
        send(entry) { entry.callback.onError(failure.toBundle(entry.version)) }
        return true
    }

    /** The user closed the viewfinder without a picture. zh-CN: 用户未拍照就关闭了取景页. */
    fun cancel(token: String): Boolean = fail(token, CameraFailure(CameraErrorCodes.CANCELLED, "the user closed the viewfinder"))

    private fun take(token: String): Pending? {
        val entry = pending.remove(token) ?: return null
        if (!entry.done.compareAndSet(false, true)) return null
        entry.deathRecipient?.let { runCatching { entry.callback.asBinder().unlinkToDeath(it, 0) } }
        return entry
    }

    private inline fun send(entry: Pending, call: () -> Unit) {
        try {
            call()
        } catch (e: RemoteException) {
            Log.w(TAG, "Viewfinder callback ${entry.token} failed: $e")
        } catch (e: RuntimeException) {
            Log.w(TAG, "Viewfinder callback ${entry.token} failed: $e")
        }
    }

}
