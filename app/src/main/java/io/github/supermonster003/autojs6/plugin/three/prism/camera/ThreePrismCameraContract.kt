package io.github.supermonster003.autojs6.plugin.three.prism.camera

/**
 * Identity and protocol constants of the plugin (roadmap 5.5, appendix B.1).
 *
 * The capability action, category, Binder descriptor, capability keys, option values and limits are a hand-written mirror
 * of the contract draft in roadmap appendix B. They live here only until the host contract module `plugin-api/camera-api`
 * (roadmap P1.1) is staged as `libs/camera-api.aar`; roadmap P2.1 switches the services to the contract constants
 * (`CameraPluginActions`, `CameraPluginIds`, `CameraContract`, `CameraCapabilityKeys`, `CameraLimits`) so the host and the
 * plugin cannot drift apart.
 *
 * zh-CN: 插件的身份与协议常量 (路线图 5.5, 附录 B.1). 能力服务的 action, category, Binder 描述符, 能力键, 选项取值与限制值
 * 是路线图附录 B 契约草案的手写镜像, 只在宿主契约模块 `plugin-api/camera-api` (路线图 P1.1) 以 `libs/camera-api.aar` 暂存之前
 * 存在于此; 路线图 P2.1 让服务改用契约常量 (`CameraPluginActions`, `CameraPluginIds`, `CameraContract`, `CameraCapabilityKeys`,
 * `CameraLimits`), 避免宿主与插件两侧漂移.
 */
internal object ThreePrismCameraContract {

    const val PLUGIN_ID = "three-prism-camera"
    const val ENGINE = "camera"
    const val VARIANT = "default"

    /** Plugin Center identity service (roadmap 5.5). zh-CN: 插件中心身份服务 (路线图 5.5). */
    const val INFO_ACTION = "org.autojs.plugin.INFO"
    const val INFO_CATEGORY = PLUGIN_ID

    /**
     * Capability service discovery, `CameraPluginActions.CAMERA` / `SERVICE_CATEGORY` of roadmap D23.
     * zh-CN: 能力服务的发现 action 与 category, 即路线图 D23 的 `CameraPluginActions.CAMERA` / `SERVICE_CATEGORY`.
     */
    const val SERVICE_ACTION = "org.autojs.plugin.CAMERA"
    const val SERVICE_CATEGORY = "camera"

    /** Descriptor of the future `ICameraPlugin` (roadmap D23). zh-CN: 未来 `ICameraPlugin` 的描述符 (路线图 D23). */
    const val CAPABILITY_DESCRIPTOR = "org.autojs.plugin.camera.api.ICameraPlugin"

    /**
     * Minimum host build (roadmap D39): the temporary baseline of host 6.8.1 (`VERSION_BUILD` 6640, the versionCode),
     * mirrored by `plugin_requires_host_version` in `app/build.gradle.kts`; roadmap P1.3 and P5.4 replace both together.
     * zh-CN: 最低宿主构建号 (路线图 D39): 宿主 6.8.1 的临时基线 (`VERSION_BUILD` 6640, 即 versionCode), 与 `app/build.gradle.kts`
     * 中的 `plugin_requires_host_version` 一致; 路线图 P1.3 与 P5.4 同时替换两者.
     */
    const val REQUIRED_HOST_VERSION = 6640L

    /** Camera contract version, `CameraContract.CONTRACT_VERSION` (roadmap D16). zh-CN: 相机契约版本 (路线图 D16). */
    const val CONTRACT_VERSION = 1

    /**
     * Capability keys of roadmap D42 (`CameraCapabilityKeys`). The INFO service publishes the first two; the others belong
     * to `ICameraPlugin.getCapabilities` (roadmap P2.1).
     * zh-CN: 路线图 D42 的能力键 (`CameraCapabilityKeys`). INFO 服务发布前两个, 其余属于 `ICameraPlugin.getCapabilities` (路线图 P2.1).
     */
    const val CAPABILITY_CONTRACT_VERSION = "cameraContractVersion"
    const val CAPABILITY_FEATURES = "cameraFeatures"
    const val CAPABILITY_PERMISSION_GRANTED = "cameraPermissionGranted"
    const val CAPABILITY_ENGINE_VERSION = "cameraEngineVersion"
    const val CAPABILITY_CAMERA_COUNT = "cameraCount"
    const val CAPABILITY_SHARED_MEMORY = "cameraSharedMemory"

    /**
     * Features this build implements (roadmap D42). The skeleton implements none; `preview`, `analysis`, `capture`, `torch`,
     * `zoom`, `exposure`, `focus`, `flash`, `captureUi` and `sharedMemory` join as each one lands in roadmap P2 / P3, and
     * the host never calls a method whose feature is not advertised.
     * zh-CN: 本构建实现的功能 (路线图 D42). 骨架尚未实现任何功能; `preview`, `analysis`, `capture`, `torch`, `zoom`, `exposure`,
     * `focus`, `flash`, `captureUi` 与 `sharedMemory` 在路线图 P2 / P3 各自落地时加入, 宿主不调用未声明功能的方法.
     */
    val FEATURES: List<String> = emptyList()

    /** Lens facing values of roadmap appendix A. zh-CN: 路线图附录 A 的镜头朝向取值. */
    const val FACING_BACK = "back"
    const val FACING_FRONT = "front"
    const val FACING_EXTERNAL = "external"

    /** Capture flash modes of roadmap appendix A. zh-CN: 路线图附录 A 的拍照闪光模式. */
    const val FLASH_AUTO = "auto"
    const val FLASH_ON = "on"
    const val FLASH_OFF = "off"

    /** The analysis frame format (roadmap D18). zh-CN: 连续帧格式 (路线图 D18). */
    const val FORMAT_RGBA_8888 = "RGBA_8888"

    /**
     * Limits of roadmap D43 (`CameraLimits`).
     * zh-CN: 路线图 D43 的限制值 (`CameraLimits`).
     */
    object Limits {
        const val ANALYSIS_MIN_WIDTH = 160
        const val ANALYSIS_MIN_HEIGHT = 120
        const val ANALYSIS_MAX_WIDTH = 1920
        const val ANALYSIS_MAX_HEIGHT = 1080
        const val ANALYSIS_DEFAULT_WIDTH = 640
        const val ANALYSIS_DEFAULT_HEIGHT = 480
        const val MIN_FPS = 1
        const val MAX_FPS = 60
        const val FRAME_BUFFERS = 3
        const val MAX_JPEG_BYTES = 64L * 1024 * 1024
        const val MIN_QUALITY = 1
        const val MAX_QUALITY = 100
        const val DEFAULT_QUALITY = 95
        const val MAX_SESSIONS = 1
        const val FOCUS_TIMEOUT_MILLIS = 5_000L
        const val CAPTURE_TIMEOUT_MILLIS = 15_000L
    }

    /** Resource reference of the localized Plugin Center instruction. zh-CN: 插件中心本地化说明的资源引用. */
    const val INSTRUCTION_REFERENCE = "@raw/plugin_instruction"

}
