package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SharedMemory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraFrameKeys
import org.autojs.plugin.camera.api.CameraLimits
import org.autojs.plugin.camera.api.CameraOptionKeys
import org.autojs.plugin.camera.api.CameraResultKeys
import org.autojs.plugin.camera.api.ICameraCaptureCallback
import org.autojs.plugin.camera.api.ICameraFrameCallback
import org.autojs.plugin.camera.api.ICameraPlugin
import org.autojs.plugin.camera.api.ICameraSession
import org.autojs.plugin.camera.api.ICameraSessionCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * CameraX smoke test (roadmap P0.4, completed by P4.1): `ProcessCameraProvider` reports a camera, and a session of the
 * capability service opens, delivers 10 frames whose `seq` rises by one while the 3 buffers are used in turn, takes a
 * picture that decodes as a JPEG and closes, after which a second session opens again. The test binds the service in
 * its own process, the way `ThreePrismCameraContractTest` does, and plays the host's part of the frame protocol: it
 * reads every frame from its buffer and releases it. `CAMERA` is granted by the rule (no dialog). The CI AVD starts
 * with `-camera-back virtualscene -camera-front emulated` (roadmap D35).
 * zh-CN: CameraX 冒烟测试 (路线图 P0.4, 由 P4.1 补全): `ProcessCameraProvider` 报告镜头; 能力服务的会话能打开, 交付 10 帧
 * (`seq` 逐一递增, 3 个缓冲依次轮用), 拍出可解码的 JPEG 并关闭, 之后第二个会话能再次打开. 测试像 `ThreePrismCameraContractTest`
 * 一样在自身进程内绑定服务, 并扮演帧协议中宿主的一方: 读取每一帧的缓冲并释放. `CAMERA` 由规则授予 (不弹对话框). CI 的 AVD 以
 * `-camera-back virtualscene -camera-front emulated` 启动 (路线图 D35).
 */
@RunWith(AndroidJUnit4::class)
class CameraSmokeTest {

    @get:Rule
    val camera: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun cameraXReportsAvailableCameras() {
        val provider = ProcessCameraProvider.getInstance(context).get(20, TimeUnit.SECONDS)
        val count = provider.availableCameraInfos.size
        assertTrue("availableCameraInfos must not be empty, got $count", count > 0)
    }

    @Test
    fun sessionOpensStreamsCapturesClosesAndReopens() = withPlugin { plugin ->
        // First session: open, 10 frames, capture, close.
        // zh-CN: 第一个会话: 打开, 10 帧, 拍照, 关闭.
        val first = openReady(plugin)
        val frames = analyze(first.session, 10)
        assertEquals("seq rises by one from 1", (1L..10L).toList(), frames.map { it.first })
        assertEquals("the 3 buffers are used in turn", (0 until 10).map { it % CameraLimits.FRAME_BUFFER_COUNT }, frames.map { it.second })
        capture(first.session)
        closeAndWait(first)

        // Second session: the camera opens again after a close and still delivers frames.
        // zh-CN: 第二个会话: 关闭后镜头能再次打开, 仍能出帧.
        val second = openReady(plugin)
        assertEquals((1L..3L).toList(), analyze(second.session, 3).map { it.first })
        closeAndWait(second)
    }

    private class Opened(val session: ICameraSession, val closed: CountDownLatch)

    private fun openReady(plugin: ICameraPlugin): Opened {
        val ready = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val error = AtomicReference<Bundle>()
        val callback = object : ICameraSessionCallback.Stub() {
            override fun onStateChanged(state: Bundle) {
                if (state.getString(CameraResultKeys.STATE) == CameraContract.STATE_READY) ready.countDown()
            }

            override fun onError(bundle: Bundle) {
                error.set(bundle)
                ready.countDown()
            }

            override fun onClosed(reason: Bundle) = closed.countDown()
        }
        val answer = plugin.openSession(request(), callback)
        assertNull("openSession error: ${answer.getString(CameraContract.KEY_ERROR_MESSAGE)}", answer.getString(CameraContract.KEY_ERROR_CODE))
        val session = ICameraSession.Stub.asInterface(requireNotNull(answer.getBinder(CameraResultKeys.SESSION)))
        if (answer.getString(CameraResultKeys.STATE) != CameraContract.STATE_READY) {
            assertTrue("the session must become ready within 15 s", ready.await(CameraLimits.OPEN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        }
        assertNull("session error: ${error.get()?.getString(CameraContract.KEY_ERROR_MESSAGE)}", error.get())
        return Opened(session, closed)
    }

    /** Collects [count] frames as (`seq`, `bufferIndex`), reading and releasing each one like the host. */
    private fun analyze(session: ICameraSession, count: Int): List<Pair<Long, Int>> {
        val queue = LinkedBlockingQueue<Bundle>()
        val callback = object : ICameraFrameCallback.Stub() {
            override fun onFrame(frame: Bundle) {
                queue.add(frame)
            }

            override fun onDropped(info: Bundle) = Unit
        }
        val answer = session.startAnalysis(request(), callback)
        assertNull("startAnalysis error: ${answer.getString(CameraContract.KEY_ERROR_MESSAGE)}", answer.getString(CameraContract.KEY_ERROR_CODE))
        val width = answer.getInt(CameraResultKeys.WIDTH)
        val height = answer.getInt(CameraResultKeys.HEIGHT)
        assertTrue("analysis size ${width}x$height", width >= CameraLimits.MIN_ANALYSIS_WIDTH && height >= CameraLimits.MIN_ANALYSIS_HEIGHT)
        assertEquals(width * CameraLimits.BYTES_PER_PIXEL, answer.getInt(CameraResultKeys.ROW_STRIDE))
        assertEquals(CameraContract.FORMAT_RGBA_8888, answer.getString(CameraResultKeys.FORMAT))
        val frameBytes = width * height * CameraLimits.BYTES_PER_PIXEL
        val buffers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            val blocks = requireNotNull(answer.getParcelableArray(CameraResultKeys.BUFFERS)) { "API 27+ answers buffers" }
            assertEquals(CameraLimits.FRAME_BUFFER_COUNT, blocks.size)
            blocks.map { (it as SharedMemory).mapReadOnly() }
        } else {
            assertFalse("below API 27 there are no buffers", answer.containsKey(CameraResultKeys.BUFFERS))
            null
        }
        val seen = ArrayList<Pair<Long, Int>>()
        try {
            while (seen.size < count) {
                val frame = assertNotNull(queue.poll(10, TimeUnit.SECONDS), "frame ${seen.size + 1} within 10 s")
                val seq = frame.getLong(CameraFrameKeys.SEQ)
                val index = frame.getInt(CameraFrameKeys.BUFFER_INDEX)
                assertEquals(width, frame.getInt(CameraFrameKeys.WIDTH))
                assertEquals(height, frame.getInt(CameraFrameKeys.HEIGHT))
                if (buffers != null) {
                    assertTrue("block $index holds a frame", buffers[index].capacity() >= frameBytes)
                    buffers[index].get(0)
                } else {
                    @Suppress("DEPRECATION")
                    val fd = requireNotNull(frame.getParcelable<ParcelFileDescriptor>(CameraFrameKeys.FRAME_FD)) { "below API 27 frames carry frameFd" }
                    fd.use { assertTrue("frame file holds a frame", it.statSize >= frameBytes) }
                }
                seen += seq to index
                session.releaseFrame(index, seq)
            }
        } finally {
            session.stopAnalysis()
            buffers?.forEach { SharedMemory.unmap(it) }
        }
        return seen
    }

    private fun capture(session: ICameraSession) {
        val done = CountDownLatch(1)
        val result = AtomicReference<Pair<ByteArray, Bundle>>()
        val error = AtomicReference<Bundle>()
        val callback = object : ICameraCaptureCallback.Stub() {
            override fun onCaptured(jpeg: ParcelFileDescriptor, meta: Bundle) {
                val bytes = FileInputStream(jpeg.fileDescriptor).use { it.readBytes() }
                jpeg.close()
                result.set(bytes to meta)
                done.countDown()
            }

            override fun onError(bundle: Bundle) {
                error.set(bundle)
                done.countDown()
            }
        }
        val answer = session.capture(Bundle(), callback)
        assertNull("capture error: ${answer.getString(CameraContract.KEY_ERROR_MESSAGE)}", answer.getString(CameraContract.KEY_ERROR_CODE))
        assertTrue("the picture arrives within 15 s", done.await(CameraLimits.CAPTURE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        assertNull("capture error: ${error.get()?.getString(CameraContract.KEY_ERROR_MESSAGE)}", error.get())
        val (bytes, meta) = result.get()
        assertEquals(meta.getLong(CameraResultKeys.BYTE_COUNT, bytes.size.toLong()), bytes.size.toLong())
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        assertEquals("image/jpeg", bounds.outMimeType)
        assertTrue("decoded size ${bounds.outWidth}x${bounds.outHeight}", bounds.outWidth > 0 && bounds.outHeight > 0)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull("the JPEG decodes", bitmap)
        bitmap.recycle()
    }

    private fun closeAndWait(opened: Opened) {
        val answer = opened.session.close()
        assertNull("close error: ${answer.getString(CameraContract.KEY_ERROR_MESSAGE)}", answer.getString(CameraContract.KEY_ERROR_CODE))
        assertTrue("onClosed within 10 s", opened.closed.await(10, TimeUnit.SECONDS))
    }

    private fun request() = Bundle().apply {
        putInt(CameraContract.KEY_CONTRACT_VERSION, CameraContract.CONTRACT_VERSION)
        putInt(CameraOptionKeys.WIDTH, CameraLimits.DEFAULT_ANALYSIS_WIDTH)
        putInt(CameraOptionKeys.HEIGHT, CameraLimits.DEFAULT_ANALYSIS_HEIGHT)
    }

    private fun <T> assertNotNull(value: T?, message: String): T {
        assertTrue(message, value != null)
        return value!!
    }

    private fun withPlugin(block: (ICameraPlugin) -> Unit) {
        val binder = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        val intent = Intent(ThreePrismCameraContract.SERVICE_ACTION).addCategory(ThreePrismCameraContract.SERVICE_CATEGORY)
            .setComponent(ComponentName(context, ThreePrismCameraService::class.java))
        assertTrue("bindService", context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("the capability service connects within 10 s", connected.await(10, TimeUnit.SECONDS))
            block(ICameraPlugin.Stub.asInterface(binder.get()))
        } finally {
            context.unbindService(connection)
        }
    }

}
