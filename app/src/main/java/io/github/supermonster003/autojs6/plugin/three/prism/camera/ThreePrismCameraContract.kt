package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.os.Build
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraPluginActions
import org.autojs.plugin.camera.api.CameraPluginIds

/**
 * Identity constants of the plugin (roadmap 5.5, appendix B.1). Since roadmap P2.1 the protocol values come from the host
 * contract `libs/camera-api.aar` (`CameraPluginIds`, `CameraPluginActions`, `CameraContract`, `CameraCapabilityKeys`,
 * `CameraLimits`), so the host and the plugin cannot drift apart; only the plugin-side choices (the minimum host build of
 * this release, the native library inventory and the features this build implements) live here.
 *
 * zh-CN: 插件的身份常量 (路线图 5.5, 附录 B.1). 自路线图 P2.1 起协议取值来自宿主契约 `libs/camera-api.aar` (`CameraPluginIds`,
 * `CameraPluginActions`, `CameraContract`, `CameraCapabilityKeys`, `CameraLimits`), 宿主与插件两侧不会漂移; 这里只保留插件侧的选择
 * (本版本的最低宿主构建号, 原生库清单, 以及本构建实现的功能).
 */
internal object ThreePrismCameraContract {

    const val PLUGIN_ID = CameraPluginIds.OFFICIAL_PLUGIN_ID
    const val ENGINE = CameraPluginIds.ENGINE
    const val VARIANT = CameraPluginIds.VARIANT_DEFAULT

    /** Plugin Center identity service (roadmap 5.5). zh-CN: 插件中心身份服务 (路线图 5.5). */
    const val INFO_ACTION = CameraPluginActions.INFO
    const val INFO_CATEGORY = PLUGIN_ID

    /** Capability service discovery (roadmap D23). zh-CN: 能力服务的发现 action 与 category (路线图 D23). */
    const val SERVICE_ACTION = CameraPluginActions.CAMERA
    const val SERVICE_CATEGORY = CameraPluginActions.SERVICE_CATEGORY

    /** Descriptor of `ICameraPlugin` (roadmap D23). zh-CN: `ICameraPlugin` 的描述符 (路线图 D23). */
    const val CAPABILITY_DESCRIPTOR = "org.autojs.plugin.camera.api.ICameraPlugin"

    /**
     * Minimum host build (roadmap D39, appendix D Q9 = (a)): 6641, equal to `CameraPluginIds.REQUIRED_HOST_VERSION_CODE`,
     * mirrored by `requiredHostVersion` in `app/build.gradle.kts` and `.readme/common.json`; roadmap P5.4 replaces all
     * three with the build that ships the camera contract.
     * zh-CN: 最低宿主构建号 (路线图 D39, 附录 D Q9 = (a)): 6641, 与 `CameraPluginIds.REQUIRED_HOST_VERSION_CODE` 相同, 与
     * `app/build.gradle.kts` 中的 `requiredHostVersion` 及 `.readme/common.json` 一致; 路线图 P5.4 同时把三处替换为发布相机契约的构建.
     */
    const val REQUIRED_HOST_VERSION = 6641L

    /** Camera contract version (roadmap D16). zh-CN: 相机契约版本 (路线图 D16). */
    const val CONTRACT_VERSION = CameraContract.CONTRACT_VERSION

    /**
     * The ABIs and the whitelisted native libraries of roadmap D22 (`nativeAbis` / `allowedNativeLibraries` of
     * `app/build.gradle.kts`, kept equal by a JVM test); [NativeLibraryInventory] reports the ABIs of the installed package.
     * zh-CN: 路线图 D22 的 ABI 与原生库白名单 (即 `app/build.gradle.kts` 的 `nativeAbis` / `allowedNativeLibraries`, JVM 测试保证一致);
     * [NativeLibraryInventory] 据此报告已安装包的 ABI.
     */
    val NATIVE_ABIS: List<String> = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
    val NATIVE_LIBRARIES: List<String> = listOf("libandroidx.graphics.path.so", "libimage_processing_util_jni.so", "libsurface_util_jni.so")

    /**
     * Features this build implements end to end (roadmap D42); the host never calls a method whose feature is not
     * advertised. `sharedMemory` needs API 27 (frames travel through files on API 24 - 26, roadmap D27). `captureUi` is
     * the viewfinder of roadmap P3.1 (appendix D Q3 = (b)).
     * zh-CN: 本构建端到端实现的功能 (路线图 D42); 宿主不调用未声明功能的方法. `sharedMemory` 需要 API 27 (API 24 - 26 经文件传帧,
     * 路线图 D27). `captureUi` 即路线图 P3.1 的取景页 (附录 D Q3 = (b)).
     */
    fun features(sdkInt: Int = Build.VERSION.SDK_INT): List<String> = buildList {
        add(CameraContract.FEATURE_PREVIEW)
        add(CameraContract.FEATURE_ANALYSIS)
        add(CameraContract.FEATURE_CAPTURE)
        add(CameraContract.FEATURE_TORCH)
        add(CameraContract.FEATURE_ZOOM)
        add(CameraContract.FEATURE_EXPOSURE)
        add(CameraContract.FEATURE_FOCUS)
        add(CameraContract.FEATURE_FLASH)
        add(CameraContract.FEATURE_CAPTURE_UI)
        if (sdkInt >= Build.VERSION_CODES.O_MR1) add(CameraContract.FEATURE_SHARED_MEMORY)
    }

    /** Resource reference of the localized Plugin Center instruction. zh-CN: 插件中心本地化说明的资源引用. */
    const val INSTRUCTION_REFERENCE = "@raw/plugin_instruction"

}
