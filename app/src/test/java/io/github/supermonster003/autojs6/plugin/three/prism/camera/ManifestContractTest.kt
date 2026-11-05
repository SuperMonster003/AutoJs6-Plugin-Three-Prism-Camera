package io.github.supermonster003.autojs6.plugin.three.prism.camera

import io.github.supermonster003.autojs6.plugin.three.prism.camera.RepositoryFiles.android
import io.github.supermonster003.autojs6.plugin.three.prism.camera.RepositoryFiles.androidOrNull
import io.github.supermonster003.autojs6.plugin.three.prism.camera.RepositoryFiles.child
import io.github.supermonster003.autojs6.plugin.three.prism.camera.RepositoryFiles.children
import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Keeps `AndroidManifest.xml` and [ThreePrismCameraContract] from drifting apart: the host discovers the plugin through
 * the Manifest (Wake activity, INFO and CAMERA services), while the services and the tests use the Kotlin constants
 * (roadmap 5.5, D23, D25, appendix B.2).
 * zh-CN: 防止 `AndroidManifest.xml` 与 [ThreePrismCameraContract] 漂移: 宿主经 Manifest 发现插件 (Wake Activity, INFO 与
 * CAMERA 服务), 服务与测试则使用 Kotlin 常量 (路线图 5.5, D23, D25, 附录 B.2).
 */
class ManifestContractTest {

    private val manifest: Element by lazy { RepositoryFiles.xml("app/src/main/AndroidManifest.xml") }

    private val application: Element get() = manifest.child("application")

    @Test
    fun `the permissions are exactly the plugin permission, the camera and the camera foreground service`() {
        assertEquals(
            listOf(
                PLUGIN_PERMISSION,
                "android.permission.CAMERA",
                "android.permission.FOREGROUND_SERVICE",
                CAMERA_SERVICE_PERMISSION,
            ),
            manifest.children("uses-permission").map { it.android("name") },
        )
        val permissions = manifest.children("uses-permission").map { it.android("name") }
        assertTrue("no storage permission (roadmap D25)", permissions.none { it.contains("STORAGE") || it.contains("MEDIA") })
        assertTrue("no network permission (roadmap D25)", permissions.none { it.contains("INTERNET") || it.contains("NETWORK") })
        assertTrue("no microphone permission: 1.0.0 records no audio", permissions.none { it.contains("RECORD_AUDIO") })
        assertTrue("no uses-sdk override: SDK levels come from version.properties", manifest.children("uses-sdk").isEmpty())
    }

    @Test
    fun `the camera is optional hardware`() {
        val features = manifest.children("uses-feature")
        assertEquals(listOf("android.hardware.camera"), features.map { it.android("name") })
        assertEquals("false", features.single().android("required"))
    }

    @Test
    fun `package queries name the host only`() {
        val queries = manifest.child("queries")
        assertEquals(listOf("org.autojs.autojs6"), queries.children("package").map { it.android("name") })
        assertTrue(queries.children("intent").isEmpty())
    }

    @Test
    fun `the application disables backups and uses the generated title and system icon`() {
        assertEquals("false", application.android("allowBackup"))
        assertEquals("false", application.android("fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.android("dataExtractionRules"))
        assertEquals("@string/app_name", application.android("label"))
        assertEquals("true", application.android("supportsRtl"))
        assertEquals("@mipmap/ic_icon_studio_application", application.android("icon"))
        assertEquals("@mipmap/ic_icon_studio_application", application.android("roundIcon"))
        assertEquals(".ThreePrismCameraApplication", application.android("name"))
        // Roadmap D22 (amended in P0.2): the bundled native libraries stay stored and page-aligned, so the default applies.
        assertNull("no extraction override", application.androidOrNull("extractNativeLibs"))
    }

    @Test
    fun `application meta-data names the wake activity and the author only`() {
        val metaData = application.children("meta-data").associate { it.android("name") to it.android("value") }
        assertEquals(
            mapOf(
                "org.autojs.plugin.WAKE_ACTIVITY" to ".WakeActivity",
                "org.autojs.plugin.info.AUTHOR" to "@string/plugin_author",
            ),
            metaData,
        )
        assertTrue(PluginCapabilityKeys.NATIVE_PAGE_ALIGNMENT !in metaData.keys)
    }

    @Test
    fun `the wake activity follows the activation contract`() {
        val wake = activity(".WakeActivity")
        assertEquals("true", wake.android("exported"))
        assertEquals("true", wake.android("excludeFromRecents"))
        assertEquals("true", wake.android("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.android("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.android("theme"))
        assertFilter(wake, action = "org.autojs.plugin.action.WAKE", category = DEFAULT_CATEGORY)
    }

    @Test
    fun `the activities are the wake, capture and transparent start screens`() {
        assertEquals(
            listOf(".WakeActivity", CAPTURE_ACTIVITY, ".ThreePrismCameraStartActivity"),
            application.children("activity").map { it.android("name") },
        )
        assertEquals("false", activity(CAPTURE_ACTIVITY).android("exported"))
        val start = activity(".ThreePrismCameraStartActivity")
        assertEquals("false", start.android("exported"))
        assertEquals("true", start.android("excludeFromRecents"))
        assertEquals("@style/Theme.ThreePrismCamera.Transparent", start.android("theme"))
    }

    @Test
    fun `four launcher aliases point at the capture screen and only the automatic one is enabled`() {
        val aliases = application.children("activity-alias")
        assertEquals(
            listOf(
                ".launcher.AdaptiveLightIconAlias" to "@mipmap/ic_launcher_system_light",
                ".launcher.AdaptiveDarkIconAlias" to "@mipmap/ic_launcher_system",
                ".launcher.AdaptiveAutoIconAlias" to "@mipmap/ic_launcher_system_auto",
                ".launcher.TransparentIconAlias" to "@mipmap/ic_launcher",
            ),
            aliases.map { it.android("name") to it.android("icon") },
        )
        aliases.forEach { alias ->
            assertEquals(CAPTURE_ACTIVITY, alias.android("targetActivity"))
            assertEquals(alias.android("icon"), alias.android("roundIcon"))
            assertEquals("true", alias.android("exported"))
            assertEquals(listOf("android.intent.action.MAIN"), alias.child("intent-filter").children("action").map { it.android("name") })
            assertEquals(listOf(LAUNCHER_CATEGORY), alias.child("intent-filter").children("category").map { it.android("name") })
        }
        assertEquals(listOf(".launcher.AdaptiveAutoIconAlias"), aliases.filter { it.android("enabled") == "true" }.map { it.android("name") })
    }

    @Test
    fun `the INFO and CAMERA services match the discovery constants and the foreground service stays internal`() {
        assertEquals(
            listOf(".ThreePrismCameraForegroundService", ".ThreePrismCameraInfoService", ".ThreePrismCameraService"),
            application.children("service").map { it.android("name") },
        )
        val info = service(".ThreePrismCameraInfoService")
        assertFilter(info, action = PluginActions.INFO, category = ThreePrismCameraContract.INFO_CATEGORY)
        assertEquals(ThreePrismCameraContract.INFO_ACTION, PluginActions.INFO)
        assertRequiresHostVersion(info)

        val capability = service(".ThreePrismCameraService")
        assertFilter(capability, action = ThreePrismCameraContract.SERVICE_ACTION, category = ThreePrismCameraContract.SERVICE_CATEGORY)
        assertRequiresHostVersion(capability)

        listOf(info, capability).forEach { service ->
            assertEquals("true", service.android("exported"))
            assertEquals(PLUGIN_PERMISSION, service.android("permission"))
            assertNull("services run in the plugin's default process", service.androidOrNull("process"))
            assertNull("discovery services are not foreground services", service.androidOrNull("foregroundServiceType"))
        }

        val foreground = service(".ThreePrismCameraForegroundService")
        assertEquals("false", foreground.android("exported"))
        // Roadmap D25: the camera type and FOREGROUND_SERVICE_CAMERA change together.
        assertEquals("camera", foreground.android("foregroundServiceType"))
        assertTrue(foreground.children("intent-filter").isEmpty())
    }

    @Test
    fun `every exported component requires the plugin permission`() {
        val components = listOf("activity", "service", "receiver", "provider").flatMap { application.children(it) }
        val exported = components.filter { it.androidOrNull("exported") == "true" }
        assertEquals(listOf(".WakeActivity", ".ThreePrismCameraInfoService", ".ThreePrismCameraService"), exported.map { it.android("name") })
        exported.forEach { assertEquals(PLUGIN_PERMISSION, it.androidOrNull("permission")) }
        assertTrue("receivers and providers are not declared yet", application.children("receiver").isEmpty() && application.children("provider").isEmpty())
        // Launcher aliases are the only components exported without a permission: the launcher must start them.
        application.children("activity-alias").forEach { assertNull(it.androidOrNull("permission")) }
    }

    private fun activity(name: String): Element = application.children("activity").single { it.android("name") == name }

    private fun service(name: String): Element = application.children("service").single { it.android("name") == name }

    private fun assertFilter(component: Element, action: String, category: String) {
        val filter = component.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.android("name") })
        assertEquals(listOf(category), filter.children("category").map { it.android("name") })
        assertTrue(filter.children("data").isEmpty())
    }

    private fun assertRequiresHostVersion(service: Element) {
        val metaData = service.children("meta-data").associate { it.android("name") to it.android("value") }
        assertEquals(mapOf(PluginCapabilityKeys.REQUIRES_HOST_VERSION to "@string/plugin_requires_host_version"), metaData)
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val CAMERA_SERVICE_PERMISSION = "android.permission.FOREGROUND_SERVICE_CAMERA"
        const val CAPTURE_ACTIVITY = ".ui.ThreePrismCameraCaptureActivity"
        const val DEFAULT_CATEGORY = "android.intent.category.DEFAULT"
        const val LAUNCHER_CATEGORY = "android.intent.category.LAUNCHER"
    }

}
