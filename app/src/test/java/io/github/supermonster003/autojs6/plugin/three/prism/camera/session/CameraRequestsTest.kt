package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraLimits
import org.autojs.plugin.camera.api.CameraOptionKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The option parsing of every call (protocol "Per method", "Common rules"): defaults, bounds with their `limit`, the
 * contract version stamp, and unknown keys being ignored.
 * zh-CN: 各调用的选项解析 (协议 "Per method", "Common rules" 两节): 默认值, 带 `limit` 的边界, 契约版本戳, 以及忽略未知键.
 */
class CameraRequestsTest {

    private fun options(vararg pairs: Pair<String, Any?>) = MapOptions(mapOf(*pairs))

    private fun failure(code: String = CameraErrorCodes.INVALID_ARGUMENT, block: () -> Unit): CameraFailure {
        try {
            block()
        } catch (failure: CameraFailure) {
            assertEquals(failure.message, code, failure.code)
            return failure
        }
        fail("expected $code")
        throw AssertionError()
    }

    @Test
    fun `a request without the stamp is version 1 and an unknown version is unsupported`() {
        assertEquals(1, CameraRequests.contractVersion(null))
        assertEquals(1, CameraRequests.contractVersion(options()))
        assertEquals(1, CameraRequests.contractVersion(options(CameraContract.KEY_CONTRACT_VERSION to 1)))
        assertEquals(1, CameraRequests.contractVersion(options(CameraContract.KEY_CONTRACT_VERSION to 1.0)))
        failure(CameraErrorCodes.UNSUPPORTED) { CameraRequests.contractVersion(options(CameraContract.KEY_CONTRACT_VERSION to CameraContract.MAX_CONTRACT_VERSION + 1)) }
        failure(CameraErrorCodes.UNSUPPORTED) { CameraRequests.contractVersion(options(CameraContract.KEY_CONTRACT_VERSION to "1")) }
    }

    @Test
    fun `open keeps the camera choice, the analysis defaults and the holder label`() {
        val open = CameraRequests.open(
            options(
                CameraOptionKeys.FACING to CameraContract.FACING_FRONT,
                CameraOptionKeys.WIDTH to 1280,
                CameraOptionKeys.HEIGHT to 720,
                CameraOptionKeys.FPS to 15,
                CameraOptionKeys.FLASH_MODE to CameraContract.FLASH_AUTO,
                CameraOptionKeys.EXPOSURE_INDEX to -2,
                CameraOptionKeys.HOLDER_LABEL to "/sdcard/Scripts/a.js",
                "aNewerOption" to true,
            ),
        )
        assertEquals(OpenRequest(CameraContract.FACING_FRONT, null, 1280, 720, 15, CameraContract.FLASH_AUTO, -2, "/sdcard/Scripts/a.js"), open)
        assertEquals(
            OpenRequest(CameraContract.FACING_BACK, null, CameraLimits.DEFAULT_ANALYSIS_WIDTH, CameraLimits.DEFAULT_ANALYSIS_HEIGHT, null, null, null, ""),
            CameraRequests.open(null),
        )
    }

    /** Appendix D Q41 = (b): the host omits the keys the script omits, the plugin fills them from its settings. */
    @Test
    fun `missing keys take the defaults of the settings page and present keys win`() {
        val defaults = SessionDefaults(CameraContract.FACING_FRONT, 1280, 720, 15)
        // A newer host: the script set nothing, so nothing is sent.
        assertEquals(OpenRequest(CameraContract.FACING_FRONT, null, 1280, 720, 15, null, null, ""), CameraRequests.open(options(), defaults))
        assertEquals(OpenRequest(CameraContract.FACING_FRONT, null, 1280, 720, 15, null, null, ""), CameraRequests.open(null, defaults))
        // A host that still sends every key (host 6641): the request wins over the settings.
        assertEquals(
            OpenRequest(CameraContract.FACING_BACK, null, 640, 480, 30, null, null, ""),
            CameraRequests.open(
                options(CameraOptionKeys.FACING to CameraContract.FACING_BACK, CameraOptionKeys.WIDTH to 640, CameraOptionKeys.HEIGHT to 480, CameraOptionKeys.FPS to 30),
                defaults,
            ),
        )
        // Each key falls back on its own; a camera id picks the lens itself, so no default facing is added.
        assertEquals(OpenRequest(null, "1", 1280, 720, 10, null, null, ""), CameraRequests.open(options(CameraOptionKeys.CAMERA_ID to "1", CameraOptionKeys.FPS to 10), defaults))
        assertEquals(OpenRequest(CameraContract.FACING_FRONT, null, 320, 240, 15, null, null, ""), CameraRequests.open(options(CameraOptionKeys.WIDTH to 320, CameraOptionKeys.HEIGHT to 240), defaults))
        // A null value counts as absent, so it takes the default too.
        assertEquals(CameraContract.FACING_FRONT, CameraRequests.open(options(CameraOptionKeys.FACING to null), defaults).facing)
        // The camera's own rate as the default stays null.
        assertNull(CameraRequests.open(options(), defaults.copy(fps = null)).fps)
        // startAnalysis without a size or rate inherits the filled open request.
        assertEquals(AnalysisRequest(1280, 720, 15, upright = true, mirror = false), CameraRequests.analysis(options(), CameraRequests.open(options(), defaults)))
        // The viewfinder of a script takes the default lens unless the script names one or a camera id.
        assertEquals(CameraContract.FACING_FRONT, CameraRequests.captureUi(options(), defaults).facing)
        assertEquals(CameraContract.FACING_BACK, CameraRequests.captureUi(options(CameraOptionKeys.FACING to CameraContract.FACING_BACK), defaults).facing)
        assertEquals(CaptureUiRequest(null, "0", CaptureRequest(CameraLimits.DEFAULT_QUALITY, null, null, null, false)), CameraRequests.captureUi(options(CameraOptionKeys.CAMERA_ID to "0"), defaults))
        assertEquals(CameraContract.FACING_BACK, CameraRequests.captureUi(null).facing)
    }

    @Test
    fun `the choices of the settings page are inside the protocol limits`() {
        assertEquals(SessionDefaults(CameraContract.FACING_BACK, CameraLimits.DEFAULT_ANALYSIS_WIDTH, CameraLimits.DEFAULT_ANALYSIS_HEIGHT, null), SessionDefaults.PROTOCOL)
        assertTrue((SessionDefaults.PROTOCOL.width to SessionDefaults.PROTOCOL.height) in SessionDefaults.SIZES)
        SessionDefaults.SIZES.forEach { (width, height) ->
            assertTrue("$width", width in CameraLimits.MIN_ANALYSIS_WIDTH..CameraLimits.MAX_ANALYSIS_WIDTH)
            assertTrue("$height", height in CameraLimits.MIN_ANALYSIS_HEIGHT..CameraLimits.MAX_ANALYSIS_HEIGHT)
            // Every choice passes the request checks unchanged.
            assertEquals(width to height, CameraRequests.open(options(CameraOptionKeys.WIDTH to width, CameraOptionKeys.HEIGHT to height)).let { it.width to it.height })
            assertEquals(width to height, SessionDefaults.parseSize(SessionDefaults.formatSize(width to height)))
        }
        assertTrue(null in SessionDefaults.FPS_CHOICES)
        SessionDefaults.FPS_CHOICES.filterNotNull().forEach { assertTrue("$it", it in CameraLimits.MIN_FPS..CameraLimits.MAX_FPS) }
        listOf(null, "", "640", "640x", "x480", "640x480x1", "640*480", "ax480").forEach { assertNull(it, SessionDefaults.parseSize(it)) }
    }

    @Test
    fun `open rejects bad cameras, sizes, rates and labels`() {
        failure { CameraRequests.open(options(CameraOptionKeys.FACING to "side")) }
        failure { CameraRequests.open(options(CameraOptionKeys.CAMERA_ID to "")) }
        failure { CameraRequests.open(options(CameraOptionKeys.WIDTH to 640)) }
        assertEquals(CameraLimits.MIN_ANALYSIS_WIDTH.toLong(), failure { CameraRequests.open(options(CameraOptionKeys.WIDTH to 100, CameraOptionKeys.HEIGHT to 480)) }.limit)
        assertEquals(CameraLimits.MAX_ANALYSIS_HEIGHT.toLong(), failure { CameraRequests.open(options(CameraOptionKeys.WIDTH to 1920, CameraOptionKeys.HEIGHT to 1440)) }.limit)
        assertEquals(CameraLimits.MAX_FPS.toLong(), failure { CameraRequests.open(options(CameraOptionKeys.FPS to 61)) }.limit)
        assertEquals(CameraLimits.MIN_FPS.toLong(), failure { CameraRequests.open(options(CameraOptionKeys.FPS to 0)) }.limit)
        failure { CameraRequests.open(options(CameraOptionKeys.FPS to 1.5)) }
        failure { CameraRequests.open(options(CameraOptionKeys.FLASH_MODE to "torch")) }
        failure { CameraRequests.open(options(CameraOptionKeys.FACING to 1)) }
        val label = "x".repeat(CameraLimits.MAX_HOLDER_LABEL_CHARS + 1)
        assertEquals(CameraLimits.MAX_HOLDER_LABEL_CHARS.toLong(), failure { CameraRequests.open(options(CameraOptionKeys.HOLDER_LABEL to label)) }.limit)
        assertEquals(CameraLimits.MAX_HOLDER_LABEL_CHARS, CameraRequests.open(options(CameraOptionKeys.HOLDER_LABEL to label.drop(1))).holderLabel.length)
    }

    @Test
    fun `analysis falls back to the open defaults and then to 640x480`() {
        val open = CameraRequests.open(options(CameraOptionKeys.WIDTH to 1280, CameraOptionKeys.HEIGHT to 720, CameraOptionKeys.FPS to 20))
        assertEquals(AnalysisRequest(1280, 720, 20, upright = true, mirror = false), CameraRequests.analysis(null, open))
        assertEquals(
            AnalysisRequest(CameraLimits.DEFAULT_ANALYSIS_WIDTH, CameraLimits.DEFAULT_ANALYSIS_HEIGHT, null, upright = true, mirror = false),
            CameraRequests.analysis(options(), null),
        )
        assertEquals(
            AnalysisRequest(320, 240, 5, upright = false, mirror = true),
            CameraRequests.analysis(
                options(CameraOptionKeys.WIDTH to 320, CameraOptionKeys.HEIGHT to 240, CameraOptionKeys.FPS to 5L, CameraOptionKeys.UPRIGHT to false, CameraOptionKeys.MIRROR to true),
                open,
            ),
        )
        failure { CameraRequests.analysis(options(CameraOptionKeys.UPRIGHT to "yes"), null) }
    }

    @Test
    fun `analysis turns frames upright only when the host asks`() {
        // Roadmap appendix D Q66 (a): an older host never sends outputUpright and turns the frames itself.
        // zh-CN: 路线图附录 D Q66 (a): 旧宿主从不发送 outputUpright, 自行旋正.
        assertFalse(CameraRequests.analysis(options(), null).outputUpright)
        assertFalse(CameraRequests.analysis(null, null).outputUpright)
        assertFalse(CameraRequests.analysis(options(CameraOptionKeys.OUTPUT_UPRIGHT to false), null).outputUpright)
        assertTrue(CameraRequests.analysis(options(CameraOptionKeys.OUTPUT_UPRIGHT to true), null).outputUpright)
        assertEquals(
            AnalysisRequest(640, 480, null, upright = true, mirror = true, outputUpright = true),
            CameraRequests.analysis(options(CameraOptionKeys.UPRIGHT to true, CameraOptionKeys.MIRROR to true, CameraOptionKeys.OUTPUT_UPRIGHT to true), null),
        )
        failure { CameraRequests.analysis(options(CameraOptionKeys.OUTPUT_UPRIGHT to "yes"), null) }
    }

    @Test
    fun `capture checks the quality, the size pair and the mirror flag`() {
        assertEquals(CaptureRequest(CameraLimits.DEFAULT_QUALITY, null, null, null, false), CameraRequests.capture(null))
        assertEquals(
            CaptureRequest(80, CameraContract.FLASH_ON, 4000, 3000, true),
            CameraRequests.capture(
                options(CameraOptionKeys.QUALITY to 80, CameraOptionKeys.FLASH_MODE to CameraContract.FLASH_ON, CameraOptionKeys.WIDTH to 4000, CameraOptionKeys.HEIGHT to 3000, CameraOptionKeys.MIRROR to true),
            ),
        )
        assertEquals(CameraLimits.MIN_QUALITY.toLong(), failure { CameraRequests.capture(options(CameraOptionKeys.QUALITY to 0)) }.limit)
        assertEquals(CameraLimits.MAX_QUALITY.toLong(), failure { CameraRequests.capture(options(CameraOptionKeys.QUALITY to 101)) }.limit)
        failure { CameraRequests.capture(options(CameraOptionKeys.WIDTH to 4000)) }
        failure { CameraRequests.capture(options(CameraOptionKeys.WIDTH to 0, CameraOptionKeys.HEIGHT to 10)) }
        failure { CameraRequests.capture(options(CameraOptionKeys.WIDTH to CameraRequests.MAX_CAPTURE_SIDE + 1, CameraOptionKeys.HEIGHT to 10)) }
    }

    @Test
    fun `the viewfinder request carries the camera and the capture options`() {
        val request = CameraRequests.captureUi(options(CameraOptionKeys.FACING to CameraContract.FACING_FRONT, CameraOptionKeys.QUALITY to 70, CameraOptionKeys.MIRROR to true))
        assertEquals(CameraContract.FACING_FRONT, request.facing)
        assertEquals(CaptureUiRequest(CameraContract.FACING_FRONT, null, CaptureRequest(70, null, null, null, true)), request)
        failure { CameraRequests.captureUi(options(CameraOptionKeys.CAMERA_ID to "")) }
    }

    @Test
    fun `zoom takes exactly one of the ratio or the linear value`() {
        assertEquals(ZoomRequest(2f, null), CameraRequests.zoom(options(CameraOptionKeys.ZOOM_RATIO to 2)))
        assertEquals(ZoomRequest(null, 0.5f), CameraRequests.zoom(options(CameraOptionKeys.ZOOM_LINEAR to 0.5)))
        failure { CameraRequests.zoom(options()) }
        failure { CameraRequests.zoom(options(CameraOptionKeys.ZOOM_RATIO to 2, CameraOptionKeys.ZOOM_LINEAR to 0.5)) }
        failure { CameraRequests.zoom(options(CameraOptionKeys.ZOOM_RATIO to 0)) }
        assertEquals(1L, failure { CameraRequests.zoom(options(CameraOptionKeys.ZOOM_LINEAR to 1.5)) }.limit)
        failure { CameraRequests.zoom(options(CameraOptionKeys.ZOOM_RATIO to Double.NaN)) }
    }

    @Test
    fun `focus needs a point inside the frame and bounded options`() {
        assertEquals(
            FocusRequest(0.25f, 0.75f, CameraLimits.DEFAULT_FOCUS_SIZE, CameraLimits.DEFAULT_FOCUS_AUTO_CANCEL_MILLIS),
            CameraRequests.focus(options(CameraOptionKeys.FOCUS_X to 0.25, CameraOptionKeys.FOCUS_Y to 0.75)),
        )
        assertEquals(0L, CameraRequests.focus(options(CameraOptionKeys.FOCUS_X to 0, CameraOptionKeys.FOCUS_Y to 1, CameraOptionKeys.AUTO_CANCEL_MILLIS to 0)).autoCancelMillis)
        failure { CameraRequests.focus(options(CameraOptionKeys.FOCUS_X to 0.5)) }
        assertEquals(0L, failure { CameraRequests.focus(options(CameraOptionKeys.FOCUS_X to -0.1, CameraOptionKeys.FOCUS_Y to 0.5)) }.limit)
        failure { CameraRequests.focus(options(CameraOptionKeys.FOCUS_X to 0.5, CameraOptionKeys.FOCUS_Y to 0.5, CameraOptionKeys.FOCUS_SIZE to 0)) }
        assertEquals(
            CameraLimits.MAX_FOCUS_AUTO_CANCEL_MILLIS,
            failure { CameraRequests.focus(options(CameraOptionKeys.FOCUS_X to 0.5, CameraOptionKeys.FOCUS_Y to 0.5, CameraOptionKeys.AUTO_CANCEL_MILLIS to 60_001)) }.limit,
        )
    }

    @Test
    fun `preview sizes are positive and the rotation is a Surface rotation`() {
        assertEquals(
            PreviewRequest(1080, 1920, 1),
            CameraRequests.preview(options(CameraOptionKeys.SURFACE_WIDTH to 1080, CameraOptionKeys.SURFACE_HEIGHT to 1920, CameraOptionKeys.SURFACE_ROTATION to 1)),
        )
        assertEquals(PreviewRequest(null, null, null), CameraRequests.preview(null))
        failure { CameraRequests.preview(options(CameraOptionKeys.SURFACE_ROTATION to 90)) }
        failure { CameraRequests.preview(options(CameraOptionKeys.SURFACE_WIDTH to 0)) }
    }

    @Test
    fun `setFlashMode takes the three modes only`() {
        CameraContract.FLASH_MODES.forEach { assertEquals(it, CameraRequests.flashMode(it)) }
        failure { CameraRequests.flashMode(null) }
        failure { CameraRequests.flashMode("torch") }
    }

    @Test
    fun `a null value counts as absent`() {
        val open = CameraRequests.open(options(CameraOptionKeys.FACING to null, CameraOptionKeys.FPS to null))
        assertEquals(CameraContract.FACING_BACK, open.facing)
        assertNull(open.fps)
        assertFalse(CameraRequests.capture(options(CameraOptionKeys.MIRROR to null)).mirror)
        assertTrue(CameraRequests.analysis(options(CameraOptionKeys.UPRIGHT to null), null).upright)
    }

}
