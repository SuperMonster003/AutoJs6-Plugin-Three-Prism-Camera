package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import kotlin.math.abs

/**
 * Frames the plugin turns upright itself (protocol "Upright frames from the plugin", roadmap appendix D Q66 (a)): with
 * `outputUpright` the `ImageAnalysis` turns every frame clockwise by the camera's rotation during its native YUV to RGBA
 * conversion and reports `rotationDegrees` 0, so the plugin learns the applied turn from the frame itself: the quarter
 * turn of its sensor-to-buffer matrix, or else the rotation the use case is set to, whichever agrees with the frame's
 * sides. The block keeps the size of the `startAnalysis` answer (the same bytes either way); a frame that fits neither
 * reading is skipped. Pure Kotlin, so the JVM tests check every case.
 * zh-CN: 插件自行旋正的帧 (协议 "Upright frames from the plugin" 一节, 路线图附录 D Q66 (a)): 带 `outputUpright` 时, `ImageAnalysis` 在原生 YUV 转
 * RGBA 时把每帧按相机的旋转顺时针转正, 并报告 `rotationDegrees` 为 0, 因此插件从帧本身得知已施加的旋转: 取其传感器到缓冲矩阵的四分之一圈, 否则取用例
 * 所设的旋转, 以与帧宽高相符者为准. 帧块保持 `startAnalysis` 应答的尺寸 (两种方向字节数相同); 两种读法都不相符的帧被跳过. 纯 Kotlin 实现, JVM 测试覆盖
 * 每种情况.
 */
internal object UprightFrames {

    /** The size of one frame in its block and the turn the plugin applied (null: not turned). zh-CN: 一帧在块内的尺寸与插件施加的旋转 (null 表示未旋转). */
    data class Shape(val width: Int, val height: Int, val appliedRotationDegrees: Int?)

    fun swapsSides(degrees: Int): Boolean = degrees == 90 || degrees == 270

    /**
     * The clockwise quarter turn of a matrix in the order of `Matrix.getValues` (`MSCALE_X`, `MSKEW_X`, `MTRANS_X`,
     * `MSKEW_Y`, `MSCALE_Y`, ...; y grows downwards), whatever its scale and translation; null when it is no clean
     * quarter turn (a mirror, a shear, too few values).
     * zh-CN: `Matrix.getValues` 顺序 (`MSCALE_X`, `MSKEW_X`, `MTRANS_X`, `MSKEW_Y`, `MSCALE_Y`, ...; y 轴向下) 的矩阵所含的顺时针四分之一圈, 与缩放和平移
     * 无关; 不是干净的四分之一圈 (镜像, 错切, 数值不足) 时为 null.
     */
    fun quarterTurnOf(values: FloatArray): Int? {
        if (values.size < 5) return null
        val scaleX = values[0]
        val skewX = values[1]
        val skewY = values[3]
        val scaleY = values[4]
        return if (abs(scaleX) + abs(scaleY) >= abs(skewX) + abs(skewY)) {
            when {
                scaleX > 0f && scaleY > 0f -> 0
                scaleX < 0f && scaleY < 0f -> 180
                else -> null
            }
        } else {
            // x' = -sin * y, y' = sin * x: a clockwise 90 has a negative MSKEW_X and a positive MSKEW_Y.
            // zh-CN: x' = -sin * y, y' = sin * x: 顺时针 90 度时 MSKEW_X 为负, MSKEW_Y 为正.
            when {
                skewX < 0f && skewY > 0f -> 90
                skewX > 0f && skewY < 0f -> 270
                else -> null
            }
        }
    }

    /**
     * The shape of an `ImageProxy` of [imageWidth] x [imageHeight] in blocks of [blockWidth] x [blockHeight] (the answered
     * size), or null when it does not fit and is skipped. Without [outputUpright] the frame must have the block's size
     * and carries no applied turn. With it the applied turn is [matrixTurn] (from [quarterTurnOf]) when it agrees with the
     * frame's sides, else [expectedTurn] (the camera's rotation for the use case's target rotation) when that agrees, and
     * the frame may have the block's sides swapped.
     * zh-CN: [imageWidth] x [imageHeight] 的 `ImageProxy` 在 [blockWidth] x [blockHeight] (应答的尺寸) 块中的形态; 不相符时为 null, 该帧被跳过. 没有
     * [outputUpright] 时帧必须是块的尺寸, 不带施加的旋转. 有时, 施加的旋转取与帧宽高相符的 [matrixTurn] (来自 [quarterTurnOf]), 否则取相符的
     * [expectedTurn] (用例目标旋转对应的相机旋转), 帧的宽高可以与块互换.
     */
    fun shape(blockWidth: Int, blockHeight: Int, imageWidth: Int, imageHeight: Int, outputUpright: Boolean, matrixTurn: Int?, expectedTurn: Int): Shape? {
        val sameSides = imageWidth == blockWidth && imageHeight == blockHeight
        if (!outputUpright) return if (sameSides) Shape(imageWidth, imageHeight, null) else null
        val swappedSides = imageWidth == blockHeight && imageHeight == blockWidth
        if (!sameSides && !swappedSides) return null
        fun fits(turn: Int?): Boolean = when {
            turn == null || turn !in TURNS -> false
            sameSides && swappedSides -> true
            else -> swapsSides(turn) == swappedSides
        }
        val applied = when {
            fits(matrixTurn) -> matrixTurn
            fits(expectedTurn) -> expectedTurn
            else -> return null
        }
        return Shape(imageWidth, imageHeight, applied)
    }

    private val TURNS = setOf(0, 90, 180, 270)

}
