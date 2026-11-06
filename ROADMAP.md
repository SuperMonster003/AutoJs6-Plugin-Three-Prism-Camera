# 3-Prism Camera Roadmap

更新日期: 2026-11-05

本文是 `AutoJs6-Plugin-Three-Prism-Camera` (显示名 `3-Prism Camera`; 基于 Jetpack CameraX 为 AutoJs6 脚本提供拍照, 连续帧分析与取景预览, 帧以宿主 `ImageWrapper` 交给脚本, 由脚本自行组合 OCR / 条码 / YOLO 等识别模块) 的开发路线图. 立项来源: 2026-10-15 插件选题分析 (记忆 `plugin-gap-analysis-2026-10`) 中优先级 2 的 "相机" 方向: 宿主没有任何脚本可用的相机能力, 条码等识别插件只收图片不能实时扫码, 相机是 "最便宜的空白".

以 2026-10-22 的宿主本地代码快照 (`AutoJs6 master@63d1cb259`, `VERSION_NAME=6.8.0` 尚未发布, `VERSION_BUILD=4183` + `VERSION_CODE_OFFSET=1652`, 即 versionCode 5835; 宿主 `speech-api` 契约, `AidlPluginHost` 的 `BIND_INCLUDE_CAPABILITIES` / `allowActivityStarts` 绑定路径与 OCR 的 `SharedMemory` 传图代码均已在库) 与平台版本插件 `1.9.1` 为基线. 2026-11-05 P0 起基线改为宿主 `master@3ce52d2fd7` (`VERSION_NAME=6.8.1`, `VERSION_BUILD=6640`; 6.8.0 已于 2026-11-03 公开并重写公开历史, `63d1cb259` 等旧 hash 失效; 重写后 versionCode = `VERSION_BUILD`, 不再加 1652), 见附录 F. 插件只做 CameraX 的 Android 集成, 前台服务, 帧传输与宿主契约, 不自带识别模型, 不做滤镜与相册管理.

使用方式:

1. 每次会话开始时, 从 "阶段总览" (第 6 节) 选取一个或多个未完成条目, 优先级按阶段顺序; 单次会话可完成多个小节, 除非单个小节已足够繁杂.
2. 条目完成后勾选 `[x]`, 并在条目后追加证据 (提交 hash / 测试类名 / 设备型号与 API / 度量值), 证据等级见附录 E. 未勾选条目属于规划意向, 不代表当前版本能力.
3. 条目前缀标明主要落点: `(插件)` 本仓库, `(宿主)` `D:/idea-projects/AutoJs6`, `(索引)` `D:/idea-projects/AutoJs6-Official-Plugins-Index`, `(文档)` AutoJs6-Documentation / AutoJs6-TypeScript-Declarations / AutoJs6-Plugin-Ace-Editor / AutoJs6-Plugin-Offline-Docs, `(测试)` 真机或 AVD 验证, `(发布)` 版本, tag, Release 与索引.
4. 涉及宿主公开契约 (camera 契约, 脚本 API) 的条目, 完成后必须同步宿主 `docs/dev/camera-plugin-protocol-v1.md`, 宿主 `.changelog` (10 种语言, 本轮先只写 zh-Hans, 其他语言由维护者同步) 与本仓库 `.changelog`. 宿主侧提交只落本地, 由维护者合并与推送. 宿主工作区多会话共用: 只提交自己的文件, 不推送, 不切分支, 不碰其他会话在改的文件.
5. 第 1 节 "固定决策" 由维护者 2026-10-22 经五轮选择题确认, 后续阶段不再重新讨论; 第 2 节 "派生技术决策" 可在进入对应阶段前推翻 (推翻点已逐条标注), 之后视同固定; 附录 D "待决事项" 在进入对应阶段前由维护者拍板 (调度会话用选择题提问), 拍板结果回填到附录 D 与对应决策行.
6. 本仓库骨架 (Gradle / Manifest / 资源 / CI) 在 P0.1 按 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成, 其裁剪版即本仓库 `AGENTS.md`; 命名与身份派生按 `AUTOJS6_PLUGIN_THREE_SERIES_RENAME_AGENTS.md` 第 3 节 (Prism 为 5 字母单词); 图标按 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md`; 独立界面按 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`.
7. 2026-10-22 的首个会话只落盘路线图与图标源图并 `git init` + 首次提交 (D8), 同时建看板 `D:/idea-projects/.claude/boards/3-prism.md` 与调度会话; 之后按 `AGENTS.md` 的提交规则工作 (每笔提交前 `VERSION_BUILD` = 当前提交数 + 1).

---

## 1. 固定决策

以下决策 D1-D20 已由维护者于 2026-10-22 分五轮确认 (全部为推荐项; D11 另附维护者的文档要求).

| 编号 | 决策 | 含义 |
| --- | --- | --- |
| D1 | 命名: `AutoJs6-Plugin-Three-Prism-Camera` | 显示名 `3-Prism Camera` (Prism 为棱镜, 单反取景的五棱镜与分光棱镜都指向相机与图像, 与 Maple / Amber / Folio / Macaw 同为具象名词), 插件 ID `three-prism-camera`, 点分包名 `io.github.supermonster003.autojs6.plugin.three.prism.camera`, 并入 Three 系列. 身份派生表见 5.5. |
| D2 | 1.0.0 能力范围: 拍照 + 连续帧分析 + 预览 | 单张拍照 (JPEG), 连续帧 (RGBA 位图, 供识别与自定义处理), 取景预览 (宿主 Surface 与插件取景页). 录像 (VideoCapture) 排到 1.1.0 (P6.1), 契约按 D16 预留. |
| D3 | 预览载体: 宿主 Surface 过 Binder + 插件自带取景页 | 脚本把 ui 脚本或悬浮窗中的 `SurfaceView` 交给会话, 宿主把 `Surface` 经 Binder 传给插件, CameraX `Preview` 直接渲染到该 Surface (像素不过 Binder); 插件另有自己的取景页 (D6, D15). |
| D4 | 脚本 API 形态: 新增全局模块 `camera` | `camera.list` / `open` / `capture` 与会话对象 (`preview`, `analyze`, `capture`, 控制与状态); `$camera` 指向同一对象. 草案见附录 A. 不改 `images` 模块. |
| D5 | 图标: 候选 A "机身 + 棱镜顶" | 圆角机身, 顶部梯形五棱镜凸起内镂空一个三角形 (棱镜), 双层镜头环与高光点, 右上快门缺口; 纯中性灰阶 (R=G=B), 1024 px 透明 PNG, 墨色 #272727 (亮) / #D8D8D8 (暗) 共用同一 alpha. 源图 `.python/icons/three-prism-ic-launcher-light.png` 与 `-dark.png` 由立项会话生成并入库 (候选 B 光圈, C 分光棱镜, D 取景框已放弃). |
| D6 | 独立界面: 取景测试 + 设置 + 发行历史 | 有启动器入口 (Icon Studio 亮暗图标), 按独立设置规范; 取景测试页兼作脚本的一键拍照页 (D15); 不做相册, 滤镜或编辑界面. |
| D7 | 识别联动: 脚本层组合 | 帧是 `ImageWrapper`, 脚本自己调 `ocr` / `barcode` / `yolo` 等已有模块; 本插件不内置识别, 不与其他插件直连. 宿主附样例 (P1.6) 展示三种组合. |
| D8 | 本次会话交付 | 新建 `D:/idea-projects/AutoJs6-Plugin-Three-Prism-Camera`, 写入 `ROADMAP.md` 与两张图标源图, 设置仓库级提交身份后做一笔 `docs` 提交; 新建看板 `.claude/boards/3-prism.md` 并从 dispatch 环境启动调度会话. 骨架 (Gradle / Manifest / AGENTS.md 裁剪版) 作为 P0 由子会话完成. |
| D9 | 帧传输: `SharedMemory` 环形缓冲 (API 27+) + API 24-26 回退临时文件 fd | 会话开始分析时插件一次性分配 3 块 `SharedMemory` (Parcelable, 随应答 Bundle 传给宿主), 每帧只走 oneway 回调传缓冲序号与元数据, 宿主用完回调 `releaseFrame`; 零拷贝过 Binder, 宿主侧一次拷贝成 Bitmap. API 24-26 每帧写插件 `cacheDir` 临时文件并传只读 fd (YOLO `YoloImageTransport` 的做法, 方向相反). 细节见 D26 与附录 C. |
| D10 | 后台相机: 插件自带 camera 前台服务, 复用 Macaw 的绑定路径 | 会话打开时插件启动 `foregroundServiceType="camera"` 的前台服务 (通知栏显示相机使用中), 宿主以 `BIND_INCLUDE_CAPABILITIES` + `BIND_IMPORTANT` (+ API 34 `BIND_ALLOW_ACTIVITY_STARTS`) 绑定 (与 `SpeechPluginHost` 同构); API 34+ 经透明 Activity 在可见状态启动服务 (Macaw `MicrophoneStartActivity` 同构). 宿主被杀, Binder 断开或脚本退出即释放相机并停止服务. 后台与熄屏在设备矩阵逐台实测 (P4.3). |
| D11 | 帧生命周期: 回调期间有效, 返回即回收; 要保留就 `img.clone()` | 回调返回后宿主立即 `recycle()` 并 `releaseFrame` 归还缓冲, 之后访问该帧抛 "图像已回收" 错误; 回调耗时超过帧间隔时自动丢帧 (`dropped` 计数可读). 与 `images.captureScreen` 的 oneShot 习惯一致. 维护者要求: 这一行为必须在文档 (API 文档首屏, d.ts 注释, 示例) 中明确说明 (P5.1 / P5.2 验收项). |
| D12 | Node / Bun 桥: 1.0.0 随宿主契约一起 | Rhino 脚本 API 完成后在同一阶段补 `RuntimeCameraBridge` + `NodeBridgeProtocol` / Bun 能力授权清单 + api-coverage 检查 (speech 先例), 文档与声明一次写全 (P1.5). |
| D13 | 拍照交付: 返回 `ImageWrapper`, 可选 `path` 由宿主落盘 | 插件把 JPEG (含 EXIF 方向) 经只读 fd 传给宿主, 宿主按 EXIF 校正方向后解码成 `ImageWrapper` 返回; 传 `{ path }` 时宿主把原始 JPEG 字节 (含 EXIF) 写到脚本指定路径再返回. 插件不申请存储权限, 不写系统相册. |
| D14 | 1.0.0 控制范围: 全部 8 项 | 镜头选择 (前 / 后 / 按 id), 分辨率与宽高比, 目标帧率, 手电 (torch), 拍照闪光模式 (auto / on / off), 变焦 (ratio 与线性), 曝光补偿, 对焦 / 测光点 (归一化坐标); 另提供只读状态 (zoom 范围与当前值, hasFlash, sensorRotation, exposure 范围与步长). 契约约 16 个方法, 文档一次写全. |
| D15 | 取景页入口: `camera.capture({ ui: true })` 打开插件取景页 | 用户在取景页按快门或取消, 脚本拿到与无界面 `capture` 相同的 `ImageWrapper` (取消返回 `null`); 取景页支持切换镜头, 手电, 变焦与点按对焦, 与设置页共用同一套 CameraX 代码. 需要宿主可见 (后台调用报 `UI_REQUIRES_FOREGROUND`). |
| D16 | 录像预留: 契约 v1 只定 1.0.0 方法, 带版本号与能力位; 录像在 v2 追加 | `CameraContract.CONTRACT_VERSION = 1`, `getCapabilities` 返回 `cameraFeatures[]`; 1.1.0 追加 v2 方法与 `camera.record()`, 旧宿主 + 新插件只看到 v1 能力, 新宿主 + 旧插件按能力位降级. |
| D17 | 并发: 独占, 第二个 `open` 报 `CAMERA_BUSY` 并带占用者信息 | 插件进程同一时刻只有一个会话 (脚本或取景页); 错误里带占用者 (脚本路径或 "插件取景页"); 脚本退出, 宿主被杀或 Binder 断开时自动释放. 同一会话内预览 + 连续帧 + 拍照三个 use case 共存. |
| D18 | 连续帧默认: `RGBA_8888`, 640x480, 脚本可指定分辨率 | 与 CameraX `ImageAnalysis` 默认一致, 每帧 1.2 MB, 3 块缓冲共 3.6 MB; 脚本传 `{ resolution: "1280x720" }` 时按设备支持列表就近选, 实际尺寸在 `ready` 状态与帧元数据中返回. 上限见 D43. |
| D19 | 帧方向: 默认旋正 (与屏幕方向一致), `{ upright: false }` 关闭 | 宿主按 `rotationDegrees` 旋正后再交给脚本, 元数据仍带原始 `rotationDegrees` 与 `upright` 标记. 前置镜头 (附录 D Q6 = (b), 2026-11-05 拍板, 改写原 "一律不镜像"): 预览默认镜像 (系统相机习惯; 插件 `Preview` 用 `MIRROR_MODE_ON_FRONT_ONLY`, `getState` 报告 `previewMirrored`, 键为宿主契约 `CameraResultKeys.STATE_PREVIEW_MIRRORED`, 附录 D Q11 = (b)), 帧与拍照默认不镜像 (识别不受影响, 与保存的照片一致), `{ mirror: true }` 只作用于帧与拍照; 后置与外接镜头都不镜像. |
| D20 | CI: AVD 虚拟场景覆盖 打开 / 取帧 / 拍照 / 释放 | GitHub Actions ubuntu runner (KVM) 启动模拟器, 后置相机设为 `virtualscene`, 跑 instrumentation 冒烟; 前台服务, 后台 / 熄屏与多厂商行为由本地真机矩阵覆盖 (附录 F). |

---

## 2. 派生技术决策

D21-D45 据第 1 节派生, 进入对应阶段前可推翻 (推翻点见各条), 之后视同固定. 实现中如有偏差逐条回填.

| 编号 | 决策 | 含义 | 推翻点 |
| --- | --- | --- | --- |
| D21 | CameraX `1.6.2` | `androidx.camera:camera-core` / `camera-camera2` / `camera-lifecycle` / `camera-compose` 固定 `1.6.2` (Android Developers 发布页 2026-10-21 更新, 四者均为稳定版; 同日 `1.7.0-rc01`, 不采用 rc). 维护者 2026-11-05 Q3 = (b): 取景用 `camera-compose`, 不引入 `camera-view`, 不自行退回. 1.0.0 不引入 `camera-video` (1.1.0), `camera-extensions`, `camera-mlkit-vision`. Google Maven 坐标直接引用, 不入 `libs/`, 版本常量在 `gradle/libs.versions.toml`; 实际解析另带入同为 1.6.2 的 `camera-camera2-pipe`, `viewfinder-core` / `viewfinder-compose`, `featurecombinationquery` (见 `THIRD_PARTY_NOTICES.md`). 1.6 线用 CameraPipe 实现, `camera-camera2` 仍为默认后端. P0.2 实测 `camera-compose` 体积: release 骨架 +590 B (取景器尚未使用, R8 删去), 全部保留时上限 +82510 B; debug dex 未压缩 +185600 B. | P0.2 |
| D22 | 按 ABI 拆包 (4 个单 ABI APK + Universal APK), 原生库白名单 | 立项时 "CameraX 不含原生库" 的前提经 P0.2 实测不成立: `camera-core` 1.6.2 自带 `libimage_processing_util_jni.so` 与 `libsurface_util_jni.so`, Compose `ui-graphics` 经 `androidx.graphics:graphics-path` 1.0.1 带入 `libandroidx.graphics.path.so`, 各 4 个 ABI 共 12 个文件 202140 B (debug 与 release 相同). 附录 D Q8 = (b) (2026-11-05 拍板, 推翻 P0.2 的单 APK 先行): `splits.abi` 出 `arm64-v8a` / `armeabi-v7a` / `x86_64` / `x86` 4 个单 ABI 包, 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 5.5 另出 Universal APK (规范 MUST, 与 3-Macaw Speech / Image Quantization 一致); `PluginInfo.supportedAbis` 按包填写: 运行时扫描已安装 APK (含 split) 的 `lib/<abi>/` 条目, 单 ABI 包为 `[该 ABI]`, Universal 为 4 个 ABI (BuildConfig 各包共用, 不能在编译期区分). `appendDigestToReleasedFiles` 按多 APK 形态实现, 文件名 `autojs6-plugin-three-prism-camera-v{VERSION_NAME}-{ABI}-{CRC32}.apk` (Universal 的 `{ABI}` 为 `universal`), 任务内逐包校验签名, 原生库集合与白名单完全一致 (单 ABI 包 3 个库, Universal 4 ABI x 3), STORED 不压缩, 数据偏移按 16384 对齐, ELF `PT_LOAD` 对齐 >= 16384; 16 KB 页校验适用于 `arm64-v8a` 包, 并在 16 KB 页环境实际安装运行该包; CI `build.yml` 与 `.python/release_sums.py` 对 5 个 release 产物做同样校验并生成 `SHA256SUMS`. 插件自身不写原生代码; 依赖升级带入新库时白名单校验失败, 须先登记再放行. | P0.2, Q8 |
| D23 | 契约模块 `plugin-api/camera-api` | 包 `org.autojs.plugin.camera.api`; 常量 `CameraPluginActions.CAMERA = "org.autojs.plugin.CAMERA"`, `SERVICE_CATEGORY = "camera"`, `CameraPluginIds` (官方 ID / 包名, `ENGINE = "camera"`, `CATEGORY = "camera"`, `REQUIRED_HOST_VERSION_CODE`), `CameraContract` (`CONTRACT_VERSION = 1`, 状态, 镜头朝向, 闪光模式, 帧格式, 关闭原因), `CameraCapabilityKeys`, `CameraLimits`, `CameraOptionKeys` / `CameraResultKeys` / `CameraFrameKeys` / `CameraErrorCodes`; AIDL `ICameraPlugin` (`getInfo` 居首), `ICameraSession`, oneway `ICameraFrameCallback` / `ICameraCaptureCallback` / `ICameraSessionCallback`; 请求与结果以 `Bundle` 传输, 不走 JSON 字符串; `SharedMemory` 与 `Surface` 作为 Parcelable 直接入参. 草案见附录 B. 事务顺序由 `CameraAidlOrderTest` 冻结 (speech 先例). | P1.1 |
| D24 | 宿主绑定 `CameraPluginHost` | 基于 `core/plugin/AidlPluginHost.kt`, 包 `org.autojs.autojs.core.plugin.camera`; flags 与 `SpeechPluginHost` 相同 (`BIND_AUTO_CREATE` + `BIND_INCLUDE_CAPABILITIES` + `BIND_IMPORTANT`, API 34+ `allowActivityStarts = true`); 启用, 授权, 宿主版本门禁与缺插件文案复用既有逻辑; `linkToDeath` 两个方向. | P1.2 |
| D25 | 插件前台服务与权限 | `ThreePrismCameraForegroundService` (`foregroundServiceType="camera"`), Manifest 声明 `CAMERA`, `FOREGROUND_SERVICE` (API 28+), `FOREGROUND_SERVICE_CAMERA` (API 34+), `uses-feature android.hardware.camera` `required=false`; 会话打开即 `startForeground`, 会话关闭 / 宿主死亡即停止; API 34+ 由透明 `ThreePrismCameraStartActivity` 在可见状态启动 (宿主绑定带 `BIND_ALLOW_ACTIVITY_STARTS`); 通知文案 10 语言; API 33+ 另声明并申请 `POST_NOTIFICATIONS` (附录 D Q12 = (a), 2026-11-06 已决); 另声明 `INTERNET`, 只用于设置页的应用内更新检查 (附录 D Q42 = (b), 2026-11-06 已决; 只访问 `api.github.com` 的 `releases/latest`, `res/xml/network_security_config.xml` 在系统信任库之外追加 ISRG Root X1, 不放开明文与用户证书). 插件未获 `CAMERA` 时 `open` 报 `PERMISSION_DENIED`, 脚本可调 `camera.requestPermission()` 打开插件授权页 (需宿主可见). | P2.1 |
| D26 | 环形缓冲协议 | `startAnalysis` 应答带 `buffers: SharedMemory[3]` (插件按实际分辨率分配, 每块 `width * 4 * height` 字节, 行紧排), `width`, `height`, `format`; 插件把 `ImageProxy` 的 RGBA 平面按行拷入空闲块 (CameraX 行跨度有填充时去填充), 发 oneway `onFrame(meta)` (`seq`, `bufferIndex`, `width`, `height`, `rotationDegrees`, `timestampNanos`, `facing`); 宿主 `mapReadOnly` 后 `copyPixelsFromBuffer` 到 Bitmap, 处理完 `releaseFrame(bufferIndex, seq)`; 无空闲块时插件丢帧 (`STRATEGY_KEEP_ONLY_LATEST`) 并累计 `dropped`, 每 30 帧或分析结束时发 `onDropped`. API 24-26: 插件把帧写到 `cacheDir/frames/<bufferIndex>.rgba` 并在 `onFrame` 附只读 `ParcelFileDescriptor`, 宿主读完关闭; 其余语义不变. 协议全文见附录 C. | P2.2 |
| D27 | 宿主帧处理与线程 | 宿主在 Binder 线程收 `onFrame`, 拷贝到 Bitmap (必要时按 D19 旋正 / 镜像), 包成 `ImageWrapper` 后投递到脚本主循环 (与 `events` 模块一致); 宿主侧队列深度 1, 回调未返回时新帧覆盖旧帧并计 `dropped`; 回调返回后 `recycle()` + `releaseFrame`. 单次 `onFrame` 的宿主处理不超过 5 ms @720p (P4.4 基线). | P1.4 |
| D28 | 脚本 API `camera` 草案 | 模块方法 `isAvailable`, `capabilities`, `requestPermission`, `list`, `open`, `capture` / `captureAsync` (一步到位: open + capture + close; `{ ui: true }` 走取景页); 会话方法 `preview`, `stopPreview`, `analyze`, `stopAnalysis`, `capture` / `captureAsync`, `torch`, `flashMode`, `zoom` / `zoomLinear`, `exposure`, `focusAt`, `state`, `close`, 事件 `frame` / `dropped` / `error` / `close`. 全文见附录 A. `$camera` 同对象. | P1.4 |
| D29 | 错误模型 | 宿主抛 `CameraError` (`code`, `message`, `details`): `PLUGIN_NOT_INSTALLED`, `PLUGIN_DISABLED`, `HOST_VERSION_TOO_LOW` (既有宿主门禁), `PERMISSION_DENIED`, `CAMERA_BUSY` (`details.holder`), `CAMERA_UNAVAILABLE` (无对应镜头或被其他应用占用), `CAMERA_DISABLED` (后台无能力或系统策略), `UI_REQUIRES_FOREGROUND`, `UNSUPPORTED`, `INVALID_ARGUMENT`, `SESSION_CLOSED`, `TIMEOUT`, `LIMIT_EXCEEDED`, `INTERNAL`. 取景页取消不抛错, `capture` 返回 `null`. 10 语言文案在宿主 `strings_runtime_camera.xml`. | P1.1 |
| D30 | 会话释放与死亡 | 插件 `linkToDeath` 宿主回调 Binder: 死亡即关会话, 关相机, 停前台服务 (Macaw 实测 0.25 - 0.45 秒); 宿主脚本退出时 `ScriptRuntime` 关闭该脚本的全部会话 (与 `closeSpeechClient` 同构); 系统收回相机 (`onError` / 断开) 时会话发 `close` 事件并带原因. | P2.1 |
| D31 | 取景页结果回传 | `ICameraPlugin.openCaptureUi(request, callback)`: 插件自己启动 `ThreePrismCameraCaptureActivity` (`FLAG_ACTIVITY_NEW_TASK`, 需 `allowActivityStarts` 且宿主可见), 结果 JPEG 经与 D13 相同的只读 fd 回调, 取消回调 `onError(CANCELLED)` 并由宿主转为 `null`; 不用跨包 `startActivityForResult`. 取景页占用相机时脚本 `open` 报 `CAMERA_BUSY` (holder = 取景页). | P3.1 |
| D32 | 预览 Surface | `session.preview(view)` 接受 ui 脚本的 `SurfaceView` (宿主已有 `core/ui/inflater/inflaters/SurfaceViewInflater.kt`, XML 标签 P1.4 核对) 或悬浮窗中的 `SurfaceView`; 宿主在 `surfaceCreated` 后调 `startPreview(Surface, {width, height, rotation})`, 尺寸变化时重调, `surfaceDestroyed` 时 `stopPreview`; 插件用 `Preview.SurfaceProvider` 把 Surface 交给 CameraX, 预览分辨率由 CameraX 按 Surface 尺寸选择; 旋转由 `setTargetRotation(display rotation)` 处理. | P2.5 |
| D33 | 分辨率与帧率协商 | `resolution` 为目标 (`ResolutionSelector` + `ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER`), `fps` 为目标范围 (`setTargetFrameRate(Range(fps, fps))`, 不支持时取最近); 实际值在 `ready` 状态返回并写入会话 `state()`. 拍照分辨率默认设备最大 (`CAPTURE_MODE_MINIMIZE_LATENCY`), `{ resolution }` 可降. | P2.2 |
| D34 | Node / Bun 桥 | `engine/RuntimeCameraBridge.kt` (与 `RuntimeSpeechBridge` 同构), `NodeBridgeProtocol` 新增 `CAMERA` 域与 Bun 能力授权条目; `list` / `open` / `capture` (返回 JPEG 路径或 base64) / 控制 / 状态 / `close` 全部覆盖; 连续帧以 RGBA 字节经桥现有传输交给运行时 (P1.5 实测吞吐: 480p 30 fps 为 36 MB/s, 不达标时文档标注 Node / Bun 连续帧上限并按 `fps` 限流); `preview` 在 Node / Bun 不可用 (无 ui), 报 `UNSUPPORTED`. | P1.5 |
| D35 | CI 模拟器相机 | `build.yml` 增加 `connected-avd` job: `reactivecircus/android-emulator-runner`, API 33 `google_apis` x86_64, `-camera-back virtualscene -camera-front emulated`, 跑 `CameraSmokeTest` (打开 / 连续 10 帧且 `seq` 递增 / 拍照 JPEG 可解码 / 释放后 `CAMERA_BUSY` 消失); 本地等价脚本 `.python/ci_connected_tests.py` (Macaw 同名脚本可参照). 单次约 8 - 12 分钟, 只在 `main` 推送与 PR 运行. | P4.1 |
| D36 | 设备矩阵 | 附录 F: API 24 AVD (回退路径), API 28 Sony G8441, API 31 Sony XQ-AT72, API 33 Sony XQ-DQ72 与 Redmi 22120RN86C, API 35 Xiaomi Pad 23046RP50C 与 OnePlus OPD2413 (ColorOS); 可租用 API 36 / 37 Samsung 按需. 每台记录: 安装, 授权, `list`, `open` + 10 帧, `capture` 到文件, torch, 后台 30 秒取帧, 熄屏 30 秒取帧, 取景页拍照. | P4.2 |
| D37 | 文档与声明 | Documentation `api/camera.md` (10 语言目录按仓库惯例), 首屏 "帧的生命周期" 提示框 (D11); TypeScript 声明 `declarations/autojs6/aj6-int-camera.d.ts` 与 `declarations/autojs6-node/camera.d.ts` (JSDoc 写明回调期间有效); Ace Editor 与 Offline Docs 同步副本; 宿主 `docs/dev/camera-plugin-protocol-v1.md`. | P5.1 |
| D38 | 安装向导分类 | `PluginInstallWizardCatalog` 新增 `entry(official("three.prism.camera"), "Three Prism Camera", <分类>)`, 分类待拍板 (附录 D Q1, 推荐 `IMAGE`, 与 OCR / 条码 / YOLO 同列). | P1.3 |
| D39 | `requiresHostVersion` | P0.1 临时值 6640 (宿主 6.8.1 `3ce52d2fd7`; 原基线 5835 / `63d1cb259` 随 6.8.0 公开时的历史重写失效); P1.3 写契约落地构建的 versionCode 为临时值 (`CameraPluginIds.REQUIRED_HOST_VERSION_CODE` 与插件 `requiredHostVersion` 一致), P5.4 回填为实际发布所依赖的宿主构建. P1.3 定为 6641 (附录 D Q9, 已按推荐先行): 宿主提交未改 `VERSION_BUILD` (仍 6640, 即已公开的 6.8.0 构建), 真机联调用 `VERSION_BUILD` 临时 6641 的隔离克隆宿主 (Chime Q12 (a) 先例); 插件三处 (`requiredHostVersion`, `ThreePrismCameraContract.REQUIRED_HOST_VERSION`, `.readme/common.json`) 在 P2.1 改为 6641. | P1.3 |
| D40 | 许可证 | 源码 MPL-2.0 (与 Three 系列一致); CameraX Apache-2.0, `THIRD_PARTY_NOTICES.md` 列出 CameraX 与 Compose; APK 无 GPL 组件. | P0.1 |
| D41 | 插件界面技术 | Compose + Material 3 (BOM 与 Macaw 一致, P0.1 取 `2026.09.00`, 即 Compose 1.12.1 / Material 3 1.4.0), 取景页用 `camera-compose` 的 `CameraXViewfinder` (维护者 2026-11-05 Q3 = (b), 不用 `camera-view` 的 `PreviewView`); 设置页按独立设置规范 (权限状态, 默认镜头, 默认分辨率, 前台服务通知说明, 诊断信息, 发行历史入口). | P3.1 |
| D42 | `PluginInfo` 与能力键 | `getInfo` 返回包版本, 10 语言描述, ID, engine, variant, `supportedAbis = emptyArray()`, `requiresHostVersion`, 契约版本; `getCapabilities` 返回 `cameraContractVersion`, `cameraFeatures[]` (`preview`, `analysis`, `capture`, `torch`, `zoom`, `exposure`, `focus`, `flash`, `captureUi`, `sharedMemory`), `cameraPermissionGranted`, `cameraEngineVersion` (CameraX 版本), `cameraCount`. | P1.1 |
| D43 | 限制值 `CameraLimits` | 分析分辨率 160x120 - 1920x1080 (1080p 每帧 8.3 MB, 3 块 25 MB), 目标帧率 1 - 60, 缓冲块数 3, 拍照 JPEG 上限 64 MB (与 Barcode 一致), `quality` 1 - 100 (默认 95), 会话数 1, 对焦超时 5 秒, `capture` 超时 15 秒, 取景页等待无上限 (用户操作). | P1.1 |
| D44 | 图标生成 | 两张源图已入库 (D5); P0.3 用 Icon Studio 双源 `grayscale` 配方 (与 Macaw `recipe.json` 相同形态) 生成, `generate_icon_studio.py --check` 通过; Icon Studio 的 derived 色调模式 (内核 1.4.0, 本地提交) 可选但不依赖. | P0.3 |
| D45 | 拍照 JPEG 传输 | 插件 `ImageCapture.takePicture(OnImageCapturedCallback)` 得到 JPEG 字节, 写入 `cacheDir/captures/<id>.jpg`, 打开只读 fd 后立即删除文件 (YOLO 做法, fd 仍有效), 经 `onCaptured(fd, meta)` 回传; 不用管道 (写端 3 - 6 MB 会阻塞 Binder 线程). 写盘后插件用 `ExifInterface` 确定方向: 相机漏写 90 / 270 度旋转时先补写 (附录 D Q14 = (b)), `mirror: true` 再按旋正后水平镜像改写 (1 <-> 2, 3 <-> 4, 5 <-> 6, 7 <-> 8), 不重新编码 (附录 D Q13 = (b), 2026-11-06 拍板; P2.3 初版曾改用 `takePicture(OutputFileOptions)`, 已改回). 宿主读完关闭 fd, 按 `{ path }` 写文件或 `BitmapFactory` + EXIF 旋正解码. | P2.3 |

---

## 3. 范围与非目标

范围 (1.0.0):

- 脚本 API `camera` 全集 (附录 A) 与 10 种语言的文档, d.ts, 离线文档, Ace 声明同步; Node / Bun 桥 (D12).
- 拍照 (JPEG, EXIF, 可落盘), 连续帧分析 (RGBA, 环形缓冲, 旋正), 预览 (宿主 Surface), 8 项控制与只读状态 (D14).
- 后台与熄屏可用 (插件前台服务 + 宿主绑定能力), 独占会话与自动释放.
- 插件取景页 (取景测试与脚本一键拍照), 设置页, 发行历史, 图标.
- 宿主契约 `camera-api`, `CameraPluginHost`, 插件中心注册, 安装向导条目, 协议文档, 官方索引登记.
- 宿主样例脚本: 拍一张存文件, 连续帧 + `ocr`, 连续帧 + `barcode` (实时扫码), 连续帧 + `yolo`, 取景页拍照.
- CI: 构建, 单元测试, Icon Studio 校验, AVD 虚拟场景冒烟.

非目标 (不做或排到 1.1.0 之后, 评估记录见附录 G):

- 录像 (VideoCapture, Media3 Muxer) 与音频: 1.1.0 (P6.1, 契约 v2).
- RAW / DNG, Ultra HDR, 厂商扩展 (HDR / 夜景 / 人像, `camera-extensions`), 多摄同开, 外接 USB 相机 (UVC).
- 内置识别 (条码扫描 UI, 人脸, 文字), 滤镜, 美颜, 水印, 相册管理, 写入 MediaStore.
- 宿主自行用 camera2 实现相机 (插件化是立项前提); 系统相机 Intent 作为脚本入口 (宿主 `RuntimeWebFileChooser` 的 WebView 路径保持原样).
- 多脚本共享会话 (D17 已选独占).

---

## 4. 现状诊断

### 4.1 可直接复用的宿主能力

- 图像对象: `core/image/ImageWrapper.kt` (Bitmap + 懒 Mat, `recycle()` / `isRecycled()` / `clone()` / `setOneShot`, 每个运行时的弱引用登记与 `recycleAll(scriptRuntime)`, 回收后访问抛 `R.string.error_image_has_been_recycled`), `images` 模块的保存, 旋转与裁剪.
- 插件发现与绑定: `core/plugin/AidlPluginHost.kt` (`BIND_INCLUDE_CAPABILITIES` 可选 flag 与 `allowActivityStarts` 已实现, 启用 / 授权 / 宿主版本门禁 / 缺插件文案); 同构模板 `core/plugin/speech/SpeechPluginHost.kt` (D22 绑定路径, API 28 / 31 / 33 / 35 实测).
- 契约模板: `plugin-api/speech-api` (常量, 能力键, 限制值, 错误码, AIDL 顺序冻结测试 `SpeechAidlOrderTest`, `consumer-rules.pro`, `aarMetadata minCompileSdk 36`), 注册于 `settings.gradle.kts:141`; `plugin-api/common-plugin-api` (`PluginInfo`, `PluginCapabilityKeys.REQUIRES_HOST_VERSION`).
- 跨进程传图先例: OCR `core/plugin/ocr/OcrPluginHost.kt:643-700` (`SharedMemory.create` + fd 复制, `rawImage` 路径), YOLO `YoloImageTransport.kt` (RGBA 临时文件 + 只读 fd + 立即删除), Barcode (压缩图经 fd, 16M 像素 / 64 MB 上限).
- 脚本挂载与运行时桥: `runtime/ScriptRuntime.kt` 的 speech 注册点 (约 332 / 559 / 910 / 1170 行) 与 `runtime/api/modules/ScriptModuleLayer.kt:41`; `engine/RuntimeSpeechBridge.kt`, `engine/NodeBridgeProtocol.kt:703` (`SPEECH` 域), `engine/BunAutomationBridge.kt`; 10 语言 `strings_runtime_speech.xml`.
- 注册表与向导: `core/plugin/center/InstalledPluginRepository.kt`, `core/plugin/center/PluginCenterViewModel.kt`, `core/plugin/center/wizard/PluginInstallWizardCatalog.kt` (分类 `ACCESSIBILITY` / `EDITOR` / `UI` / `FILE_MANAGER` / `RUNTIME` / `IMAGE` / `COMPILER` / `TEXT_CONVERSION` / `DOCS` / `PLAYER` / `TOOLS`).
- ui 控件: `core/ui/inflater/inflaters/SurfaceViewInflater.kt` (预览载体), 悬浮窗 `floaty`.
- 宿主 Manifest 已声明 `CAMERA` (328 行) 与 `FLASHLIGHT` (329 行), `uses-feature android.hardware.camera required=false`; 宿主自身不替插件拍照 (D10 插件自持权限).
- 界面与仓库参照: 3-Macaw Speech (前台服务, 透明启动 Activity, 10 语言, CI, `.python` 脚本族), 3-Block Compose (单 APK 无拆分 D22, 预览页跨进程结果), 3-Maple Image (Compose, 独立设置, Icon Studio 配方), MediaInfo (契约模板).

### 4.2 缺口 (需要新建或修改)

- 宿主没有任何脚本可用的相机能力: 只有 `engine/RuntimeWebCameraRecording.kt` (camera2 + MediaRecorder, 仅供 WebView `getUserMedia`) 与 `RuntimeWebFileChooser.kt` (系统相机 Intent), 均不复用; 没有 `setTorchMode` 调用.
- 没有 `camera-api` 契约模块, `CameraPluginHost`, `camera` 脚本模块, `RuntimeCameraBridge`, `strings_runtime_camera.xml`, 文档, d.ts 与离线文档; `InstalledPluginRepository` / 向导目录 / `<queries>` 均无条目.
- 宿主没有 `SharedMemory` 作为 Parcelable 直接入参的先例 (OCR 用的是 fd 复制), P1.2 需验证 `Bundle.putParcelableArray(SharedMemory[])` 跨进程映射.
- 立项时本地 Gradle 缓存无 CameraX (P0.2 已下载 1.6.2); 宿主与平台版本插件均无 CameraX 版本常量.
- 本仓库尚不存在 (本次会话只落盘路线图与图标源图).

### 4.3 外部事实

- CameraX 稳定版 `1.6.2` (Android Developers 发布页 2026-10-21 更新), 同日 `1.7.0-rc01`; 1.6 线以 CameraPipe 为内部实现, `VideoCapture` 用 Media3 Muxer; 1.5 起有 Feature Group API 与 DNG 输出 (均不在 1.0.0 范围). CameraX 不含原生库.
- Binder 单次事务上限 1 MB (全进程共享), 每帧 `byte[]` 不可行; `SharedMemory` API 27+ 且为 Parcelable; `Surface` 为 Parcelable 可跨进程.
- 后台相机政策: Android 9 起后台 (idle) 应用不能访问相机; Android 11 起后台访问相机的前台服务必须声明 `foregroundServiceType="camera"`; Android 14 起另需 `FOREGROUND_SERVICE_CAMERA` 权限, 且被可见宿主绑定的进程启动 camera 前台服务需满足 while-in-use 条件 (Macaw 麦克风路径已验证透明 Activity + `BIND_ALLOW_ACTIVITY_STARTS` 可行).
- 模拟器后置相机可设为 `virtualscene` (三维场景, 含可配置海报), 前置可设为 `emulated`, CameraX 在 API 33 x86_64 系统镜像上可出帧与拍照 (P4.1 以 CI 实跑为准).
- 用户需求信号: 在线 issue 检索只命中前置镜头镜像 (#349) 与手电权限 (#157) 两条附带提及, 没有本地 issue 导出; 立项依据主要是能力空白与识别插件的实时输入缺口, 不是 issue 数.
- 平台版本插件当前 `1.9.1`, 建仓时以该值或更新的已发布版本为准.

---

## 5. 目标架构

### 5.1 数据流: 连续帧分析

```text
脚本 (Rhino)                        宿主进程                                   插件进程 (3-Prism Camera)
camera.open({resolution})  --->  CameraPluginHost.bind (BIND_INCLUDE_CAPABILITIES)
                                 ICameraPlugin.openSession(req, sessionCb) ---> 前台服务 start, CameraX bindToLifecycle
session.analyze(cb)        --->  ICameraSession.startAnalysis(opts, frameCb) ---> 分配 SharedMemory x3, 应答 buffers / width / height
                                                                                ImageAnalysis.Analyzer: RGBA 拷入空闲块
                                 frameCb.onFrame(meta)  <--------------------  oneway (seq, bufferIndex, rotation, ts)
                                 mapReadOnly -> Bitmap (旋正) -> ImageWrapper
cb(img, meta)              <---  投递脚本主循环 (队列深度 1, 超时覆盖 + dropped)
(返回)                     --->  recycle(img); ICameraSession.releaseFrame(idx, seq) ---> 块回到空闲池
session.close()            --->  ICameraSession.close() ---------------------> unbind use cases, 停前台服务
```

### 5.2 数据流: 拍照与取景页

```text
camera.capture({path})     --->  open (若无会话) -> ICameraSession.capture(opts, captureCb)
                                                                           ---> ImageCapture.takePicture -> JPEG 写 cacheDir 临时文件
                                 captureCb.onCaptured(fd, meta) <------------  只读 fd (文件已删除), meta: 宽高 / EXIF 旋转 / 字节数
                                 读 fd: {path} 则原样写文件; 解码 + EXIF 旋正 -> ImageWrapper
camera.capture({ui: true}) --->  ICameraPlugin.openCaptureUi(req, captureCb) ---> 启动 ThreePrismCameraCaptureActivity (宿主可见)
                                 onCaptured / onError(CANCELLED) <-----------  用户按快门 / 取消
```

### 5.3 数据流: 预览

```text
ui 脚本 <surfaceview id="cam"/>  --->  session.preview(ui.cam)
宿主: surfaceCreated -> ICameraSession.startPreview(Surface, {w, h, rotation}) ---> Preview.SurfaceProvider 提供 Surface
      尺寸变化 -> startPreview 重调; surfaceDestroyed -> stopPreview                 像素由 CameraX 直接渲染到 Surface, 不过 Binder
```

### 5.4 包结构

插件 (本仓库):

```text
app/src/main/java/io/github/supermonster003/autojs6/plugin/three/prism/camera/
  ThreePrismCameraApplication.kt
  ThreePrismCameraContract.kt           插件 ID / engine / variant / 能力 / 限制 (镜像宿主常量)
  ThreePrismCameraInfoService.kt        org.autojs.plugin.INFO, category three-prism-camera
  ThreePrismCameraService.kt            org.autojs.plugin.CAMERA, category camera (ICameraPlugin.Stub)
  ThreePrismCameraForegroundService.kt  foregroundServiceType camera
  ThreePrismCameraStartActivity.kt      透明, API 34+ 可见状态启动前台服务 / 授权
  session/  CameraSessionBinder.kt, CameraController.kt (CameraX use cases), FrameRing.kt (SharedMemory / 文件回退), CaptureWriter.kt
  ui/       CaptureActivity (取景页, 兼一键拍照), SettingsActivity, ReleaseHistory, theme
app/src/main/aidl/ (由 libs/camera-api.aar 提供, 不自带)
libs/common-plugin-api.aar, libs/camera-api.aar, locks/host-api-aars.lock
.python/ (check_repository, generate_markdown, generate_icon_studio, icon_studio_runtime, ci_connected_tests, device_smoke, release_sums), .python/icons/three-prism-ic-launcher-{light,dark}.png
.icons/recipe.json, .changelog/lang_*.json, .readme/lang_*.json, .github/workflows/{build,icon-studio,markdown}.yml, build-logic/, docs/development/
```

宿主 (`D:/idea-projects/AutoJs6`):

```text
plugin-api/camera-api/                                  契约 (D23), settings.gradle.kts 登记
app/src/main/java/org/autojs/autojs/core/plugin/camera/ CameraPluginHost.kt, CameraFrameTransport.kt, CameraSessionClient.kt
app/src/main/java/org/autojs/autojs/runtime/api/augment/camera/ Camera.kt, CameraSession.kt, CameraErrors.kt
app/src/main/java/org/autojs/autojs/runtime/api/camera/ CameraService.kt (脚本退出时关会话)
app/src/main/java/org/autojs/autojs/engine/RuntimeCameraBridge.kt (+ NodeBridgeProtocol / BunAutomationBridge 条目)
app/src/main/res/values*/strings_runtime_camera.xml     10 语言错误文案
docs/dev/camera-plugin-protocol-v1.md, .changelog (zh-Hans), 样例脚本
```

### 5.5 身份派生表

| 项 | 值 |
| --- | --- |
| `{PROJECT_NAME}` | `AutoJs6-Plugin-Three-Prism-Camera` |
| `{ROOT_PROJECT_NAME}` | `autojs6-plugin-three-prism-camera` |
| `{APP_NAME}` (不可翻译) | `3-Prism Camera` |
| `{APPLICATION_ID}` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.three.prism.camera` |
| `{PLUGIN_ID}` / `{PLUGIN_ENGINE}` / `{PLUGIN_VARIANT}` | `three-prism-camera` / `camera` / `default` |
| 身份类 | `ThreePrismCameraContract`, `ThreePrismCameraService`, `ThreePrismCameraInfoService`, `ThreePrismCameraForegroundService`, `ThreePrismCameraStartActivity`, `ThreePrismCameraCaptureActivity`, `ThreePrismCameraApplication`, `Theme.ThreePrismCamera`, 测试类同名后缀 |
| 插件中心身份服务 | `ThreePrismCameraInfoService`, action `org.autojs.plugin.INFO`, category `three-prism-camera` |
| 能力服务 | `ThreePrismCameraService`, action `org.autojs.plugin.CAMERA`, category `camera`, 受 `org.autojs.permission.PLUGIN` 保护 |
| 前台服务 | `ThreePrismCameraForegroundService`, `foregroundServiceType="camera"`, 不导出 |
| `{CAPABILITY_API}` | `camera-api` (宿主 `plugin-api/camera-api`, AAR 复制到 `libs/camera-api.aar`; 另需 `libs/common-plugin-api.aar`; 全部登记在 `locks/host-api-aars.lock`) |
| 宿主契约包 | `org.autojs.plugin.camera.api` |
| 宿主桥接包 | `org.autojs.autojs.core.plugin.camera` |
| 脚本 API | `camera` (`$camera` 同对象) |
| `{REQUIRES_HOST_VERSION}` | P0.1 临时值 6640 (宿主 6.8.1 `3ce52d2fd7`), P1.3 写契约落地构建的临时值, P5.4 回填 (D39) |
| 向导目录 | `PluginInstallWizardCatalog` `official("three.prism.camera")`, 回退标题 `Three Prism Camera`, 分类见附录 D Q1 |
| `plugin_description` (zh-Hans) | `通过 CameraX 拍照, 连续取帧与取景预览, 帧可直接交给 OCR, 条码与 YOLO 等识别模块` (句尾无点号, 不含 "AutoJs6"; P0.1 定稿, 10 语言见 `values*/strings.xml`) |
| GitHub 仓库描述 | `Camera capture, live frame analysis and preview for AutoJs6 scripts, powered by CameraX` (功能性描述, 不复述名称) |
| 发布文件名 | `autojs6-plugin-three-prism-camera-v{VERSION_NAME}-{CRC32}.apk` (单 APK, D22) |
| 索引清单 | `release-manifests/io.github.supermonster003.autojs6.plugin.three.prism.camera/<versionCode>.json` (`supportedAbis` 空数组) |
| 图标 | `.python/icons/three-prism-ic-launcher-light.png` / `-dark.png` (1024 px, 立项会话生成, D5), Icon Studio 合成 (P0.3) |
| 许可证 | 源码 MPL-2.0; CameraX Apache-2.0; `THIRD_PARTY_NOTICES.md` 见 D40 |

---

## 6. 阶段总览

| 阶段 | 版本 | 主题 | 状态 |
| --- | --- | --- | --- |
| P0 | 1.0.0 | 仓库骨架: 构建链, CameraX 接入与按 ABI 拆包, 图标, 测试与 CI | 完成 (P0.2 按附录 D Q8 = (b) 由 prism-p2 返工) |
| P1 | 宿主 6.8.0 | 宿主契约 `camera-api`, `CameraPluginHost` 与帧传输宿主端, 注册与协议文档, 脚本 API `camera`, Node / Bun 桥, 样例 | 完成 (最后一条 P1.6 由 prism-p3 于 2026-11-06 勾选) |
| P2 | 1.0.0 | 相机核心: 会话与独占, 前台服务与权限, 连续帧环形缓冲, 拍照, 8 项控制, 预览 Surface, 契约一致性测试 | 进行中 (prism-p2) |
| P3 | 1.0.0 | 独立界面: 取景页 (含一键拍照), 设置页, 发行历史, 图标核对 | 完成 (prism-p3, `9cbbe98`; 附录 D Q41 = (b) / Q42 = (b) 2026-11-06 拍板后由插件构建 16 落实, 实施差异见 Q45 / Q46) |
| P4 | 1.0.0 | 验证: CI 模拟器相机, 真机矩阵, 后台 / 熄屏 / 宿主被杀, 性能基线, 识别联动样例实测 | 未开始 |
| P5 | 1.0.0 | 文档, 声明, 四仓库同步, README / changelog / 截图, 发布与索引登记 | 未开始 |
| P6 | 1.1.0 | 录像 (契约 v2) 与其他候选 | 未开始 |

依赖顺序:

```text
P0 (插件骨架)                      P1.1 - P1.3 (宿主契约, 绑定, 注册) 可与 P0 并行 (不同仓库; 附录 B 为共同依据)
 -> P2 (相机核心)                  依赖 P0 与 P1.1 (libs/camera-api.aar 来自宿主契约模块)
     P1.4 - P1.6 (脚本 API, 桥, 样例) 依赖 P1.1 - P1.3; 与 P2 并行, 联调需要 P2.1 / P2.2 的 debug 包
     -> P3 (界面)                  依赖 P2.1 - P2.4
         -> P4 (验证)              依赖 P1 - P3 全部; P4.1 的 CI job 骨架可在 P0.4 先搭
             -> P5 (文档, 发布)    依赖 P1 - P4 全部通过
                 -> P6 (1.1.0)
```

发布门禁 (每个阶段的 "(发布)" 条目都指向本清单):

- R1 `version.properties` 的 `VERSION_NAME` 更新, `VERSION_BUILD` = 提交数 + 1; 10 种语言 `.changelog/lang_*.json` 补齐该版本的 `hint` / `feature` / `fix` / `improvement` / `dependency`, 日期为发布当日.
- R2 `.readme/lang_*.json` (功能, 安装, 权限与后台说明, 脚本示例, 帧生命周期提示, 兼容性, 常见问题, 第三方致谢), `plugin_instruction` 10 语言, `strings.xml` 描述与 README 一致性校验通过; 界面有变化时重采 `docs/images/screenshots` (API 33 设备或模拟器, 1080 x 2340, 合成素材, 明暗背景可读).
- R3 `py D:/idea-projects/prepush_check.py` 通过 (文档校验, `check_repository.py`, 构建号容差, Gradle 单元测试); 附录 F 设备矩阵中每台在线设备完成冒烟 (安装, 激活, 授权, `camera.list`, `open` + 10 帧, `capture` 到文件, torch 切换, 后台 30 秒取帧, 取景页拍照), 记录型号 / API / APK 摘要 / 实际分辨率与帧率.
- R4 release APK 经 `appendDigestToReleasedFiles` 收集 (D22: 4 个单 ABI 包 + Universal, 文件名带 ABI 与 CRC32), 每包原生库与 D22 白名单一致 (单 ABI 包 3 个, Universal 4 ABI x 3, STORED, 16 KB 对齐), `py .python/release_sums.py` 校验 5 个产物并生成 `SHA256SUMS`, 创建轻量 tag `vX.Y.Z` 与 GitHub Release (5 个 APK + SHA256SUMS), 复核下载产物哈希.
- R5 官方索引: `official-repositories.json` 加入 `AutoJs6-Plugin-Three-Prism-Camera` (首次, 数量以索引仓库当前值 + 1 为准), `release-manifests/.../<versionCode>.json` 登记 (按包列出资产, 每个单 ABI 包的 `supportedAbis` 为 `[该 ABI]`, Universal 为 4 个 ABI, 与运行时 `PluginInfo.supportedAbis` 一致; `requiresHostVersion` 与实际一致), `catalog-icons` 图稿登记, 运行 `tools/generate_official_plugin_index.py` 与索引仓库的 `generate_markdown.py`.

---

## P0: 仓库骨架

目标: 可构建, 可测试, 能被宿主插件中心识别的空壳插件 (CameraX 已接入但无会话能力), 满足 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 全部 MUST. 设备: 任一真机或 AVD (安装与插件中心识别).

### P0.1 构建链与身份

- [x] (插件) 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 第 2 节占位表生成骨架: `settings.gradle.kts`, `build-logic/`, `app/build.gradle.kts` (`globalApplicationId`, `requiredHostVersion` 临时值 6640, 无 `splits`), `version.properties` (`VERSION_NAME=1.0.0`, `VERSION_BUILD` = 提交数 + 1, SDK 37 / 24 / 37), `gradle.properties`, `AGENTS.md` 裁剪版, `LICENSE` (MPL-2.0), `THIRD_PARTY_NOTICES.md`, `.gitignore`, `.editorconfig`. 证据: 提交 `25996da` (2026-11-05, `VERSION_BUILD=2`); `gradlew assembleDebug` 与 `assembleDebugAndroidTest` 通过, debug 包 versionCode 2.
- [x] (插件) Manifest: `ThreePrismCameraApplication`, `ThreePrismCameraInfoService` (`org.autojs.plugin.INFO` / `three-prism-camera`), `ThreePrismCameraService` (`org.autojs.plugin.CAMERA` / `camera`, `org.autojs.permission.PLUGIN`), `ThreePrismCameraForegroundService` (`camera` 类型, 不导出), `ThreePrismCameraStartActivity` (透明), 权限 `CAMERA` / `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_CAMERA`, `uses-feature camera required=false`, `<queries>` 宿主包. 证据: 提交 `25996da`; `ManifestContractTest` 10 例通过. 偏差: P2.1 前 `ThreePrismCameraService` 返回描述符为 `org.autojs.plugin.camera.api.ICameraPlugin` 的占位 Binder (无 AAR 可用), INFO 声明 `cameraContractVersion = 1` 与空 `cameraFeatures` (D42). 真机: G8441 (API 28) 与 XQ-AT72 (API 31) 的 debug 宿主 5935 插件中心列出 "3-Prism Camera 1.0.0 (2)" 与英文描述 (设备语言为英语; 其余 9 种语言由 `StringResourceParityTest` 与 `check_repository.py` 核对, 未在设备上逐一切换); 状态为 "启用失败", 因宿主 5935 低于 `requiredHostVersion` 6640, 预期内, P1.3 用 >= 6640 的宿主复核.
- [x] (插件) 10 语言 `strings.xml` (`plugin_name` 资源不可翻译为 `3-Prism Camera`, `plugin_description`, `plugin_instruction`, 前台服务通知文案), `.readme/lang_*.json` 与 `.changelog/lang_*.json` 初始条目, `generate_markdown.py` 生成 README 10 语言; ASCII 标点, 阿拉伯语品牌名 FSI / PDI 包裹. 证据: 提交 `25996da`; `check_repository.py` (提交前后) 与 `check_markdown.bat` 通过 (`MARKDOWN_OK languages=10 artifacts=36`); `StringResourceParityTest` 6 例, `ApplicationTextPunctuationTest` 2 例通过.
- [x] (插件) `PluginInfo` 元数据映射与 JVM 测试 (名称, 描述, 作者, 三个身份字段, 包版本, 日期, `supportedAbis` 为显式空数组, 最低宿主版本, 契约版本). 证据: 提交 `25996da`; `PluginRuntimeInfoTest` 5 例, `HostApiAarLockTest` 5 例; `testDebugUnitTest` 共 5 个类 28 例全部通过.

### P0.2 CameraX 接入与按 ABI 拆包 (D21, D22)

- [x] (插件) 依赖 `androidx.camera:camera-core / camera-camera2 / camera-lifecycle / camera-compose:1.6.2` (Q3 = (b)) (版本常量集中在 `build-logic` 或 `gradle/libs.versions.toml`), Compose BOM 与 Macaw 一致; `CameraController` 空壳能 `ProcessCameraProvider.getInstance` 成功. 证据: 提交 `25996da`; `device_smoke.py` 的 CameraX 探测日志: G8441 (API 28) `availableCameraInfos` = 2, XQ-AT72 (API 31) = 5 (2026-11-05). `camera-compose` 体积见 D21 (release +590 B, 上限 +82510 B, debug dex +185600 B).
- [x] (插件) 单 APK: `appendDigestToReleasedFiles` 按 3-Block Compose 形态实现 (`{ROOT_PROJECT_NAME}-v{VERSION_NAME}-{CRC32}.apk`), 任务内校验原生库集合与 D22 白名单一致 (4 ABI x 3 个依赖自带库, STORED, 16 KB 对齐); `PluginInfo.supportedAbis = emptyArray()`; `AGENTS.md` 与 README 记录原生库来源与 "不拆 ABI" (附录 D Q8). 证据: 提交 `25996da`; `appendDigestToReleasedFiles` 产出 `releases/autojs6-plugin-three-prism-camera-v1.0.0-b9c39b26.apk` (1330321 B, 单 APK), 原生库 12 个 (`arm64-v8a` / `armeabi-v7a` / `x86` / `x86_64` 各 `libimage_processing_util_jni.so`, `libsurface_util_jni.so`, `libandroidx.graphics.path.so`) 共 202140 B, STORED, 16 KB 对齐, 与白名单一致; 附录 D Q8 (a) 先行待确认. (已被下一条按 Q8 = (b) 的返工取代, 留作历史.)
- [x] (插件) 返工 (附录 D Q8 = (b), D22): `splits.abi` 出 `arm64-v8a` / `armeabi-v7a` / `x86_64` / `x86` 4 个单 ABI 包 + Universal (规范 5.5); `NativeLibraryInventory` 运行时扫描已安装 APK 的 `lib/<abi>/` 条目填写 `PluginInfo.supportedAbis` (单 ABI 包为 `[该 ABI]`, Universal 为 4 个 ABI); `appendDigestToReleasedFiles` 逐包校验签名, 文件名与原生库后收集 `autojs6-plugin-three-prism-camera-v{VERSION_NAME}-{ABI}-{CRC32}.apk` (Universal 为 `universal`); 新增 `.python/release_sums.py` (默认模式校验最新一组并写 `releases/SHA256SUMS`, `--check` 只比对, `--artifacts` 供 CI 校验未签名产物), CI `Check the release artifacts` 改用它; `check_repository.py` 要求构建脚本与 `release_sums.py` / `device_smoke.py` 的 ABI 与原生库白名单一致; `device_smoke.py` 按设备 `ro.product.cpu.abilist` 选 debug 包 (`--abi` 指定其他包), 并核对 `supportedAbis` 等于所装包的 ABI; `AGENTS.md` / README (10 语言) / `THIRD_PARTY_NOTICES.md` / `libs/README.md` 改写. 证据: 提交见附录 D Q8 行; release 产物 `arm64-v8a` 1116727 B, `armeabi-v7a` 1115297 B, `x86_64` 1149502 B, `x86` 1132009 B (各 3 个原生库), `universal` 1363265 B (12 个), 全部 STORED, 数据偏移与 ELF `PT_LOAD` 16 KB 对齐, `py .python/release_sums.py` RELEASE_SUMS_OK, `--artifacts app/build/outputs/apk/release` RELEASE_OK; debug 包各约 13.5 MB (universal 13792239 B); `NativeLibraryInventoryTest` 4 例通过; 16 KB 页环境实装运行 `arm64-v8a` 包的记录在 P2 设备验证中补.

### P0.3 图标 (D5, D44)

- [x] (插件) `.icons/recipe.json` 双源 `grayscale` 配方 (亮 #272727 / 暗 #D8D8D8, 背景 #FAFAFA / #212121, `alphaMode: alpha`), `generate_icon_studio.py` + `icon_studio_runtime.py` 生成 `mipmap` 全套与 `ic_launcher.png`, `--check` 通过; `icon-studio.yml` 工作流. 证据: 提交 `25996da`; `generate_icon_studio.py --check` 输出 "Verified 28 Icon Studio resources"; `.github/workflows/icon-studio.yml`; 截图 (不入库) `.bak/visual/1.0.0/launcher-BH900ASK9E-light.png`, `launcher-QV710AF65F-dark.png`, `plugin-center-BH900ASK9E-system-light.png`, `plugin-center-QV710AF65F-system-dark.png` (启动器与插件中心各明暗一张, 未改设备设置). 深色插件中心的 "启用失败" 行图标为宿主 5935 的渲染, P3.4 在 >= 6640 的宿主上复核.

### P0.4 测试与 CI

- [x] (插件) JVM 单元测试骨架 (契约常量镜像, `PluginInfo`), instrumentation 骨架 (`CameraSmokeTest` 占位: 只断言 `availableCameraInfos` 非空); `.github/workflows/build.yml` (构建, 单元测试, release 产物校验), `markdown.yml`; `connected-avd` job 骨架 (D35, 用例在 P4.1 填), 默认只在 `main` 推送与 PR 运行. 证据: 提交 `25996da`; 本地 `testDebugUnitTest` 28 / 28, `lintDebug` 0 错误 14 警告 (AGP / 依赖版本提示 3, Icon Studio 生成图标的 `IconDuplicates` 7 与 `IconLauncherShape` 2, `UnusedResources` 2); `build.yml` (push `master` 与 PR, 含 release 产物原生库校验与 `connected-avd` job), `markdown.yml`, `icon-studio.yml`; 分支为 `master` (条目原文写 `main`, 仓库沿用 master). 远端运行待首次推送.
- [x] (插件) `.python/ci_connected_tests.py` / `device_smoke.py` 骨架 (从 Macaw 裁剪: 安装, 激活, 调用 `getInfo`), 输出只保留每项通过 / 失败与关键数值. 证据: 提交 `25996da`; `device_smoke.py` 在 G8441 (API 28) 与 XQ-AT72 (API 31, 宿主 5935) 上 8 / 8 通过 (安装, 激活, 插件中心发现, INFO 应答, CameraX 探测等; 唤醒检查改为按设备时间 `logcat -t` 过滤, 避免 G8441 主缓冲被 CameraX 日志挤掉标记行); `ci_connected_tests.py` 在只读 AVD_API_33 (API 33 google_apis x86_64, `-camera-back virtualscene -camera-front emulated`, 2 个镜头) 上首次尝试通过, `ThreePrismCameraContractTest` 4 例 + `CameraSmokeTest` 1 例 = 5 / 5 (2026-11-05).

---

## P1: 宿主契约, 绑定与脚本 API

目标: 宿主能发现, 绑定并以 `camera` 模块调用插件; 帧传输宿主端就绪; 插件未安装 / 未启用 / 不兼容时合理退化. 落点宿主仓库, 只落本地提交. 联调设备: 任一真机 (与 P2 的 debug 包).

### P1.1 契约模块 `plugin-api/camera-api` (D23, D29, D42, D43)

- [x] (宿主) 常量与 key 对象 (附录 B), `CameraLimits`, `CameraErrorCodes`, `consumer-rules.pro`, `settings.gradle.kts` 登记; JVM 测试 (字面值快照, 键唯一, 闭合集合, 限制值, 错误码 encode / decode). 证据: 宿主提交 `4a10154c44` (2026-11-05, 宿主 `VERSION_BUILD=6640` 未变); `CameraContractTest` 8 例通过; AAR `plugin-api/camera-api/build/outputs/aar/camera-api-release.aar` (38747 B, P2.1 复制到 `libs/` 并入锁).
- [x] (宿主) AIDL: `ICameraPlugin` (`getInfo` 居首, 其后 `getCapabilities`, `listCameras`, `openSession`, `openCaptureUi`, `requestPermission`), `ICameraSession`, oneway `ICameraFrameCallback` / `ICameraCaptureCallback` / `ICameraSessionCallback`; 每个方法的 nullability, 输入上限, 输出上限, 错误语义, 线程行为与资源所有权写入 `docs/dev/camera-plugin-protocol-v1.md`; `CameraAidlOrderTest` 冻结事务顺序与 oneway 标记. 证据: 宿主提交 `4a10154c44`; `CameraAidlOrderTest` 2 例通过; 协议文档 `docs/dev/camera-plugin-protocol-v1.md` 同提交.

### P1.2 `CameraPluginHost` 与帧传输宿主端 (D24, D26, D27, D45)

- [x] (宿主) `CameraPluginHost` (发现, 绑定 flags 与 `SpeechPluginHost` 一致, `linkToDeath`, 会话句柄与关闭), `CameraFrameTransport` (`SharedMemory.mapReadOnly` 路径与 API 24-26 fd 路径, 行紧排拷贝到 Bitmap, 旋正 / 镜像, `releaseFrame`), `CameraSessionClient` (队列深度 1, `dropped` 计数, 事件投递). 证据: 宿主提交 `349cc56532`; JVM (假帧): `CameraFrameSlotTest` 6, `CameraPixelTransformTest` 6, `CameraFrameGeometryTest` 6, `CameraSessionEventsTest` 6, `CameraCallsTest` 5, `CameraRequestsTest` 5, `CameraErrorMapperTest` 8 例通过, `StaticReferenceGuardTest` 9 例通过; 交给插件的 Binder 存根是只持 `AtomicReference` 委托的中转存根, 关闭时清空. `SharedMemory[]` 跨进程映射的 instrumentation (P2.2 联调, 2026-11-06 补): 宿主提交 `02e965f81a` 的 `CameraFrameTransportInstrumentedTest` 2 例在 `AVD_API_35_16K` (API 35, 隔离克隆宿主 6641 + 插件 P2 debug 包) 上通过: AIDL 直连时应答 3 块 (各 1228800 B, 640x480, 行跨度 2560), 10 帧 `seq` 1 - 10 递增, 块序号 0, 1, 2 轮转, 复用次数 4 / 3 / 3, 每帧像素非空, 归还之前插件从不重复发布同一块; 经 `CameraPluginHost.openSession` / `CameraSessionClient.takeFrame` 同样取到 `seq` 1 - 10, 丢帧 0, 旋转 90 度. 联调发现宿主缺陷并已修复: `CameraBundles.intOf` / `longOf` 复用 `PluginCapabilityResolver.coerceLong` (只保留正数, 原为宿主版本号而写), 第 0 块的帧因 `bufferIndex` 读成 null 被静默丢弃且不归还, 环形缓冲实际只用 2 块 (`cameraCount` 0 与声明 `byteCount` 0 同样读成缺失); 宿主提交 `c415503fcd` 改为 `CameraBundles.wholeNumber` (保留零与负数), JVM `CameraBundlesTest` 3 例, 宿主 camera 10 类 54 例通过. 修复后 API 35 的 480p 30 秒为 29.2 fps, 宿主丢帧 15, 插件丢帧 0 (修复前 29.7 fps / 0 / 6: 三块都在流转后, 丢帧从插件侧移到宿主的深度 1 队列, 符合 D27). API 24 - 26 走临时文件, 不在此测试范围, 但同样带 `bufferIndex` 0, 修复后 `AVD_API_26_PRISM` / `AVD_API_24` 的 check, busy, perf, capture 复测通过 (480p 28.6 / 23.3 fps, 修复前 18.8 / 23.1 fps, 见 P2.2). 偏差: 宿主端 `UI_REQUIRES_FOREGROUND` 预检 (宿主不可见时不发起 `openCaptureUi` / `requestPermission`) 留给 P1.4 脚本层, P1.2 只映射插件应答的该错误码.
- [x] (宿主) 拍照接收: 读只读 fd, `{ path }` 原样落盘 (含 EXIF), 否则 `BitmapFactory` 解码 + EXIF 旋正成 `ImageWrapper`; 超过 `CameraLimits` 的 JPEG 报 `LIMIT_EXCEEDED`. 证据: 宿主提交 `349cc56532`; `CameraCaptureReceiverTest` 5 例 (超限报 `LIMIT_EXCEEDED` 带 `limit`, 与声明大小不符报 `INTERNAL`, `path` 原样落盘且不留暂存文件, 目录目标报 `INVALID_ARGUMENT`), `JpegExifOrientationTest` 4 例 (8 个方向 x 两种字节序, 缺失 / 畸形 / 截断 EXIF) 通过; 解码出的 Bitmap 交给脚本, 宿主不回收 (D13, 附录 A.5).

### P1.3 注册, 协议文档, changelog 与版本回填 (D38, D39)

- [x] (宿主) `InstalledPluginRepository` 两张表, `<queries>`, `PluginInstallWizardCatalog` 条目 (分类按附录 D Q1), 插件中心能力行; `.changelog` zh-Hans 条目; `CameraPluginIds.REQUIRED_HOST_VERSION_CODE` 写契约落地构建的 versionCode. 证据: 宿主提交 `b785489b14`; `PluginCenterCameraRegistrationTest` 7 例, `PluginInstallWizardCatalogTest` 5 例 (条目数 51 -> 52), `PluginInstallWizardLoaderTest` 6 例, `PluginInstallWizardRowTest` 4 例通过; `REQUIRED_HOST_VERSION_CODE = 6641` (附录 D Q9, 已按推荐先行); changelog zh-Hans v6.8.1 feature 条目 (camera 模块, P1.4 定稿措辞) 与生成的 README / CHANGELOG 同提交. 设备: G8441 `BH900ASK9E` (API 28), 宿主 debug 6640 (本树构建, base.apk SHA-256 一致) + 插件 P0 debug (versionCode 2), `py -3.12 -I tools/camera/device_registration_check.py` REGISTRATION PASS: 插件中心行 "3-Prism Camera" / "1.0.0 (2) | 2026-11-05" / 开关开, 宿主可见 1 个 CAMERA 服务与插件包, 向导目录条目 `IMAGE`; 向导列表正常加载 (含 "Images" 分类) 但还没有本插件的行: 向导只为官方索引已收录的目录条目建行, 发布并登记索引后才显示 (附录 D Q10, 已按推荐先行, 真机显示改到 P5.4 登记索引后复核, 脚本加 `--expect-wizard-row`). 截图 `.bak/visual/1.0.0/p1.3-plugin-center-BH900ASK9E.png` / `p1.3-install-wizard-BH900ASK9E.png` (不入库). 偏差: P0 debug 包声明 `requiresHostVersion` 6640 < 6641, 宿主相机校验拒绝该包 (插件中心照常列出), P2.1 把插件三处改为 6641 后消除.

### P1.4 脚本 API `camera` (D4, D11, D19, D28)

- [x] (宿主) `runtime/api/augment/camera/Camera.kt` + `CameraSession.kt` + `CameraErrors.kt`, `ScriptRuntime` / `ScriptModuleLayer` 注册 (speech 先例的 4 处注册点), `CameraService` 在脚本退出时关闭会话并 `recycleAll`; 事件对象与 `events` 模块一致; 帧回调返回即 `recycle` + `releaseFrame` (D11), `dropped` 可读; `preview(view)` 接 `SurfaceView` (核对 XML 标签名); `capture({ ui: true })` 走 `openCaptureUi`; `strings_runtime_camera.xml` 10 语言. 证据: 提交 hash; 宿主 debug 包 + 插件 P2 debug 包在任一真机上跑附录 A.5 的 5 个示例. 实施: 宿主提交 `24473822f9` (模块 `runtime/api/modules/camera/` 10 个文件, `ScriptModuleLayer` 注册 + KSP 清单 + augment / api-coverage 基线; 会话登记到 `RuntimeResourceScope` 新阶段 `CAMERA`, 在 `THREADS` 之前关闭; 帧经 keepAlive 派发到脚本线程, 回调返回后在 finally 中 `recycle` + 释放帧; 事件 `frame` / `dropped` (total, lastSeq) / `error` / `close` (reason: script / host / pluginUi / cameraDisconnected / policy / error); Q7 = (a): `close` 原因 `pluginUi` 时停止持有会话的脚本, `holderLabel` 为脚本路径; `capture({ ui: true })` 取消时返回 null, 宿主不在前台时先报 `UI_REQUIRES_FOREGROUND`; `strings_runtime_camera.xml` 11 个目录各 18 条). JVM: `CameraFramePumpTest` 4, `CameraScriptOptionsTest` 11, `CameraScriptValuesTest` 5, `CameraPermissionWaiterTest` 3, `RuntimeResourceScopeTest` 15, `ScriptModuleArityTest` 3, `ScriptModuleInstallsTest` 2 例通过; `ScriptModuleOrderTest` / `ScriptSurfaceGateTest` 的相机部分与基线一致 (共享工作区中这两类失败只因 3-Slate Studio 未提交的 video / audio 注册: 基线差异 0 行删除, 新增行全是 audio / video); `py -3.12 tools/api-coverage/check.py` 只剩此前积压的 `console.restoredEntryCount`. 未执行: 真机跑附录 A.5 的 5 个示例 (待联调: 需要插件 P2 debug 包与合入本提交的隔离克隆宿主 6641, 步骤见共享笔记 `prism-p1-script-handover.md`; A.5 示例 2 / 4 的写法见附录 D Q23 与该笔记). 偏差: 模块目录为 `runtime/api/modules/camera/` (现行模块层的位置, 条目原写 `augment/camera`); 布局标签为 `<SurfaceView>` (附录 D Q23); `open` 可在 UI 线程调用, `requestPermission` / `capture` / `focusAt` 会阻塞, 在 UI 线程调用报 `INVALID_ARGUMENT`; `state()` 另含 `id`, `flashMode` 与 `previewMirrored` (初版读插件的附加状态键, 附录 D Q11 = (a), 宿主 `cf398968fa`, 同提交按 Q13 = (a) 更新协议文档 "Capture" 一节; 2026-11-06 维护者改选 Q11 / Q13 = (b), 宿主 `c82c5b27f9` 改读契约常量 `CameraResultKeys.STATE_PREVIEW_MIRRORED` 并把 "Capture" 一节恢复为内存回调原文), `meta.timestamp` 为毫秒; 持有会话时 `camera.capture({ ui: true })` 报 `CAMERA_BUSY` (附录 D Q22). 联调 (prism-p2, 2026-11-06, 模拟器; 真机未执行): 插件 P2 debug 包 + 隔离克隆宿主 6641, `py .python/device_camera_samples.py <serial> --phases main,ui` 在 `AVD_API_26_PRISM` 与 `AVD_API_24` 上 PASS: 模块成员齐全, 第二次 `camera.open()` 得到 `CAMERA_BUSY` (`holder` 为脚本路径), 持有会话时 `capture({ ui: true })` 报 `CAMERA_BUSY` (Q22); ui 脚本 `<SurfaceView>` 预览 + 640x480 分析 + 每 10 秒拍一张, 30 秒内 API 26 800 帧 (26.7 fps, 丢帧 11), API 24 669 帧 (22.3 fps, 丢帧 9). 发现: 帧回调经 keepAlive 投递到调用 `startAnalysis` 的线程的事件循环, 脚本在该线程忙等或 `sleep` 时回调排不上 (曾得到 frames 0, dropped 881), 样例与设备脚本改用 `setInterval` / `setTimeout`, P5.1 写进文档. 真机未执行 (G8441 等维护者授权). 真机 (prism-p3, 2026-11-06, G8441 `BH900ASK9E` API 28, 隔离克隆宿主 6641, 插件构建 12 arm64 包, `device_camera_samples.py --phases a5`): 附录 A.5 示例 1 `camera.capture({ path })` 2448x3264 (1692392 B) PASS; 示例 3 用 `img.clone()` 保留一帧, 回调与会话结束后读出 720x1280 与中心像素 PASS; 示例 4 原样运行 (ui 脚本在 UI 线程上 `camera.open()`) 时宿主无响应 (ANR, G8441 与 `AVD_API_26_PRISM` 都能复现), 见附录 D Q44, 改在 `threads.start` 里链式调用 `camera.open().preview(ui.cam).torch(true).zoom(2)` 后 PASS (`previewing=true torch=true zoom=2`, 截图 `.bak/visual/1.0.0/a5-preview-BH900ASK9E.png`, 不入库); Q44 = (b) 宿主修复 (69c5528499) 后, 2026-11-06 12:57 在 G8441 (克隆宿主 6641 (base.apk md5 fab3debb9d035f1dcbdd900ae4a3a0bb, 含 Q41 / Q43 / Q44 修复) + 插件构建 16) 原样运行 (ui 脚本的 UI 线程) PASS: `open` 23 ms 返回, 1765 ms 后打开, `previewing=true torch=true zoom=2` (上限 8), 无错误事件, 无 ANR / 崩溃 (宿主工具 `tools/camera/ui_thread_open_check.py --phases a5,repro,starve`, Q44 复现脚本 35 ms 打开); 示例 5 即 P3.1 的 `capture({ ui: true })` (拍 / 取消都通过, 见 P3.1); 示例 2 (扫码) 即设备脚本的 barcode 阶段: G8441 2026-11-06 12:12 PASS (维护者对准二维码, 识别到 `hello world`, 1 帧, 丢帧 0; 见 P1.6). 另: 帧回调慢于帧间隔时 `setTimeout` 被饿死, 见附录 D Q43.
- [x] (宿主) 错误路径: 插件未安装 / 未启用 / 版本过低 / 未授权 / 被占用 / 后台无能力, 每种一个确定的 `CameraError.code` 与 10 语言文案. 证据: 单元测试 + 真机手动触发清单. 实施: 宿主提交 `24473822f9`; 六种情形依次为 `PLUGIN_NOT_INSTALLED`, `PLUGIN_DISABLED`, `HOST_VERSION_TOO_LOW`, `PERMISSION_DENIED`, `CAMERA_BUSY` (`details.holder` 为占用者, 文案带占用者), `UI_REQUIRES_FOREGROUND`, 另有 `CAMERA_UNAVAILABLE`, `CAMERA_DISABLED`, `UNSUPPORTED`, `INVALID_ARGUMENT`, `SESSION_CLOSED`, `TIMEOUT`, `LIMIT_EXCEEDED`, `INTERNAL`; 脚本可见 `camera.CameraError` (`code`, `message`, `details`), 文案在 11 个目录. JVM: `CameraJsErrorsTest` 7 例通过. 真机手动触发清单 (6 种情形的操作与预期) 写在共享笔记 `prism-p1-script-handover.md`; 未执行: 真机逐项触发 (待联调, 同上). 联调 (prism-p2, 2026-11-06, `AVD_API_26_PRISM`, 撤销权限与卸载只在自己的 AVD 上做, `--allow-device-changes`): 被占用 `CAMERA_BUSY` (`details.holder` 为脚本路径), 后台 `capture({ ui: true })` 报 `UI_REQUIRES_FOREGROUND`, 撤销插件相机权限后 `open` 报 `PERMISSION_DENIED` (之后重新授予), 卸载插件后 `isAvailable()` 为 false 且 `open` 报 `PLUGIN_NOT_INSTALLED`, 均 PASS. 待联调: "未启用" (插件中心关闭开关) 与 "宿主版本过低" (要一个 `requiresHostVersion` 高于宿主的插件构建或低于 6641 的宿主), 以及真机逐项触发. 联调 (prism-p3, 2026-11-06, `AVD_API_26_PRISM`, 插件构建 12): "未启用" (`--phases disabled`: 在宿主的启用状态存储中关闭插件开关, 结束后放回原文件) `isAvailable()` 为 false, `open` 与 `capture({ ui: true })` 报 `PLUGIN_DISABLED`, PASS; "宿主版本过低" (`--phases toolow`: 临时构建 `requiredHostVersion` 6642 的插件包对宿主 6641, 结束后装回正常包) 同样两项报 `HOST_VERSION_TOO_LOW`, PASS; 构建 12 复跑后台 / 未授权 / 未安装仍 PASS. 真机: 被占用 (`CAMERA_BUSY`, 持有会话时 `capture({ ui: true })` 同样 `CAMERA_BUSY`) 在 G8441 上 PASS; 未授权 / 未安装 / 未启用 / 版本过低 / 后台要撤销权限, 卸载插件或改插件开关, 维护者的手机上不做这些改动, 只在自己的 AVD 上验证.

### P1.5 Node / Bun 桥 (D12, D34)

- [x] (宿主) `RuntimeCameraBridge`, `NodeBridgeProtocol` `CAMERA` 域, Bun 能力授权清单, api-coverage 检查; 连续帧吞吐实测 (480p / 720p 各 30 秒, 记录 fps 与丢帧), 不达标时按 D34 限流并在文档标注. 证据: 提交 hash, 吞吐数字, Node 与 Bun 各一个样例跑通 (任一真机). 实施 (宿主部分, 附录 D Q21 = (a) 先行): 宿主提交 `589d727793`; `RuntimeCameraBridge` 承接 `CAMERA` 域 (`list`, `capabilities`, `open`, 按会话拉取帧为 Node 图像句柄, `capture` 到路径, 各项控制, `state`, `close`; 会话以 open 操作 id 为键, 执行结束时关闭), Bun 授权 `camera.control` (插件族, 插件设置页 "官方" 区新开关, 默认关闭), `NodeBridgePermissionManifest.CAMERA`; api-coverage 为 Node main / Worker / Bun 各登记 8 个 camera 待定 id (`pending-baseline.json`, 随 `24473822f9`). JVM: `RuntimeCameraBridgeTest` 4, `RuntimePluginFamilyGrantTest` 3 例通过. 偏差: Node / Bun 不提供 `preview` (无 ui); 桥的 JSON 不带 null 值; Q7 停止脚本在 Node 侧留给 facade. 吞吐实测与 Node / Bun 样例移到下一条.
- [x] (Node / Bun 运行时插件) camera facade (Node `require('autojs6').camera` 一类入口与 Bun 同名模块, 帧拉取循环, `close` 原因与 Q7 停止动作), Node 与 Bun 各一个样例; 连续帧吞吐实测 (480p / 720p 各 30 秒, 记录 fps 与丢帧), 不达标时按附录 D Q4 处理. 证据: 运行时插件提交 hash, 吞吐数字, 两个样例在任一真机跑通. 来源: 附录 D Q21 (a), 从上一条拆出; 依赖插件 P2.2 出帧. 实施 (prism-facade, 2026-11-06, 均只在本地): Node 运行时插件 `2a8f2ac2` (2.2.0, 构建 380): `camera` / `autojs6:camera` 与 `$autojs.camera`, 主线程与 Worker, CommonJS 与 ESM, 授权声明 `camera` (回收帧另需 `image`), d.ts, catalog / runtime-kit, HOST-API 相机一节, 样例 `sample/nodejs/camera-frames` (状态 `host_provider_required`: 需要宿主 6641+, 已启用的本插件与相机权限), 设备脚本 `tools/nodejs/api/camera/device_camera_frames.py`, 10 语言 changelog; Bun 运行时插件 `cbbaa7c` (0.23.0, 构建 352): `host.camera` 走授权 `camera.control` (默认关闭), 与 Node facade 同源 (`tools/host-client/sync-camera.mjs --check`), 样例 `samples/camera-frames.bun.js`, 回收帧需要 `image.process` 或 `image.files` (附录 D Q31 (a)), `camera.control` 调用与单个图像句柄的回收不计入每次执行 1024 次调用上限 (Q32 (a)), docs/host-api.md 相机一节, 10 语言 changelog; 宿主 `18bdc55c9b` 只改 api-coverage 矩阵与基线 (Node main / Worker / Bun 各 8 个 camera 单元格转为 covered, pending 9 -> 1, 剩下的 `console.restoredEntryCount` 属 feedback-680). 行为: 帧在回调 (或其返回的 Promise) 结束后回收, 回调期间到来的帧互相替换并计入 `dropped`, 拍照得到的图片由脚本回收; 关闭原因 `script` / `host` / `pluginUi` / `cameraDisconnected` / `policy` / `error`, `pluginUi` 在 close 监听器执行后结束本次执行 (Q7 (a)); JS 运行时没有预览视图, `preview` / `stopPreview` 以 `UNSUPPORTED` 拒绝. 测试: Node `test-camera.js` + `test-worker-camera.js` 12 / 12 (已加入 CI), `:verifyNodePluginExamples` / `:verifyAutoJs6NodeTypeDeclarations` / `:typeCheckNodeTypescriptSmoke` 通过, `:app:testDebugUnitTest` 与 `:app:assembleDebug` 通过; Bun `camera.test.mjs` 3 / 3, `BunHostBridgeDispatcherTest` 9 / 9, `sync-camera.mjs --check` 通过. 真机 (克隆宿主 6641, 插件构建 9, 设备脚本每次 30 秒, 同机 Rhino 脚本作对照, 数字见附录 F "运行时连续帧吞吐"): XQ-AT72 (API 31) Node 样例 PASS (10 秒 282 帧, 拍照 3024x4032, 关闭原因 `script`), 480p 30 fps / 880 帧 / 丢 1, 720p 30 fps / 881 帧 / 丢 1 (Rhino 30 fps / 881 帧 / 丢 0); Redmi 22120RN86C (API 33) Bun 样例 PASS (10 秒 135 帧, 拍照 2448x3264, 关闭原因 `script`), 480p 14.3 fps / 421 帧 / 丢 0, 720p 13.9 fps / 410 帧 / 丢 11 (Rhino 14.3 fps / 422 帧 / 丢 0 与 14.2 fps / 420 帧 / 丢 2: 该机相机在测试环境只出约 14 fps, Bun 与对照持平). 模拟器 `AVD_API_33` 的拍照在 Rhino 中同样超时 (模拟器 HAL), 只记帧数. Q4 不需拍板 (运行时帧率与同机 Rhino 持平, 见附录 D). 未执行: Bun 在能出 30 fps 的 API 33+ 真机上的上限 (本卡可用的 API 33+ 真机只有 Redmi); 两台真机测完已装回原宿主与运行时插件.

### P1.6 宿主样例脚本 (D7)

- [x] (宿主) 样例 5 个: 拍一张存文件; 连续帧 + `ocr` (识别到目标文本即停); 连续帧 + `barcode` (实时扫码, 命中即停); 连续帧 + `yolo` (画框到悬浮窗); 取景页拍照. 每个样例注释 D11 的生命周期规则. 证据: 提交 hash, 任一真机跑通 (OCR / 条码 / YOLO 插件已安装). 进度: 宿主提交 `c0cd90db44`, `assets-app/sample/3-Prism Camera/` 下 5 个样例 (`拍一张存文件`, `连续帧识别文字`, `实时扫码`, `物体检测画框`, `取景页拍照`, 均为 `[v6.8.1+]`), 文件头写明 D11 规则, 都接受 `output` 参数写 JSON 摘要供设备脚本取回执; 条码用 `barcode.recognizeText(img)` (返回首个结果或 null), YOLO 用 `yolo.load(modelDir, { component })` + 悬浮窗 `<SurfaceView>` 预览 + 画布画框; 同提交定稿 v6.8.1 changelog camera 条目 10 种语言 (补 "帧只在回调执行期间有效, 需要保留时用 clone 复制" 与 "参阅 示例代码 > 3-Prism Camera"), `check_translations.py` 已记录, `generate_markdown.py --check` 通过. 未执行: 真机跑通 5 个样例 (待联调, 需插件 P2 debug 包, ML Kit Barcode / YOLO NCNN 插件与 yolo11n 模型目录); 跑通前本条不勾选. 联调 (prism-p2, 2026-11-06, 模拟器 `AVD_API_26_PRISM` / `AVD_API_24`, `device_camera_samples.py --phases main`): `拍一张存文件` PASS (API 26 960x1280; API 24 返工前 1280x960, 插件构建 9 补写方向后 960x1280, 附录 D Q14); `连续帧识别文字` 跑通 (API 26 2 - 3 帧, API 24 18 - 22 帧, 丢帧 0; 画面中没有目标文本, `found` 为 null); `取景页拍照` 得到 `UNSUPPORTED` (插件在 P3.1 之前不声明 `captureUi`, 符合预期); `实时扫码` 与 `物体检测画框` SKIP. 待联调: 扫码 (ML Kit Barcode 插件 + 镜头前的条码), 物体检测 (YOLO NCNN 插件 + 模型目录 + 悬浮窗权限, 同时验证 P2.5 的悬浮窗预览), 取景页 (P3.1). 真机 G8441 `BH900ASK9E` (2026-11-06, 插件构建 9, `--phases main,ui`): `拍一张存文件` 2448x3264, `连续帧识别文字` 65 帧 (丢帧 0, `found` 为 null), `取景页拍照` `UNSUPPORTED`, ui 样例 30 秒 800 帧; 扫码与物体检测同样 SKIP. 联调 (prism-p3, 2026-11-06, 插件构建 11 / 12): `取景页拍照` 在 P3.1 声明 `captureUi` 后跑通, G8441 拍到 2448x3264 (1384736 B), 取消返回 null, `AVD_API_26_PRISM` 960x1280; G8441 复跑 `拍一张存文件` 2448x3264 (1087070 B), `连续帧识别文字` 58 帧 (丢帧 0). `物体检测画框` (`--phases detect`, YOLO NCNN 插件 + yolo11n 模型目录 + 宿主悬浮窗权限): `AVD_API_26_PRISM` (YOLO 0.2.0 x86_64) 30 秒 154 帧, 框 0 (虚拟场景里没有可识别物体), 按时结束 (`reason=script`), 截图 `detect-floaty-emulator-5630.png`; G8441 (YOLO 0.1.4 arm64) 检测正常 (悬浮窗里画出 clock 69%, sports ball 67%, 截图 `detect-floaty-BH900ASK9E.png`), 但样例不结束 (85 秒内没有摘要, 之后用 `engines` 停止, 相机与悬浮窗都已释放): 每帧检测耗时长于帧间隔, 用于结束的 `setTimeout` 被饿死, 属宿主缺陷, 见附录 D Q43. `实时扫码` (prism-p3, 2026-11-06 12:12, G8441, 克隆宿主 6641 + 插件构建 12 + ML Kit Barcode 1.0.5, `--phases barcode`, 维护者把后置镜头对准二维码): PASS, 识别到 `hello world`, 第 1 帧命中即停 (`reason=script`, 丢帧 0). Q43 = (a) 宿主修复 (25196d7f34) 后, G8441 复跑 `物体检测画框` (prism-p3, 2026-11-06 12:56, 克隆宿主 6641 (base.apk md5 fab3debb9d035f1dcbdd900ae4a3a0bb, 含 Q41 / Q43 / Q44 修复) + 插件构建 16, YOLO 0.1.4 + yolo11n, `--phases detect --seconds 25`): PASS, 按时结束 (`reason=script`, 62 帧, 修复前 85 秒不结束), 无 ANR; 这次镜头对着暗处, 框 0 (检测与画框已在修复前的运行中验证: clock 69%, sports ball 67%), 截图 `detect-floaty-BH900ASK9E.png` 被这次覆盖 (不入库). 5 个样例都已在真机跑通, 本条勾选.

---

## P2: 相机核心

目标: 插件能独占打开相机, 在前台服务保护下持续出帧, 拍照, 响应 8 项控制并把预览渲染到宿主 Surface. 设备: 开发阶段任一真机 (API 31 XQ-AT72 为主), API 24 AVD 验证回退路径; 完整矩阵在 P4.

### P2.1 会话, 独占, 前台服务与权限 (D10, D17, D25, D30)

- [x] (插件) `ThreePrismCameraService` 实现 `ICameraPlugin` (`getInfo`, `getCapabilities`, `listCameras`, `openSession`, `requestPermission`), `CameraSessionBinder` 实现 `ICameraSession` 生命周期 (`close`, `getState`), 独占锁与 `CAMERA_BUSY` 占用者信息, `linkToDeath` 宿主回调, 前台服务启停 (API 34+ 经 `ThreePrismCameraStartActivity`), 通知渠道. 证据: 提交 hash; JVM 测试 (独占状态机); 真机: 第二个 `open` 得到 `CAMERA_BUSY` 且 `details.holder` 正确, 宿主被 `am kill` 后 1 秒内通知消失. 进度 (2026-11-06): 实现 `f79848a`; JVM `CameraExclusivityTest` 7, `CameraErrorPolicyTest` 6 例通过; 宿主 instrumentation `CameraPluginSessionDeviceTest` (隔离克隆宿主 6641, 由 `py .python/device_camera_core.py <serial>` 驱动, 每项一行 PASS / FAIL) 在 `AVD_API_26_PRISM` (API 26), `AVD_API_24` (API 24), `AVD_API_35_16K` (API 35, 16 KB 页) 上: busy PASS (第二个 `open` 得到 `CAMERA_BUSY`, `details.holder` 为第一个会话的 `holderLabel` `prism-busy-a`), kill PASS (宿主进程被 `am force-stop` 结束后通知分别在 0.11 / 0.06 / 0.14 秒内消失, API 35 为宿主修复 `c415503fcd` 后复测). 偏差: API 34+ 透明 Activity 启动前台服务要求绑定它的 AutoJs6 可见 (D25, 系统规则 BAL_ALLOW_VISIBLE_WINDOW); instrumentation 中的宿主没有自己的 Activity, 测试先把宿主启动器切到前台, 否则 `open` 报 `CAMERA_DISABLED`. 真机 (2026-11-06, G8441 `BH900ASK9E` (API 28, 隔离克隆宿主 6641 含宿主 `c82c5b27f9`, 插件构建 9 arm64 包), 维护者已在授权对话框点 "允许"): busy PASS (`details.holder` 为 `prism-busy-a`), kill PASS (`am force-stop` 0.08 秒返回, 通知在 0.77 秒内消失); 宿主样例 busy PASS (`holder=check-main.js`, 持有期间 `capture({ ui: true })` 同样得到 `CAMERA_BUSY`).
- [x] (插件) 未授权路径: `open` 报 `PERMISSION_DENIED`; `requestPermission` 启动授权 Activity 并等待结果 (授权对话框由维护者在手机上点). 证据: 真机一次授权流程记录. 进度 (2026-11-06): `AVD_API_35_16K` denied PASS (授权前 `open` 报 `PERMISSION_DENIED`), `AVD_API_26_PRISM` 宿主样例错误路径 denied PASS (`pm revoke` 后报 `PERMISSION_DENIED`, 只在自己的 AVD 上撤销); G8441 上 `requestPermission` 已拉起 `ThreePrismCameraPermissionActivity` 与系统授权对话框. 真机授权流程 (2026-11-06, G8441): 维护者在 `requestPermission` 拉起的系统对话框中点 "允许" 后, `dumpsys package` 显示插件 `android.permission.CAMERA: granted=true`, 之后 `open` 正常 (core 各项), 已授权时 `denied` 按设计 SKIP; 宿主收到的应答值未单独记录. 偏差: 拒绝分支只在模拟器上验证, 不在维护者手机上撤销权限.

### P2.2 连续帧: `ImageAnalysis` + 环形缓冲 + 回退 (D9, D18, D26, D33)

- [x] (插件) `FrameRing`: 3 块 `SharedMemory` (API 27+) 或 3 个临时文件 (API 24-26), 空闲 / 在用状态, `releaseFrame` 归还, 行紧排拷贝, 丢帧计数与 `onDropped`; `ImageAnalysis` RGBA_8888 + `STRATEGY_KEEP_ONLY_LATEST` + `ResolutionSelector` + 目标帧率; `startAnalysis` / `stopAnalysis` 可重复调用 (改分辨率时重建缓冲). 证据: 提交 hash; JVM 测试 (环形状态机, 跨度去填充); 真机 480p 30 秒: 帧率, 丢帧数, 插件进程 CPU (记入附录 F 性能表); API 24 AVD 回退路径出帧. 进度 (2026-11-06): 实现 `f79848a`; JVM `FrameRingTest` 11, `FramePacingTest` 8 例通过; 480p 30 秒 (`device_camera_core.py --steps perf`, 附录 F 性能表): 宿主修复 `c415503fcd` (见 P1.2) 后复测, `AVD_API_35_16K` `SharedMemory` 路径 29.2 fps, 宿主丢帧 15 (插件侧 0), 插件 CPU 11.7%; `AVD_API_26_PRISM` 临时文件回退 28.6 fps, 丢帧 0, 插件 CPU 27.4%; `AVD_API_24` 临时文件回退 23.3 fps (镜头实际 24 fps), 丢帧 0, 插件 CPU 16.6% (回退路径出帧). 修复前宿主丢弃第 0 块的帧, 环形缓冲只用 2 块, 当时为 29.7 fps / 宿主丢帧 0 (插件侧 6) / 11.4%, 18.8 fps / 0 / 51.0%, 23.1 fps / 0 / 18.1%. 三块 `SharedMemory` 的跨进程映射与复用见 P1.2 的 instrumentation. 真机 G8441 `BH900ASK9E` (API 28, 隔离克隆宿主 6641 含宿主 `c82c5b27f9`, 插件构建 9 arm64 包): 480p 30 秒 (`SharedMemory` 路径) 29.5 fps (884 帧), 宿主丢帧 0 (插件侧 0), 插件 CPU 40.8% (附录 F); transport 3 块各 1228800 B, `seq` 1 - 10, 块序号 0, 1, 2 轮转, 复用 4 / 3 / 3, 每帧像素非空, 经 `CameraSessionClient` 取到 640x480, 旋转 90 度, 丢帧 0.

### P2.3 拍照与取景页服务端 (D13, D31, D45)

- [x] (插件) `ImageCapture` (`CAPTURE_MODE_MINIMIZE_LATENCY`, `quality`, 闪光模式, 目标分辨率, 前置镜像选项), `CaptureWriter` (临时文件 + 只读 fd + 立即删除), `onCaptured` 元数据 (宽高, EXIF 旋转, 字节数, 镜像); `openCaptureUi` 启动取景页并把结果经同一回调回传 (界面本体在 P3.1). 证据: 提交 hash; 真机: 竖持 / 横持各拍一张, 宿主解码后方向正确, `{ path }` 文件用 `exiftool` 或 `ExifInterface` 读到方向. 进度 (2026-11-06): 实现 `f79848a`, 按附录 D Q13 / Q14 = (b) 返工为插件构建 9 (`OnImageCapturedCallback` 写盘, EXIF 补写与镜像). 返工前竖持拍照: `AVD_API_35_16K` / `AVD_API_26_PRISM` 得到 1280x960 JPEG 且 EXIF 方向 6, 宿主解码为 960x1280 (竖向); `AVD_API_24` 的相机 HAL 忽略 `JPEG_ORIENTATION`, 存下 EXIF 方向 1 的 1280x960, 宿主解码为横图. 返工后复测 (隔离克隆宿主 6641 含 `c82c5b27f9`, `device_camera_core.py --steps check,capture,frontcapture,front --orientation portrait`; 两台只读 AVD 重启后丢了授权, 用 `pm grant` 授予插件相机权限, API 35 另用 `appops` 允许宿主悬浮窗, 关机即还原): 三台 AVD 竖持拍照都是 EXIF 方向 6, 解码 960x1280 (API 35 133712 B, API 26 150084 B, API 24 149349 B, API 24 的方向 6 由插件补写), 宿主样例 `拍一张存文件` 在 API 24 也得到 960x1280; 前置 `mirror: true` 的 EXIF 方向是普通照片方向的水平镜像, 解码尺寸相同, 元数据 `mirrored=true` (frontcapture PASS: API 35 8 -> 7, 1440x1920; API 26 6 -> 5, 480x640; API 24 6 -> 5, 480x640). 真机 G8441 `BH900ASK9E` (API 28, 隔离克隆宿主 6641 含宿主 `c82c5b27f9`, 插件构建 9 arm64 包) 竖持: 3264x2448 JPEG (1790240 B) EXIF 方向 6, 解码 2448x3264 (竖向); 前置镜像 8 -> 7, 解码都是 2448x3264; captureui PASS; ui 样例旋屏后拍到 1280x960 (模拟器旋屏只改加速度计, 横持方向留真机看). `openCaptureUi`: 宿主可见时拉起取景页骨架, 返回后回调 `CANCELLED` (P3.1 前骨架不拍照): API 35 843 ms, API 26 4563 ms, API 24 4911 ms; API 24 上已 `finish()` 的 Activity 约 10 秒后才 `onDestroy`, 取消应答因此改在 `onPause` (isFinishing) 发出, `onDestroy` 兜底 (令牌幂等). 横持 (维护者横拿 G8441, `--steps capture,preview,controls --orientation landscape --expect-focus`): 3264x2448 JPEG (1054276 B) EXIF 方向 1, `rotationDegrees` 0, 解码 3264x2448 (横向); ui 样例转成横屏后 3 次拍照都是 3264x2448. 偏差: EXIF 方向由宿主 `JpegExifOrientation` 从回传的 JPEG 字节读取 (与 `{ path }` 写出的字节相同), 未对 `{ path }` 文件另跑 exiftool.

### P2.4 8 项控制与状态 (D14)

- [x] (插件) `setTorch`, `setFlashMode`, `setZoom` (ratio / linear), `setExposure`, `focusAt` (`FocusMeteringAction`, AF + AE 点, 自动取消时间, 同步等待结果不超过 5 秒), 镜头选择 / 分辨率 / 帧率在 `openSession` 与 `startAnalysis` 的 options 中; `getState` 与 `onStateChanged` (ready, torch, zoom, exposure, focus); 不支持的项报 `UNSUPPORTED` (如前置无闪光). 证据: 提交 hash; 真机逐项验证清单 (torch 亮灭可见, zoom 2x 画面变化, 曝光 +2 变亮, 点按对焦返回 focused). 进度 (2026-11-06): 实现 `f79848a`; JVM `CameraRequestsTest` 11, `CameraMathTest` 6 例通过; `device_camera_core.py --steps controls`: 模拟器相机无闪光灯, 变焦上限 1.0 (torch 与 zoom 2x 无从验证); 曝光 +2 EV: `AVD_API_24` 平均亮度 99.1 -> 178.4 (变亮), `AVD_API_26_PRISM` / `AVD_API_35_16K` 的 `virtualscene` 不响应曝光补偿 (未判定); `focusAt`: API 35 返回 focused, API 24 / 26 的 HAL 没有 AF 区域, 报 `UNSUPPORTED`. 真机 G8441 `BH900ASK9E` (API 28, 隔离克隆宿主 6641 含宿主 `c82c5b27f9`, 插件构建 9 arm64 包), 两次: torch 打开后平均亮度 70.6 -> 83.5 (第二次 72.2 -> 84.3), 变焦上限 8.0, 2x 生效 (`zoomRatio` 2.0), 曝光步长 1/3 EV, +2 EV (索引 6) 亮度 67.9 -> 75.5 (70.5 -> 82.5, 判定变亮); `focusAt` 两次都返回 `focused=false`: CameraX 的 3A 结果状态正常 (`Result3A status=0`), 但相机没有报告对焦锁定, 当时后置镜头对着一片无纹理的灰色平面 (预览截图), 判断为场景所致. 维护者让镜头对准有纹理的物体后重跑 (`--steps controls --expect-focus`) PASS: `focusAt` 返回 `focused=true`, 曝光 +2 EV 亮度 100.9 -> 189.9, 2x 生效; 这次画面明亮, torch 打开后亮度 112.0 -> 109.6, 没有变亮 (前两次暗场景可见变亮). 偏差: torch 与 zoom 2x 按亮度与相机报告的 `zoomRatio` 核对, 未目视比较画面.

### P2.5 预览 Surface (D3, D32)

- [x] (插件) `startPreview(Surface, options)` / `stopPreview`, `Preview.SurfaceProvider` 与 `setTargetRotation`, Surface 尺寸变化时重绑, Surface 失效时自动停预览不影响分析与拍照. 证据: 提交 hash; 真机: ui 脚本 `<surfaceview>` 与悬浮窗各显示预览, 旋屏后方向正确, 三个 use case 同时运行 30 秒无异常. 进度 (2026-11-06): 实现 `f79848a`; `device_camera_core.py --steps preview,front`: 预览 + 分析 + 拍照同时运行 30 秒, `AVD_API_35_16K` 860 帧 / 拍照 3 / 丢帧 30 (宿主修复后复测), `AVD_API_26_PRISM` 404 / 3 / 7, `AVD_API_24` 656 / 3 / 6; 释放 Surface 后 `previewing=false`, `analyzing=true`, 之后仍出帧 (5 帧) 且能拍照; 前置镜头 `previewMirrored=true` (附录 D Q6 = (b), Q11). 宿主 ui 样例 (`device_camera_samples.py --phases ui`, 布局标签 `<SurfaceView>`, 附录 D Q23): `AVD_API_26_PRISM` 30 秒 800 帧 (26.7 fps, 丢帧 11), `AVD_API_24` 669 帧 (22.3 fps, 丢帧 9), 旋屏前后都能拍照. 截图 `.bak/visual/1.0.0/preview-emulator-*.png`, `ui-preview-emulator-*.png` (不入库). 真机 G8441 `BH900ASK9E` (API 28, 隔离克隆宿主 6641 含宿主 `c82c5b27f9`, 插件构建 9 arm64 包): core preview (悬浮窗中的 `SurfaceView`, 720x960, 旋转 0) PASS, 三个用例同时运行 30 秒 773 帧 / 拍照 3 / 丢帧 53, 释放 Surface 后 `previewing=false`, `analyzing=true`, 之后 5 帧与 1 次拍照; 前置 `previewMirrored=true`; ui 样例 `<SurfaceView>` 30 秒 800 帧 (26.7 fps, 丢帧 57), 3 次拍照都是 2448x3264. 截图 `.bak/visual/1.0.0/preview-BH900ASK9E-portrait.png` (不入库). 旋屏 (维护者横拿手机, 系统自动旋转已开): ui 样例界面转为横屏 (1280x720) 后预览画面正立, 30 秒 810 帧 (27.0 fps, 丢帧 50), 3 次拍照都是 3264x2448; 悬浮窗预览所在的桌面锁定竖屏, 显示旋转保持 0, 横拿时画面随显示方向转 90 度, 属预期 (预览按显示旋转对齐, D32), 横持时 core preview 30 秒 777 帧 / 拍照 3 / 丢帧 53. 截图 `ui-preview-BH900ASK9E-landscape.png`, `preview-BH900ASK9E-landscape.png` (不入库; ui 样例的竖持截图已被这次复测覆盖). YOLO 样例的悬浮窗预览 (prism-p3, 2026-11-06, `--phases detect`): `AVD_API_26_PRISM` 与 G8441 都在悬浮窗中显示预览并画框 (截图 `detect-floaty-emulator-5630.png`, `detect-floaty-BH900ASK9E.png`, 不入库); 该样例在 G8441 上不结束属宿主缺陷 (附录 D Q43), 与预览无关.

### P2.6 契约一致性测试

- [x] (插件) JVM 测试镜像宿主常量 (`ThreePrismCameraContract` 与 `camera-api` 字面值一致), AIDL 顺序与宿主冻结表一致; `locks/host-api-aars.lock` 登记 `camera-api.aar` / `common-plugin-api.aar` 的来源提交与 SHA-256. 证据: 测试类名, 锁文件内容. 实施: 插件提交 `f79848a`; `CameraContractMirrorTest` 6 例 (插件, 会话与回调接口的事务顺序与协议 "Interfaces" 一节一致, 描述符与最低宿主构建号取自宿主契约, 声明的功能是协议功能且按协议顺序排列, `sharedMemory` 只在 API 27+ 声明, 引擎版本为构建所用的 CameraX 版本), `HostApiAarLockTest` 5 例 (锁列出的 AAR 与 SHA-256 一致, `libs/` 恰好只有锁定的 AAR, AAR 为不含原生代码的 release 字节码, `libs/README.md` 与 `THIRD_PARTY_NOTICES.md` 逐个记录, 构建脚本读取锁而不是列文件) 通过; 锁文件 `format=1`, 来源宿主 master `2a5bafc67a`, `camera-api.sha256=bd3d074a958de5a3a5f188136174951c94c764e984e693b3622c573987ff3a63`, `common-plugin-api.sha256=9849b15091dcb255cd1c47140790081eebc074aec2e3e9b28c18f12bf9ef4a6a`.

---

## P3: 独立界面

目标: 有启动器入口的取景页, 设置页与发行历史, 取景页兼作脚本一键拍照. 设备: API 33 (截图) 与任一其他真机.

### P3.1 取景页与一键拍照 (D6, D15, D31, D41)

- [x] (插件) `ThreePrismCameraCaptureActivity`: `PreviewView` (或 `CameraXViewfinder`, 附录 D Q3) 取景, 切换镜头, 手电, 变焦滑条 / 捏合, 点按对焦, 快门, 取消; 普通模式 (从启动器进入, 拍后显示结果与分辨率, 不保存) 与脚本模式 (`openCaptureUi` 进入, 拍后经回调回传并关闭, 取消回传 `CANCELLED`); 与脚本会话互斥 (`CAMERA_BUSY`). 证据: 提交 hash; 真机: 脚本 `camera.capture({ ui: true })` 两次 (拍 / 取消) 结果正确; 截图明暗各一. 实施: 插件提交 `9cbbe98` (构建 12): 取景页用 `CameraXViewfinder` (附录 D Q3 = (b)), 顶栏切换镜头 / 手电 / 设置, 底部变焦滑条 (与捏合同步), 点按对焦, 快门, 关闭; 普通模式拍后显示照片与分辨率 (不保存, "再拍一张" 回到取景); 脚本模式拍后经 `CaptureUiRegistry` 回传并关闭, 关闭按钮或返回键回传 `CANCELLED`; 镜像只用于前置镜头的取景 (附录 D Q6 = (b)); 相机被脚本会话占用时显示占用者与 "停止该脚本" (附录 D Q7 = (a), 经 `preemptForViewfinder` 发 `close` 原因 `pluginUi`); 插件声明 `captureUi`, 系统丢弃的启动 10 秒后以 `UI_REQUIRES_FOREGROUND` 过期; 取景页自己处理旋转 (`configChanges`), 拍照途中旋屏不丢结果; 应答脚本之前先解绑相机 (`releaseCamera`), 修复脚本紧接着再次打开取景页时的 `CAMERA_BUSY` 竞态; 对话框也开启 `testTagsAsResourceId`, 设备脚本能按测试标签找到其中的控件. 真机 G8441 `BH900ASK9E` (API 28, 隔离克隆宿主 6641, 插件构建 11 / 12 arm64 包, `device_camera_samples.py --phases viewfinder,busyui`): `camera.capture({ ui: true })` 拍到 2448x3264 (1384736 B), 取消返回 null; 占用时取景页显示占用者与 "停止该脚本", 点按后持有会话的脚本结束, 取景页接过相机并能拍照; `AVD_API_26_PRISM` 同样 PASS (960x1280). 设备脚本覆盖快门, 取消, 结果页, 占用与停止; 手电, 变焦, 捏合, 点按对焦与切换镜头没有自动断言 (截图中控件可见, 底层调用与 P2.4 相同, P2.4 已在真机验证). 截图 (`.bak/visual/1.0.0/`, 不入库): `p3.1-viewfinder-{light,dark}-emulator-5630.png`, `p3.1-result-{light,dark}-emulator-5630.png`, API 33 临时 AVD 的同名 `-emulator-5632` 两组 (该模拟器 virtualscene 静态拍照在 HAL 中超时, 属模拟器已知问题, 结果页截图为取景页 15 秒后的拍照失败提示, 失败路径正常), G8441 `p3.1-viewfinder-light-BH900ASK9E.png` / `p3.1-result-light-BH900ASK9E.png` (维护者的手机只截当前模式), `viewfinder-script-*.png`, `viewfinder-busy-*.png`, `viewfinder-after-stop-*.png`.

### P3.2 设置页

- [x] (插件) 按独立设置规范: 相机权限状态与申请, 默认镜头, 默认分析分辨率与帧率, 前台服务通知说明, 诊断 (CameraX 版本, 镜头列表与能力, 当前占用者), 语言, 发行历史入口. 证据: 提交 hash, 截图. 实施: 插件提交 `9cbbe98`: `ui/settings/SettingsActivity` 按 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`, 从取景页右上角进入: 相机组 (相机权限状态与申请, 通知权限, 默认镜头 (只作用于从启动器打开的取景页, 附录 D Q41), 默认分析分辨率与帧率 (只读显示宿主缺省值 640x480 / 设备帧率与脚本参数, 附录 D Q41), 前台服务通知说明), 外观组 (语言 -> 夜间模式 -> 主题色 -> 启动器图标, 先选后确定; 主题色的 HCT 色调规则用 MaterialKolor 4.1.1, 已登记 `THIRD_PARTY_NOTICES.md`), 诊断组 (CameraX 版本, 镜头列表与能力对话框, 当前占用者), 关于组 (版本, 发行历史, 检查更新); 文案 11 个目录. JVM: `AppearanceTest` 7 例, `StringResourceParityTest` 6 例, `ApplicationTextPunctuationTest` 2 例通过 (全部 95 例通过). 截图 (不入库): `p3.2-settings-top-*`, `p3.2-settings-bottom-*`, `p3.2-lenses-*` (`AVD_API_26_PRISM` 与 API 33 临时 AVD 明暗各一, G8441 亮色). 附录 D Q41 = (b) 落实 (插件构建 16, 2026-11-06): 默认镜头, 默认分析分辨率与默认帧率三项改为可编辑, 作用于从启动器打开的取景页与省略对应键的脚本请求 (对脚本生效需要 prism-host-fixes 的宿主构建; 宿主发送了的键总是优先); 关于组加 "自动检查更新" 与 "已忽略的版本" (附录 D Q42 = (b)).

### P3.3 发行历史与更新检查

- [x] (插件) `.changelog` 渲染与官方索引更新检查 (与 Macaw P5.4 同构). 证据: 提交 hash, 截图. 实施: 插件提交 `9cbbe98`: `ui/release/ReleaseHistory` 按界面语言读取随包的 `assets/doc/CHANGELOG-<语言>.md` (由 `.changelog` 10 种语言生成, 其他语言用英文), 按版本列出日期与带标签的条目; 更新检查按附录 D Q42 = (a) 先行: 不加网络权限, 设置页 "检查更新" 用浏览器打开 GitHub Releases 页面 (与 Macaw 的应用内检查不同构, 待维护者确认). 截图 (不入库): `p3.3-release-*` (`AVD_API_26_PRISM` 与 API 33 临时 AVD 明暗各一, G8441 亮色). 附录 D Q42 = (b) 落实 (插件构建 16, 2026-11-06): 移植 Macaw P5.4 的应用内检查 (`ui/update/AppUpdate.kt`: GitHub `releases/latest` + 语义化版本比较 (附录 D Q45), 连接 15 秒 / 读取 20 秒 / 总计 30 秒超时, 不跟随重定向, 响应上限 256 KiB, 404 视为尚无发行版; 有新版时显示更新说明, 可打开发行页, 稍后再说或忽略该版本), 自动检查在打开设置页时运行, 间隔 12 小时, 可关闭 (附录 D Q46); Manifest 加 `INTERNET` 与 ISRG Root X1 网络安全配置 (见 D25).

### P3.4 图标核对

- [x] (插件) 启动器, 插件中心, 最近任务三处在明暗模式下核对 P0.3 产物; 不合格时调整 `recipe.json` 的 geometry 并重跑 `--check`. 证据: 截图路径. 核对 (prism-p3, 2026-11-06, 插件构建 12, `device_camera_screens.py`): 三处图标都合格, 不需要调整 `recipe.json`, `generate_icon_studio.py --check` 通过 (28 个资源). API 33 临时 AVD (`emulator-5632`): 最近任务与插件中心在明暗下都换成对应变体, 插件中心一行为 "3-Prism Camera 1.0.0 (12)"; 启动器 (Launcher3) 的图标缓存不随 `cmd uimode night` 刷新, 在夜间模式下重装后显示暗色变体, 两种变体都正确. `AVD_API_26_PRISM`: 最近任务与启动器明暗正确, 宿主插件中心不随运行时的 uimode 切换 (明暗两张截图相同), 插件中心的暗色以 API 33 与 P0.3 的 XQ-AT72 截图为准. G8441 亮色最近任务. 截图 (`.bak/visual/1.0.0/`, 不入库): `p3.4-launcher-*`, `p3.4-recents-*`, `p3.4-plugin-center-*`, `plugin-center-QV710AF65F-system-dark.png`, `launcher-QV710AF65F-dark.png`.

---

## P4: 验证

目标: CI 与设备矩阵证明 1.0.0 的核心路径在 API 24 - 35 可用, 后台与熄屏行为逐台记录, 性能有基线.

### P4.1 CI 模拟器相机 (D20, D35)

- [ ] (插件) `CameraSmokeTest` 完整用例 (打开 / 10 帧 `seq` 递增且缓冲序号循环 / 拍照 JPEG 可解码 / 关闭后再开成功), `connected-avd` job 在远端通过; 本地 `ci_connected_tests.py` 等价运行. 证据: 远端运行链接, 本地输出.

### P4.2 真机矩阵 (D36)

- [ ] (测试) 附录 F 每台设备跑 R3 冒烟清单, 记录实际分辨率 / 帧率 / 丢帧 / 拍照延迟; OnePlus (ColorOS) 另记录激活与前台服务通知行为. 证据: 附录 F 表格填满, 未在线设备写 "未执行".

### P4.3 后台, 熄屏与宿主被杀 (D10)

- [ ] (测试) 每台: 宿主退后台 30 秒持续出帧; 熄屏 30 秒持续出帧; 宿主已在后台时 `open` 的行为 (预期 API 33 `CAMERA_DISABLED`, API 35 需透明 Activity 路径); `am kill` 宿主后插件 1 秒内释放相机与通知. 证据: 每台的 `dumpsys activity services` 能力标记 (`--C` 或等价) 与日志片段.

### P4.4 性能基线

- [ ] (测试) API 28 / 31 / 35 各一台: 480p 与 720p 的 30 秒分析 (帧率, 丢帧, 宿主 `onFrame` 处理耗时 P50 / P95, 两进程 CPU 与内存增量), 拍照延迟 (调用到 `ImageWrapper` 返回), 取景页冷启动时间. 目标: 480p 不低于 25 fps 且宿主处理 P95 不超过 5 ms; 不达标时记录并登记待决. 证据: 附录 F 性能表.

### P4.5 识别联动样例实测 (D7)

- [ ] (测试) P1.6 的 OCR / 条码 / YOLO 三个样例在 API 31 与 35 各跑一次, 记录从出帧到首次命中的时间. 证据: 数字与日志片段.

---

## P5: 文档, 声明, 四仓库同步, 样例与发布

### P5.1 文档 (AutoJs6-Documentation) (D11, D37)

- [ ] (文档) `api/camera.md`: 模块方法, 会话对象, options, 事件, 错误码, 示例; 首屏 "帧的生命周期" 提示框 (回调期间有效, 返回即回收, 要保留就 `img.clone()`, 超时自动丢帧); 10 语言目录按仓库惯例. 证据: 文档仓库提交 hash (本地), 维护者确认提示框文案.

### P5.2 声明 (AutoJs6-TypeScript-Declarations, Ace Editor, Offline Docs)

- [ ] (文档) `aj6-int-camera.d.ts` 与 `autojs6-node/camera.d.ts` (JSDoc 注明生命周期), Ace Editor 与 Offline Docs 同步副本. 证据: 各仓库提交 hash (本地).

### P5.3 README, changelog, 兼容矩阵与截图

- [ ] (插件) R1 / R2 全部条目; README 的权限与后台章节解释前台服务通知; 兼容矩阵 (API 24 - 35, 无 ABI 限制). 证据: 提交 hash, `generate_markdown.py` 输出.

### P5.4 1.0.0 发布

- [ ] (发布) R3 / R4 / R5; `requiresHostVersion` 回填 (D39); 是否等宿主 6.8.0 公开按附录 D Q5. 证据: tag, Release 链接, 索引提交 hash, SHA-256.

---

## P6: 1.1.0 候选

### P6.1 录像 (契约 v2)

- [ ] (宿主) `camera-api` v2: `ICameraSession` 追加 `startRecording(options, callback)` / `pauseRecording` / `resumeRecording` / `stopRecording`, 能力位 `recording`; 脚本 `session.record(path, options)` 返回录制句柄 (`pause` / `resume` / `stop`), 事件 `recording` (进度, 字节数); `CameraContract.CONTRACT_VERSION = 2`, 旧插件按能力位降级.
- [ ] (插件) `camera-video:1.6.x` `VideoCapture` + `Recorder` (Media3 Muxer), 质量选择, 音频开关 (需 `RECORD_AUDIO`, 默认关), 输出到宿主传入的 fd; 前台服务类型增加 `microphone` (录音时).
- [ ] (测试) 设备矩阵补录像用例; (文档) 文档与声明更新.

### P6.2 其他候选

| 候选 | 说明 | 依赖 |
| --- | --- | --- |
| RAW / DNG 输出 | CameraX 1.5+ `OUTPUT_FORMAT_RAW`; 文件大 (20 - 50 MB), 仅部分设备支持 | 用户需求 |
| 厂商扩展 (HDR / 夜景 / 人像) | `camera-extensions`, 仅拍照, 设备差异大 | 设备矩阵支持情况 |
| 条码扫描取景页 | 取景页内直接调条码插件并显示框 (本插件与条码插件直连, 推翻 D7 的一部分) | 维护者决定 |
| 连拍与定时拍照 | 脚本层已可实现, 插件内做可降低延迟 | 性能基线 |
| 多摄同开 | `ConcurrentCamera`, 设备支持稀少 | 用户需求 |
| 外接 USB 相机 (UVC) | CameraX 不支持, 需另一套实现 | 不做 |

### P6.3 1.1.0 发布

- [ ] (发布) R1 - R5.

---

## 附录 A. 脚本 API 草案

### A.1 模块方法

| 方法 | 返回 | 说明 |
| --- | --- | --- |
| `camera.isAvailable()` | `boolean` | 插件已安装, 已启用且版本兼容 |
| `camera.capabilities()` | `object` | `{contractVersion, features[], engineVersion, permissionGranted, sharedMemory, cameraCount}` |
| `camera.requestPermission()` | `boolean` | 打开插件授权页并等待结果; 需宿主可见, 否则 `UI_REQUIRES_FOREGROUND` |
| `camera.list()` | `CameraInfo[]` | `{id, facing: "back" / "front" / "external", hasFlash, sensorRotation, zoomRange: [min, max], exposureRange: [min, max], exposureStep, analysisResolutions: string[], captureResolutions: string[]}` |
| `camera.open(options?)` | `CameraSession` | 独占打开; options 见 A.2; 省略的 `facing` / `resolution` / `fps` 取插件设置页的默认值 (附录 D Q41 = (b)) |
| `camera.capture(options?)` | `ImageWrapper` 或 `null` | 一步到位: 无会话时临时 open + capture + close; `{ ui: true }` 走取景页, 取消返回 `null`; `captureAsync` 返回 Promise |

### A.2 会话

```text
camera.open 的 options:
facing        "back" / "front"             镜头朝向; 与 id 二选一; 省略时取插件设置页的默认镜头 (初始 "back", 附录 D Q41 = (b))
id            string                       camera.list() 的 id
resolution    string, 如 "640x480"         分析目标分辨率 (D18, 上限 D43); 省略时取设置页的默认分析分辨率 (初始 "640x480")
fps           number                       目标帧率; 省略时取设置页的默认帧率 (初始由设备决定)
upright       true (默认)                  帧按屏幕方向旋正 (D19)
mirror        false (默认)                 帧与拍照镜像 (两者一致); 前置预览总是镜像, 不受此项影响 (D19, 附录 D Q6)
flashMode     "auto" (默认) / "on" / "off" 拍照闪光

CameraSession:
preview(view, options?)          view: ui 脚本或悬浮窗中的 SurfaceView; 返回 this
stopPreview()
analyze(callback, options?)      callback(img: ImageWrapper, meta); options 可覆盖 resolution / fps / upright / mirror; 返回 this
stopAnalysis()
capture(options?)                {path, quality (1-100, 默认 95), flashMode, resolution, mirror}; 返回 ImageWrapper; captureAsync 返回 Promise
torch(enabled)                   返回 this
flashMode(mode)                  返回 this
zoom(ratio) / zoomLinear(0..1)   返回 this; zoomState() 返回 {ratio, min, max, linear}
exposure(index)                  返回 this; exposureState() 返回 {index, min, max, step}
focusAt(x, y, options?)          归一化坐标 (0..1, 相对旋正后的画面); options {autoCancelMillis, size}; 返回 boolean (是否对上焦), 最多等 5 秒
state()                          {open, previewing, analyzing, id, facing, resolution, fps, previewMirrored, torch, flashMode, zoom, exposure, frames, dropped, holder}
on(event, listener) / off        事件: "frame" (img, meta), "dropped" (count, lastSeq), "error" (err), "close" (reason)
close()                          幂等
isClosed()
```

帧 `meta`: `{seq, width, height, rotationDegrees, upright, mirrored, timestamp, facing, dropped}`.

### A.3 帧的生命周期 (D11, 文档首屏)

- `analyze` 回调里的 `img` 只在回调执行期间有效; 回调返回后宿主立即回收它并把缓冲还给插件, 再访问会抛 "图像已回收".
- 要在回调之外使用, 调 `img.clone()` 得到独立副本, 用完自己 `recycle()`.
- 回调耗时超过帧间隔时, 新帧会被丢弃而不是排队; `session.state().dropped` 与 `"dropped"` 事件可以看到数量.
- `capture` 返回的 `ImageWrapper` 不受此限制, 与 `images.captureScreen` 返回值相同, 由脚本或脚本退出时回收.

### A.4 错误

`CameraError` (继承 `Error`): `code` 见 D29, `message` 为 10 语言文案, `details` 可含 `holder` (占用者), `cameraId`, `limit`.

### A.5 示例

```javascript
// 1. 拍一张存文件
let img = camera.capture({ path: files.join(files.getSdcardPath(), "prism.jpg") });
toastLog(img.getWidth() + "x" + img.getHeight());
img.recycle();

// 2. 实时扫码 (帧只在回调里有效)
let session = camera.open({ resolution: "1280x720" });
session.analyze((img, meta) => {
    let codes = barcode.detect(img);
    if (codes.length > 0) {
        toastLog(codes[0].rawValue);
        session.close();
    }
});
events.on("exit", () => session.close());

// 3. 保留一帧到回调之外
let kept = null;
session.analyze((img) => { if (!kept) kept = img.clone(); });

// 4. 预览 + 手电 + 变焦
"ui";
ui.layout(<frame><surfaceview id="cam" w="*" h="*"/></frame>);
let s = camera.open().preview(ui.cam).torch(true).zoom(2);

// 5. 让用户在取景页拍一张
let shot = camera.capture({ ui: true });
if (shot === null) toastLog("已取消");
```

---

## 附录 B. 契约草案

### B.1 `plugin-api/camera-api`

```text
CameraPluginActions   CAMERA = "org.autojs.plugin.CAMERA", SERVICE_CATEGORY = "camera"
CameraPluginIds       OFFICIAL_PLUGIN_ID = "three-prism-camera", OFFICIAL_PACKAGE = "io.github.supermonster003.autojs6.plugin.three.prism.camera", ENGINE = "camera", CATEGORY = "camera", REQUIRED_HOST_VERSION_CODE
CameraContract        CONTRACT_VERSION = 1, MIN / MAX; KEY_CONTRACT_VERSION, KEY_ERROR_CODE, KEY_ERROR_MESSAGE; FACING_BACK / FRONT / EXTERNAL; FLASH_AUTO / ON / OFF; FORMAT_RGBA_8888; STATE_*; CLOSE_REASON_HOST / SCRIPT / PLUGIN_UI / CAMERA_DISCONNECTED / POLICY / ERROR
CameraCapabilityKeys  cameraContractVersion, cameraFeatures, cameraPermissionGranted, cameraEngineVersion, cameraCount, cameraSharedMemory
CameraLimits          D43 的全部数值
CameraOptionKeys      facing, cameraId, width, height, fps, upright, mirror, flashMode, quality, path 不过 Binder (宿主侧), zoomRatio, zoomLinear, exposureIndex, focusX, focusY, focusSize, autoCancelMillis, surfaceWidth, surfaceHeight, surfaceRotation, holderLabel
CameraResultKeys      session (IBinder), buffers (SharedMemory[]), width, height, rowStride, format, actualFps, cameras (Bundle[]), state 字段
CameraFrameKeys       seq, bufferIndex, width, height, rotationDegrees, timestampNanos, facing, frameFd (API 24-26), droppedCount, lastSeq
CameraErrorCodes      D29 的集合 + encode / decode

interface ICameraPlugin {
    PluginInfo getInfo();
    Bundle getCapabilities();
    Bundle listCameras();
    Bundle openSession(in Bundle request, in ICameraSessionCallback callback);     // 应答含 session IBinder 或错误
    Bundle openCaptureUi(in Bundle request, in ICameraCaptureCallback callback);   // 启动取景页 (D31)
    Bundle requestPermission(in Bundle request);                                   // 启动授权页, 结果由宿主再查 getCapabilities
}

interface ICameraSession {
    Bundle startPreview(in Surface surface, in Bundle options);
    Bundle stopPreview();
    Bundle startAnalysis(in Bundle options, in ICameraFrameCallback callback);     // 应答含 buffers / width / height / rowStride
    Bundle stopAnalysis();
    oneway void releaseFrame(int bufferIndex, long seq);
    Bundle capture(in Bundle options, in ICameraCaptureCallback callback);
    Bundle setTorch(boolean enabled);
    Bundle setFlashMode(String mode);
    Bundle setZoom(in Bundle request);
    Bundle setExposure(int index);
    Bundle focusAt(in Bundle request);                                             // 同步, 最多 5 秒, 应答含 focused
    Bundle getState();
    Bundle close();
}

oneway interface ICameraFrameCallback {
    void onFrame(in Bundle frame);
    void onDropped(in Bundle info);
}

oneway interface ICameraCaptureCallback {
    void onCaptured(in ParcelFileDescriptor jpeg, in Bundle meta);
    void onError(in Bundle error);
}

oneway interface ICameraSessionCallback {
    void onStateChanged(in Bundle state);
    void onError(in Bundle error);
    void onClosed(in Bundle reason);
}
```

每个应答 Bundle 都带 `contractVersion`; 错误以 `errorCode` + `errorMessage` 表达, 不抛跨进程异常.

### B.2 插件 Manifest 片段

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />

<service android:name=".ThreePrismCameraService" android:exported="true" android:permission="org.autojs.permission.PLUGIN">
    <intent-filter>
        <action android:name="org.autojs.plugin.CAMERA" />
        <category android:name="camera" />
    </intent-filter>
</service>
<service android:name=".ThreePrismCameraForegroundService" android:exported="false" android:foregroundServiceType="camera" />
<activity android:name=".ThreePrismCameraStartActivity" android:exported="false" android:theme="@style/Theme.ThreePrismCamera.Transparent" android:excludeFromRecents="true" />
```

---

## 附录 C. 帧传输协议 (D9, D26)

1. 宿主调 `startAnalysis(options, callback)`; 插件绑定 `ImageAnalysis` 并取得实际分辨率 `W x H`, 分配 3 块 `SharedMemory` (名 `prism-frame-<i>`, 大小 `W * 4 * H`), 应答 `buffers`, `width`, `height`, `rowStride = W * 4`, `format = RGBA_8888`, `actualFps`.
2. 宿主对每块 `mapReadOnly()`, 保留 `ByteBuffer`; API 24-26 应答不含 `buffers`, 宿主按 `frameFd` 路径工作.
3. 插件 `Analyzer` 收到 `ImageProxy`: 取空闲块 (无空闲则 `close()` 该帧并 `dropped++`), 按行拷贝 RGBA 平面 (去掉行跨度填充), 标记在用, 发 `onFrame({seq, bufferIndex, width, height, rotationDegrees, timestampNanos, facing})`; API 24-26 写 `cacheDir/frames/<bufferIndex>.rgba` 并附只读 `frameFd`.
4. 宿主 `copyPixelsFromBuffer` 到 `Bitmap` (必要时旋正 / 镜像到新 Bitmap), 包成 `ImageWrapper` (oneShot), 投递脚本主循环; 宿主队列深度 1, 回调未返回时新帧替换旧帧 (旧帧立即 `releaseFrame`, `dropped++`).
5. 回调返回: `recycle()` 该帧, `releaseFrame(bufferIndex, seq)`; 插件把块标为空闲. `seq` 不匹配 (迟到的 release) 忽略.
6. `dropped` 每累计 30 或 `stopAnalysis` 时经 `onDropped({droppedCount, lastSeq})` 通知; 宿主侧丢帧与插件侧丢帧合计为 `state().dropped`.
7. `stopAnalysis` / `close` / 宿主死亡: 插件 unbind `ImageAnalysis`, 关闭 `SharedMemory`; 宿主 `unmap` 并丢弃映射. 重新 `startAnalysis` 重新分配 (分辨率可能不同).
8. 时序约束: 宿主在 `releaseFrame` 之前不得再读该块; 插件在收到 `releaseFrame` 之前不得写该块. 两侧都以 (`bufferIndex`, `seq`) 校验.

---

## 附录 D. 待决事项

| 编号 | 事项 | 选项 | 拍板时点 |
| --- | --- | --- | --- |
| Q1 | 安装向导分类 | (a) `IMAGE` (与 OCR / 条码 / YOLO 同列, 相机是它们的输入源, 宿主零枚举改动; 推荐); (b) `TOOLS` (与 3-Macaw Speech 同列); (c) 新分类 `CAMERA` 或 `MEDIA` (需宿主枚举, 10 语言标题, 插件中心分组) | 已拍板 (2026-11-05): (a) `IMAGE`, P1.3 实施 |
| Q2 | API 24-26 回退路径的覆盖范围 | (a) 只在 API 24 AVD 验证, 文档标注 "API 24-26 帧率受限" (推荐; 本地无 API 25 / 26 设备); (b) 另建 API 26 AVD 验证; (c) 最低宿主 API 改为 27, 放弃回退 (推翻 D9 一半, 插件 minSdk 与宿主 24 不一致) | 已拍板 (2026-11-05): (b) 另建 API 26 AVD (非推荐项); P2.2 在 `AVD_API_24` 与新建的 `AVD_API_26_PRISM` (google_apis x86_64, 后置 virtualscene) 上都验证回退路径, 该 AVD 保留到 P4.2 之后, 由维护者决定何时删 |
| Q3 | 取景页技术 | (a) `camera-view` `PreviewView` 包进 `AndroidView` (推荐; 1.6.2 稳定, 文档多); (b) `camera-compose` `CameraXViewfinder` (纯 Compose, 需确认 1.6.2 内稳定且体积可接受) | 已拍板 (2026-11-05): (b) `CameraXViewfinder` (非推荐项, 维护者选定, 不自行退回); P0.2 实测 release +590 B, 上限 +82510 B (D21) |
| Q4 | Node / Bun 连续帧不达标时的策略 | (a) 按 `fps` 限流到实测可承受值并在文档标注 (推荐); (b) Node / Bun 只提供 `capture` 与控制, 不提供连续帧; (c) 为运行时另开共享内存通道 (桥协议改动大) | 不需拍板 (2026-11-06 实测达标, prism-facade): 30 秒连续帧中 Node 在 XQ-AT72 480p / 720p 都是 30 fps (丢 1 帧), 与同机 Rhino 对照相同; Bun 在 Redmi 22120RN86C 480p 14.3 fps / 720p 13.9 fps, 同机 Rhino 14.3 / 14.2 fps, 瓶颈是该机相机出帧而不是运行时桥, 限流没有收益, 未实施. 未测: Bun 在 30 fps 相机上的上限 (见附录 F "运行时连续帧吞吐"). 宿主桥 `589d727793`, facade Node `2a8f2ac2` / Bun `cbbaa7c` |
| Q5 | 1.0.0 是否等宿主 6.8.0 公开 | (a) 不等, 宿主契约落本地 master 即发布插件, `requiresHostVersion` 指向该构建 (推荐; Macaw 1.0.0 先例, 维护者 2026-10-20 决定不等); (b) 等宿主公开后再发 | 已拍板 (2026-11-05): (a) 不等宿主 6.8.1 公开即发布 (6.8.0 已于 2026-11-03 公开, 问题顺延到 6.8.1) |
| Q6 | 前置镜头默认是否镜像 | (a) 不镜像 (推荐, D19 曾按推荐先行; 与拍照一致, 识别不受影响); (b) 预览镜像但帧与拍照不镜像 (系统相机习惯, 需在预览路径单独处理); (c) 全部镜像 | 已拍板 (2026-11-05): (b) 预览镜像, 帧与拍照不镜像 (非推荐项); D19 已改写, P2.5 实施 (`MIRROR_MODE_ON_FRONT_ONLY`, `getState.previewMirrored`) |
| Q7 | 取景页 `CAMERA_BUSY` 时的行为 | (a) 显示 "脚本 xxx 正在使用相机" 并提供 "停止该脚本" 按钮 (推荐; 需宿主提供停止入口, 经 `ICameraSessionCallback.onClosed(PLUGIN_UI)` 通知脚本); (b) 只提示, 不提供按钮 | 已拍板 (2026-11-05): (a) 取景页提示 + "停止该脚本" 按钮, 契约保留 `CLOSE_REASON_PLUGIN_UI` (P1.1 协议语义), 宿主停止动作在 P1.4 |
| Q8 | 依赖自带原生库时的 APK 形态 (P0.2 发现 `camera-core` 与 Compose `graphics-path` 各带原生库, 4 ABI 共 12 个文件 202140 B) | (a) 单 APK, 白名单校验原生库集合 / STORED / 16 KB 对齐, `supportedAbis` 显式空数组 (推荐, 已按推荐先行); (b) 按 ABI 拆成 4 个 APK (每包约省 150 KB, 要改 `splits`, `supportedAbis`, 发布文件名, 索引清单与 CI 校验) | 已拍板 (2026-11-05): (b) 按 ABI 拆包 (非推荐项, 推翻 P0.2 的 (a) 先行); 按规范 5.5 出 4 个单 ABI 包 + Universal, D22 / R4 / R5 已改写, 由 prism-p2 返工 P0.2; 返工提交 `c4d3a71` |
| Q9 | 宿主 `CameraPluginIds.REQUIRED_HOST_VERSION_CODE` 与插件 `requiredHostVersion` 的临时值 (P1.3, D39) | (a) 6641 (推荐, 已按推荐先行; 与 3-Chime Alarm T33 / Java Libraries 一致: 6640 是已公开且不含相机契约的 6.8.0 构建, 6.8.0 宿主不应接受本插件; 代价: 真机联调要用 `VERSION_BUILD` 临时 6641 的隔离克隆宿主 (Chime Q12 (a) 先例, 不提交), P0 debug 包 (6640) 在 P2.1 前被宿主相机校验拒绝); (b) 6640 (本地 debug 宿主直接可用, 但 6.8.0 正式宿主也满足门槛, 装上后才在调用时失败) | 已拍板 (2026-11-05): (a) 保持 6641; 插件三处在 P2.1 改为 6641, 真机联调用 `VERSION_BUILD` 临时 6641 的隔离克隆宿主 (`E:/.cache/autojs6-prism-host`, 不提交) |
| Q10 | 安装向导在插件发布前不显示本插件 (P1.3 发现: `PluginInstallWizardLoader.buildItems` 只为官方索引已收录的目录条目建行; 3-Chime Alarm 条目同此) | (a) 保持现状, P1.3 以 JVM 测试 (索引收录后列在 `IMAGE` 下) 与真机上的插件中心行为证据, 向导行的真机显示改到 P5.4 登记索引后用 `device_registration_check.py --expect-wizard-row` 复核 (推荐, 已按推荐先行; 宿主零改动); (b) 向导对已安装但索引未收录的目录条目也建行 (改宿主 loader 与向导测试, 约 1 小时, 影响所有尚未入索引的官方插件); (c) 现在用临时索引源在 debug 宿主上验证 (改设备上的索引缓存, 不入库, 约半小时, 下次联网刷新即失效) | 已按推荐先行待确认 (2026-11-05); 影响 P1.3 证据, P5.4; 改选 (b) 返工约 1 小时 (宿主), (c) 约半小时 (仅证据) |
| Q11 | 预览镜像状态的上报方式 (P2.5 发现: D19 要求 `getState` 报告 `previewMirrored`, 但宿主契约 `CameraResultKeys` 没有这个键) | (a) 插件在 `getState` 与 `onStateChanged` 的 Bundle 里另放布尔键 `previewMirrored` (取自 `SurfaceRequest.TransformationInfo.isMirroring`), 宿主按协议忽略未知键, 由宿主脚本层决定是否转给 `state()` (推荐, 已按推荐先行; 契约零改动); (b) 宿主契约新增 `CameraResultKeys.STATE_PREVIEW_MIRRORED` (改 `camera-api`, 重新暂存 AAR 并更新 `locks/host-api-aars.lock`, 插件改用常量); (c) 不上报, 文档写明前置预览总是镜像 | 已拍板 (2026-11-06): (b) 宿主契约新增 `CameraResultKeys.STATE_PREVIEW_MIRRORED` (非推荐项). 实施: 宿主 `c82c5b27f9` (契约常量与 `CameraContractTest`, 脚本层 `state()` 与 `CameraScriptValuesTest`, 设备测试, 协议文档状态键一节); `camera-api.aar` 按该提交重新暂存 (`assembleRelease`, SHA-256 `9063b4a9019b896119f5688d5d2b0142d240737869fa534102079706cf1a1200`, `locks/host-api-aars.lock` 与 `libs/README.md` 已更新), 插件 `getState` 改用常量 (插件构建 9); D19 已改写 |
| Q12 | 通知权限 `POST_NOTIFICATIONS` (P2.1 发现: API 33+ 未授予时前台服务照常运行, 但通知栏不显示 "相机使用中" 通知, 与 D25 "会话打开即显示通知" 的意图不符; D25 原文的权限清单不含它) | (a) Manifest 声明 `POST_NOTIFICATIONS`, `ThreePrismCameraPermissionActivity` 在 API 33+ 与 `CAMERA` 一并申请, 拒绝不影响相机 (推荐, 已按推荐先行; README 安全一节与 changelog 10 语言已写明); (b) 不申请, 严格按 D25, API 33+ 上通知只出现在系统的前台服务管理器里 | 已决 (2026-11-06, 维护者选 (a)); 保持现状, D25 已注明 |
| Q13 | 拍照的落盘方式 (P2.3 实现时的偏差: 协议 "Capture" 一节与 D45 写的是 `takePicture(OnImageCapturedCallback)` 内存回调后插件自己写文件) | (a) `takePicture(OutputFileOptions)` 由 CameraX 直接写 `cacheDir/captures/<id>.jpg`, `ImageCapture.Metadata.isReversedHorizontal = mirror` 让 CameraX 按镜像翻转 EXIF 方向, 之后照旧只读 fd + 立即删除 + `onCaptured(fd, meta)` (推荐, 已按推荐先行; JPEG 不经插件堆内存, 不用自写 EXIF; 交给宿主的 fd 与元数据与协议完全相同); (b) 按协议原文改回内存回调, 插件复制 `ImageProxy` 的 JPEG 缓冲写文件, 镜像时用 `ExifInterface` 自行翻转方向 | 已拍板 (2026-11-06): (b) 改回内存回调 (非推荐项). 实施 (插件构建 9): `takePicture(OnImageCapturedCallback)`, `CaptureWriter.write` 把 `ImageProxy` 的 JPEG 缓冲写到 `cacheDir/captures/<id>.jpg` 后立即关闭图像, `mirror: true` 时用 `androidx.exifinterface` 按自有映射表 (`ExifOrientation.mirroredOf`, JVM 测试覆盖) 改写方向, 不用 `ExifInterface.flipHorizontally` (它按传感器坐标翻转, 5 / 6 / 7 / 8 的结果与协议不同); 之后照旧只读 fd + 立即删除 + `onCaptured(fd, meta)`. 宿主 `c82c5b27f9` 把协议 "Capture" 一节恢复为原文并加设备测试 `frontCaptureIsMirrored` (插件 `device_camera_core.py --steps frontcapture`). 复测见 P2.3 进度; D45 已改写 |
| Q14 | API 24 模拟器拍照方向 (P2.3 发现: `AVD_API_24` 的相机 HAL 忽略 `JPEG_ORIENTATION`, 竖持拍到 1280x960 且 EXIF 方向 1, 宿主解码后是横图; `AVD_API_26_PRISM` / `AVD_API_35_16K` 正常, EXIF 方向 6, 解码为 960x1280) | (a) 视为模拟器 HAL 缺陷, 插件不改, P4.2 在真机 (含 API 24 - 26 能找到的设备) 复核方向, 文档不另作说明 (推荐, 已按推荐先行; 插件零改动); (b) 插件兜底: 写盘后读 EXIF, 方向为 1 而传感器方向与目标旋转要求转 90 / 270 度时补写方向 (加 `androidx.exifinterface` 依赖与 `THIRD_PARTY_NOTICES.md` 条目, 约 1 - 2 小时; 只能识别 90 / 270 度, HAL 漏掉的 180 度无法从宽高判断) | 已拍板 (2026-11-06): (b) 插件兜底 (非推荐项). 实施 (插件构建 9): 拍照前取 `cameraInfo.getSensorRotationDegrees(targetRotation)`, 写盘后 EXIF 方向为 1 或缺失, 期望旋转为 90 / 270 度且像素仍宽大于高 (传感器的横向形状) 时补写 6 / 8, 再按 `mirror` 镜像 (`ExifOrientation.completed`, JVM 测试覆盖); HAL 漏掉的 180 度不改变宽高, 无法识别, 协议 "Capture" 一节与 changelog 已如实写明; 依赖 `androidx.exifinterface:exifinterface` 1.4.2 (CameraX 1.6.2 原已传递引入, 现改为直接依赖, `THIRD_PARTY_NOTICES.md` 已注明). 复测见 P2.3 进度 |
| Q21 | P1.5 的范围 (P1.5 发现: Node 与 Bun 的 `camera` facade 在 Node / Bun 运行时插件仓库, 不在宿主; 条目要求的 "Node 与 Bun 各一个样例" 和 480p / 720p 吞吐实测都要先有 facade) | (a) P1.5 只做宿主部分 (`RuntimeCameraBridge`, `NodeBridgeProtocol` `CAMERA` 域, Bun 授权 `camera.control`, api-coverage), facade, 两个样例与吞吐实测拆成 P1.5 之后的新条目, 由后续卡片在两个运行时插件仓库做 (推荐, 已按推荐先行; 宿主部分已提交 `589d727793`; 代价: 6.8.1 宿主的插件设置页已有 Bun `camera.control` 开关, facade 发布前打开它没有可调用的入口); (b) 本卡继续在 Node 与 Bun 运行时插件仓库写 facade 与样例并实测吞吐 (要动两个插件仓库及各自的发版) | 已按推荐先行待确认 (2026-11-06); 影响 P1.5, 新拆出的 facade 条目, Q4 的拍板时点; 改选 (b) 不返工已提交的宿主部分, 另需约 1 - 2 天 (两个运行时插件仓库) |
| Q22 | 脚本自己持有相机会话时调用 `camera.capture({ ui: true })` (P1.4 发现: 取景页会从该会话手中接管相机, 按 Q7 = (a) 的 `pluginUi` 关闭原因会停止脚本自己) | (a) 宿主在打开取景页前报 `CAMERA_BUSY` (`details.holder` 为本脚本路径, 文案提示先关闭会话), 不调用插件 (推荐, 已按推荐先行; 宿主 `24473822f9`, 样例 "取景页拍照" 文件头已写明); (b) 宿主先关闭本脚本的会话 (关闭原因 `script`) 再打开取景页, 返回后不恢复会话; (c) 不拦截, 交给插件取景页的 Q7 流程 (提示 + "停止该脚本" 按钮, 点了会停止调用者自己) | 已按推荐先行待确认 (2026-11-06); 影响 P1.4, P3.1, 附录 A.2 / A.5 示例 5; 改选 (b) 约 1 小时 (宿主 + 测试 + 文档), (c) 删宿主预检约 15 分钟并改样例与文档 |
| Q23 | 预览视图的布局标签 (P1.4 核对: 宿主 ui 布局与悬浮窗只认 `<SurfaceView>`, 没有小写别名; 附录 A.5 示例 4 写的是 `<surfaceview>`) | (a) 保持宿主现状, 宿主样例与文档写 `<SurfaceView>`, 附录 A.5 示例 4 在 P5.1 文档卡片里改写 (推荐, 已按推荐先行; 宿主零改动); (b) 宿主 ui 标签表新增小写别名 `surfaceview` (改布局标签表与对应测试, 约 1 小时) | 已按推荐先行待确认 (2026-11-06); 影响 P1.4, P1.6, P5.1, 附录 A.5; 改选 (b) 约 1 小时 (宿主). 另: A.5 示例 2 的 `barcode.detect(img)` / `codes[0].rawValue` 与宿主现行 API 不符, 应为 `barcode.recognizeText(img)` (返回首个结果文本或 null), 宿主样例已按此写, 不需拍板, 随 P5.1 改写 |
| Q31 | Bun 帧回收所需的授权 (facade 条目发现: Bun 的帧在回调结束后经 `host.images.recycle` 回收, 该调用属于 `image.process` (默认开启) 或 `image.files`, 不属于 `camera.control`; 两项都关闭时帧无法回收, 帧句柄留在本次执行里直到执行结束) | (a) `analyze` 开始前检查, 两项都未授予时以 `NOT_GRANTED` 拒绝并带 `missingCapabilities: ["image.process"]`, Bun `docs/host-api.md` 相机一节与 changelog 写明 (推荐, 已按推荐先行; Bun `cbbaa7c`); (b) 宿主桥 `CAMERA` 域新增 `recycle` 方法, 回收帧只需 `camera.control` (改宿主 `RuntimeCameraBridge` 与 Bun 授权表, 另需宿主提交, 约 1 - 2 小时); (c) 不检查, 回收失败时静默忽略 (长时间分析时帧句柄与内存持续增长) | 已拍板 (2026-11-06): (b) 宿主桥 `CAMERA` 域提供 `recycle` (非推荐项, 维护者选择, 取代已先行的 (a)). 落实 (prism-q31, 均只在本地): 宿主 `418e9d26ba` (`RuntimeCameraBridge` 新增 `recycle(image)`, 归入 Bun `camera.control` 与 Node `camera` 能力; `CameraImageHandles` 记录本桥发出的帧与拍照句柄: 本桥句柄回收, 已失效或未知的句柄与 `images.recycle` 一样幂等返回 true, 其他域的有效句柄与非句柄值报 `ERR_AUTOJS6_CAMERA_INVALID_ARGUMENT`; `RuntimeCameraBridgeTest`, `BunAutomationPolicyTest` 与 api-coverage 检查通过, 协议文档已写明); Bun `f0d85db` (`host-client.mjs` 帧回收改调 `camera.recycle`, 删除 (a) 的 `analyze` 前检查与 `NOT_GRANTED` 分支, `host-client.d.mts`, `docs/host-api.md` 与 10 语言 changelog 改为只需 `camera.control`; 这些改动随 3-Slate P1.6 的同一提交落地), `4667ec4` (`camera.test.mjs`), `78f1c71` (样例 `camera-frames.bun.js`: 拍照图片仍由脚本经 `host.images.recycle` 回收, 未授图像能力时留到执行结束). Q32 (a) 的豁免保留. Node 经 `images` 模块回收, 不受影响, 是否同样改走见 Q33. 联调 (`AVD_API_33`, 克隆宿主 6641 含本提交, 插件构建 12, Bun 353; 只授 `camera.control`, `image.process` / `image.files` 关闭): 480p 30 秒 693 帧 23.3 fps 丢 16, 720p 30 秒 534 帧 18.1 fps 丢 62, 宿主 native heap 分别保持在 18.8 - 53.3 MB 与 11.5 - 60.3 MB (帧不回收时按帧大小估算约增 830 MB / 1.9 GB); 480p 一次执行超过 1386 次宿主调用, 未触发 1024 次上限; 前置镜头样例 10 秒 218 帧丢 3, 拍照 1440x1920, PASS (后置 virtualscene 拍照超时是模拟器 HAL 问题, Rhino 同样超时) |
| Q32 | Bun 每次执行 1024 次宿主调用上限与连续帧 (facade 条目发现: 每帧至少有拉帧 (`camera.control`) 与回收 (`image.process`) 两次调用, 20 fps 时约 25 秒耗尽上限, 之后执行被中断, 长时间分析无法进行) | (a) `camera.control` 调用与只含单个图像句柄的 `recycle` 请求 (按请求体精确匹配) 不计入上限, 授权, 请求大小, 并发与单次超时检查不变, 与 `console.control` 和邮件轮询的豁免同类, 文档与 changelog 写明 (推荐, 已按推荐先行; Bun `cbbaa7c`, `BunHostBridgeDispatcher` 与其测试); (b) 按 Q31 (b) 由宿主桥提供回收, 只豁免 `camera.control` (约 1 - 2 小时, 宿主 + Bun); (c) 只豁免 `camera.control`, 回收照常计数, 文档写明连续分析的时长上限 (20 fps 约 50 秒) | 已按推荐先行待确认 (2026-11-06); 影响 facade 条目, Bun `BunHostBridgeDispatcher`, Bun changelog 的改进条目; 改选 (b) 约 1 - 2 小时, (c) 约 30 分钟 (删匹配与测试, 改文档与 10 语言 changelog) |
| Q33 | Node 的帧回收是否也改走 `camera.recycle` (prism-q31 发现: Q31 (b) 后 Bun 的帧经宿主 `camera.recycle` 回收, 只需 `camera.control`; Node 主线程与 Worker 仍经 `images` 模块回收, 脚本声明 `camera` 外还需 `images` 能力, 未声明时回收失败被共享 facade 静默忽略, 帧留到执行结束; facade 里 Q31 (a) 留下的 `missing()` 钩子与 `NOT_GRANTED` 分支已无调用方) | (a) Node 也改走 `camera.recycle`, 只需 `camera` 能力, 删除共享 facade 的 `missing()` 钩子与 `NOT_GRANTED` 分支, 可顺带在 facade 公开回收拍照图片的入口 (推荐; 宿主零改动, `418e9d26ba` 已把 `recycle` 归入 Node `camera` 能力并兼容 `images` 回收路径; 未先行: 3-Slate 正在改 `node_bridge_sources.cpp`); (b) 保持现状, Node 文档写明连续分析另需声明 `images` 能力 | 已拍板 (a), 2026-11-06 11:40, 由 prism-q33 落实: Node `defc3bdf` (2.2.0, build 382: 共享 facade 的帧回收与新公开的 `camera.recycle(image)` 都调宿主 `camera.recycle`, 只需 `camera` 能力, 删除 `missing()` / `recycle` 钩子与 `NOT_GRANTED` 分支; camera.d.ts 与 HOST-API 相机方法表加 `recycle`, 样例 camera-frames 只声明 `camera` 并用 `camera.recycle` 回收照片, 测试含 Worker, changelog 10 语言并入 2.2.0) 与 `64bd94d8` (设备脚本在清空的任务里启动宿主); Bun `c05b505` (0.23.0, build 356: 重新运行 `sync-camera.mjs`, 删除 host-client 的 recycle 钩子, `host.camera.recycle` 类型与测试, 样例改用 `camera.recycle`, changelog 10 语言并入 0.23.0); 宿主零改动. XQ-AT72 (宿主克隆 6641, 脚本只声明 `camera`): 主线程与 Worker 各跑 sample / 480p / 720p, 6 / 6 PASS, 照片 `camera.recycle` = true; 480p 主线程 29.9 fps / 875 帧 / 丢 5, Worker 29.7 fps / 872 帧 / 丢 4, 720p 两路 29.7 fps; 同现场 Rhino 对照 29.8 fps / 874 帧, Node 复测 29.8 fps / 873 帧 (相机出帧封顶, 与 facade 当日 30 fps 持平); 30 秒内宿主 native heap 平稳 (480p 约 34 - 37 MB, 720p 约 34 - 42 MB, 末值回落). 已知差异: 经 `camera.recycle` 回收的句柄, Node 本地 `images.isRecycled` 不会变为 true, 不影响回收 |
| Q41 | 设置页 "默认镜头" 与 "默认分析分辨率 / 帧率" 的作用范围 (P3.2 发现: 宿主脚本层在脚本省略 `facing` 时总是发送 `back`, 省略 `resolution` / `fps` 时总是按 640x480 / 设备帧率请求, 插件收不到 "未指定", 插件侧的默认值无法作用于脚本会话) | (a) "默认镜头" 只作用于从启动器打开的取景页 (设置页写明), "默认分析分辨率与帧率" 只读显示宿主的固定默认值 (640x480, 设备帧率) 与脚本可传的参数 (推荐, 已按推荐先行; 插件构建 11, 宿主零改动); (b) 宿主在脚本省略这些参数时不发送对应键, 插件按设置页的默认值补齐 (改宿主脚本层, 协议文档 "Open" 一节与测试, 插件设置页改为可编辑, 约 1.5 小时, 另需宿主提交); (c) 设置页去掉这两项 (与 P3.2 条目原文不符) | 已拍板 (2026-11-06 11:40): (b) 宿主省略, 插件按设置页补齐 (非推荐项, 维护者选择). 宿主侧由 prism-host-fixes 落实 (脚本省略 `facing` / `resolution` / `fps` 时不发送对应键, 协议文档 "Open" 一节与宿主测试); 插件侧实施 (插件构建 16): `session/CameraRequests.kt` 的 `SessionDefaults` 按设置页补齐缺失的键 (请求带 `cameraId` 时不补镜头, `analysis()` 省略的分辨率与帧率沿用 `open` 的值), `open` 与 `openCaptureUi` 都适用, 宿主发送了的键总是优先, 所以仍发送缺省值的旧宿主照常工作; `CameraRequestsTest` 补缺键回退与设置选项不越界的用例; 设置页 "默认镜头", "默认分析分辨率" (320x240 / 640x480 / 1280x720 / 1920x1080), "默认帧率" (设备自身 / 5 / 10 / 15 / 24 / 30 / 60) 改为可编辑, strings 11 个目录, changelog 10 种语言; 附录 A.1 / A.2 的默认值说明已改. 影响 P3.2, 附录 A.1 / A.2 宿主侧已落实 (prism-host-fixes, 2026-11-06): 宿主提交 091263677c (CameraScriptOptions 对脚本省略的 `facing` / `resolution` / `fps` 不再发送对应键, Rhino 与 Node / Bun 桥共用; 协议文档 "Open: keys left out"; 6.8.1 camera 功能条目 10 种语言) 与 339988789d (补回被他人提交 52ba898be7 冲掉的 changelog 子句). 证据: 克隆 JVM camera / core.plugin.camera / RuntimeCameraBridgeTest / ScriptAsyncDispatcherRoundsTest 99 例 0 失败, `AVD_API_26_PRISM` 上 CameraRequestBundleDeviceTest 3 例通过. 范围外: NodeJs-Runtime 仓库 (Bun 可能同样) camera.d.ts `OpenOptions` 注释仍写 "the back camera by default" / "640x480 by default", 已转告调度会话. |
| Q42 | 更新检查的方式 (P3.3 发现: D25 的权限清单没有网络权限, 而条目要求 "官方索引更新检查 (与 Macaw P5.4 同构)", Macaw 的应用内检查需要 `INTERNET`) | (a) 不加网络权限, 设置页 "检查更新" 用浏览器打开 GitHub Releases 页面, 发行历史读取随包的 `.changelog` (推荐, 已按推荐先行; 插件构建 11, README 安全一节 "无网络权限" 保持成立); (b) 加 `INTERNET`, 照搬 Macaw 的应用内检查 (读官方索引, 比较 versionCode, 提示下载; 改 Manifest, D25, README 安全一节与 changelog 10 语言, 约 1 - 1.5 小时, API 24 要另带 ISRG Root X1 网络安全配置) | 已拍板 (2026-11-06 11:40): (b) 加 `INTERNET`, 应用内更新检查 (非推荐项, 维护者选择). 实施 (插件构建 16): 移植 3-Macaw Speech P5.4 (`ui/update/AppUpdate.kt`, 设置页 "检查更新", "自动检查更新" 开关, "已忽略的版本"), Manifest 加 `INTERNET` 与 `network_security_config` (ISRG Root X1, 移植自 Macaw cf32b5e, `locks/trust-anchors.lock` 与 `TrustAnchorsTest`; 2026-11-06 用 openssl 核对 `api.github.com` 当前是 Sectigo -> USERTrust ECC 链, Android 7.0 已信任, 该锚点属预防, 防 API 主机改用 Let's Encrypt 链), D25, README 安全一节与 changelog 10 种语言已改; `ManifestContractTest` 的权限清单加 `INTERNET`, 仍断言不申请网络状态权限. 实施中与选项原文的两处差异登记为 Q45 (数据源) 与 Q46 (自动检查时机). 影响 P3.3, D25, README |
| Q43 | 帧回调慢于帧间隔时脚本的定时器被饿死 (P1.6 联调发现: G8441 上 `物体检测画框` 样例每帧在回调里同步做 YOLO 检测, 结束依靠 `setTimeout(() => session.close(), seconds * 1000)`, 85 秒内始终没有触发, 样例不结束; jdb 栈显示脚本线程一直在 `ScriptAsyncDispatcher.drain` 里: `CameraFramePump.run` 先清除 `scheduled` 再交付, 回调执行期间到达的下一帧又排进正在排空的队列, `drain` 的 `while (true)` 不回到 Looper, `setTimeout` / `setInterval` 等 Looper 消息排不上. 复现: `AVD_API_26_PRISM`, 宿主 6641, 共享笔记 `prism-p3-q43-starve-repro.js`: 回调里 `sleep(300)` 时 3 秒的 `setTimeout` 在 15.5 秒内未触发 (44 帧), `sleep(10)` 时 3.4 秒触发) | (a) 宿主修复, 另开卡片: 分发器每轮只排空开始时已有的动作 (或每帧单独调度一次), 让 Looper 消息在帧之间得到执行, 复现脚本改为宿主测试 (推荐; 宿主 `ScriptAsyncDispatcher` / `CameraFramePump` 与测试约 1 - 2 小时, 该分发器也服务其他模块的异步回调, 需回归 speech 等模块); (b) 宿主不改, 文档写明 "回调耗时超过帧间隔时定时器不会执行", 样例改为在回调里按时间判断并关闭会话 (宿主样例与 P5.1 文档约 30 分钟) | 已拍板 (2026-11-06 11:40): (a) 宿主修复 (推荐项). 由 prism-host-fixes 落实 (宿主 `ScriptAsyncDispatcher` / `CameraFramePump` 与宿主测试, 回归 speech 等共用分发器的模块); 落实后 prism-p3 在 G8441 复跑 `--phases detect` 并勾选 P1.6. 影响 P1.6, P4.5, P5.1 宿主已落实 (prism-host-fixes, 2026-11-06): 宿主提交 25196d7f34 (`ScriptAsyncDispatcher` 在 Looper 上每轮只运行开始时已排队的动作, 其余重新投递, 阻塞等待的 `drainOnOriginatingThread` 仍全部排空; JVM ScriptAsyncDispatcherRoundsTest 7 例, 设备用例 RhinoAsyncDeviceTest.aDueTimerRunsBetweenDeliveriesThatKeepComing; changelog fix 10 种语言). 证据 (宿主工具 `tools/camera/ui_thread_open_check.py --phases starve,fps`, 回调 `sleep(300)`): 旧宿主 3 秒定时器未触发 (FAIL), 新宿主 3179 ms (`AVD_API_26_PRISM`) / 3299 ms (`AVD_API_33`) 触发; 吞吐 API 26 旧 29.1 / 新 28.9 fps (基线 28.6), API 33 29.1 fps, 无回退; RhinoAsyncDeviceTest 27 例通过. G8441 复跑 (prism-p3, 2026-11-06 12:56, 克隆宿主 6641 (base.apk md5 fab3debb9d035f1dcbdd900ae4a3a0bb, 含 Q41 / Q43 / Q44 修复) + 插件构建 16): `--phases detect --seconds 25` PASS, 按时结束 (`reason=script`, 62 帧; 修复前 85 秒不结束); 宿主工具 `starve` PASS (回调 `sleep(300)` 时 3 秒定时器 3015 ms 触发, 8 帧), 无 ANR / 崩溃; P1.6 已勾选. |
| Q44 | ui 脚本在 UI 线程上调用 `camera.open()` 时宿主无响应 (附录 A.5 联调发现: 示例 4 `camera.open().preview(ui.cam)...` 原样运行时, G8441 与 `AVD_API_26_PRISM` 都弹出 AutoJs6 无响应对话框, 结果文件始终没有写出; jdb 栈: 主线程停在 `Camera.openSession` -> `CameraScriptCalls.call` 的 `runBlocking`, 等待插件绑定, 而 `AidlPluginHost` 的 `bindService` 回调与绑定超时都投递到主线程, 两者都无法执行, 一直卡住; P1.4 实施记录里 "`open` 可在 UI 线程调用" 不成立. 复现: 共享笔记 `prism-p3-q44-ui-thread-open-repro.js`) | (a) 宿主让 `camera.open` 以及其他要绑定插件的同步调用 (如 `camera.list`) 在 UI 线程上与 `capture` / `focusAt` 一样报 `INVALID_ARGUMENT`, 附录 A.5 示例 4 与 P5.1 文档改为在 `threads.start` 里打开 (推荐; 宿主约 30 分钟加测试; 设备脚本 `--phases a5` 已按此写); (b) 宿主让 UI 线程上的 `open` 可用: 绑定改到后台线程 (API 29+ 用带 Executor 的 `bindService`, API 24 - 28 要预先异步绑定), 连上之前排队控制调用 (约 3 - 4 小时, 风险较高); (c) 只在文档写明限制, 宿主不改 (误用时仍会 ANR) | 已拍板 (2026-11-06 11:40): (b) 宿主让 UI 线程上的 `open` 可用 (非推荐项, 维护者选择). 由 prism-host-fixes 落实 (绑定移到后台线程, 连上之前排队控制调用); 附录 A.5 示例 4 保持 UI 线程写法, 设备脚本 `--phases a5` 的 `threads.start` 变体留作补充用例, 落实后 prism-p3 在 G8441 原样复跑示例 4. 影响 P1.4, 附录 A.5 示例 4, P5.1 宿主已落实 (prism-host-fixes, 2026-11-06): 宿主提交 69c5528499, 只改宿主 camera 模块, `AidlPluginHost` 的公共绑定路径未动: UI 线程上 `camera.open` 校验参数后立即返回, 会话在脚本运行时的协程作用域 (IO 线程) 里连接; 连接期间 `state().open` 为 false, UI 线程上的 preview / stopPreview / analyze / stopAnalysis / torch / flashMode / zoom / zoomLinear / exposure 立即校验参数并按调用顺序排队, 连上后执行 (其他线程等待打开); zoomState / exposureState 连接期间在 UI 线程报 `INVALID_ARGUMENT`; capture / focusAt 在 UI 线程仍拒绝; 连接期间 close 以 `script` 结束并取消打开; 打开失败先发 `error` 事件 (无监听时 console.error) 再 `close("error")`; 协议文档 "Open: on the UI thread". 证据: JVM CameraPendingCallsTest 6 例, 克隆 camera / core.plugin.camera / RuntimeCameraBridgeTest / ScriptAsyncDispatcherRoundsTest / speech 共 168 例 0 失败; `tools/camera/ui_thread_open_check.py` 全部阶段 (repro, a5 原样 UI 线程写法, uiopen, uifail, uiclose, thread, 末尾 ANR / 崩溃检查) 在 `AVD_API_26_PRISM` 与 `AVD_API_33` 通过, repro 中 open 30 ms 返回, 无 ANR. G8441 原样复跑示例 4 (prism-p3, 2026-11-06 12:57, 克隆宿主 6641 (base.apk md5 fab3debb9d035f1dcbdd900ae4a3a0bb, 含 Q41 / Q43 / Q44 修复) + 插件构建 16, `ui_thread_open_check.py --phases a5,repro,starve`): a5 PASS (`open` 23 ms 返回, 1765 ms 后打开, `previewing=true torch=true zoom=2`, 上限 8, 无错误事件), repro PASS (35 ms 打开), 无 ANR / 崩溃. 新待决 Q51 (camera.list / capabilities / isAvailable / requestPermission 在 UI 线程且插件未绑定时仍会卡住) 与 Q52 (连接期间 zoomState / exposureState 的处理) 登记在看板. |
| Q45 | 应用内更新检查的数据源 (Q42 = (b) 实施发现: 选项 (b) 写的是 "读官方索引, 比较 versionCode, 提示下载", 而 Macaw P5.4 实际读 GitHub `releases/latest` 并按语义化版本比较 tag) | (a) 照搬 Macaw: GitHub `releases/latest` + 语义化版本 (推荐, 已按推荐先行; 插件构建 16; 与 Macaw 同构, 发布 Release 即可被发现, 更新说明直接取 Release 正文, 不依赖官方索引登记); (b) 改读官方插件索引的条目, 比较 versionCode (约 1 - 1.5 小时: 新的解析与测试, 更新说明另取 changelog; 登记索引之前查不到新版) | 已按推荐先行待确认 (2026-11-06); 影响 P3.3; 改选 (b) 约 1 - 1.5 小时 |
| Q46 | 自动检查更新的时机 (Q42 = (b) 实施发现: Macaw 在首页打开时自动检查, Prism 没有首页, 启动器入口是取景页) | (a) 打开设置页时自动检查, 每 12 小时最多一次, 设置页可关闭 (推荐, 已按推荐先行; 插件构建 16; 取景页不联网, 拍照入口保持即开即用); (b) 从启动器打开取景页时自动检查, 有新版时在取景页弹出提示 (约 30 分钟, 取景页要接入更新对话框); (c) 不自动检查, 只保留手动 "检查更新" (约 10 分钟, 去掉开关与相关文案) | 已按推荐先行待确认 (2026-11-06); 影响 P3.3; 改选 (b) 约 30 分钟, (c) 约 10 分钟 |
| Q51 | `camera.list` / `capabilities` / `isAvailable` / `requestPermission` 在 UI 线程且插件尚未绑定时仍会卡住 (Q44 (b) 实施发现: 四者都经 `AidlPluginHost` 绑定插件, `isAvailable` 的探测也一样, 绑定回调与超时都投递到主线程, `CameraScriptCalls.call` 的 `runBlocking` 在 UI 线程上等不到; 同 Q44 根因, `open` 已解决) | 子会话原登记的选项 (保留作记录): (a) 这四个调用在 UI 线程且未绑定时报 `INVALID_ARGUMENT` (`isAvailable` 文档写明 "从不抛出", 改为返回 false) (推荐, 未先行; 宿主约 1 小时含测试与文档); (b) `AidlPluginHost` 改用带 Executor 的 `bindService` 让 UI 线程可等待 (仅 API 29+, API 24 - 28 仍需 (a) 或预先异步绑定; 影响全部插件, 约 3 - 4 小时并需回归全部插件); (c) 只靠文档 (现状, 误用仍 ANR). 调度会话出题时的 (b) (维护者所选的变体): 与 Q44 (b) 的 `open` 同机制, UI 线程未绑定时走后台线程绑定并排队, 不阻塞 UI 线程, 这四个调用的交付形式可改为异步 | 已拍板 (2026-11-06 13:05): (b) (非推荐项, 维护者选择), 指调度会话出题时的变体 (后台线程绑定并排队, 交付形式可改为异步), 不是原登记的 (b) "`AidlPluginHost` 改用带 Executor 的 `bindService`" (该方案不做, `AidlPluginHost` 保持不动, 与 Q44 (b) 一致). UI 线程上的同步返回值在插件未绑定时等不到, 交付形式涉及公开签名, 登记为 Q53 并由维护者拍板 (2026-11-06, AskUserQuestion): A 沿用 `dialogs` 惯例 (推荐; 其余选项: B 只在 UI 线程返回 Promise, C 只加回调参数且 UI 线程未绑定报 `INVALID_ARGUMENT`, D 新增 `xxxAsync` 方法). 宿主已落实 (prism-host-fixes, 2026-11-06): 宿主提交 e856c613f5, 只改宿主 camera 模块, `AidlPluginHost` 未动: UI 线程上不传回调时返回 Promise (以同步形式的值兑现, 失败以 `CameraError` 拒绝; `isAvailable` 只兑现); 任意线程上唯一参数为函数时立即返回 undefined, 在调用它的脚本线程上调用 `callback(value, null)` 或 `callback(null, error)` (与 `http` 一致); 其他线程不传回调时仍同步返回; 返回形式只取决于线程, 与是否已绑定无关; `undefined` / null 视为未传回调, 其他参数报 `INVALID_ARGUMENT`; `requestPermission` 在 UI 线程也可用 (Promise / 回调形式). 协议文档新增 "Discovery calls: on the UI thread and with a callback", 6.8.1 camera 功能条目 10 种语言补充说明. 附录 A.1 的四行与 P5.1 文档 / d.ts 须写明这两种形式. 证据: JVM CameraDiscoveryArgsTest 4 例, 克隆 camera / core.plugin.camera / RuntimeCameraBridgeTest 共 102 例 0 失败; `tools/camera/ui_thread_open_check.py` 新增 discovery 阶段 (先 force-stop 宿主, 首批调用遇到未绑定的插件), 克隆宿主 6641 (x86_64 md5 e9b672434411ef60594e5e3a2ad6a253) + 插件构建 16 在 `AVD_API_26_PRISM` 与 `AVD_API_33` 全部阶段 (discovery, starve, fps, repro, a5, uiopen, uifail, uiclose, thread) 通过: UI 线程上四个调用 23 - 55 ms 返回 Promise 并兑现 (API 33 插件冷启动 6.3 s 后兑现), 回调形式在 UI 线程与工作线程各回调一次且都在原线程, 工作线程同步形式不变, 4 种坏参数均为 `INVALID_ARGUMENT`, 50 ms UI 定时器最大间隔 134 ms, 无 ANR / 崩溃; API 33 只读 AVD 未装插件时的一次运行顺带验证失败路径 (Promise 以 `PLUGIN_NOT_INSTALLED` 拒绝, 回调收到 `(null, CameraError)`). |
| Q52 | Q44 (b) 连接期间 UI 线程读 `zoomState` / `exposureState` 报 `INVALID_ARGUMENT` (两者要镜头应答, 连接期间没有可返回的值) | (a) 保持, 脚本在 `state().open` 为 true 后再读 (推荐, 已先行, 宿主 69c5528499); (b) 新增 `open` 事件 (宿主约 1 小时, 另需 d.ts / 文档 / Node / Bun facade 同步) | 已拍板 (2026-11-06 13:05): (a) 保持现状 (已实施, 69c5528499). 协议文档 "Open: on the UI thread" 已写明在 `state().open` 为 true 后读取 (宿主提交 e856c613f5); 影响附录 A.2 / A.5 与 P5.1 文档. |

---

## 附录 E. 证据等级

- E1 代码与配置: 提交 hash 可追溯, 相关文件在仓库中.
- E2 JVM 单元测试: 测试类名, 用例数, 运行命令.
- E3 instrumentation: 测试类名 + 设备型号 + API 级别 (真机或 AVD) + 日期 + 实际分辨率 / 帧率.
- E4 人工验收: 设备, 步骤, 结果, 截图路径 (`.bak/visual/<version>/`, 不入库); 相机类条目另附拍摄环境 (光线, 目标物).
- E5 发布产物: APK 文件名, SHA-256, tag, GitHub Release 链接, 索引清单文件名.

勾选 `[x]` 的条目至少满足 E1 + (E2 或 E3); 涉及界面的条目另需 E4; "(发布)" 条目需 E5. 未执行的验证 (例如某机型的熄屏实测, ColorOS 激活) 必须如实写 "未执行".

---

## 附录 F. 基线快照与设备矩阵

基线 (2026-10-22, 宿主与 CameraX 两行于 2026-11-05 P0 更新):

| 项 | 值 |
| --- | --- |
| 宿主 | `AutoJs6 master@3ce52d2fd7`, `VERSION_NAME=6.8.1` (未发布), `VERSION_BUILD=6640` = versionCode (2026-11-05; 原基线 `63d1cb259` / 5835 随 6.8.0 公开时的历史重写失效); 工作区多会话共用, 本插件的宿主提交只碰自己的文件 |
| 宿主相机相关 | Manifest 已有 `CAMERA` / `FLASHLIGHT`; 无脚本相机模块; `RuntimeWebCameraRecording` (WebView 专用) 不复用 |
| 宿主绑定 | `AidlPluginHost` 已支持 `BIND_INCLUDE_CAPABILITIES` 与 `allowActivityStarts`; `SpeechPluginHost` 为模板 |
| 跨进程传图 | OCR `SharedMemory` (fd 复制), YOLO 临时文件 fd, Barcode 压缩图 fd |
| CameraX | `1.6.2` 稳定 (发布页 2026-10-21 更新), `1.7.0-rc01` 同日; P0.2 已解析 `camera-core` / `camera-camera2` / `camera-camera2-pipe` / `camera-lifecycle` / `camera-compose` / `viewfinder-*` / `featurecombinationquery` 1.6.2 |
| 平台版本插件 | `1.9.1` |
| 参照插件 | 3-Macaw Speech (前台服务, 透明启动 Activity, CI 与脚本族), 3-Block Compose (单 APK, 跨进程预览页), 3-Maple Image (Compose, 独立设置, Icon Studio), MediaInfo (契约模板), Yolo NCNN (图像传输) |

设备矩阵 (来自 `COMMON_AGENTS.md`; 2026-10-15 起已连接设备可自由安装 / 卸载官方插件; 手机上的权限对话框由维护者操作):

| 设备 | API | 用途 |
| --- | --- | --- |
| AVD_API_24 (E:/.android/avd) | 24 | 帧传输回退路径 (临时文件 fd), 最低 API 冒烟 (模拟器相机 `virtualscene`) |
| Sony G8441 (BH900ASK9E) | 28 | 低端机性能基线, Android 9 后台相机限制 |
| Sony XQ-AT72 (QV710AF65F) | 31 | 开发主力机, 后台 / 熄屏, camera 前台服务类型 |
| Sony XQ-DQ72 (QV770340J7) | 33 | 截图 (1096 x 2560), 后台 `open` 行为, 三 use case 并行 (注意记忆中 debug 宿主 ART JIT ANR 问题) |
| Redmi 22120RN86C (bek749scrwv4wo8h) | 33 | MIUI 前台服务通知与后台策略 |
| Xiaomi Pad 23046RP50C (968e9f18) | 35 | API 35 透明 Activity 启动前台服务, 平板横屏旋正 |
| OnePlus OPD2413 (63751f63) | 35 | ColorOS 激活, 唤醒与通知行为 |
| Samsung SM-A566B / SM-F971B (可租用) | 36 / 37 | 按需: 最新 API 的前台服务与权限行为 |
| CI AVD (API 33 google_apis x86_64) | 33 | `CameraSmokeTest` (D35) |

性能基线 (P4.4 填写; P2.2 先记下模拟器上的数字, 只作开发参考, 不算基线. 模拟器行由 `py .python/device_camera_core.py <serial> --steps perf` 在 2026-11-06 测得 (宿主修复 `c415503fcd` 之后, 见 P1.2; G8441 行同法测得, 插件构建 9, 宿主 `c82c5b27f9`): 宿主 instrumentation 在 640x480 目标 30 fps 下取帧 30 秒, 帧率按宿主收到的帧计, 丢帧为宿主丢帧 (括号内为插件侧丢帧), 插件 CPU 按单核 100% 计; "未测" 的列留给 P4.4):

| 设备 | 分辨率 | 帧率 | 丢帧 | 宿主 onFrame P50 / P95 | 插件 CPU | 宿主 CPU | 拍照延迟 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 模拟器 `AVD_API_35_16K` (API 35, `SharedMemory`) | 640x480 | 29.2 fps (876 帧) | 15 (0) | 未测 | 11.7% | 未测 | 未测 |
| 模拟器 `AVD_API_26_PRISM` (API 26, 临时文件回退) | 640x480 | 28.6 fps (857 帧) | 0 (0) | 未测 | 27.4% | 未测 | 未测 |
| 模拟器 `AVD_API_24` (API 24, 临时文件回退) | 640x480 | 23.3 fps (698 帧) | 0 (0) | 未测 | 16.6% | 未测 | 未测 |
| 真机 G8441 `BH900ASK9E` (API 28, `SharedMemory`) | 640x480 | 29.5 fps (884 帧) | 0 (0) | 未测 | 40.8% | 未测 | 未测 |

运行时连续帧吞吐 (facade 条目, 2026-11-06, prism-facade; 只作参考, 不代替 P4.4 基线): Node 运行时插件 `py -3 tools/nodejs/api/camera/device_camera_frames.py <serial> --runtimes rhino,node,bun --runs 480p,720p` 跑 camera-frames 样例的 30 秒版本, 不指定帧率 (相机默认). 帧率按脚本 `analyze` 回调收到的首帧到末帧计, 丢帧取会话的 `dropped`, 与上表的宿主 instrumentation 口径不同. Rhino 行是同一帧循环的普通 AutoJs6 脚本, 不经运行时桥, 作同机对照. 克隆宿主 6641, 插件构建 9, Node 380, Bun 352.

| 设备 | 运行时 | 480p 帧率 (帧) | 480p 丢帧 | 720p 帧率 (帧) | 720p 丢帧 |
| --- | --- | --- | --- | --- | --- |
| 真机 XQ-AT72 `QV710AF65F` (API 31) | Rhino (对照) | 30 fps (881) | 0 | 30 fps (881) | 0 |
| 真机 XQ-AT72 `QV710AF65F` (API 31) | Node | 30 fps (880) | 1 | 30 fps (881) | 1 |
| 真机 Redmi 22120RN86C `bek749scrwv4wo8h` (API 33) | Rhino (对照) | 14.3 fps (422) | 0 | 14.2 fps (420) | 2 |
| 真机 Redmi 22120RN86C `bek749scrwv4wo8h` (API 33) | Bun | 14.3 fps (421) | 0 | 13.9 fps (410) | 11 |
| 模拟器 `AVD_API_33` (API 33, virtualscene) | Rhino (对照) | 11 fps (318) | 43 | 15.7 fps (450) | 64 |
| 模拟器 `AVD_API_33` (API 33, virtualscene) | Node | 19.1 fps (570) | 6 | 19.5 fps (577) | 16 |
| 模拟器 `AVD_API_33` (API 33, virtualscene) | Bun | 16.7 fps (495) | 37 | 17.6 fps (521) | 57 |

---

## 附录 G. 已评估暂缓

| 事项 | 结论 |
| --- | --- |
| 每帧 JPEG 管道 | 两进程各多 15 - 30 ms 编解码, 720p 上限约 15 fps, 有损 (第三轮已否决, D9) |
| 每帧临时文件 + fd 作为主路径 | 每帧多两次拷贝与文件系统元数据操作, 30 fps 抖动; 只作 API 24-26 回退 (D9) |
| 宿主内多脚本共享会话 | 多一层订阅管理与引用计数, 帧生命周期按最慢订阅者; 独占更确定 (第五轮已否决, D17) |
| 每帧独立拷贝由脚本 recycle | 720p 每帧多 3.7 MB 拷贝, 忘记 recycle 时每秒堆积 110 MB (第三轮已否决, D11) |
| 系统相机 Intent 作为一键拍照 | 不能控制镜头, 结果尺寸随厂商, 后台不可用 (第四轮已否决, D15) |
| 插件写入系统相册 (MediaStore) | API 24-28 需存储权限, 与宿主文件 API 不一致 (第四轮已否决, D13) |
| 默认不旋正只给 rotationDegrees | 用户忘记旋转时 OCR 几乎全错 (第五轮已否决, D19) |
| 默认 1280x720 分析分辨率 | 每帧 3.7 MB, 低端机帧率明显下降 (第五轮已否决, D18) |
| 契约 v1 预定义录像接口 | 未实现先冻结参数, 1.1.0 改签名要走破坏性升级 (第四轮已否决, D16) |
| 宿主自己用 camera2 实现 | 立项前提是插件化; 宿主只保留入口, 发现与降级 |
| 1.0.0 录像 | 排 1.1.0 (第一轮已定, D2) |
