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

        /**
         * `onError` with [code] first, then the session closes with [reason] (appendix D Q57 = (a)): the close reason keeps
         * its meaning, the error tells the script why the camera never delivered.
         * zh-CN: 先以 [code] 调用 `onError`, 再以 [reason] 关闭会话 (附录 D Q57 = (a)): 关闭原因的含义不变, 错误告诉脚本镜头为何没有出帧.
         */
        data class FailAndClose(val code: String, val reason: String, val message: String) : Action
    }

    /**
     * The action for CameraX error [error]; [policyDisabled] tells whether a device policy disables the camera, which only
     * changes the message of `ERROR_CAMERA_DISABLED`.
     * zh-CN: CameraX 错误 [error] 对应的动作; [policyDisabled] 表示设备策略是否禁用了相机, 只影响 `ERROR_CAMERA_DISABLED` 的消息.
     */
    fun actionOf(error: Int, policyDisabled: Boolean = false): Action = when (error) {
        ERROR_MAX_CAMERAS_IN_USE, ERROR_CAMERA_IN_USE ->
            Action.Report(CameraErrorCodes.CAMERA_UNAVAILABLE, "another application holds the camera (CameraState error $error)")
        ERROR_OTHER_RECOVERABLE_ERROR ->
            Action.Report(CameraErrorCodes.CAMERA_UNAVAILABLE, "the camera device reported a recoverable error (CameraState error $error)")
        ERROR_STREAM_CONFIG ->
            Action.Report(CameraErrorCodes.INTERNAL, "the camera could not configure the streams of the bound use cases (CameraState error $error)")
        // The system refuses the camera to a foreground service started while the host was in the background or at the
        // secure lock screen (API 30+ while-in-use rule), and to any app under a device policy.
        // zh-CN: 宿主在后台或停在安全锁屏时启动的前台服务, 系统不给相机 (API 30+ 使用期间规则); 设备策略禁用相机时对任何应用都是如此.
        ERROR_CAMERA_DISABLED -> Action.FailAndClose(
            CameraErrorCodes.CAMERA_DISABLED,
            CameraContract.CLOSE_REASON_POLICY,
            if (policyDisabled) {
                "a device policy disabled the camera (CameraState error $error)"
            } else {
                "the system does not let the plugin use the camera now: the host is in the background or the phone is at the lock screen, or the camera privacy toggle is on (CameraState error $error)"
            },
        )
        // No code of the contract names do not disturb mode; CAMERA_DISABLED ("the camera may not be used now") is closest.
        // zh-CN: 契约没有表示勿扰模式的错误码; 最接近的是 CAMERA_DISABLED ("现在不能使用相机").
        ERROR_DO_NOT_DISTURB_MODE_ENABLED ->
            Action.FailAndClose(CameraErrorCodes.CAMERA_DISABLED, CameraContract.CLOSE_REASON_POLICY, "do not disturb mode keeps the camera closed (CameraState error $error)")
        ERROR_CAMERA_REMOVED ->
            Action.Close(CameraContract.CLOSE_REASON_CAMERA_DISCONNECTED, null, "the camera was disconnected")
        ERROR_CAMERA_FATAL_ERROR ->
            Action.Close(CameraContract.CLOSE_REASON_ERROR, CameraErrorCodes.INTERNAL, "the camera device failed (CameraState error $error)")
        else ->
            Action.Close(CameraContract.CLOSE_REASON_ERROR, CameraErrorCodes.INTERNAL, "unknown CameraState error $error")
    }

}
