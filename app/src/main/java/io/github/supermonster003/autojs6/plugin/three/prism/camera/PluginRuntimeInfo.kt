package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import org.autojs.plugin.camera.api.CameraCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo

/**
 * The PluginInfo fields as plain values, so JVM tests can freeze the mapping without Android.
 * zh-CN: 以纯值表示的 PluginInfo 字段, 使 JVM 测试无需 Android 即可冻结映射.
 */
internal data class PluginRuntimeFields(
    val name: String,
    val description: String,
    val instruction: String,
    val author: String,
    val id: String,
    val engine: String,
    val variant: String,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
    /**
     * The ABIs of the installed package (roadmap D22, appendix D Q8 = (b)): one for a single-ABI APK, all four for the
     * universal APK, as [NativeLibraryInventory] finds them.
     * zh-CN: 已安装包的 ABI (路线图 D22, 附录 D Q8 = (b)): 单 ABI 包为一个, Universal 包为全部四个, 由 [NativeLibraryInventory] 得出.
     */
    val supportedAbis: List<String>,
    val requiredHostVersion: Long,
    /** Camera contract version and features (roadmap D42). zh-CN: 相机契约版本与功能集合 (路线图 D42). */
    val contractVersion: Int,
    val features: List<String>,
)

/**
 * Assembles the fields; [id], [engine] and [variant] come from the build resources, which the JVM tests keep equal to
 * [ThreePrismCameraContract] and the README sources.
 * zh-CN: 组装字段; [id], [engine] 与 [variant] 来自构建资源, JVM 测试保证其与 [ThreePrismCameraContract] 及 README 源一致.
 */
internal fun pluginRuntimeFields(
    name: String,
    description: String,
    author: String,
    id: String,
    engine: String,
    variant: String,
    versionName: String,
    versionCode: Long,
    versionDate: String,
    supportedAbis: List<String>,
    features: List<String> = ThreePrismCameraContract.features(),
): PluginRuntimeFields = PluginRuntimeFields(
    name = name,
    description = description,
    instruction = ThreePrismCameraContract.INSTRUCTION_REFERENCE,
    author = author,
    id = id,
    engine = engine,
    variant = variant,
    versionName = versionName,
    versionCode = versionCode,
    versionDate = versionDate,
    supportedAbis = supportedAbis,
    requiredHostVersion = ThreePrismCameraContract.REQUIRED_HOST_VERSION,
    contractVersion = ThreePrismCameraContract.CONTRACT_VERSION,
    features = features,
)

/**
 * The identity answered by the INFO service and by `ICameraPlugin.getInfo` (roadmap P2.1).
 * zh-CN: INFO 服务与 `ICameraPlugin.getInfo` 返回的身份 (路线图 P2.1).
 */
internal fun PluginRuntimeFields.toPluginInfo(): PluginInfo = PluginInfo().also { info ->
    info.name = name
    info.description = description
    info.instruction = instruction
    info.author = author
    info.id = id
    info.engine = engine
    info.variant = variant
    info.versionName = versionName
    info.versionCode = versionCode
    info.versionDate = versionDate
    // The ABIs of this package, never null (new repository specification 5.5, roadmap D22, appendix D Q8 = (b)).
    // zh-CN: 本包的 ABI, 不为 null (新仓库规范 5.5, 路线图 D22, 附录 D Q8 = (b)).
    info.supportedAbis = supportedAbis.toTypedArray()
    info.capabilities = Bundle().apply {
        putLong(CameraCapabilityKeys.REQUIRES_HOST_VERSION, requiredHostVersion)
        putInt(CameraCapabilityKeys.CONTRACT_VERSION, contractVersion)
        putStringArray(CameraCapabilityKeys.FEATURES, features.toTypedArray())
    }
}

/**
 * Reads the localized description, build resources and the installed package version.
 * zh-CN: 读取本地化描述, 构建资源与已安装包的版本.
 */
internal fun Context.pluginRuntimeFields(): PluginRuntimeFields {
    val appContext = applicationContext
    val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    return pluginRuntimeFields(
        name = appContext.getString(R.string.plugin_name),
        description = appContext.getString(R.string.plugin_description),
        author = appContext.getString(R.string.plugin_author),
        id = appContext.getString(R.string.plugin_id),
        engine = appContext.getString(R.string.plugin_engine),
        variant = appContext.getString(R.string.plugin_variant),
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = packageInfo.versionCodeCompat(),
        versionDate = appContext.getString(R.string.plugin_version_date),
        supportedAbis = NativeLibraryInventory.supportedAbis(appContext),
    )
}

private fun PackageInfo.versionCodeCompat(): Long {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return longVersionCode
    @Suppress("DEPRECATION")
    return versionCode.toLong()
}
