package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The host-facing activation and discovery contract against the installed APK (roadmap P0.4): the Wake activity,
 * discovery of exactly one service per formal action and category, explicit binding with the expected Binder descriptors,
 * `getInfo()` marshalled through a real Parcel with the transaction code the host uses, and the placeholder capability
 * Binder that rejects every call until roadmap P2.1 implements `ICameraPlugin`.
 *
 * The services run in the plugin's own process, so the calls below use `transact` with explicit Parcels instead of
 * `asInterface`, which would return the local object and skip marshalling. Roadmap P2 and P5.1 add the camera calls.
 *
 * zh-CN: 针对已安装 APK 验证面向宿主的激活与发现契约 (路线图 P0.4): Wake Activity, 每组正式 action 与 category 恰好发现一个服务,
 * 显式绑定与 Binder 描述符, 以宿主使用的事务号经真实 Parcel 编组的 `getInfo()`, 以及在路线图 P2.1 实现 `ICameraPlugin` 之前拒绝全部
 * 调用的占位能力 Binder. 服务与测试同进程, 因此下文用显式 Parcel 调用 `transact`, 不用会直接返回本地对象而跳过编组的
 * `asInterface`. 相机调用由路线图 P2 与 P5.1 补充.
 */
@RunWith(AndroidJUnit4::class)
class ThreePrismCameraContractTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val packageName: String
        get() = context.packageName

    @Test
    fun wakeActivityFollowsTheHostActivationContract() {
        val metaData = requireNotNull(context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData)
        assertEquals(".WakeActivity", metaData.getString("org.autojs.plugin.WAKE_ACTIVITY"))
        assertEquals(context.getString(R.string.plugin_author), metaData.getString("org.autojs.plugin.info.AUTHOR"))

        val component = ComponentName(packageName, "$packageName.WakeActivity")
        val activityInfo = context.packageManager.getActivityInfo(component, 0)
        assertTrue(activityInfo.exported)
        assertTrue(activityInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, activityInfo.permission)
        assertEquals(android.R.style.Theme_NoDisplay, activityInfo.theme)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH != 0)

        val matches = queryActivities(Intent("org.autojs.plugin.action.WAKE").addCategory(Intent.CATEGORY_DEFAULT).setPackage(packageName))
        assertEquals("the WAKE action resolves exactly one activity", listOf(component.className), matches)
    }

    @Test
    fun infoServiceAnswersTheCameraIdentity() {
        val serviceInfo = discoverSingleService(
            ThreePrismCameraContract.INFO_ACTION, ThreePrismCameraContract.INFO_CATEGORY, ThreePrismCameraInfoService::class.java.name,
        )
        assertEquals(ThreePrismCameraContract.REQUIRED_HOST_VERSION.toString(), serviceInfo.metaData?.get(PluginCapabilityKeys.REQUIRES_HOST_VERSION)?.toString())
        withBoundService(serviceInfo) { binder ->
            assertEquals(IPluginInfoProvider.DESCRIPTOR, binder.interfaceDescriptor)
            val info = transactPluginInfo(binder)
            assertIdentity(info)
            val capabilities = requireNotNull(info.capabilities)
            assertEquals(
                setOf(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ThreePrismCameraContract.CAPABILITY_CONTRACT_VERSION, ThreePrismCameraContract.CAPABILITY_FEATURES),
                capabilities.keySet(),
            )
            assertEquals(ThreePrismCameraContract.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
            assertEquals(1, capabilities.getInt(ThreePrismCameraContract.CAPABILITY_CONTRACT_VERSION))
            // Roadmap D42: the skeleton advertises no feature, so the host calls nothing yet.
            assertArrayEquals(emptyArray<String>(), capabilities.getStringArray(ThreePrismCameraContract.CAPABILITY_FEATURES))
        }
    }

    @Test
    fun capabilityServiceCarriesThePlaceholderDescriptor() {
        val serviceInfo = discoverSingleService(
            ThreePrismCameraContract.SERVICE_ACTION, ThreePrismCameraContract.SERVICE_CATEGORY, ThreePrismCameraService::class.java.name,
        )
        assertEquals(ThreePrismCameraContract.REQUIRED_HOST_VERSION.toString(), serviceInfo.metaData?.get(PluginCapabilityKeys.REQUIRES_HOST_VERSION)?.toString())
        withBoundService(serviceInfo) { binder ->
            assertEquals(ThreePrismCameraContract.CAPABILITY_DESCRIPTOR, binder.interfaceDescriptor)
            // The first AIDL transaction of ICameraPlugin (roadmap P1.1) is not handled by the placeholder.
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(ThreePrismCameraContract.CAPABILITY_DESCRIPTOR)
                assertFalse("the placeholder rejects every call", binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0))
            } finally {
                data.recycle()
                reply.recycle()
            }
        }
    }

    @Test
    fun repeatedBindingsAnswerTheSameIdentity() {
        val serviceInfo = discoverSingleService(
            ThreePrismCameraContract.INFO_ACTION, ThreePrismCameraContract.INFO_CATEGORY, ThreePrismCameraInfoService::class.java.name,
        )
        val ids = (1..3).map {
            var id: String? = null
            withBoundService(serviceInfo) { binder -> id = transactPluginInfo(binder).id }
            id
        }
        assertEquals(List(3) { ThreePrismCameraContract.PLUGIN_ID }, ids)
    }

    private fun assertIdentity(info: PluginInfo) {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        assertEquals("3-Prism Camera", info.name)
        assertEquals(context.getString(R.string.plugin_name), info.name)
        assertEquals(context.getString(R.string.plugin_description), info.description)
        assertEquals(ThreePrismCameraContract.INSTRUCTION_REFERENCE, info.instruction)
        assertTrue("the instruction resource exists", context.resources.getIdentifier("plugin_instruction", "raw", packageName) != 0)
        assertEquals("SuperMonster003", info.author)
        assertEquals(ThreePrismCameraContract.PLUGIN_ID, info.id)
        assertEquals(context.getString(R.string.plugin_id), info.id)
        assertEquals(ThreePrismCameraContract.ENGINE, info.engine)
        assertEquals(ThreePrismCameraContract.VARIANT, info.variant)
        assertEquals(packageInfo.versionName, info.versionName)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        assertEquals(versionCode, info.versionCode)
        assertEquals(context.getString(R.string.plugin_version_date), info.versionDate)
        // Roadmap D22: one universal APK despite the CameraX and Compose native helpers, so an explicit empty array (never null).
        assertArrayEquals(emptyArray<String>(), requireNotNull(info.supportedAbis))
    }

    /** `getInfo()` is the first transaction of IPluginInfoProvider. */
    private fun transactPluginInfo(binder: IBinder): PluginInfo {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(IPluginInfoProvider.DESCRIPTOR)
            assertTrue("getInfo() transaction must be handled", binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0))
            reply.readException()
            assertTrue("getInfo() must not return null", reply.readInt() != 0)
            return pluginInfoCreator.createFromParcel(reply)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun queryActivities(intent: Intent): List<String> {
        @Suppress("DEPRECATION")
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.name }
    }

    private fun discoverSingleService(action: String, category: String, expectedClassName: String): ServiceInfo {
        val intent = Intent(action).addCategory(category).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
        assertEquals("$action / $category must resolve exactly one service: ${matches.map { it.serviceInfo.name }}", 1, matches.size)
        val serviceInfo = matches.single().serviceInfo
        assertEquals(packageName, serviceInfo.packageName)
        assertEquals(expectedClassName, serviceInfo.name)
        assertTrue("$expectedClassName must be exported", serviceInfo.exported)
        assertTrue("$expectedClassName must be enabled", serviceInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, serviceInfo.permission)
        assertEquals("services run in the plugin's default process", packageName, serviceInfo.processName)
        return serviceInfo
    }

    private fun withBoundService(serviceInfo: ServiceInfo, block: (IBinder) -> Unit) {
        val binderReference = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binderReference.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit

            override fun onNullBinding(name: ComponentName) {
                connected.countDown()
            }
        }
        val explicit = Intent().setComponent(ComponentName(serviceInfo.packageName, serviceInfo.name))
        assertTrue("bindService returned false", context.bindService(explicit, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("timed out waiting for ${serviceInfo.name}", connected.await(10, TimeUnit.SECONDS))
            block(requireNotNull(binderReference.get()) { "${serviceInfo.name} returned a null binding" })
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"

        @Suppress("UNCHECKED_CAST")
        val pluginInfoCreator: Parcelable.Creator<PluginInfo> =
            PluginInfo::class.java.getField("CREATOR").get(null) as Parcelable.Creator<PluginInfo>
    }

}
