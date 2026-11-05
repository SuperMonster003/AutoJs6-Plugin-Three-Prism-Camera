package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Zoom, focus point, EXIF orientation and device rotation arithmetic of roadmap P2.3 and P2.4.
 * zh-CN: 路线图 P2.3 与 P2.4 的变焦, 对焦点, EXIF 方向与设备旋转计算.
 */
class CameraMathTest {

    private val delta = 1e-4f

    @Test
    fun `linear zoom is linear in the field of view and round trips`() {
        assertEquals(0f, ZoomMath.linearOf(1f, 1f, 8f), delta)
        assertEquals(1f, ZoomMath.linearOf(8f, 1f, 8f), delta)
        // Halfway in the field of view between 1x and 3x is 1.5x.
        // zh-CN: 1x 与 3x 之间视野的一半是 1.5x.
        assertEquals(1.5f, ZoomMath.ratioOf(0.5f, 1f, 3f), delta)
        listOf(0f, 0.1f, 0.25f, 0.5f, 0.9f, 1f).forEach { linear ->
            assertEquals(linear, ZoomMath.linearOf(ZoomMath.ratioOf(linear, 0.6f, 10f), 0.6f, 10f), delta)
        }
    }

    @Test
    fun `zoom values are clamped and a fixed lens stays at its ratio`() {
        assertEquals(8f, ZoomMath.clampRatio(20f, 1f, 8f), delta)
        assertEquals(1f, ZoomMath.clampRatio(0.5f, 1f, 8f), delta)
        assertEquals(1f, ZoomMath.linearOf(20f, 1f, 8f), delta)
        assertEquals(0f, ZoomMath.linearOf(2f, 1f, 1f), delta)
        assertEquals(1f, ZoomMath.ratioOf(0.7f, 1f, 1f), delta)
        assertEquals(8f, ZoomMath.ratioOf(4f, 1f, 8f), delta)
    }

    @Test
    fun `focus points turn with the buffer`() {
        assertEquals(0.2f to 0.7f, FocusGeometry.bufferPoint(0.2f, 0.7f, 0))
        assertEquals(0.7f to 0.8f, rounded(FocusGeometry.bufferPoint(0.2f, 0.7f, 90)))
        assertEquals(0.8f to 0.3f, rounded(FocusGeometry.bufferPoint(0.2f, 0.7f, 180)))
        assertEquals(0.3f to 0.2f, rounded(FocusGeometry.bufferPoint(0.2f, 0.7f, 270)))
        assertEquals(FocusGeometry.bufferPoint(0.2f, 0.7f, 270), FocusGeometry.bufferPoint(0.2f, 0.7f, -90))
        assertEquals(FocusGeometry.bufferPoint(0.2f, 0.7f, 90), FocusGeometry.bufferPoint(0.2f, 0.7f, 450))
    }

    private fun rounded(point: Pair<Float, Float>) = Math.round(point.first * 1000) / 1000f to Math.round(point.second * 1000) / 1000f

    @Test
    fun `EXIF orientations map to a rotation and a flip`() {
        val expected = mapOf(
            1 to (0 to false), 2 to (0 to true), 3 to (180 to false), 4 to (180 to true),
            5 to (90 to true), 6 to (90 to false), 7 to (270 to true), 8 to (270 to false),
            0 to (0 to false), 9 to (0 to false),
        )
        expected.forEach { (orientation, transform) ->
            assertEquals("orientation $orientation", ExifOrientation.Transform(transform.first, transform.second), ExifOrientation.transformOf(orientation))
        }
    }

    @Test
    fun `mirroring an orientation swaps the pairs and keeps the rotation`() {
        assertEquals(listOf(2, 1, 4, 3, 6, 5, 8, 7), (1..8).map(ExifOrientation::mirroredOf))
        assertEquals(2, ExifOrientation.mirroredOf(0))
        (1..8).forEach { orientation ->
            val plain = ExifOrientation.transformOf(orientation)
            val mirrored = ExifOrientation.transformOf(ExifOrientation.mirroredOf(orientation))
            assertEquals("orientation $orientation", !plain.mirrored, mirrored.mirrored)
            assertEquals("orientation $orientation", plain.rotationDegrees, mirrored.rotationDegrees)
        }
    }

    @Test
    fun `the device orientation picks the capture rotation`() {
        assertEquals(0, DeviceRotation.surfaceRotationOf(0, 2))
        assertEquals(0, DeviceRotation.surfaceRotationOf(44, 2))
        assertEquals(3, DeviceRotation.surfaceRotationOf(90, 0))
        assertEquals(2, DeviceRotation.surfaceRotationOf(180, 0))
        assertEquals(1, DeviceRotation.surfaceRotationOf(270, 0))
        assertEquals(0, DeviceRotation.surfaceRotationOf(359, 1))
        assertEquals("a phone lying flat keeps the last rotation", 1, DeviceRotation.surfaceRotationOf(-1, 1))
        assertEquals(listOf(0, 90, 180, 270), (0..3).map(DeviceRotation::degreesOf))
    }

}
