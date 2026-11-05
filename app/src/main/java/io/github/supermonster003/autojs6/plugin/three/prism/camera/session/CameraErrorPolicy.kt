package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraErrorCodes

/**
 * What a CameraX `CameraState` error does to a session (protocol "Callbacks", "Error codes"). The codes are the
 * `CameraState.ERROR_*` constants; the policy stays free of Android types for the JVM tests.
 * zh-CN: CameraX `CameraState` 错误对会话的影响 (协议 "Callbacks", "Error codes" 两节). 错误码即 `CameraState.ERROR_*` 常量; 策略不含 Android 类型,
 * 便于 JVM 测试.
 */
internal object CameraErrorPolicy {

    const val ERROR_MAX_CAMERAS_IN_USE = 1
    const val ERROR_CAMERA_IN_USE = 2
    const val ERROR_OTHER_RECOVERABLE_ERROR = 3
    const val ERROR_STREAM_CONFIG = 4
    const val ERROR_CAMERA_DISABLED = 5
    const val ERROR_CAMERA_FATAL_ERROR = 6
    const val ERROR_DO_NOT_DISTURB_MODE_ENABLED = 7
    const val ERROR_CAMERA_REMOVED = 8

    sealed interface Action {
        /** `onError` with [code]; the session stays (CameraX retries). zh-CN: 以 [code] 调用 `onError`; 会话保留 (CameraX 会重试). */
        data class Report(val code: String, val message: String) : Action

        /** The session closes with [reason] ([code] for reason `error`). zh-CN: 会话以 [reason] 关闭 (原因 `error` 时带 [code]). */
        data class Close(val reason: String, val code: String?, val message: String) : Action
    }

    fun actionOf(error: Int): Action = when (error) {
        ERROR_MAX_CAMERAS_IN_USE, ERROR_CAMERA_IN_USE ->
            Action.Report(CameraErrorCodes.CAMERA_UNAVAILABLE, "another application holds the camera (CameraState error $error)")
        ERROR_OTHER_RECOVERABLE_ERROR ->
            Action.Report(CameraErrorCodes.CAMERA_UNAVAILABLE, "the camera device reported a recoverable error (CameraState error $error)")
        ERROR_STREAM_CONFIG ->
            Action.Report(CameraErrorCodes.INTERNAL, "the camera could not configure the streams of the bound use cases (CameraState error $error)")
        ERROR_CAMERA_DISABLED ->
            Action.Close(CameraContract.CLOSE_REASON_POLICY, null, "a device policy or the camera privacy toggle disabled the camera")
        ERROR_DO_NOT_DISTURB_MODE_ENABLED ->
            Action.Close(CameraContract.CLOSE_REASON_POLICY, null, "do not disturb mode keeps the camera closed")
        ERROR_CAMERA_REMOVED ->
            Action.Close(CameraContract.CLOSE_REASON_CAMERA_DISCONNECTED, null, "the camera was disconnected")
        ERROR_CAMERA_FATAL_ERROR ->
            Action.Close(CameraContract.CLOSE_REASON_ERROR, CameraErrorCodes.INTERNAL, "the camera device failed (CameraState error $error)")
        else ->
            Action.Close(CameraContract.CLOSE_REASON_ERROR, CameraErrorCodes.INTERNAL, "unknown CameraState error $error")
    }

}
