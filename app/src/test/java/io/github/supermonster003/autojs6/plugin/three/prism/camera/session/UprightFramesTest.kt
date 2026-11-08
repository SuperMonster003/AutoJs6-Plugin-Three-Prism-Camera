package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.UprightFrames.Shape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * Frames the plugin turns upright (protocol "Upright frames from the plugin", roadmap appendix D Q66 (a)): the quarter
 * turn read off a sensor-to-buffer matrix whatever its scale and translation, and the header size and applied rotation
 * of a frame, which falls back to the use case's rotation when the matrix disagrees with the frame's sides and skips
 * the frame when neither agrees.
 * zh-CN: 插件旋正的帧 (协议 "Upright frames from the plugin" 一节, 路线图附录 D Q66 (a)): 从传感器到缓冲矩阵读出四分之一圈 (与缩放和平移无关), 以及一帧的
 * 帧头尺寸与已施加的旋转: 矩阵与帧宽高不符时退回用例的旋转, 两者都不符时跳过该帧.
 */
class UprightFramesTest {

    /**
     * The values (`Matrix.getValues` order) of a sensor-to-buffer matrix: a positive scale of the active array onto the
     * buffer, then a clockwise turn by [degrees] and a translation back into the buffer, either order of composition.
     */
    private fun matrix(degrees: Int, scaleX: Float = 0.25f, scaleY: Float = 0.3f, scaleFirst: Boolean = true): FloatArray {
        val r = Math.toRadians(degrees.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        // Rotation [c -s; s c] (y down: clockwise for a positive angle), scale diag(scaleX, scaleY).
        val (a, b, d, e) = if (scaleFirst) {
            listOf(c * scaleX, -s * scaleY, s * scaleX, c * scaleY)
        } else {
            listOf(scaleX * c, -scaleX * s, scaleY * s, scaleY * c)
        }
        return floatArrayOf(a, b, 640f, d, e, 480f, 0f, 0f, 1f)
    }

    @Test
    fun `the quarter turn of a sensor-to-buffer matrix ignores scale and translation`() {
        listOf(0, 90, 180, 270).forEach { degrees ->
            assertEquals(degrees, UprightFrames.quarterTurnOf(matrix(degrees)))
            assertEquals(degrees, UprightFrames.quarterTurnOf(matrix(degrees, scaleFirst = false)))
            assertEquals(degrees, UprightFrames.quarterTurnOf(matrix(degrees, scaleX = 3f, scaleY = 2f)))
        }
        // The identity of a frame CameraX did not turn. zh-CN: CameraX 未旋转的帧的单位矩阵.
        assertEquals(0, UprightFrames.quarterTurnOf(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)))
        // A mirror, an empty matrix and too few values are no quarter turn. zh-CN: 镜像, 全零矩阵与数值不足都不是四分之一圈.
        assertNull(UprightFrames.quarterTurnOf(floatArrayOf(-1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)))
        assertNull(UprightFrames.quarterTurnOf(floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)))
        assertNull(UprightFrames.quarterTurnOf(FloatArray(9)))
        assertNull(UprightFrames.quarterTurnOf(floatArrayOf(1f, 0f, 0f)))
    }

    @Test
    fun `without outputUpright a frame keeps the block size and names no turn`() {
        assertEquals(Shape(640, 480, null), UprightFrames.shape(640, 480, 640, 480, outputUpright = false, matrixTurn = null, expectedTurn = 90))
        // A turned image without the request does not fit. zh-CN: 未请求却已旋转的图像不相符.
        assertNull(UprightFrames.shape(640, 480, 480, 640, outputUpright = false, matrixTurn = 90, expectedTurn = 90))
        assertNull(UprightFrames.shape(640, 480, 320, 240, outputUpright = false, matrixTurn = null, expectedTurn = 0))
    }

    @Test
    fun `an upright frame names the turn of its matrix and the size of the turned image`() {
        listOf(90, 270).forEach { turn ->
            assertEquals(Shape(480, 640, turn), UprightFrames.shape(640, 480, 480, 640, outputUpright = true, matrixTurn = turn, expectedTurn = turn))
        }
        listOf(0, 180).forEach { turn ->
            assertEquals(Shape(640, 480, turn), UprightFrames.shape(640, 480, 640, 480, outputUpright = true, matrixTurn = turn, expectedTurn = turn))
        }
        // The matrix wins over a use case that already turned on: the frame was converted before. zh-CN: 矩阵优先于已转向的用例: 帧在转向之前已转换.
        assertEquals(Shape(640, 480, 180), UprightFrames.shape(640, 480, 640, 480, outputUpright = true, matrixTurn = 180, expectedTurn = 0))
    }

    @Test
    fun `a matrix that disagrees with the sides falls back to the use case, and a frame fitting neither is skipped`() {
        assertEquals(Shape(480, 640, 90), UprightFrames.shape(640, 480, 480, 640, outputUpright = true, matrixTurn = 0, expectedTurn = 90))
        assertEquals(Shape(480, 640, 270), UprightFrames.shape(640, 480, 480, 640, outputUpright = true, matrixTurn = null, expectedTurn = 270))
        assertEquals(Shape(640, 480, 0), UprightFrames.shape(640, 480, 640, 480, outputUpright = true, matrixTurn = 90, expectedTurn = 0))
        // The display turned between the conversion and the analyzer: neither reading fits the swapped sides.
        // zh-CN: 屏幕在转换与分析器之间转向: 两种读法都与互换后的宽高不符.
        assertNull(UprightFrames.shape(640, 480, 480, 640, outputUpright = true, matrixTurn = 0, expectedTurn = 180))
        assertNull(UprightFrames.shape(640, 480, 640, 480, outputUpright = true, matrixTurn = null, expectedTurn = 90))
        assertNull(UprightFrames.shape(640, 480, 320, 240, outputUpright = true, matrixTurn = 0, expectedTurn = 0))
        assertNull(UprightFrames.shape(640, 480, 640, 480, outputUpright = true, matrixTurn = 45, expectedTurn = 45))
    }

    @Test
    fun `a square frame takes any quarter turn`() {
        listOf(0, 90, 180, 270).forEach { turn ->
            assertEquals(Shape(480, 480, turn), UprightFrames.shape(480, 480, 480, 480, outputUpright = true, matrixTurn = turn, expectedTurn = 0))
        }
    }

    @Test
    fun `the turned frame has the bytes of the block`() {
        val shape = UprightFrames.shape(1280, 720, 720, 1280, outputUpright = true, matrixTurn = 270, expectedTurn = 270)!!
        assertEquals(FrameStore.byteCount(1280, 720), FrameStore.byteCount(shape.width, shape.height))
    }

}
