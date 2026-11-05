package io.github.supermonster003.autojs6.plugin.three.prism.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure PluginInfo field mapping and the identity published by the build, the Manifest and the README sources
 * (roadmap 5.5, D22, D42). The Android-backed `toPluginInfo()` mapping is exercised by the device contract test.
 * zh-CN: 纯数据的 PluginInfo 字段映射, 以及构建, Manifest 与 README 源发布的身份 (路线图 5.5, D22, D42). 依赖 Android 的
 * `toPluginInfo()` 映射由设备契约测试覆盖.
 */
class PluginRuntimeInfoTest {

    @Test
    fun `runtime fields keep the identity, the package version, no ABI and the camera capabilities`() {
        val fields = pluginRuntimeFields(
            name = "3-Prism Camera",
            description = "Camera",
            author = "SuperMonster003",
            id = "three-prism-camera",
            engine = "camera",
            variant = "default",
            versionName = "1.0.0",
            versionCode = 2L,
            versionDate = "Nov 5, 2026",
        )
        assertEquals("3-Prism Camera", fields.name)
        assertEquals("Camera", fields.description)
        assertEquals("@raw/plugin_instruction", fields.instruction)
        assertEquals(ThreePrismCameraContract.INSTRUCTION_REFERENCE, fields.instruction)
        assertEquals("SuperMonster003", fields.author)
        assertEquals("three-prism-camera", fields.id)
        assertEquals("camera", fields.engine)
        assertEquals("default", fields.variant)
        assertEquals("1.0.0", fields.versionName)
        assertEquals(2L, fields.versionCode)
        assertEquals("Nov 5, 2026", fields.versionDate)
        // Roadmap D22: one universal APK, so it reports no ABI list even though CameraX and Compose bundle native helpers.
        assertEquals(emptyList<String>(), fields.supportedAbis)
        // Roadmap D39: the temporary floor is the host build of 2026-11-05; P5.4 sets the build that ships the contract.
        assertEquals(6640L, fields.requiredHostVersion)
        assertEquals(ThreePrismCameraContract.REQUIRED_HOST_VERSION, fields.requiredHostVersion)
        assertEquals(1, fields.contractVersion)
        assertEquals("the skeleton advertises no feature yet (roadmap D42)", emptyList<String>(), fields.features)
    }

    @Test
    fun `identity constants follow roadmap 5_5`() {
        assertEquals("three-prism-camera", ThreePrismCameraContract.PLUGIN_ID)
        assertEquals("camera", ThreePrismCameraContract.ENGINE)
        assertEquals("default", ThreePrismCameraContract.VARIANT)
        assertEquals("org.autojs.plugin.INFO", ThreePrismCameraContract.INFO_ACTION)
        assertEquals(ThreePrismCameraContract.PLUGIN_ID, ThreePrismCameraContract.INFO_CATEGORY)
        assertEquals("org.autojs.plugin.CAMERA", ThreePrismCameraContract.SERVICE_ACTION)
        assertEquals("camera", ThreePrismCameraContract.SERVICE_CATEGORY)
        assertEquals("org.autojs.plugin.camera.api.ICameraPlugin", ThreePrismCameraContract.CAPABILITY_DESCRIPTOR)
        assertEquals("cameraContractVersion", ThreePrismCameraContract.CAPABILITY_CONTRACT_VERSION)
        assertEquals("cameraFeatures", ThreePrismCameraContract.CAPABILITY_FEATURES)
        assertEquals(1, ThreePrismCameraContract.CONTRACT_VERSION)
    }

    @Test
    fun `the build publishes the same identity as the constants and builds one APK`() {
        val build = RepositoryFiles.text("app/build.gradle.kts")
        assertTrue(build.contains("val globalApplicationId = \"$PACKAGE_NAME\""))
        assertTrue(build.contains("namespace = globalApplicationId"))
        assertTrue(build.contains("applicationId = globalApplicationId"))
        assertTrue(build.contains("resValue(\"string\", \"app_name\", \"3-Prism Camera\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_name\", \"3-Prism Camera\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_author\", \"SuperMonster003\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_id\", \"${ThreePrismCameraContract.PLUGIN_ID}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_engine\", \"${ThreePrismCameraContract.ENGINE}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_variant\", \"${ThreePrismCameraContract.VARIANT}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_requires_host_version\", \"\$requiredHostVersion\")"))
        // The minimum host build is changed in the build script, the constant and the README sources together.
        assertTrue(build.contains("val requiredHostVersion = ${ThreePrismCameraContract.REQUIRED_HOST_VERSION}\n"))
        // Roadmap D22 (amended in P0.2): one APK, no ABI splits, and the release task accepts exactly the CameraX and
        // Compose native helpers for the four ABIs, stored and 16 KB aligned.
        assertFalse("no ABI splits (roadmap D22)", build.contains("splits {") || build.contains("abi {"))
        assertTrue(build.contains("register(\"appendDigestToReleasedFiles\")"))
        assertTrue(build.contains("val nativeAbis = listOf(\"arm64-v8a\", \"armeabi-v7a\", \"x86_64\", \"x86\")"))
        assertTrue(
            build.contains(
                "val allowedNativeLibraries = listOf(\"libandroidx.graphics.path.so\", \"libimage_processing_util_jni.so\", \"libsurface_util_jni.so\")",
            ),
        )
        assertTrue(build.contains("val nativePageAlignment = 16384L"))
        assertTrue(build.contains("            verifyNativeLibraries(apk)\n"))
        assertTrue(build.contains("ApkVerifier.Builder(apk).build().verify()"))

        val settings = RepositoryFiles.text("settings.gradle.kts")
        assertTrue(settings.contains("rootProject.name = \"autojs6-plugin-three-prism-camera\""))
    }

    @Test
    fun `the README sources publish the same identity as the constants`() {
        val common = RepositoryFiles.text(".readme/common.json")
        mapOf(
            "plugin_application_id" to PACKAGE_NAME,
            "plugin_id" to ThreePrismCameraContract.PLUGIN_ID,
            "plugin_engine" to ThreePrismCameraContract.ENGINE,
            "plugin_variant" to ThreePrismCameraContract.VARIANT,
            "info_action" to ThreePrismCameraContract.INFO_ACTION,
            "info_category" to ThreePrismCameraContract.INFO_CATEGORY,
            "discovery_action" to ThreePrismCameraContract.SERVICE_ACTION,
            "discovery_category" to ThreePrismCameraContract.SERVICE_CATEGORY,
            "required_host_version_code" to ThreePrismCameraContract.REQUIRED_HOST_VERSION.toString(),
            "app_name" to "3-Prism Camera",
        ).forEach { (key, value) ->
            assertTrue(".readme/common.json must publish $key = $value", common.contains("\"$key\": \"$value\""))
        }
    }

    @Test
    fun `the package of the Kotlin sources is the application id`() {
        assertEquals(PACKAGE_NAME, ThreePrismCameraContract::class.java.packageName)
    }

    private companion object {
        const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.prism.camera"
    }

}
