<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-prism-camera-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>通过 CameraX 拍照, 连续取帧与取景预览, 帧可直接交给 OCR, 条码与 YOLO 等识别模块</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言 (Languages)

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ar.md)

******

### 简介

******

3-Prism Camera 是 AutoJs6 的相机插件, 基于 Jetpack CameraX 为脚本提供拍照, 连续帧分析与取景预览. 连续帧以 AutoJs6 的 `ImageWrapper` 交给脚本, 脚本可以直接把它交给 `ocr`, `barcode` 或 `yolo` 等已有模块, 实现实时扫码, 文字识别与目标检测.

插件持有相机并通过 Binder 与 AutoJs6 通信: 连续帧经共享内存环形缓冲传给 AutoJs6 (Android 7.0 - 8.0 改用临时文件), 照片以 JPEG 交给 AutoJs6 解码或原样保存, 预览直接渲染到脚本界面中的 `SurfaceView`, 像素不经过 Binder. 会话打开期间插件运行相机前台服务, 脚本在后台或熄屏时也能继续取帧.

> 当前为开发预览版: 插件可以被 AutoJs6 插件中心识别, 相机核心 (打开相机, 连续帧, 拍照, 预览与各项控制) 已可供 AutoJs6 调用, 但取景页与 `camera` 脚本模块尚未交付. 下文描述的是 1.0.0 的设计目标, 进度见开发路线图.

******

### 功能亮点

******

- 拍照: 返回按 EXIF 方向旋正的 `ImageWrapper`, 也可以把原始 JPEG (含 EXIF) 保存到指定路径; 可设置闪光模式, 画质与分辨率.
- 连续帧: RGBA 帧默认 640x480, 最高 1920x1080, 默认按屏幕方向旋正; 处理不过来时自动丢帧并计数, 不会排队积压.
- 预览: 把 ui 脚本或悬浮窗中的 `SurfaceView` 交给会话即可显示取景画面, 可与连续帧和拍照同时进行.
- 控制: 镜头选择, 分辨率, 目标帧率, 手电, 闪光模式, 变焦, 曝光补偿与点按对焦, 并可读取各项范围与当前状态.
- 取景页: `camera.capture({ ui: true })` 打开插件自带的取景页, 用户按下快门后脚本拿到照片, 取消时返回 `null`.
- 后台可用: 会话打开期间运行相机前台服务, 脚本切到后台或熄屏后仍可取帧; 脚本退出或 AutoJs6 被结束时自动释放相机.

******

### 使用方法

******

1. 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases) 下载与设备 ABI 对应的 APK 并安装: 大多数手机选择 `arm64-v8a`, 不确定时选择适用于全部设备但体积更大的 `universal`.
2. 打开插件, 按提示授予相机权限; 脚本也可以调用 `camera.requestPermission()` 打开授权页.
3. 要在脚本中使用 `camera` 模块, 请将 AutoJs6 升级到内部版本号 6641 (6.8.1) 或更高版本, 并在插件中心启用本插件.

> 插件支持 Android 7.0 (API 24) 及以上的设备. 每种 ABI 各有一个安装包, 另有适用于全部设备的 `universal` 安装包; CameraX 与 Compose 自带的少量原生库 (每种 ABI 约 50 KB) 已按 16 KB 内存页对齐, 采用 16 KB 内存页的新设备也能正常安装.

******

### 脚本 API

******

脚本通过全局模块 `camera` (也可写作 `$camera`) 使用这些能力 (1.0.0 设计草案, 以发布版本的文档为准):

```javascript
// Take one photo and keep the original JPEG.
let photo = camera.capture({ path: files.join(files.getSdcardPath(), "prism.jpg") });
toastLog(photo.getWidth() + "x" + photo.getHeight());
photo.recycle();

// Scan barcodes from live frames; a frame is valid only inside the callback.
let session = camera.open({ resolution: "1280x720" });
session.analyze((img, meta) => {
    let codes = barcode.detect(img);
    if (codes.length > 0) {
        toastLog(codes[0].rawValue);
        session.close();
    }
});
events.on("exit", () => session.close());

// Let the user take the picture on the plugin's capture screen; cancelling returns null.
let shot = camera.capture({ ui: true });
```

- `camera.capture(options)` 一步完成打开, 拍照与关闭; `camera.open(options)` 打开独占会话, 会话对象提供 `preview`, `analyze`, `capture`, `torch`, `zoom`, `exposure`, `focusAt`, `state` 与 `close`.
- 帧的生命周期: `analyze` 回调里的 `img` 只在回调执行期间有效, 回调返回后立即回收; 要在回调之外使用, 请调用 `img.clone()`, 并在用完后自行 `recycle()`.
- `camera.list()` 列出镜头及其分辨率, 变焦与曝光范围; `camera.capabilities()` 返回契约版本, 功能列表与授权状态.
- 同一时刻只有一个会话: 再次 `open` 会报 `CAMERA_BUSY` 并说明占用者; 脚本退出时会话自动关闭.

******

### 常见问题

******

#### 为什么相机做成插件, 而不是内置在 AutoJs6 中?

相机依赖 CameraX 与前台服务权限. 做成插件后, 不需要相机的用户不必承担这部分体积与权限, 插件也可以独立于 AutoJs6 更新.

#### 脚本在后台还能取帧吗?

能. 会话打开期间插件运行相机前台服务, 通知栏会显示相机正在使用. 部分厂商系统会额外限制后台相机, 兼容情况见开发路线图中的设备矩阵.

#### 插件会把照片存进相册吗?

不会. 插件不申请存储权限, 也不写系统相册; 只有脚本传入 `path` 时, AutoJs6 才把照片写到该路径.

******

### 权限与安全

******

插件在设计上尽量收窄权限面:

- 受保护的入口: 能力服务与激活入口受 AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`) 保护, 其他应用无法直接调用; 前台服务与透明启动页不导出.
- 相机: 只申请 `CAMERA`, 相机类前台服务权限, 以及 Android 13 及以上显示相机使用通知所需的通知权限; 相机只在会话打开期间使用, 会话关闭, 脚本退出或 AutoJs6 被结束时立即释放.
- 网络与存储: 不申请网络与存储权限, 帧与照片只在插件和 AutoJs6 之间传递, 不写系统相册.
- 独占: 同一时刻只有一个会话 (脚本或插件取景页), 占用者会写在错误信息中.

******

### 插件接口

******

以下信息面向 AutoJs6 宿主与插件开发者, 宿主通过这些标识发现插件并完成能力协商:

```text
application id: io.github.supermonster003.autojs6.plugin.three.prism.camera
plugin id: three-prism-camera
engine: camera
variant: default
info service: org.autojs.plugin.INFO (category three-prism-camera)
capability service: org.autojs.plugin.CAMERA (category camera)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6641
native libraries: libimage_processing_util_jni.so, libsurface_util_jni.so (CameraX), libandroidx.graphics.path.so (Compose); one APK per ABI plus a universal APK, stored uncompressed and 16 KB page-aligned
```

******

### 开发路线图

******

功能规划, 设计决策与完成情况以可勾选清单维护在 ROADMAP.md 中. 未勾选条目表示规划意向, 不代表当前版本能力.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.0.0

_2026/11/05_

- `提示` 开发预览: 相机核心已可供 AutoJs6 调用 (打开相机, 连续取帧, 拍照, 预览与各项控制), 拍摄页与 camera 脚本模块尚未交付
- `提示` 需要 AutoJs6 6.8.1 (6641) 或更高版本 (准确的最低版本号待宿主侧改动落地后回填)
- `提示` 当前图标为候选图案, 之后可能更换
- `新增` 插件骨架: Wake 激活协议与插件中心身份服务
- `新增` 相机能力服务: 同一时间只有一个会话能使用相机, 被占用时报告占用者; 使用相机期间显示前台通知, AutoJs6 退出后相机立即释放, 通知随之消失
- `新增` 连续取帧: RGBA 帧经 3 块共享内存 (Android 8.1 及以上) 或 3 个临时文件 (Android 7.0 - 8.0) 轮流交给 AutoJs6, 可设分辨率与帧率; 来不及处理的帧会被跳过并计数
- `新增` 拍照: JPEG 以只读文件交给 AutoJs6, 并附带宽高, EXIF 方向, 字节数与是否镜像; 可设画质, 分辨率与闪光模式
- `新增` 预览: 画面显示在 AutoJs6 提供的界面上, 旋转屏幕后方向随之调整; 前置镜头的预览默认镜像, 帧与照片默认不镜像
- `新增` 控制: 手电筒, 闪光模式, 变焦 (倍率或线性), 曝光补偿, 点按对焦与测光; 设备不支持的项 (例如前置镜头的闪光灯) 报告 UNSUPPORTED
- `新增` 相机权限: 未授权时打开相机报告 PERMISSION_DENIED, 可以请求授权; Android 13 及以上同时请求通知权限, 以显示相机使用中的通知
- `新增` 按 ABI 分别提供安装包 (arm64-v8a, armeabi-v7a, x86_64, x86), 另有适用于全部设备的 universal 安装包; CameraX 与 Compose 自带的原生库已按 16 KB 内存页对齐
- `新增` 10 种语言的 README, 插件中心说明与更新日志, 由 JSON 源文件统一生成
- `依赖` 附加 Jetpack CameraX 版本 1.6.2 (打开相机, 拍照, 取帧与取景)
- `依赖` 附加 Jetpack Compose BOM 版本 2026.09.00 (插件界面)
- `依赖` 附加 AndroidX Activity Compose 版本 1.12.2
- `依赖` 附加 AndroidX Core KTX 版本 1.16.0

##### 更多发行历史可参阅

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建

******

本节面向希望从源码构建插件的开发者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### 许可

******

项目代码使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE). 第三方组件的许可见第三方声明.

- [第三方声明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
