package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraLimits
import org.autojs.plugin.camera.api.CameraOptionKeys

/**
 * Read-only view of a request Bundle, so the checks below run in JVM tests on plain maps.
 * zh-CN: 请求 Bundle 的只读视图, 使下面的检查可以在 JVM 测试中直接用 Map 运行.
 */
internal interface OptionSource {
    fun has(key: String): Boolean
    fun value(key: String): Any?
}

internal class MapOptions(private val map: Map<String, Any?>) : OptionSource {
    override fun has(key: String): Boolean = map.containsKey(key)
    override fun value(key: String): Any? = map[key]
}

internal data class OpenRequest(
    val facing: String?,
    val cameraId: String?,
    val width: Int?,
    val height: Int?,
    val fps: Int?,
    val flashMode: String?,
    val exposureIndex: Int?,
    val holderLabel: String,
)

/**
 * `startAnalysis`: [upright] and [mirror] are informational (the host mirrors); [outputUpright] asks the plugin to turn
 * the frames upright itself (protocol "Upright frames from the plugin", roadmap appendix D Q66 (a)).
 * zh-CN: `startAnalysis`: [upright] 与 [mirror] 仅供参考 (由宿主镜像); [outputUpright] 请插件自行旋正帧 (协议 "Upright frames from the plugin" 一节,
 * 路线图附录 D Q66 (a)).
 */
internal data class AnalysisRequest(
    val width: Int,
    val height: Int,
    val fps: Int?,
    val upright: Boolean,
    val mirror: Boolean,
    val outputUpright: Boolean = false,
)

internal data class CaptureRequest(val quality: Int, val flashMode: String?, val width: Int?, val height: Int?, val mirror: Boolean)

internal data class CaptureUiRequest(val facing: String?, val cameraId: String?, val capture: CaptureRequest)

/**
 * The values a request falls back to when it omits a key (appendix D Q41 = (b)): the defaults of the settings page,
 * which the binder reads; [PROTOCOL] holds the protocol defaults (the back lens, 640x480, the camera's own rate). A key
 * the request carries always wins, so a host that still sends every key gets exactly what it asks for.
 * zh-CN: 请求省略某个键时补齐所用的值 (附录 D Q41 = (b)): 设置页的默认值, 由 binder 读取; [PROTOCOL] 是协议缺省值 (后置镜头, 640x480, 相机
 * 自身的帧率). 请求携带的键总是优先, 所以仍然发送全部键的宿主得到的正是它所请求的.
 */
internal data class SessionDefaults(val facing: String, val width: Int, val height: Int, val fps: Int?) {

    companion object {
        val PROTOCOL = SessionDefaults(CameraContract.FACING_BACK, CameraLimits.DEFAULT_ANALYSIS_WIDTH, CameraLimits.DEFAULT_ANALYSIS_HEIGHT, null)

        /** The analysis sizes the settings page offers, all inside the protocol limits. zh-CN: 设置页提供的分析尺寸, 都在协议限制之内. */
        val SIZES: List<Pair<Int, Int>> = listOf(320 to 240, 640 to 480, 1280 to 720, 1920 to 1080)

        /** The frame rates the settings page offers; null is the camera's own rate. zh-CN: 设置页提供的帧率; null 为相机自身的帧率. */
        val FPS_CHOICES: List<Int?> = listOf(null, 5, 10, 15, 24, 30, 60)

        /** `640x480` to its pair, or null for anything else. zh-CN: 把 `640x480` 转为尺寸对, 其他内容为 null. */
        fun parseSize(value: String?): Pair<Int, Int>? {
            val parts = value?.split('x')?.takeIf { it.size == 2 } ?: return null
            val width = parts[0].toIntOrNull() ?: return null
            val height = parts[1].toIntOrNull() ?: return null
            return width to height
        }

        fun formatSize(size: Pair<Int, Int>): String = "${size.first}x${size.second}"
    }
}

internal data class ZoomRequest(val ratio: Float?, val linear: Float?)

internal data class FocusRequest(val x: Float, val y: Float, val size: Float, val autoCancelMillis: Long)

internal data class PreviewRequest(val surfaceWidth: Int?, val surfaceHeight: Int?, val surfaceRotation: Int?)

/**
 * The plugin-side checks of every request (protocol "Per method", "Limits"): unknown keys are ignored, a mistyped or
 * out-of-range value fails with `INVALID_ARGUMENT` (with `limit` where one applies). The host checks the same values
 * first; the plugin's answer is authoritative.
 * zh-CN: 插件对每个请求的检查 (协议 "Per method", "Limits" 两节): 忽略未知键, 类型错误或越界的值以 `INVALID_ARGUMENT` 失败 (适用时带
 * `limit`). 宿主会先检查同样的值; 以插件的应答为准.
 */
internal object CameraRequests {

    /** The largest capture size a request may name (no sensor is larger). zh-CN: 请求可指定的最大拍照尺寸 (没有更大的传感器). */
    const val MAX_CAPTURE_SIDE = 16_384

    /**
     * The contract version of a request: absent means 1; a version this plugin does not implement is `UNSUPPORTED`.
     * zh-CN: 请求的契约版本: 缺省为 1; 本插件未实现的版本为 `UNSUPPORTED`.
     */
    fun contractVersion(source: OptionSource?): Int {
        if (source == null || !source.has(CameraContract.KEY_CONTRACT_VERSION)) return CameraContract.MIN_CONTRACT_VERSION
        val version = integral(source.value(CameraContract.KEY_CONTRACT_VERSION))?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
            ?: throw CameraFailure(CameraErrorCodes.UNSUPPORTED, "${CameraContract.KEY_CONTRACT_VERSION} is not an integer")
        if (!CameraContract.supportsContractVersion(version)) {
            throw CameraFailure(
                CameraErrorCodes.UNSUPPORTED,
                "contract version $version is not implemented (${CameraContract.MIN_CONTRACT_VERSION} - ${CameraContract.MAX_CONTRACT_VERSION})",
            )
        }
        return version
    }

    /**
     * `openSession`: a missing key takes [defaults] (appendix D Q41 = (b)); a request naming `cameraId` picks the lens by
     * that id, so the default lens applies only when neither `facing` nor `cameraId` is present.
     * zh-CN: `openSession`: 缺少的键取 [defaults] (附录 D Q41 = (b)); 指定 `cameraId` 的请求按该 id 选镜头, 所以只有 `facing` 与 `cameraId`
     * 都不存在时才使用默认镜头.
     */
    fun open(source: OptionSource?, defaults: SessionDefaults = SessionDefaults.PROTOCOL): OpenRequest {
        val options = source ?: MapOptions(emptyMap())
        val cameraId = cameraId(options)
        val facing = facing(options) ?: defaults.facing.takeIf { cameraId == null }
        val (width, height) = analysisSize(options, required = false)
        val holderLabel = string(options, CameraOptionKeys.HOLDER_LABEL).orEmpty()
        if (holderLabel.length > CameraLimits.MAX_HOLDER_LABEL_CHARS) {
            invalid("${CameraOptionKeys.HOLDER_LABEL} has ${holderLabel.length} characters", CameraLimits.MAX_HOLDER_LABEL_CHARS.toLong())
        }
        return OpenRequest(
            facing = facing,
            cameraId = cameraId,
            width = width ?: defaults.width,
            height = height ?: defaults.height,
            fps = fps(options) ?: defaults.fps,
            flashMode = flashMode(options, CameraOptionKeys.FLASH_MODE),
            exposureIndex = int(options, CameraOptionKeys.EXPOSURE_INDEX),
            holderLabel = holderLabel,
        )
    }

    /** `startAnalysis`: the size and frame rate default to those of `openSession`. zh-CN: 尺寸与帧率默认取 `openSession` 的值. */
    fun analysis(source: OptionSource?, defaults: OpenRequest?): AnalysisRequest {
        val options = source ?: MapOptions(emptyMap())
        val (width, height) = analysisSize(options, required = false)
        return AnalysisRequest(
            width = width ?: defaults?.width ?: CameraLimits.DEFAULT_ANALYSIS_WIDTH,
            height = height ?: defaults?.height ?: CameraLimits.DEFAULT_ANALYSIS_HEIGHT,
            fps = fps(options) ?: defaults?.fps,
            upright = bool(options, CameraOptionKeys.UPRIGHT) ?: true,
            mirror = bool(options, CameraOptionKeys.MIRROR) ?: false,
            // An older host leaves it out and turns the frames itself. zh-CN: 旧宿主不发送该键, 自行旋正.
            outputUpright = bool(options, CameraOptionKeys.OUTPUT_UPRIGHT) ?: false,
        )
    }

    fun capture(source: OptionSource?): CaptureRequest {
        val options = source ?: MapOptions(emptyMap())
        val quality = int(options, CameraOptionKeys.QUALITY) ?: CameraLimits.DEFAULT_QUALITY
        if (quality < CameraLimits.MIN_QUALITY) invalid("${CameraOptionKeys.QUALITY} $quality is below ${CameraLimits.MIN_QUALITY}", CameraLimits.MIN_QUALITY.toLong())
        if (quality > CameraLimits.MAX_QUALITY) invalid("${CameraOptionKeys.QUALITY} $quality is above ${CameraLimits.MAX_QUALITY}", CameraLimits.MAX_QUALITY.toLong())
        val width = int(options, CameraOptionKeys.WIDTH)
        val height = int(options, CameraOptionKeys.HEIGHT)
        if ((width == null) != (height == null)) invalid("${CameraOptionKeys.WIDTH} and ${CameraOptionKeys.HEIGHT} go together")
        listOfNotNull(width, height).forEach {
            if (it < 1) invalid("the capture size must be positive, not $it", 1L)
            if (it > MAX_CAPTURE_SIDE) invalid("the capture side $it is above $MAX_CAPTURE_SIDE", MAX_CAPTURE_SIDE.toLong())
        }
        return CaptureRequest(quality, flashMode(options, CameraOptionKeys.FLASH_MODE), width, height, bool(options, CameraOptionKeys.MIRROR) ?: false)
    }

    /**
     * `openCaptureUi`: the camera of the viewfinder and the options of its capture (protocol "Per method"); the lens
     * falls back to the default lens of [defaults] like [open].
     * zh-CN: `openCaptureUi`: 取景页的镜头与其拍照选项 (协议 "Per method" 一节); 镜头与 [open] 一样回退到 [defaults] 的默认镜头.
     */
    fun captureUi(source: OptionSource?, defaults: SessionDefaults = SessionDefaults.PROTOCOL): CaptureUiRequest {
        val options = source ?: MapOptions(emptyMap())
        val cameraId = cameraId(options)
        return CaptureUiRequest(facing(options) ?: defaults.facing.takeIf { cameraId == null }, cameraId, capture(options))
    }

    private fun cameraId(options: OptionSource): String? = string(options, CameraOptionKeys.CAMERA_ID)?.also {
        if (it.isEmpty()) invalid("${CameraOptionKeys.CAMERA_ID} is empty")
    }

    private fun facing(options: OptionSource): String? = string(options, CameraOptionKeys.FACING)?.also {
        if (it !in CameraContract.FACINGS) invalid("${CameraOptionKeys.FACING} must be one of ${CameraContract.FACINGS.joinToString()}, not $it")
    }

    /** `setZoom`: exactly one of `zoomRatio` (clamped later) or `zoomLinear` (0 - 1). zh-CN: `zoomRatio` (稍后夹取) 与 `zoomLinear` (0 - 1) 恰好其一. */
    fun zoom(source: OptionSource?): ZoomRequest {
        val options = source ?: MapOptions(emptyMap())
        val ratio = float(options, CameraOptionKeys.ZOOM_RATIO)
        val linear = float(options, CameraOptionKeys.ZOOM_LINEAR)
        if ((ratio == null) == (linear == null)) invalid("exactly one of ${CameraOptionKeys.ZOOM_RATIO} or ${CameraOptionKeys.ZOOM_LINEAR} is required")
        if (ratio != null && ratio <= 0f) invalid("${CameraOptionKeys.ZOOM_RATIO} must be positive, not $ratio")
        if (linear != null && linear !in 0f..1f) invalid("${CameraOptionKeys.ZOOM_LINEAR} $linear is outside 0 - 1", if (linear < 0f) 0L else 1L)
        return ZoomRequest(ratio, linear)
    }

    fun focus(source: OptionSource?): FocusRequest {
        val options = source ?: MapOptions(emptyMap())
        val x = float(options, CameraOptionKeys.FOCUS_X) ?: invalid("${CameraOptionKeys.FOCUS_X} is required")
        val y = float(options, CameraOptionKeys.FOCUS_Y) ?: invalid("${CameraOptionKeys.FOCUS_Y} is required")
        listOf(CameraOptionKeys.FOCUS_X to x, CameraOptionKeys.FOCUS_Y to y).forEach { (key, value) ->
            if (value !in 0f..1f) invalid("$key $value is outside 0 - 1", if (value < 0f) 0L else 1L)
        }
        val size = float(options, CameraOptionKeys.FOCUS_SIZE) ?: CameraLimits.DEFAULT_FOCUS_SIZE
        if (size <= 0f || size > 1f) invalid("${CameraOptionKeys.FOCUS_SIZE} $size is outside (0, 1]", if (size <= 0f) 0L else 1L)
        val autoCancel = long(options, CameraOptionKeys.AUTO_CANCEL_MILLIS) ?: CameraLimits.DEFAULT_FOCUS_AUTO_CANCEL_MILLIS
        if (autoCancel < 0L) invalid("${CameraOptionKeys.AUTO_CANCEL_MILLIS} $autoCancel is negative", 0L)
        if (autoCancel > CameraLimits.MAX_FOCUS_AUTO_CANCEL_MILLIS) {
            invalid("${CameraOptionKeys.AUTO_CANCEL_MILLIS} $autoCancel is above ${CameraLimits.MAX_FOCUS_AUTO_CANCEL_MILLIS}", CameraLimits.MAX_FOCUS_AUTO_CANCEL_MILLIS)
        }
        return FocusRequest(x, y, size, autoCancel)
    }

    fun preview(source: OptionSource?): PreviewRequest {
        val options = source ?: MapOptions(emptyMap())
        val width = int(options, CameraOptionKeys.SURFACE_WIDTH)
        val height = int(options, CameraOptionKeys.SURFACE_HEIGHT)
        listOfNotNull(width, height).forEach { if (it < 1) invalid("the surface size must be positive, not $it", 1L) }
        val rotation = int(options, CameraOptionKeys.SURFACE_ROTATION)
        // Surface.ROTATION_0 .. ROTATION_270.
        // zh-CN: 取值为 Surface.ROTATION_0 .. ROTATION_270.
        if (rotation != null && rotation !in 0..3) invalid("${CameraOptionKeys.SURFACE_ROTATION} $rotation is not a Surface rotation (0 - 3)")
        return PreviewRequest(width, height, rotation)
    }

    /** The argument of `setFlashMode`. zh-CN: `setFlashMode` 的参数. */
    fun flashMode(mode: String?): String {
        if (mode == null || mode !in CameraContract.FLASH_MODES) invalid("the flash mode must be one of ${CameraContract.FLASH_MODES.joinToString()}, not $mode")
        return mode
    }

    private fun analysisSize(options: OptionSource, required: Boolean): Pair<Int?, Int?> {
        val width = int(options, CameraOptionKeys.WIDTH)
        val height = int(options, CameraOptionKeys.HEIGHT)
        if ((width == null) != (height == null) || (required && width == null)) invalid("${CameraOptionKeys.WIDTH} and ${CameraOptionKeys.HEIGHT} go together")
        if (width != null) {
            if (width < CameraLimits.MIN_ANALYSIS_WIDTH) invalid("the analysis width $width is below ${CameraLimits.MIN_ANALYSIS_WIDTH}", CameraLimits.MIN_ANALYSIS_WIDTH.toLong())
            if (width > CameraLimits.MAX_ANALYSIS_WIDTH) invalid("the analysis width $width is above ${CameraLimits.MAX_ANALYSIS_WIDTH}", CameraLimits.MAX_ANALYSIS_WIDTH.toLong())
        }
        if (height != null) {
            if (height < CameraLimits.MIN_ANALYSIS_HEIGHT) invalid("the analysis height $height is below ${CameraLimits.MIN_ANALYSIS_HEIGHT}", CameraLimits.MIN_ANALYSIS_HEIGHT.toLong())
            if (height > CameraLimits.MAX_ANALYSIS_HEIGHT) invalid("the analysis height $height is above ${CameraLimits.MAX_ANALYSIS_HEIGHT}", CameraLimits.MAX_ANALYSIS_HEIGHT.toLong())
        }
        return width to height
    }

    private fun fps(options: OptionSource): Int? = int(options, CameraOptionKeys.FPS)?.also {
        if (it < CameraLimits.MIN_FPS) invalid("${CameraOptionKeys.FPS} $it is below ${CameraLimits.MIN_FPS}", CameraLimits.MIN_FPS.toLong())
        if (it > CameraLimits.MAX_FPS) invalid("${CameraOptionKeys.FPS} $it is above ${CameraLimits.MAX_FPS}", CameraLimits.MAX_FPS.toLong())
    }

    private fun flashMode(options: OptionSource, key: String): String? = string(options, key)?.also {
        if (it !in CameraContract.FLASH_MODES) invalid("$key must be one of ${CameraContract.FLASH_MODES.joinToString()}, not $it")
    }

    private fun string(options: OptionSource, key: String): String? {
        if (!options.has(key)) return null
        val value = options.value(key) ?: return null
        return value as? String ?: invalid("$key must be a string")
    }

    private fun bool(options: OptionSource, key: String): Boolean? {
        if (!options.has(key)) return null
        val value = options.value(key) ?: return null
        return value as? Boolean ?: invalid("$key must be a boolean")
    }

    private fun int(options: OptionSource, key: String): Int? = long(options, key)?.let {
        if (it !in Int.MIN_VALUE..Int.MAX_VALUE) invalid("$key $it is not a 32-bit integer")
        it.toInt()
    }

    private fun long(options: OptionSource, key: String): Long? {
        if (!options.has(key)) return null
        val value = options.value(key) ?: return null
        return integral(value) ?: invalid("$key must be an integer")
    }

    private fun float(options: OptionSource, key: String): Float? {
        if (!options.has(key)) return null
        val value = options.value(key) ?: return null
        val number = (value as? Number)?.toFloat() ?: invalid("$key must be a number")
        if (!number.isFinite()) invalid("$key must be finite")
        return number
    }

    /** An integer of any boxed type, or a floating value without fraction. zh-CN: 任何装箱整数类型, 或没有小数部分的浮点值. */
    private fun integral(value: Any?): Long? = when (value) {
        is Int -> value.toLong()
        is Long -> value
        is Short -> value.toLong()
        is Byte -> value.toLong()
        is Double -> value.takeIf { it.isFinite() && it == Math.rint(it) && it >= Long.MIN_VALUE && it <= Long.MAX_VALUE }?.toLong()
        is Float -> value.takeIf { it.isFinite() && it == Math.rint(it.toDouble()).toFloat() }?.toLong()
        else -> null
    }

    private fun invalid(message: String, limit: Long? = null): Nothing = throw CameraFailure(CameraErrorCodes.INVALID_ARGUMENT, message, limit = limit)

}
