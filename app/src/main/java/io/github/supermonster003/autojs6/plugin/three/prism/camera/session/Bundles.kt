package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraResultKeys
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A request Bundle as an [OptionSource]. zh-CN: 作为 [OptionSource] 的请求 Bundle.
 */
internal class BundleOptions(private val bundle: Bundle?) : OptionSource {
    override fun has(key: String): Boolean = bundle?.containsKey(key) == true

    @Suppress("DEPRECATION")
    override fun value(key: String): Any? = bundle?.get(key)
}

/** An answer or callback Bundle stamped with [version] (protocol "Common rules"). zh-CN: 带 [version] 戳的应答或回调 Bundle. */
internal inline fun answer(version: Int, block: Bundle.() -> Unit = {}): Bundle =
    Bundle().apply {
        putInt(CameraContract.KEY_CONTRACT_VERSION, version)
        block()
    }

/** The error answer of a failure: `errorCode`, `errorMessage` and the details. zh-CN: 失败的错误应答: `errorCode`, `errorMessage` 与详情. */
internal fun CameraFailure.toBundle(version: Int): Bundle = answer(version) {
    putString(CameraContract.KEY_ERROR_CODE, code)
    putString(CameraContract.KEY_ERROR_MESSAGE, answerMessage)
    limit?.let { putLong(CameraResultKeys.LIMIT, it) }
    holder?.let { putString(CameraResultKeys.HOLDER, it) }
    cameraId?.let { putString(CameraResultKeys.ERROR_CAMERA_ID, it) }
}

/** Any throwable as a plugin failure (`INTERNAL` unless it is one already). zh-CN: 把任意异常转为插件失败 (不是失败时为 `INTERNAL`). */
internal fun Throwable.asCameraFailure(): CameraFailure = this as? CameraFailure ?: CameraFailure(CameraErrorCodes.INTERNAL, ErrorText.describe(this), cause = this)

/**
 * Runs one contract call: [block] gets the request's contract version and answers a Bundle; a failure becomes the error
 * answer, so nothing throws across Binder (protocol "Common rules").
 * zh-CN: 执行一次契约调用: [block] 得到请求的契约版本并返回 Bundle; 失败转为错误应答, 因此不会有异常跨越 Binder (协议 "Common rules" 一节).
 */
internal inline fun guarded(request: Bundle?, tag: String, block: (version: Int) -> Bundle): Bundle {
    val version = try {
        CameraRequests.contractVersion(BundleOptions(request))
    } catch (failure: CameraFailure) {
        return failure.toBundle(CameraContract.MIN_CONTRACT_VERSION)
    }
    return guarded(version, tag) { block(version) }
}

internal inline fun guarded(version: Int, tag: String, block: () -> Bundle): Bundle = try {
    block()
} catch (failure: CameraFailure) {
    failure.toBundle(version)
} catch (throwable: Throwable) {
    Log.w(tag, "Camera call failed", throwable)
    throwable.asCameraFailure().toBundle(version)
}

/**
 * The main thread, where all CameraX state of the plugin lives (CameraX binds use cases and changes lifecycles on the
 * main thread only). Binder threads hand work over with [call]; the main thread never waits for a Binder thread.
 * zh-CN: 主线程, 插件的全部 CameraX 状态都在这里 (CameraX 只在主线程绑定用例与改变生命周期). Binder 线程经 [call] 移交工作; 主线程从不等待 Binder 线程.
 */
internal object MainThread {

    /** The longest a Binder thread waits (protocol: binding a use case takes up to 2 s). zh-CN: Binder 线程的最长等待 (协议: 绑定用例至多 2 秒). */
    const val CALL_TIMEOUT_MILLIS = 2_000L

    val handler = Handler(Looper.getMainLooper())

    fun isCurrent(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    /**
     * Runs [block] on the main thread and answers its result; a failure of [block] is rethrown here and a main thread
     * that does not answer in time gives `INTERNAL` (the host's own timeout is `TIMEOUT`).
     * zh-CN: 在主线程运行 [block] 并返回其结果; [block] 的失败在此重新抛出, 主线程未及时响应时为 `INTERNAL` (宿主自己的超时才是 `TIMEOUT`).
     */
    fun <T> call(timeoutMillis: Long = CALL_TIMEOUT_MILLIS, block: () -> T): T {
        if (isCurrent()) return block()
        val task = FutureTask(Callable { block() })
        if (!handler.post(task)) throw CameraFailure(CameraErrorCodes.INTERNAL, "the main thread is gone")
        return try {
            task.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            task.cancel(false)
            throw CameraFailure(CameraErrorCodes.INTERNAL, "the main thread did not answer within $timeoutMillis ms")
        } catch (e: ExecutionException) {
            throw (e.cause ?: e).asCameraFailure()
        }
    }

    fun post(block: () -> Unit) {
        if (isCurrent()) block() else handler.post(block)
    }

}
