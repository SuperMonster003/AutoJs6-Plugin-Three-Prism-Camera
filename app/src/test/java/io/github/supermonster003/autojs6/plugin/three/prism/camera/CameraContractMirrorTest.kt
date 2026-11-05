package io.github.supermonster003.autojs6.plugin.three.prism.camera

import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraEngine
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraPluginIds
import org.autojs.plugin.camera.api.ICameraCaptureCallback
import org.autojs.plugin.camera.api.ICameraFrameCallback
import org.autojs.plugin.camera.api.ICameraPlugin
import org.autojs.plugin.camera.api.ICameraSession
import org.autojs.plugin.camera.api.ICameraSessionCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Roadmap P2.6: the plugin mirrors the host contract of `libs/camera-api.aar` (protocol "Interfaces"): the minimum host
 * build, the descriptor, the transaction order of every interface, the advertised features and the CameraX version.
 * A new host contract that reorders a method or adds a feature fails here before it reaches a device.
 * zh-CN: 路线图 P2.6: 插件镜像 `libs/camera-api.aar` 的宿主契约 (协议 "Interfaces" 一节): 最低宿主构建号, 描述符, 各接口的事务顺序, 声明的功能
 * 以及 CameraX 版本. 新的宿主契约若调整方法顺序或新增功能, 在到达设备之前就会在这里失败.
 */
class CameraContractMirrorTest {

    /**
     * The `TRANSACTION_*` codes of an AIDL stub in code order. They are compile-time constants of the class file, read
     * without initializing the stub (its superclass `android.os.Binder` is a stub of the mockable `android.jar`).
     * zh-CN: AIDL Stub 的 `TRANSACTION_*` 代码 (按代码排序). 它们是类文件中的编译期常量, 读取时不初始化 Stub (其父类 `android.os.Binder` 来自
     * 可模拟的 `android.jar` 桩).
     */
    private fun transactions(stub: Class<*>): List<String> = stub.declaredFields
        .filter { Modifier.isStatic(it.modifiers) && it.name.startsWith(PREFIX) && it.type == Int::class.javaPrimitiveType }
        .map { it.name.removePrefix(PREFIX) to ClassConstants.int(stub, it.name) }
        .sortedBy { it.second }
        .also { codes -> assertEquals("${stub.name}: codes are 1..n", (1..codes.size).toList(), codes.map { it.second }) }
        .map { it.first }

    @Test
    fun `the plugin interface keeps the protocol order`() {
        assertEquals(
            listOf("getInfo", "getCapabilities", "listCameras", "openSession", "openCaptureUi", "requestPermission"),
            transactions(ICameraPlugin.Stub::class.java),
        )
    }

    @Test
    fun `the session interface keeps the protocol order`() {
        assertEquals(
            listOf(
                "startPreview", "stopPreview", "startAnalysis", "stopAnalysis", "releaseFrame", "capture",
                "setTorch", "setFlashMode", "setZoom", "setExposure", "focusAt", "getState", "close",
            ),
            transactions(ICameraSession.Stub::class.java),
        )
    }

    @Test
    fun `the callback interfaces keep the protocol order`() {
        assertEquals(listOf("onStateChanged", "onError", "onClosed"), transactions(ICameraSessionCallback.Stub::class.java))
        assertEquals(listOf("onCaptured", "onError"), transactions(ICameraCaptureCallback.Stub::class.java))
        assertEquals(listOf("onFrame", "onDropped"), transactions(ICameraFrameCallback.Stub::class.java))
    }

    @Test
    fun `the descriptor and the minimum host build are the host's`() {
        assertEquals(ICameraPlugin.DESCRIPTOR, ThreePrismCameraContract.CAPABILITY_DESCRIPTOR)
        assertEquals(CameraPluginIds.REQUIRED_HOST_VERSION_CODE, ThreePrismCameraContract.REQUIRED_HOST_VERSION)
        assertEquals(CameraContract.CONTRACT_VERSION, ThreePrismCameraContract.CONTRACT_VERSION)
        assertTrue(CameraContract.supportsContractVersion(ThreePrismCameraContract.CONTRACT_VERSION))
    }

    @Test
    fun `features are protocol features in protocol order and shared memory needs API 27`() {
        listOf(24, 26, 27, 35).forEach { sdk ->
            val features = ThreePrismCameraContract.features(sdk)
            assertTrue("API $sdk: ${features - CameraContract.FEATURES}", CameraContract.FEATURES.containsAll(features))
            assertEquals("API $sdk: protocol order", CameraContract.FEATURES.filter { it in features }, features)
            assertEquals("API $sdk", sdk >= 27, CameraContract.FEATURE_SHARED_MEMORY in features)
            // The viewfinder is advertised with the capture screen of roadmap P3.1 (appendix D Q3 = (b)).
            // zh-CN: 取景页随路线图 P3.1 的拍照界面声明 (附录 D Q3 = (b)).
            assertFalse("API $sdk", CameraContract.FEATURE_CAPTURE_UI in features)
        }
        assertEquals(CameraContract.FEATURES - CameraContract.FEATURE_CAPTURE_UI, ThreePrismCameraContract.features(35).toSet())
    }

    @Test
    fun `the engine version is the CameraX version of the build`() {
        val catalog = RepositoryFiles.text("gradle/libs.versions.toml")
        val camerax = Regex("""(?m)^camerax\s*=\s*"([^"]+)"""").find(catalog)?.groupValues?.get(1)
        assertEquals(camerax, CameraEngine.VERSION)
    }

    private companion object {
        const val PREFIX = "TRANSACTION_"
    }

}
