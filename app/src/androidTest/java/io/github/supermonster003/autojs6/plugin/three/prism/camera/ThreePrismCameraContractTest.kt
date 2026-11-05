package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraEngine
import org.autojs.plugin.camera.api.CameraCapabilityKeys
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraResultKeys
import org.autojs.plugin.camera.api.ICameraPlugin
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
 * `getInfo()` marshalled through a real Parcel with the transaction code the host uses, and the `ICameraPlugin` of the
 * capability service (roadmap P2.1): `getInfo`, `getCapabilities` and the error answer of `openSession`.
 *
 * The services run in the plugin's own process, so the calls below use `transact` with explicit Parcels instead of
 * `asInterface`, which would return the local object and skip marshalling. Roadmap P5.1 adds the camera calls.
 *
 * zh-CN: 针对已安装 APK 验证面向宿主的激活与发现契约 (路线图 P0.4): Wake Activity, 每组正式 action 与 category 恰好发现一个服务,
 * 显式绑定与 Binder 描述符, 以宿主使用的事务号经真实 Parcel 编组的 `getInfo()`, 以及能力服务的 `ICameraPlugin` (路线图 P2.1):
 * `getInfo`, `getCapabilities` 与 `openSession` 的错误应答. 服务与测试同进程, 因此下文用显式 Parcel 调用 `transact`, 不用会直接返回
 * 本地对象而跳过编组的 `asInterface`. 相机调用由路线图 P5.1 补充.
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
                setOf(CameraCapabilityKeys.REQUIRES_HOST_VERSION, CameraCapabilityKeys.CONTRACT_VERSION, CameraCapabilityKeys.FEATURES),
                capabilities.keySet(),
            )
            assertEquals(ThreePrismCameraContract.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
            assertEquals(1, capabilities.getInt(CameraCapabilityKeys.CONTRACT_VERSION))
            // Roadmap D42: exactly the features this build implements on this API level.
            assertArrayEquals(ThreePrismCameraContract.features().toTypedArray(), capabilities.getStringArray(CameraCapabilityKeys.FEATURES))
        }
    }

    @Test
    fun capabilityServiceAnswersTheCameraPlugin() {
        val serviceInfo = discoverSingleService(
            ThreePrismCameraContract.SERVICE_ACTION, ThreePrismCameraContract.SERVICE_CATEGORY, ThreePrismCameraService::class.java.name,
        )
        assertEquals(ThreePrismCameraContract.REQUIRED_HOST_VERSION.toString(), serviceInfo.metaData?.get(PluginCapabilityKeys.REQUIRES_HOST_VERSION)?.toString())
        withBoundService(serviceInfo) { binder ->
            assertEquals(ICameraPlugin.DESCRIPTOR, binder.interfaceDescriptor)
            assertEquals(ThreePrismCameraContract.CAPABILITY_DESCRIPTOR, binder.interfaceDescriptor)

            // ICameraPlugin 1: getInfo() answers the identity of the INFO service (roadmap P2.1).
            // zh-CN: ICameraPlugin 1: getInfo() 应答与 INFO 服务相同的身份 (路线图 P2.1).
            assertIdentity(transactCamera(binder, GET_INFO) { pluginInfoCreator.createFromParcel(it) })

            // ICameraPlugin 2: getCapabilities() adds the live state to the static capabilities (protocol "getCapabilities").
            // zh-CN: ICameraPlugin 2: getCapabilities() 在静态能力之外加上实时状态 (协议 "getCapabilities" 一节).
            val capabilities = transactCamera(binder, GET_CAPABILITIES) { Bundle.CREATOR.createFromParcel(it) }
            capabilities.classLoader = javaClass.classLoader
            assertFalse("no error answer", capabilities.containsKey(CameraContract.KEY_ERROR_CODE))
            assertEquals(1, capabilities.getInt(CameraContract.KEY_CONTRACT_VERSION))
            assertEquals(ThreePrismCameraContract.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
            assertArrayEquals(ThreePrismCameraContract.features().toTypedArray(), capabilities.getStringArray(CameraCapabilityKeys.FEATURES))
            val granted = context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            assertEquals(granted, capabilities.getBoolean(CameraCapabilityKeys.PERMISSION_GRANTED))
            assertEquals(CameraEngine.VERSION, capabilities.getString(CameraCapabilityKeys.ENGINE_VERSION))
            assertEquals(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1, capabilities.getBoolean(CameraCapabilityKeys.SHARED_MEMORY))
            assertTrue(capabilities.containsKey(CameraCapabilityKeys.CAMERA_COUNT))
            if (!granted) assertEquals(0, capabilities.getInt(CameraCapabilityKeys.CAMERA_COUNT))

            // ICameraPlugin 4: openSession without a callback is an error answer, not an exception (protocol "Common rules").
            // zh-CN: ICameraPlugin 4: 不带回调的 openSession 得到错误应答而不是异常 (协议 "Common rules" 一节).
            val refused = transactCamera(binder, OPEN_SESSION, write = { data ->
                data.writeInt(1)
                Bundle().apply { putInt(CameraContract.KEY_CONTRACT_VERSION, 1) }.writeToParcel(data, 0)
                data.writeStrongBinder(null)
            }) { Bundle.CREATOR.createFromParcel(it) }
            assertEquals(CameraErrorCodes.INVALID_ARGUMENT, refused.getString(CameraContract.KEY_ERROR_CODE))
            assertEquals(1, refused.getInt(CameraContract.KEY_CONTRACT_VERSION))
            assertFalse("errors carry no result keys", refused.containsKey(CameraResultKeys.SESSION))
        }
    }

    /** A call of ICameraPlugin through a real Parcel; [read] reads the non-null result. */
    private fun <T> transactCamera(binder: IBinder, code: Int, write: (Parcel) -> Unit = {}, read: (Parcel) -> T): T {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(ICameraPlugin.DESCRIPTOR)
            write(data)
            assertTrue("transaction $code must be handled", binder.transact(code, data, reply, 0))
            reply.readException()
            assertTrue("transaction $code must not answer null", reply.readInt() != 0)
            return read(reply)
        } finally {
            data.recycle()
            reply.recycle()
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
        // Roadmap D22, appendix D Q8 = (b): the installed split reports its single ABI, which the device supports; the
        // universal APK reports all four.
        val abis = requireNotNull(info.supportedAbis).toList()
        assertTrue(
            "supportedAbis $abis must be one device ABI or all of ${ThreePrismCameraContract.NATIVE_ABIS}",
            abis == ThreePrismCameraContract.NATIVE_ABIS || (abis.size == 1 && abis.single() in Build.SUPPORTED_ABIS),
        )
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

        // The transaction codes of ICameraPlugin (protocol "Interfaces"; CameraContractMirrorTest pins the order).
        // zh-CN: ICameraPlugin 的事务号 (协议 "Interfaces" 一节; CameraContractMirrorTest 固定其顺序).
        const val GET_INFO = IBinder.FIRST_CALL_TRANSACTION
        const val GET_CAPABILITIES = IBinder.FIRST_CALL_TRANSACTION + 1
        const val OPEN_SESSION = IBinder.FIRST_CALL_TRANSACTION + 3

        @Suppress("UNCHECKED_CAST")
        val pluginInfoCreator: Parcelable.Creator<PluginInfo> =
            PluginInfo::class.java.getField("CREATOR").get(null) as Parcelable.Creator<PluginInfo>
    }

}
