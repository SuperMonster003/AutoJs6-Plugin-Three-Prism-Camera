3-Prism Camera 是 AutoJs6 的相机插件, 让脚本可以拍照, 连续取帧并显示取景预览. 取到的帧可以直接交给 `ocr`, `barcode` 或 YOLO 等识别模块, 实现实时扫码, 文字识别与目标检测.

### 开始使用

1. 从启动器打开 3-Prism Camera, 在取景页点按 "授予权限", 允许插件使用相机.
2. 点按 "拍照" 试拍一张. 照片只在 "照片预览" 中显示, 不会保存.
3. 在 AutoJs6 的 "插件中心" 启用本插件.
4. 在脚本中调用 `camera` 模块拍照或取帧, 示例见下文.

### 主要功能

- **取景页**: 从启动器打开插件即进入取景页, 可以 "切换镜头", 开关手电筒, 用滑条或双指捏合变焦, 点按画面对焦.
- **拍照**: 脚本得到按方向旋正的图像, 也可以把原始 JPEG 存到指定路径; `camera.capture({ ui: true })` 改由用户在取景页拍照.
- **连续取帧**: 帧默认为 640x480, 最高 1920x1080, 并按屏幕方向旋正; 来不及处理的帧会被跳过并计数, 不会排队积压.
- **预览**: 把 ui 布局或悬浮窗中的 `SurfaceView` 交给会话即可显示取景画面, 可以与取帧和拍照同时进行.
- **相机控制**: 脚本可以控制手电筒, 闪光模式, 变焦, 曝光补偿与点按对焦, 并读取各项的范围与当前值.
- **相机独占**: 同一时间只有一个会话使用相机. 脚本占用相机时, 取景页显示是哪个脚本, 点按 "停止该脚本" 即可接管相机.
- **后台取帧**: 会话打开期间显示 "相机使用中" 通知, AutoJs6 退到后台或熄屏后仍可取帧; 脚本退出后相机立即释放.
- **默认值**: 在取景页右上角的 "设置" 中可以设定 "默认镜头", "默认分析分辨率" 与 "默认分析帧率"; 脚本指定的值总是优先.
- **诊断与更新**: 设置页的 "诊断" 分区列出各镜头的能力与 "当前持有者"; "检查更新" 在官方插件索引中查找新版本.
- **外观**: "语言", "夜间模式" 与 "主题色" 默认跟随 AutoJs6, 也可以单独设定; 还可以切换 "启动器图标".

### 在脚本中使用

在 AutoJs6 的 Rhino 脚本中通过全局模块 `camera` 使用相机. 下例拍一张照片, 并把 JPEG 保存为内部存储根目录下的 prism.jpg:

```js
let img = camera.capture({ path: files.join(files.getSdcardPath(), 'prism.jpg') });
toastLog(img.getWidth() + 'x' + img.getHeight());
img.recycle();
```

下例以 1280x720 连续取帧并识别条码, 识别到后关闭会话. 条码识别需要在 "插件中心" 启用 ML Kit Barcode 插件:

```js
let session = camera.open({ resolution: '1280x720' });
session.analyze((img) => {
    let text = barcode.recognizeText(img);
    if (text !== null) {
        toastLog(text);
        session.close();
    }
});
events.on('exit', () => session.close());
```

- `analyze` 回调中的帧只在回调执行期间有效; 要在回调之外使用, 先调用 `img.clone()` 得到副本, 用完后调用 `recycle()`.
- `camera.capture` 不能在 UI 线程上调用, ui 脚本的界面回调中请改用 `camera.captureAsync`.
- Node.js 脚本通过 `require('autojs6:camera')` 使用同一组方法, 返回值为 Promise; Node.js 与 Bun 中不能显示预览.
- 完整用法与错误码见 [AutoJs6 文档 > 相机 (Camera)](https://docs.autojs6.com/#/camera).

### 运行要求

- Android 7.0 或更高版本. 取景页与设置页不需要 AutoJs6.
- `camera` 模块需要 AutoJs6 6.8.1 或更高版本, 并在 "插件中心" 启用本插件.
- 相机权限由插件自己持有, AutoJs6 的相机权限不能代替. 可以在设置页的 "相机权限" 一行授予, 脚本也可以调用 `camera.requestPermission()`.
- 通知权限 (Android 13 及以上) 用于显示 "相机使用中" 通知; 网络权限只用于检查更新, 相机画面与照片不会离开设备.

### 常见问题

**打开相机时报告 `CAMERA_DISABLED` 的原因**

Android 11 及以上, AutoJs6 在后台或手机停在锁屏时, 系统不允许打开相机. 让 AutoJs6 回到前台后再打开即可; 在前台打开的会话退到后台或熄屏后照常取帧.

**相机被占用 (`CAMERA_BUSY`) 时的处理**

同一时间只有一个会话能使用相机, 错误的 `details.holder` 指出占用者: 脚本的路径, 或表示取景页的 `pluginUi`. 关闭占用相机的会话或取景页后再打开; 也可以在取景页上点按 "停止该脚本".

**照片的保存位置**

插件不保存照片, 也不写入系统相册. 只有脚本传入 `path` 时, AutoJs6 才把 JPEG 写到该路径.

各厂商系统的兼容情况与权限说明见 [项目主页](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
