package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import androidx.camera.core.CameraState
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What CameraX errors do to a session (protocol "Callbacks", "Error codes") and the bounds of error answers.
 * zh-CN: CameraX 错误对会话的影响 (协议 "Callbacks", "Error codes" 两节) 以及错误应答的上限.
 */
class CameraErrorPolicyTest {

    @Test
    fun `the error constants mirror CameraState`() {
        assertEquals(CameraState.ERROR_MAX_CAMERAS_IN_USE, CameraErrorPolicy.ERROR_MAX_CAMERAS_IN_USE)
        assertEquals(CameraState.ERROR_CAMERA_IN_USE, CameraErrorPolicy.ERROR_CAMERA_IN_USE)
        assertEquals(CameraState.ERROR_OTHER_RECOVERABLE_ERROR, CameraErrorPolicy.ERROR_OTHER_RECOVERABLE_ERROR)
        assertEquals(CameraState.ERROR_STREAM_CONFIG, CameraErrorPolicy.ERROR_STREAM_CONFIG)
        assertEquals(CameraState.ERROR_CAMERA_DISABLED, CameraErrorPolicy.ERROR_CAMERA_DISABLED)
        assertEquals(CameraState.ERROR_CAMERA_FATAL_ERROR, CameraErrorPolicy.ERROR_CAMERA_FATAL_ERROR)
        assertEquals(CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED, CameraErrorPolicy.ERROR_DO_NOT_DISTURB_MODE_ENABLED)
        assertEquals(CameraState.ERROR_CAMERA_REMOVED, CameraErrorPolicy.ERROR_CAMERA_REMOVED)
    }

    @Test
    fun `recoverable errors are reported and the session stays`() {
        mapOf(
            CameraErrorPolicy.ERROR_MAX_CAMERAS_IN_USE to CameraErrorCodes.CAMERA_UNAVAILABLE,
            CameraErrorPolicy.ERROR_CAMERA_IN_USE to CameraErrorCodes.CAMERA_UNAVAILABLE,
            CameraErrorPolicy.ERROR_OTHER_RECOVERABLE_ERROR to CameraErrorCodes.CAMERA_UNAVAILABLE,
            CameraErrorPolicy.ERROR_STREAM_CONFIG to CameraErrorCodes.INTERNAL,
        ).forEach { (error, code) ->
            val action = CameraErrorPolicy.actionOf(error)
            assertTrue("error $error", action is CameraErrorPolicy.Action.Report)
            assertEquals("error $error", code, (action as CameraErrorPolicy.Action.Report).code)
        }
    }

    @Test
    fun `fatal errors close the session with the protocol reason`() {
        mapOf(
            CameraErrorPolicy.ERROR_CAMERA_REMOVED to (CameraContract.CLOSE_REASON_CAMERA_DISCONNECTED to null),
            CameraErrorPolicy.ERROR_CAMERA_FATAL_ERROR to (CameraContract.CLOSE_REASON_ERROR to CameraErrorCodes.INTERNAL),
            99 to (CameraContract.CLOSE_REASON_ERROR to CameraErrorCodes.INTERNAL),
        ).forEach { (error, expected) ->
            val action = CameraErrorPolicy.actionOf(error)
            assertTrue("error $error", action is CameraErrorPolicy.Action.Close)
            action as CameraErrorPolicy.Action.Close
            assertEquals("error $error", expected.first, action.reason)
            assertEquals("error $error", expected.second, action.code)
            assertTrue("error $error: a known close reason", action.reason in CameraContract.CLOSE_REASONS)
        }
    }

    @Test
    fun `a disabled camera reports CAMERA_DISABLED before the policy close`() {
        // Appendix D Q57 = (a): a session opened while the host is in the background or at the lock screen.
        // zh-CN: 附录 D Q57 = (a): 宿主在后台或停在锁屏时打开的会话.
        listOf(CameraErrorPolicy.ERROR_CAMERA_DISABLED, CameraErrorPolicy.ERROR_DO_NOT_DISTURB_MODE_ENABLED).forEach { error ->
            listOf(false, true).forEach { policyDisabled ->
                val action = CameraErrorPolicy.actionOf(error, policyDisabled)
                assertTrue("error $error", action is CameraErrorPolicy.Action.FailAndClose)
                action as CameraErrorPolicy.Action.FailAndClose
                assertEquals("error $error", CameraErrorCodes.CAMERA_DISABLED, action.code)
                assertEquals("error $error: the close reason keeps its meaning", CameraContract.CLOSE_REASON_POLICY, action.reason)
                assertTrue("error $error: a code a plugin may send", CameraErrorCodes.isKnown(action.code) && action.code !in CameraErrorCodes.HOST_ONLY)
                assertTrue("error $error: the message names the CameraState error", action.message.endsWith("(CameraState error $error)"))
            }
        }
        assertEquals(CameraErrorPolicy.actionOf(CameraErrorPolicy.ERROR_CAMERA_DISABLED), CameraErrorPolicy.actionOf(CameraErrorPolicy.ERROR_CAMERA_DISABLED, false))
        val background = CameraErrorPolicy.actionOf(CameraErrorPolicy.ERROR_CAMERA_DISABLED) as CameraErrorPolicy.Action.FailAndClose
        val policy = CameraErrorPolicy.actionOf(CameraErrorPolicy.ERROR_CAMERA_DISABLED, policyDisabled = true) as CameraErrorPolicy.Action.FailAndClose
        assertTrue(background.message, "background" in background.message && "lock screen" in background.message)
        assertTrue(policy.message, "device policy" in policy.message)
        // Only the message of ERROR_CAMERA_DISABLED depends on the device policy.
        // zh-CN: 只有 ERROR_CAMERA_DISABLED 的消息取决于设备策略.
        assertEquals(
            CameraErrorPolicy.actionOf(CameraErrorPolicy.ERROR_DO_NOT_DISTURB_MODE_ENABLED, false),
            CameraErrorPolicy.actionOf(CameraErrorPolicy.ERROR_DO_NOT_DISTURB_MODE_ENABLED, true),
        )
    }

    @Test
    fun `a foreground service refused after the open reports CAMERA_DISABLED before the policy close`() {
        // Appendix D Q64 = (a): the same order as a disabled camera, and the message of the refused openSession.
        // zh-CN: 附录 D Q64 = (a): 与相机被禁用的顺序相同, 消息与被拒的 openSession 相同.
        val action = CameraErrorPolicy.foregroundRefused("the system dropped the start")
        assertEquals(CameraErrorCodes.CAMERA_DISABLED, action.code)
        assertEquals(CameraContract.CLOSE_REASON_POLICY, action.reason)
        assertTrue(CameraErrorCodes.isKnown(action.code) && action.code !in CameraErrorCodes.HOST_ONLY)
        assertEquals(CameraErrorPolicy.foregroundRefusedMessage("the system dropped the start"), action.message)
        assertTrue(action.message, action.message.startsWith("the camera foreground service was refused: ") && action.message.endsWith("the system dropped the start"))
    }

    @Test
    fun `no CameraState error closes silently with reason policy`() {
        // Every policy close comes with an error that says why (appendix D Q57 = (a)).
        // zh-CN: 每次以 policy 关闭都带有说明原因的错误 (附录 D Q57 = (a)).
        (0..16).forEach { error ->
            val action = CameraErrorPolicy.actionOf(error)
            assertTrue("error $error", !(action is CameraErrorPolicy.Action.Close && action.reason == CameraContract.CLOSE_REASON_POLICY))
            if (action is CameraErrorPolicy.Action.FailAndClose) assertTrue("error $error", action.reason in CameraContract.CLOSE_REASONS)
        }
    }

    @Test
    fun `error messages are cut at a code point within 4 KiB`() {
        assertEquals("abc", ErrorText.truncateUtf8("abc", 3))
        assertEquals("ab", ErrorText.truncateUtf8("abc", 2))
        // U+00E9 takes two bytes, U+4E2D three, U+1F600 four (a surrogate pair).
        // zh-CN: U+00E9 占两字节, U+4E2D 占三字节, U+1F600 占四字节 (代理对).
        assertEquals("a", ErrorText.truncateUtf8("aé", 2))
        assertEquals("aé", ErrorText.truncateUtf8("aé", 3))
        assertEquals("", ErrorText.truncateUtf8("中", 2))
        assertEquals("x", ErrorText.truncateUtf8("x😀", 4))
        assertEquals("x😀", ErrorText.truncateUtf8("x😀", 5))
        val long = CameraFailure(CameraErrorCodes.INTERNAL, "中".repeat(2000))
        assertEquals(CameraLimits.MAX_ERROR_MESSAGE_BYTES / 3, long.answerMessage.length)
        assertTrue(long.answerMessage.toByteArray(Charsets.UTF_8).size <= CameraLimits.MAX_ERROR_MESSAGE_BYTES)
    }

    @Test
    fun `a plugin failure never carries a host-only or unknown code`() {
        listOf(CameraErrorCodes.PLUGIN_NOT_INSTALLED, CameraErrorCodes.HOST_VERSION_TOO_LOW, "NOT_A_CODE").forEach { code ->
            val refused = runCatching { CameraFailure(code, "x") }.exceptionOrNull()
            assertTrue(code, refused is IllegalArgumentException)
        }
        assertEquals(CameraErrorCodes.CAMERA_BUSY, CameraFailure(CameraErrorCodes.CAMERA_BUSY, "x", holder = "a").code)
    }

    @Test
    fun `describe names the exception and its message`() {
        assertEquals("IllegalStateException: broken", ErrorText.describe(IllegalStateException("broken")))
        assertEquals("java.lang.IllegalStateException", ErrorText.describe(IllegalStateException()))
    }

}
