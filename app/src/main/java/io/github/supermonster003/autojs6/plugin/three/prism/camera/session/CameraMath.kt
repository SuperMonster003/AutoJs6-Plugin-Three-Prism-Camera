package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

/**
 * Zoom ratio and linear zoom (protocol `setZoom`): the same mapping as CameraX's `ZoomControl`, linear in the inverse
 * ratio, so `zoomLinear` 0.5 lies halfway in the field of view.
 * zh-CN: 变焦倍率与线性变焦 (协议 `setZoom`): 与 CameraX `ZoomControl` 相同的映射, 对倍率的倒数线性, 因此 `zoomLinear` 0.5 位于视野的一半.
 */
internal object ZoomMath {

    fun clampRatio(ratio: Float, min: Float, max: Float): Float = ratio.coerceIn(min, maxOf(min, max))

    fun linearOf(ratio: Float, min: Float, max: Float): Float {
        if (max <= min) return 0f
        val clamped = clampRatio(ratio, min, max)
        return ((1f / clamped - 1f / min) / (1f / max - 1f / min)).coerceIn(0f, 1f)
    }

    fun ratioOf(linear: Float, min: Float, max: Float): Float {
        if (max <= min) return min
        val value = linear.coerceIn(0f, 1f)
        return clampRatio(1f / (1f / min + value * (1f / max - 1f / min)), min, max)
    }

}

/**
 * `focusAt` points (protocol: `focusX`, `focusY` in the upright frame) on the surface of a use case, whose buffer is
 * [rotationDegrees] clockwise away from upright.
 * zh-CN: `focusAt` 的点 (协议: `focusX`, `focusY` 位于转正后的画面) 换算到用例 Surface 上, 该缓冲需顺时针旋转 [rotationDegrees] 才转正.
 */
internal object FocusGeometry {

    fun bufferPoint(x: Float, y: Float, rotationDegrees: Int): Pair<Float, Float> = when (((rotationDegrees % 360) + 360) % 360) {
        90 -> y to 1f - x
        180 -> 1f - x to 1f - y
        270 -> 1f - y to x
        else -> x to y
    }

}

/**
 * EXIF orientations 1 - 8 as the clockwise rotation and the horizontal flip that make the stored image upright, and
 * the horizontal mirror of an orientation (protocol "Capture": 1 <-> 2, 3 <-> 4, 5 <-> 6, 7 <-> 8).
 * zh-CN: EXIF 方向 1 - 8 对应使存储图像转正的顺时针旋转与水平翻转, 以及方向的水平镜像 (协议 "Capture" 一节: 1 <-> 2, 3 <-> 4, 5 <-> 6, 7 <-> 8).
 */
internal object ExifOrientation {

    data class Transform(val rotationDegrees: Int, val mirrored: Boolean)

    fun transformOf(orientation: Int): Transform = when (orientation) {
        2 -> Transform(0, true)
        3 -> Transform(180, false)
        4 -> Transform(180, true)
        5 -> Transform(90, true)
        6 -> Transform(90, false)
        7 -> Transform(270, true)
        8 -> Transform(270, false)
        else -> Transform(0, false)
    }

    fun mirroredOf(orientation: Int): Int = when (orientation) {
        in 1..8 -> if (orientation % 2 == 1) orientation + 1 else orientation - 1
        else -> 2
    }

    /**
     * The orientation a capture keeps once a turn the camera left out is filled in (appendix D Q14). A camera that
     * ignores `JPEG_ORIENTATION` (the API 24 emulator) stores the sensor pixels unrotated with orientation 1 or none
     * ([stored] 1 or 0): when the capture needs a 90 or 270 degree turn ([expectedRotation]) and the pixels still have
     * the landscape shape of the sensor ([wider]), 6 or 8 is returned. A missing 180 degree turn keeps the shape and
     * cannot be told apart; any other [stored] orientation is kept.
     * zh-CN: 补上相机遗漏的旋转后照片应保留的方向 (附录 D Q14). 忽略 `JPEG_ORIENTATION` 的相机 (API 24 模拟器) 存下未旋转的传感器像素, 方向为 1 或缺失
     * ([stored] 为 1 或 0): 拍照需要旋转 90 或 270 度 ([expectedRotation]) 而像素仍是传感器的横向形状 ([wider]) 时返回 6 或 8. 遗漏的 180 度旋转
     * 不改变形状, 无法分辨; 其他 [stored] 方向保持不变.
     */
    fun completed(stored: Int, expectedRotation: Int, wider: Boolean): Int = when {
        stored != 0 && stored != 1 -> stored
        !wider -> stored
        expectedRotation == 90 -> 6
        expectedRotation == 270 -> 8
        else -> stored
    }

}

/**
 * Display rotations (`Surface.ROTATION_*`, 0 - 3) from the device orientation of an `OrientationEventListener`, for the
 * target rotation of captures: a picture is upright as the phone is held, whatever the screen shows.
 * zh-CN: 由 `OrientationEventListener` 的设备方向得出显示旋转 (`Surface.ROTATION_*`, 0 - 3), 用作拍照的目标旋转: 照片按手持方向转正, 与屏幕显示无关.
 */
internal object DeviceRotation {

    /** -1 (`ORIENTATION_UNKNOWN`, the phone lies flat) keeps [previous]. zh-CN: -1 (平放) 时保持 [previous]. */
    fun surfaceRotationOf(orientationDegrees: Int, previous: Int): Int {
        if (orientationDegrees < 0) return previous
        return when (orientationDegrees % 360) {
            in 45 until 135 -> 3
            in 135 until 225 -> 2
            in 225 until 315 -> 1
            else -> 0
        }
    }

    fun degreesOf(surfaceRotation: Int): Int = (surfaceRotation and 3) * 90

}
