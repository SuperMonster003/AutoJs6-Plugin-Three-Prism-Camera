package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraLimits

/**
 * A failed call of the camera contract (protocol "Common rules"): one of the plugin codes of [CameraErrorCodes] with an
 * English message and the optional `limit`, `holder` and `cameraId` details. It never crosses Binder as an exception;
 * the binders turn it into an error answer or an error callback.
 * zh-CN: 相机契约的一次失败调用 (协议 "Common rules" 一节): [CameraErrorCodes] 中插件可用的错误码, 英文消息, 以及可选的 `limit`,
 * `holder`, `cameraId` 详情. 它从不作为异常跨越 Binder; Binder 实现把它转成错误应答或错误回调.
 */
internal class CameraFailure(
    val code: String,
    message: String,
    val limit: Long? = null,
    val holder: String? = null,
    val cameraId: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    init {
        require(CameraErrorCodes.isKnown(code) && code !in CameraErrorCodes.HOST_ONLY) { "not a plugin error code: $code" }
    }

    /** The message of the answer, at most 4 KiB of UTF-8. zh-CN: 应答中的消息, UTF-8 至多 4 KiB. */
    val answerMessage: String get() = ErrorText.truncateUtf8(message.orEmpty(), CameraLimits.MAX_ERROR_MESSAGE_BYTES)

}

/**
 * UTF-8 bounds of the texts the plugin answers (protocol "Limits").
 * zh-CN: 插件应答文本的 UTF-8 上限 (协议 "Limits" 一节).
 */
internal object ErrorText {

    /**
     * [text] cut at a code point boundary so that its UTF-8 form takes at most [maxBytes] bytes.
     * zh-CN: 在码点边界截断 [text], 使其 UTF-8 形式不超过 [maxBytes] 字节.
     */
    fun truncateUtf8(text: String, maxBytes: Int): String {
        var bytes = 0
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val size = when {
                codePoint < 0x80 -> 1
                codePoint < 0x800 -> 2
                codePoint < 0x10000 -> 3
                else -> 4
            }
            if (bytes + size > maxBytes) return text.substring(0, index)
            bytes += size
            index += Character.charCount(codePoint)
        }
        return text
    }

    /** A short description of [throwable] for an `INTERNAL` message. zh-CN: 用于 `INTERNAL` 消息的 [throwable] 简述. */
    fun describe(throwable: Throwable): String = throwable.message?.takeIf { it.isNotBlank() }?.let { "${throwable.javaClass.simpleName}: $it" } ?: throwable.javaClass.name

}
