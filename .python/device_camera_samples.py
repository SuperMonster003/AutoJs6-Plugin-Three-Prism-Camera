"""Runs the host `camera` script module against 3-Prism Camera on a device (roadmap P1.4 / P1.6 / P2.5 integration).

Usage: py -u .python/device_camera_samples.py <serial> [--host-tree ../AutoJs6]
       [--phases main,a5,ui,viewfinder,busyui,detect,barcode,background,denied,missing,disabled,toolow] [--seconds 30]
       [--screenshot <dir>] [--yolo-model <dir>] [--plugin-apk <apk>] [--too-low-apk <apk>] [--allow-device-changes] [--keep]

The host must already be a build with the `camera` module (versionCode >= 6641) and the plugin must be installed with the
camera permission granted. Each phase pushes one generated script, brings the host launcher activity to the front, runs
the script through RunIntentActivity and reads its result file (never logcat: OPD2413 drops console lines). One line per
check: PASS / FAIL / SKIP / INFO with the key values.

- main: module members, a second `camera.open()` gets CAMERA_BUSY naming the holder, `camera.capture({ ui: true })`
  while the script holds a session gets CAMERA_BUSY (appendix D Q22), and three samples of
  `assets-app/sample/3-Prism Camera`: "take a picture" (a file and its size), "recognize text in frames" (frames reach
  the callback; the target text is not expected in front of the camera); "viewfinder capture" waits for the shutter
  and runs in the viewfinder phase. The barcode and object detection samples need other plugins and are listed as
  SKIP.
- a5: the appendix A.5 examples as written: 1 `camera.capture({ path })` (a file and its size), 3 a frame kept with
  `img.clone()` and read after the callback and the session ended, 4 `camera.open().preview(ui.cam).torch(true).zoom(2)`
  chained on a worker thread of a ui script (appendix D Q44; the state must show the preview, the torch and zoom 2; a
  screenshot goes to --screenshot).
  Example 2 is the barcode phase, example 5 the viewfinder phase.
- ui: a ui script shows the preview in a `<SurfaceView>`, analyzes 640x480 frames and takes a picture every 10 s for
  --seconds; halfway through an emulator is turned to landscape (a phone: the person at the device turns it when the
  ROTATE line appears). Screenshots go to --screenshot.
- viewfinder (roadmap P3.1): the "viewfinder capture" sample runs twice; the first time the shutter of the plugin's
  viewfinder is tapped (the node must belong to the plugin package, found by its test tag in a uiautomator dump) and
  the script must get the photo and its file, the second time BACK closes the viewfinder and the result must be null.
- busyui (appendix D Q7 = (a)): a script holds a session and the plugin is opened from the launcher; the viewfinder
  must name the busy script, "stop this script" must end it (its exit handler writes the result) and the viewfinder
  must take the camera.
- detect (with --yolo-model and the YOLO NCNN plugin): "object detection with boxes" shows the preview in a floating
  window and draws the boxes for --seconds; frames must reach the callback, the boxes and labels are reported and the
  floating window is captured halfway. The host's overlay permission is allowed on an emulator (and put back), on a
  phone it must be granted already.
- barcode (with the ML Kit Barcode plugin; not in the default phases): "live barcode scan" runs for up to 60 s and the
  person at the device points the back camera at a barcode or QR code when the AIM line appears; the code must be read.
- background (an emulator or --allow-device-changes): the host is sent home and `camera.capture({ ui: true })` must
  fail with UI_REQUIRES_FOREGROUND.
- denied (an emulator or --allow-device-changes): the plugin's camera permission is revoked, `camera.open()` must fail
  with PERMISSION_DENIED, then the permission is granted again.
- missing (an emulator or --allow-device-changes, with --plugin-apk): the plugin is uninstalled, `camera.isAvailable()`
  must be false and `camera.open()` must fail with PLUGIN_NOT_INSTALLED, then the APK is installed again and granted.
- disabled (an emulator or --allow-device-changes; the host must be debuggable): the plugin's Plugin Center switch is
  turned off in the host's enable store, `camera.isAvailable()` must be false and `camera.open()` /
  `camera.capture({ ui: true })` must fail with PLUGIN_DISABLED, then the previous store file is put back.
- toolow (an emulator or --allow-device-changes, with --too-low-apk and --plugin-apk): a plugin build that asks for a
  newer host build than the installed one (for example `requiredHostVersion` 6642 against host 6641) replaces the
  plugin, the same calls must fail with HOST_VERSION_TOO_LOW, then the normal build is installed again.

zh-CN: 在设备上用宿主 `camera` 脚本模块驱动 3-Prism Camera (路线图 P1.4 / P1.6 / P2.5 联调). 宿主须为带 `camera` 模块的构建 (versionCode >= 6641),
插件须已安装并获得相机权限. 每个阶段推送一个生成的脚本, 把宿主启动器活动切到前台, 经 RunIntentActivity 运行, 读取其结果文件 (不读 logcat:
OPD2413 会丢 console 行). 每项检查一行 PASS / FAIL / SKIP / INFO 与关键值. main: 模块成员, 第二次 `camera.open()` 得到 CAMERA_BUSY 并写明占用者,
持有会话时 `camera.capture({ ui: true })` 得到 CAMERA_BUSY (附录 D Q22), 以及两个样例 ("拍一张存文件", "连续帧识别文字" 只要求帧到达回调);
"取景页拍照" 要等快门, 放在 viewfinder 阶段; 扫码与物体检测样例需要其他插件, 记 SKIP. a5: 附录 A.5 示例的原样写法: 示例 1
`camera.capture({ path })` (文件与尺寸), 示例 3 用 `img.clone()` 保留一帧并在回调与会话结束后读取, 示例 4 在 ui 脚本的工作线程上链式调用
`camera.open().preview(ui.cam).torch(true).zoom(2)` (附录 D Q44; 状态应显示预览, 手电与 2 倍变焦; 截图存入 --screenshot); 示例 2 即 barcode 阶段, 示例 5 即
viewfinder 阶段. ui: ui 脚本在 `<SurfaceView>` 中预览,
同时分析 640x480 帧并每 10 秒拍一张, 持续 --seconds 秒; 过半时模拟器转为横屏 (真机在出现 ROTATE 行时由设备旁的人转动), 截图存入 --screenshot.
viewfinder (路线图 P3.1): "取景页拍照" 样例运行两次, 第一次点按插件取景页的快门 (按测试标签在 uiautomator 转储中查找, 节点必须属于插件包),
脚本应拿到照片与文件, 第二次按返回键关闭取景页, 结果应为 null. busyui (附录 D Q7 = (a)): 脚本持有会话时从启动器打开插件, 取景页应写明占用的脚本,
"停止该脚本" 应结束它 (由其 exit 处理器写结果), 随后取景页接过相机. detect (需 --yolo-model 与 YOLO NCNN 插件): "物体检测画框" 在悬浮窗中预览并画框
--seconds 秒, 帧应到达回调, 报告框数与标签, 中途截取悬浮窗; 模拟器上代为允许宿主悬浮窗权限 (结束后恢复), 真机须已授予. barcode (需 ML Kit Barcode
插件, 不在默认阶段中): "实时扫码" 最长 60 秒, 出现 AIM 行时由设备旁的人把后置相机对准条码或二维码, 应读出内容.
background / denied / missing 只在模拟器或给出 --allow-device-changes 时运行: 宿主回到桌面后 `camera.capture({ ui: true })` 应为
UI_REQUIRES_FOREGROUND; 撤销插件相机权限后 `camera.open()` 应为 PERMISSION_DENIED, 之后重新授予; 卸载插件后 `camera.isAvailable()` 为 false 且
`camera.open()` 为 PLUGIN_NOT_INSTALLED, 之后用 --plugin-apk 重装并授权. disabled 与 toolow 同样只在模拟器或给出 --allow-device-changes 时运行:
disabled (宿主须可调试) 在宿主的启用状态存储中关闭插件的插件中心开关, `camera.isAvailable()` 应为 false, `camera.open()` 与
`camera.capture({ ui: true })` 应为 PLUGIN_DISABLED, 之后放回原存储文件; toolow 需要 --too-low-apk 与 --plugin-apk, 先装一个要求更高宿主构建的插件包
(例如 `requiredHostVersion` 6642 对宿主 6641), 同样的调用应为 HOST_VERSION_TOO_LOW, 之后重装正常构建.
"""
import argparse
import pathlib
import re
import subprocess
import sys
import time

HOST = "org.autojs.autojs6"
PLUGIN = "io.github.supermonster003.autojs6.plugin.three.prism.camera"
RUN_ACTIVITY = "org.autojs.autojs.external.open.RunIntentActivity"
CAPTURE_ACTIVITY = "ThreePrismCameraCaptureActivity"
DEVICE_DIR = "/sdcard/Download/aj6-prism-check"
SAMPLE_DIR = "app/src/main/assets-app/sample/3-Prism Camera"
SHOT = "拍一张存文件 [v6.8.1+].js"
OCR = "连续帧识别文字 [v6.8.1+].js"
VIEWFINDER = "取景页拍照 [v6.8.1+].js"
BARCODE = "实时扫码 [v6.8.1+].js"
BARCODE_PLUGIN = "io.github.supermonster003.autojs6.plugin.mlkit.barcode"
YOLO_PLUGIN = "io.github.supermonster003.autojs6.plugin.yolo.ncnn"
DETECT = "物体检测画框 [v6.8.1+].js"

COMMON = r"""
var DIR = '%(dir)s';
var RESULT = files.join(DIR, '%(result)s');
function out(line) { files.append(RESULT, line + '\n'); }
function report(name, state, detail) { out(name + ' ' + state + ' ' + String(detail).replace(/\s+/g, ' ').slice(0, 300)); }
function assert(cond, msg) { if (!cond) throw new Error(msg); }
function describe(e) { return e && e.code ? '[' + e.code + '] ' + e.message : String(e); }
function check(name, fn) {
    try { report(name, 'PASS', fn()); } catch (e) { report(name, 'FAIL', describe(e)); }
}
function codeOf(fn) {
    try { fn(); } catch (e) { return e; }
    return null;
}
function waitFor(fn, millis) {
    var end = Date.now() + millis;
    var value = fn();
    while (!value && Date.now() < end) { sleep(300); value = fn(); }
    return value;
}
function runSample(name, extra, millis) {
    var output = files.join(DIR, 'out-' + Date.now() + '.json');
    var argv = { output: output };
    for (var key in extra) argv[key] = extra[key];
    engines.execScriptFile(files.join(DIR, 'samples', name), { arguments: argv });
    assert(waitFor(function () { return files.exists(output); }, millis), 'no summary within ' + millis / 1000 + ' s');
    sleep(200);
    return JSON.parse(files.read(output));
}
"""

MAIN = r"""
check('module', function () {
    var names = ['isAvailable', 'requestPermission', 'list', 'open', 'capture'];
    var missing = names.filter(function (n) { return typeof camera[n] !== 'function'; });
    assert(missing.length === 0, 'camera lacks ' + missing.join(', '));
    assert(typeof camera.CameraError === 'function', 'CameraError');
    assert(camera.isAvailable() === true, 'isAvailable() = ' + camera.isAvailable());
    return 'isAvailable=true cameras=' + camera.list().length;
});
check('busy', function () {
    var first = camera.open({ facing: 'back' });
    try {
        var e = codeOf(function () { camera.open({ facing: 'back' }).close(); });
        assert(e && e.code === 'CAMERA_BUSY', 'second open: ' + describe(e));
        assert(e instanceof camera.CameraError, 'not a CameraError');
        var holder = e.details && e.details.holder;
        assert(holder && String(holder).indexOf('check-main') >= 0, 'holder ' + holder);
        var ui = codeOf(function () { camera.capture({ ui: true }); });
        assert(ui && ui.code === 'CAMERA_BUSY', 'capture({ ui: true }) while holding: ' + describe(ui));
        return 'CAMERA_BUSY holder=' + files.getName(String(holder)) + ' uiWhileHolding=' + ui.code;
    } finally {
        first.close();
    }
});
check('sampleShot', function () {
    var path = files.join(DIR, 'shot.jpg');
    var s = runSample('%(shot)s', { file: path, facing: 'back' }, 60000);
    assert(s.ok === true, JSON.stringify(s));
    assert(files.exists(path), 'no file');
    return s.width + 'x' + s.height + ' bytes=' + new java.io.File(path).length();
});
check('sampleFrames', function () {
    var s = runSample('%(ocr)s', { text: 'AutoJs6', file: files.join(DIR, 'ocr-hit.png') }, 90000);
    assert(s.frames > 0, JSON.stringify(s));
    return 'reason=' + s.reason + ' frames=' + s.frames + ' dropped=' + s.dropped + ' found=' + s.found;
});
report('sampleViewfinder', 'SKIP', 'waits for the shutter: see the viewfinder phase');
report('sampleBarcode', 'SKIP', 'needs a code in front of the camera: see the barcode phase');
report('sampleDetect', 'SKIP', 'needs a model directory and the overlay permission: see the detect phase');
out('done');
"""

UI = r""""ui";
%(common)s
var SECONDS = %(seconds)d;
ui.layout(
    <frame>
        <SurfaceView id="cam" w="*" h="*"/>
    </frame>
);
// Frame callbacks reach the thread that called analyze() through its event loop, so the worker only sets timers and
// returns; a sleeping loop would starve the callbacks and every frame would count as dropped.
// zh-CN: 帧回调经调用 analyze() 的线程的事件循环送达, 因此工作线程只设定时器后返回; 带 sleep 的循环会让回调得不到执行, 每帧都计为丢弃.
threads.start(function () {
    var errors = [];
    var frames = 0;
    var shots = [];
    var session = null;
    var finished = false;
    function finish(name, status, detail) {
        if (finished) return;
        finished = true;
        report(name, status, detail);
        try { session && session.close(); } catch (e) { report('uiClose', 'FAIL', describe(e)); }
        out('done');
        ui.post(function () { ui.finish(); });
    }
    try {
        session = camera.open({ facing: 'back' });
        session.on('error', function (e) { errors.push(e.code); });
        session.preview(ui.cam);
        session.analyze(function (img, meta) { frames += 1; }, { resolution: '640x480' });
    } catch (e) {
        finish('uiReady', 'FAIL', describe(e));
        return;
    }
    var readyDeadline = Date.now() + 15000;
    var readyTimer = setInterval(function () {
        var st = session.state();
        var ready = st.previewing && st.analyzing && frames > 0;
        if (!ready && Date.now() < readyDeadline) return;
        clearInterval(readyTimer);
        report('uiReady', ready ? 'PASS' : 'FAIL', 'previewing=' + st.previewing + ' analyzing=' + st.analyzing + ' frames=' + frames);
        if (!ready) { finish('uiRun', 'FAIL', 'not ready'); return; }
        out('ready');
        var startFrames = frames;
        var shoot = function () {
            if (finished) return;
            try {
                var img = session.capture({ quality: 80 });
                shots.push(img.getWidth() + 'x' + img.getHeight());
                img.recycle();
            } catch (e) {
                errors.push(e.code || describe(e));
            }
        };
        setTimeout(shoot, 3000);
        var shotTimer = setInterval(shoot, 10000);
        setTimeout(function () {
            clearInterval(shotTimer);
            var st = session.state();
            var counted = frames - startFrames;
            var ok = st.previewing && st.analyzing && errors.length === 0 && counted > SECONDS * 5 && shots.length >= 3;
            finish('uiRun', ok ? 'PASS' : 'FAIL', 'seconds=' + SECONDS + ' frames=' + counted + ' fps=' + (counted / SECONDS).toFixed(1) +
                ' dropped=' + st.dropped + ' shots=' + shots.join(',') + ' previewing=' + st.previewing + ' analyzing=' + st.analyzing +
                ' errors=' + errors.join(','));
        }, SECONDS * 1000 + 500);
    }, 200);
});
"""

# Appendix A.5 examples 1 and 3 as written (example 2 is the barcode phase, example 5 the viewfinder phase).
# zh-CN: 附录 A.5 示例 1 与 3 的原样写法 (示例 2 即 barcode 阶段, 示例 5 即 viewfinder 阶段).
A5 = r"""
check('a5Capture', function () {
    var path = files.join(DIR, 'a5.jpg');
    var img = camera.capture({ path: path });
    try {
        assert(files.exists(path), 'no file');
        return img.getWidth() + 'x' + img.getHeight() + ' bytes=' + new java.io.File(path).length();
    } finally {
        img.recycle();
    }
});
var session = camera.open({ resolution: '1280x720' });
var kept = null;
var frames = 0;
session.analyze(function (img) { frames += 1; if (!kept) kept = img.clone(); });
var deadline = Date.now() + 15000;
var timer = setInterval(function () {
    if (!kept && Date.now() < deadline) return;
    clearInterval(timer);
    try {
        session.close();
        assert(kept, 'no frame within 15 s');
        // The callback's own frame is recycled by now; the clone outlives the callback and the session.
        // zh-CN: 回调自己的帧此时已回收; 副本在回调与会话结束后仍然有效.
        var w = kept.getWidth();
        var h = kept.getHeight();
        var center = colors.toString(images.pixel(kept, Math.floor(w / 2), Math.floor(h / 2)));
        kept.recycle();
        report('a5Clone', 'PASS', w + 'x' + h + ' frames=' + frames + ' center=' + center);
    } catch (e) {
        report('a5Clone', 'FAIL', describe(e));
    }
    out('done');
}, 200);
"""

# Appendix A.5 example 4: open, preview, torch and zoom chained (layout tag per appendix D Q23). The chain runs on a
# worker thread: `camera.open()` on the UI thread of a ui script waits for the plugin binding, whose callback needs that
# same thread, and the host stops responding (appendix D Q44).
# zh-CN: 附录 A.5 示例 4: 链式调用 open, preview, torch 与 zoom (布局标签按附录 D Q23). 调用链在工作线程上运行: 在 ui 脚本的 UI 线程上调用
# `camera.open()` 会等待插件绑定, 而绑定回调需要同一线程, 宿主随之无响应 (附录 D Q44).
A5_UI = r""""ui";
%(common)s
ui.layout(<frame><SurfaceView id="cam" w="*" h="*"/></frame>);
threads.start(function () {
    var s = null;
    try {
        s = camera.open().preview(ui.cam).torch(true).zoom(2);
    } catch (e) {
        report('a5Preview', 'FAIL', describe(e));
        out('done');
        ui.post(function () { ui.finish(); });
        return;
    }
    var deadline = Date.now() + 10000;
    var timer = setInterval(function () {
        var st = s.state();
        if (!st.previewing && Date.now() < deadline) return;
        clearInterval(timer);
        out('step:preview');
        setTimeout(function () {
            var st = s.state();
            var ok = st.previewing && st.torch === true && Math.abs(st.zoom - 2) < 0.05;
            report('a5Preview', ok ? 'PASS' : 'FAIL', 'previewing=' + st.previewing + ' torch=' + st.torch + ' zoom=' + st.zoom);
            s.torch(false);
            s.close();
            out('done');
            ui.post(function () { ui.finish(); });
        }, 3000);
    }, 200);
});
"""

BACKGROUND = r"""
out('ready');
waitFor(function () { return files.exists(files.join(DIR, 'go-background')); }, 30000);
sleep(1500);
check('background', function () {
    var e = codeOf(function () { camera.capture({ ui: true }); });
    assert(e && e.code === 'UI_REQUIRES_FOREGROUND', describe(e));
    return 'capture({ ui: true }) in the background: ' + e.code;
});
out('done');
"""

VIEWFINDER_STEPS = r"""
function viewfinder(step) {
    out('step:' + step);
    return runSample('%(viewfinder)s', { file: files.join(DIR, 'viewfinder-' + step + '.jpg') }, 120000);
}
check('viewfinderTake', function () {
    var s = viewfinder('take');
    assert(s.taken === true && s.width > 0 && s.height > 0, JSON.stringify(s));
    var bytes = new java.io.File(s.path).length();
    assert(bytes > 0, 'empty file ' + s.path);
    return s.width + 'x' + s.height + ' bytes=' + bytes;
});
check('viewfinderCancel', function () {
    var s = viewfinder('cancel');
    assert(s.taken === false && !s.code, JSON.stringify(s));
    return 'capture({ ui: true }) = null';
});
out('done');
"""

BUSY_UI = r"""
var stopped = false;
events.on('exit', function () {
    if (stopped) return;
    report('busyStop', 'PASS', 'the script ended after "stop this script" on the viewfinder');
    out('done');
});
var session = camera.open({ facing: 'back' });
out('step:busy');
var end = Date.now() + 90000;
while (Date.now() < end) sleep(500);
stopped = true;
session.close();
report('busyStop', 'FAIL', 'the script still ran 90 s later');
out('done');
"""

DENIED = r"""
check('denied', function () {
    var e = codeOf(function () { camera.open().close(); });
    assert(e && e.code === 'PERMISSION_DENIED', describe(e));
    return 'open: ' + e.code;
});
out('done');
"""

MISSING = r"""
check('missing', function () {
    assert(camera.isAvailable() === false, 'isAvailable() = ' + camera.isAvailable());
    var e = codeOf(function () { camera.open().close(); });
    assert(e && e.code === 'PLUGIN_NOT_INSTALLED', describe(e));
    return 'isAvailable=false open: ' + e.code;
});
out('done');
"""

# "Live barcode scan": the person at the device points the back camera at a barcode or QR code within 60 s.
# zh-CN: "实时扫码": 设备旁的人在 60 秒内把后置相机对准条码或二维码.
BARCODE_STEP = r"""
out('step:barcode');
check('sampleBarcode', function () {
    var s = runSample('%(barcode)s', {}, 90000);
    assert(s.failure === null, 'barcode module: ' + s.failure);
    assert(s.frames > 0, JSON.stringify(s));
    assert(s.value !== null, 'no code within 60 s: ' + JSON.stringify(s));
    return 'value="' + String(s.value).slice(0, 80) + '" reason=' + s.reason + ' frames=' + s.frames + ' dropped=' + s.dropped;
});
out('done');
"""

# "Object detection with boxes": the preview in a floating window with boxes drawn over it for --seconds.
# zh-CN: "物体检测画框": 悬浮窗中预览并在其上画框, 持续 --seconds 秒.
DETECT_STEP = r"""
out('step:detect');
check('sampleDetect', function () {
    var s = runSample('%(detect)s', { model: '%(model)s', seconds: %(seconds)d }, %(seconds)d * 1000 + 60000);
    assert(s.frames > 0, JSON.stringify(s));
    return 'reason=' + s.reason + ' frames=' + s.frames + ' boxes=' + s.detections + ' labels=' + (s.labels.join('/') || '-');
});
out('done');
"""

# The manual error paths of the card: "not enabled" (Plugin Center switch off) and "host too old" (the plugin asks for a
# newer host build than the one installed). zh-CN: 卡片要求的人工错误路径: "未启用" (插件中心开关关闭) 与 "宿主版本过低" (插件要求的宿主构建高于已装宿主).
UNAVAILABLE = r"""
check('%(name)s', function () {
    assert(camera.isAvailable() === false, 'isAvailable() = ' + camera.isAvailable());
    var e = codeOf(function () { camera.open().close(); });
    assert(e && e.code === '%(code)s', describe(e));
    var u = codeOf(function () { camera.capture({ ui: true }); });
    assert(u && u.code === '%(code)s', 'capture({ ui: true }): ' + describe(u));
    return 'isAvailable=false open: ' + e.code + ' capture(ui): ' + u.code + ' message="' + String(e.message).slice(0, 160) + '"';
});
out('done');
"""

ENABLE_PREFS = "shared_prefs/plugin_center_enable_state.xml"
ENABLE_KEY = "key_$_enabled_plugin_$_" + PLUGIN


class Device:
    def __init__(self, serial):
        self.serial = serial

    def run(self, *args, check=False, timeout=120):
        return subprocess.run(["adb", "-s", self.serial, *args], capture_output=True, text=True, encoding="utf-8",
                              errors="replace", check=check, timeout=timeout)

    def shell(self, command, timeout=120):
        return self.run("shell", command, timeout=timeout).stdout

    @property
    def emulator(self):
        return self.serial.startswith("emulator-")

    def launcher(self):
        out = self.shell("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER " + HOST)
        for line in out.splitlines():
            line = line.strip()
            if line.startswith(HOST + "/") and "leakcanary" not in line:
                return line
        return None

    def front(self):
        self.shell("am force-stop " + HOST)
        activity = self.launcher()
        if activity:
            self.shell("am start -n " + activity)
            time.sleep(4)

    def plugin_launcher(self):
        out = self.shell("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER " + PLUGIN)
        lines = [line.strip() for line in out.splitlines() if line.strip().startswith(PLUGIN + "/")]
        return lines[-1] if lines else None

    def viewfinder_resumed(self, seconds=20):
        end = time.time() + seconds
        while time.time() < end:
            out = self.shell("dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity'")
            # From the launcher the record names the icon alias, from openCaptureUi the activity itself.
            # zh-CN: 从启动器进入时记录的是图标别名, 经 openCaptureUi 进入时是 Activity 本身.
            if PLUGIN + "/" in out and (CAPTURE_ACTIVITY in out or "IconAlias" in out):
                return True
            time.sleep(0.5)
        return False

    def nodes(self):
        """Nodes of the plugin's own window as (resource id, bounds), from a uiautomator dump.
        zh-CN: 插件自身窗口的节点 (资源 id, 边界), 取自 uiautomator 转储."""
        self.shell("uiautomator dump %s/ui.xml" % DEVICE_DIR, timeout=60)
        xml = self.read("ui.xml")
        found = []
        for node in re.finditer(r"<node [^>]*>", xml):
            text = node.group(0)
            rid = re.search(r'resource-id="([^"]*)"', text)
            package = re.search(r'package="([^"]*)"', text)
            bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', text)
            if rid and rid.group(1) and package and package.group(1) == PLUGIN and bounds:
                found.append((rid.group(1), tuple(int(v) for v in bounds.groups())))
        return found

    def find_tag(self, tag, seconds=15):
        """Bounds of a control of the plugin's own window, waiting up to [seconds]. zh-CN: 插件自身窗口中控件的边界, 最多等待 [seconds] 秒."""
        end = time.time() + seconds
        while time.time() < end:
            for rid, bounds in self.nodes():
                if rid == tag:
                    return bounds
            time.sleep(1)
        return None

    def tap_tag(self, tag, seconds=15):
        """Taps a control of the plugin's own viewfinder (never a system or permission dialog: the node must belong to
        the plugin package). zh-CN: 点按插件自身取景页的控件 (节点必须属于插件包, 绝不点系统或权限对话框)."""
        bounds = self.find_tag(tag, seconds)
        if bounds is None:
            return False
        left, top, right, bottom = bounds
        self.shell("input tap %d %d" % ((left + right) // 2, (top + bottom) // 2))
        return True

    def has_tag(self, tag):
        return any(rid == tag for rid, _ in self.nodes())

    def read(self, name):
        return self.shell("cat '%s/%s' 2>/dev/null" % (DEVICE_DIR, name))

    def host_run_as(self, command, data=None):
        """A command in the data directory of the (debuggable) host, [data] on its standard input.
        zh-CN: 在 (可调试的) 宿主数据目录中执行命令, [data] 作为标准输入."""
        return subprocess.run(["adb", "-s", self.serial, "shell", "run-as %s sh -c '%s'" % (HOST, command)], input=data,
                              capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60)

    def screenshot(self, folder, name):
        if not folder:
            return
        folder.mkdir(parents=True, exist_ok=True)
        data = subprocess.run(["adb", "-s", self.serial, "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout
        (folder / name).write_bytes(data)
        print("     screenshot=%s (%d bytes)" % (name, len(data)), flush=True)


def run_phase(device, name, body, timeout, on_line=None):
    result = "result-%s.txt" % name
    local = pathlib.Path(__file__).resolve().parent / ("device_camera_samples.%s.generated.js" % name)
    local.write_text(body.replace("%(result)s", result), encoding="utf-8")
    remote = "%s/check-%s.js" % (DEVICE_DIR, name)
    try:
        device.run("push", str(local), remote, check=True)
    finally:
        local.unlink()
    device.shell("rm -f '%s/%s'" % (DEVICE_DIR, result))
    device.front()
    device.shell("am start -n %s/%s -e path '%s'" % (HOST, RUN_ACTIVITY, remote))
    seen = 0
    deadline = time.time() + timeout
    while time.time() < deadline:
        time.sleep(1)
        lines = [line for line in device.read(result).splitlines() if line.strip()]
        for line in lines[seen:]:
            if line in ("ready", "done") or line.startswith("step:"):
                if on_line:
                    on_line(line)
            else:
                print(line, flush=True)
        # An adb read can come back empty now and then; never step back, or a line would be handled twice.
        # zh-CN: adb 读取偶尔返回空内容; 计数不回退, 否则同一行会被处理两次.
        seen = max(seen, len(lines))
        if "done" in lines:
            return True
    print("%s FAIL no result within %d s" % (name, timeout), flush=True)
    return False


def detect_phase(device, args, common, shots, changes):
    """Pushes the model directory, checks the YOLO NCNN plugin and the host's overlay permission (allowed on an emulator
    or with --allow-device-changes and put back afterwards, otherwise left to the person at the device), runs the sample
    and takes a screenshot of the floating preview halfway through.
    zh-CN: 推送模型目录, 检查 YOLO NCNN 插件与宿主的悬浮窗权限 (模拟器或给出 --allow-device-changes 时代为允许并在结束后恢复, 否则由设备旁的人授予),
    运行样例并在中途截取悬浮窗预览."""
    if not args.yolo_model:
        print("sampleDetect SKIP needs --yolo-model (a directory with model.json, model.ncnn.param and model.ncnn.bin)", flush=True)
        return
    if not device.shell("pm path " + YOLO_PLUGIN).strip():
        print("sampleDetect SKIP the YOLO NCNN plugin is not installed", flush=True)
        return
    overlay = device.shell("appops get %s SYSTEM_ALERT_WINDOW" % HOST)
    previous = None
    if "allow" not in overlay:
        if not changes:
            print("sampleDetect SKIP the host has no overlay permission (granted by the person at the device)", flush=True)
            return
        mode = re.search(r"SYSTEM_ALERT_WINDOW: (\w+)", overlay)
        previous = mode.group(1) if mode else "default"
        device.shell("appops set %s SYSTEM_ALERT_WINDOW allow" % HOST)
    model = DEVICE_DIR + "/models/yolo11n"
    device.shell("mkdir -p '%s'" % model)
    for name in ("model.json", "model.ncnn.param", "model.ncnn.bin"):
        device.run("push", str(pathlib.Path(args.yolo_model) / name), "%s/%s" % (model, name), check=True)

    def on_detect(line):
        if line == "step:detect":
            time.sleep(max(8, args.seconds // 2))
            device.screenshot(shots, "detect-floaty-%s.png" % args.serial)

    try:
        body = common + fill(DETECT_STEP, detect=DETECT, model=model, seconds=args.seconds)
        run_phase(device, "detect", body, args.seconds + 150, on_detect)
    finally:
        if previous is not None:
            device.shell("appops set %s SYSTEM_ALERT_WINDOW %s" % (HOST, previous))
            print("INFO overlayRestored=%s" % previous, flush=True)


def fill(template, **values):
    text = template
    for key, value in values.items():
        text = text.replace("%(" + key + ")s", str(value)).replace("%(" + key + ")d", str(value))
    return text


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("serial")
    parser.add_argument("--host-tree", default=str(pathlib.Path(__file__).resolve().parents[2] / "AutoJs6"))
    parser.add_argument("--phases", default="main,a5,ui,viewfinder,busyui,detect,background,denied,missing,disabled,toolow")
    parser.add_argument("--seconds", type=int, default=30)
    parser.add_argument("--screenshot", default="")
    parser.add_argument("--plugin-apk", default="")
    parser.add_argument("--too-low-apk", default="")
    parser.add_argument("--yolo-model", default="")
    parser.add_argument("--allow-device-changes", action="store_true")
    parser.add_argument("--keep", action="store_true")
    args = parser.parse_args()
    device = Device(args.serial)
    shots = pathlib.Path(args.screenshot) if args.screenshot else None
    host_code = device.shell("dumpsys package %s | grep -m1 versionCode" % HOST).split("versionCode=")[-1].split()[0:1]
    granted = "android.permission.CAMERA: granted=true" in device.shell("dumpsys package " + PLUGIN)
    print("INFO host=%s pluginCameraGranted=%s" % (host_code[0] if host_code else "-", granted), flush=True)
    changes = device.emulator or args.allow_device_changes
    samples = pathlib.Path(args.host_tree) / SAMPLE_DIR
    common = fill(COMMON, dir=DEVICE_DIR)
    device.shell("rm -r '%s'; mkdir -p '%s/samples'" % (DEVICE_DIR, DEVICE_DIR))
    for sample in sorted(samples.glob("*.js")):
        device.run("push", str(sample), "%s/samples/%s" % (DEVICE_DIR, sample.name), check=True)
    phases = [p.strip() for p in args.phases.split(",") if p.strip()]
    try:
        if "main" in phases:
            run_phase(device, "main", common + fill(MAIN, shot=SHOT, ocr=OCR, viewfinder=VIEWFINDER), 420)
        if "a5" in phases:
            run_phase(device, "a5", common + A5, 120)

            def on_a5(line):
                if line == "step:preview" and shots:
                    device.screenshot(shots, "a5-preview-%s.png" % args.serial)

            run_phase(device, "a5ui", fill(A5_UI, common=common), 90, on_a5)
        if "ui" in phases:
            state = {"rotated": False}
            rotation = device.shell("settings get system accelerometer_rotation").strip()

            def on_ui(line):
                if line != "ready":
                    return
                time.sleep(3)
                device.screenshot(shots, "ui-preview-%s-portrait.png" % args.serial)
                time.sleep(max(1, args.seconds // 2 - 6))
                if device.emulator:
                    device.shell("settings put system accelerometer_rotation 1")
                    device.run("emu", "sensor", "set", "acceleration", "9.81:0:0")
                    state["rotated"] = True
                else:
                    print("ROTATE the person at the device turns it to landscape now", flush=True)
                time.sleep(5)
                device.screenshot(shots, "ui-preview-%s-landscape.png" % args.serial)

            run_phase(device, "ui", fill(UI, common=common, seconds=args.seconds), args.seconds + 120, on_ui)
            if state["rotated"]:
                device.run("emu", "sensor", "set", "acceleration", "0:9.81:0")
                device.shell("settings put system accelerometer_rotation " + (rotation if rotation in ("0", "1") else "1"))
        if "viewfinder" in phases:
            def on_viewfinder(line):
                step = line[len("step:"):] if line.startswith("step:") else None
                if step not in ("take", "cancel"):
                    return
                if not device.viewfinder_resumed():
                    print("viewfinder-%s FAIL the viewfinder did not come to the front" % step, flush=True)
                    return
                if step == "take":
                    # The shutter appears once the preview is bound; the screenshot waits for it.
                    # zh-CN: 预览绑定后才出现快门; 截图等它出现.
                    if device.find_tag("prism.capture.shutter", 20) is None:
                        print("viewfinder-take FAIL no shutter in the plugin window", flush=True)
                        return
                    time.sleep(2)
                    device.screenshot(shots, "viewfinder-script-%s.png" % args.serial)
                    device.tap_tag("prism.capture.shutter")
                else:
                    time.sleep(3)
                    device.shell("input keyevent 4")

            run_phase(device, "viewfinder", common + fill(VIEWFINDER_STEPS, viewfinder=VIEWFINDER), 300, on_viewfinder)
        if "busyui" in phases:
            def on_busy(line):
                if line != "step:busy":
                    return
                time.sleep(1)
                launcher = device.plugin_launcher()
                device.shell("am start -n " + launcher if launcher else "monkey -p %s -c android.intent.category.LAUNCHER 1" % PLUGIN)
                if not device.viewfinder_resumed():
                    print("busyView FAIL the viewfinder did not come to the front", flush=True)
                    return
                time.sleep(2)
                busy = device.has_tag("prism.capture.stopScript")
                print("busyView %s stopScript=%s" % ("PASS" if busy else "FAIL", busy), flush=True)
                device.screenshot(shots, "viewfinder-busy-%s.png" % args.serial)
                if busy and device.tap_tag("prism.capture.stopScript"):
                    time.sleep(4)
                    taken = device.has_tag("prism.capture.shutter")
                    print("busyTakeOver %s shutter=%s" % ("PASS" if taken else "FAIL", taken), flush=True)
                    device.screenshot(shots, "viewfinder-after-stop-%s.png" % args.serial)
                device.shell("input keyevent 4")

            run_phase(device, "busyui", common + BUSY_UI, 180, on_busy)
        if "detect" in phases:
            detect_phase(device, args, common, shots, changes)
        if "barcode" in phases:
            if not device.shell("pm path " + BARCODE_PLUGIN).strip():
                print("sampleBarcode SKIP the ML Kit Barcode plugin is not installed", flush=True)
            else:
                def on_barcode(line):
                    if line == "step:barcode":
                        print("AIM the person at the device points the back camera at a barcode or QR code now (60 s)", flush=True)

                run_phase(device, "barcode", common + fill(BARCODE_STEP, barcode=BARCODE), 150, on_barcode)
        if not changes:
            for name in ("background", "denied", "missing", "disabled", "toolow"):
                if name in phases:
                    print("%s SKIP changes the device (an emulator or --allow-device-changes)" % name, flush=True)
            return
        if "background" in phases:
            def on_background(line):
                if line == "ready":
                    device.shell("input keyevent 3")
                    device.shell("touch '%s/go-background'" % DEVICE_DIR)

            run_phase(device, "background", common + BACKGROUND, 120, on_background)
        if "denied" in phases:
            device.shell("pm revoke %s android.permission.CAMERA" % PLUGIN)
            try:
                run_phase(device, "denied", common + DENIED, 120)
            finally:
                device.shell("pm grant %s android.permission.CAMERA" % PLUGIN)
        if "missing" in phases:
            if not args.plugin_apk:
                print("missing SKIP needs --plugin-apk to install the plugin again", flush=True)
            else:
                device.run("uninstall", PLUGIN)
                try:
                    run_phase(device, "missing", common + MISSING, 120)
                finally:
                    device.run("install", "-r", "-g", args.plugin_apk, timeout=300)
                    print("INFO reinstalled=%s" % bool(device.shell("pm path " + PLUGIN).strip()), flush=True)
        if "disabled" in phases:
            # The Plugin Center switch of the plugin is turned off in the host's enable store (written while the host is
            # stopped), then the previous file is put back. zh-CN: 在宿主的启用状态存储中关闭插件的插件中心开关 (宿主停止时写入), 之后放回原文件.
            saved = device.host_run_as("cat %s 2>/dev/null" % ENABLE_PREFS).stdout
            entry = '    <boolean name="%s" value="false" />\n' % ENABLE_KEY
            if "</map>" in saved:
                xml = re.sub(r'[ \t]*<boolean name="%s" value="(?:true|false)" />\r?\n' % re.escape(ENABLE_KEY), "", saved)
                xml = xml.replace("</map>", entry + "</map>")
            else:
                xml = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n" + entry + "</map>\n"
            device.shell("am force-stop " + HOST)
            device.host_run_as("cat > %s" % ENABLE_PREFS, xml)
            try:
                run_phase(device, "disabled", common + fill(UNAVAILABLE, name="disabled", code="PLUGIN_DISABLED"), 120)
            finally:
                device.shell("am force-stop " + HOST)
                if "</map>" in saved:
                    device.host_run_as("cat > %s" % ENABLE_PREFS, saved)
                else:
                    device.host_run_as("rm -f %s" % ENABLE_PREFS)
                restored = device.host_run_as("cat %s 2>/dev/null" % ENABLE_PREFS).stdout == saved
                print("INFO enableStateRestored=%s" % restored, flush=True)
        if "toolow" in phases:
            if not (args.too_low_apk and args.plugin_apk):
                print("toolow SKIP needs --too-low-apk (a build asking for a newer host) and --plugin-apk (the normal build)", flush=True)
            else:
                device.run("install", "-r", "-g", args.too_low_apk, timeout=300)
                try:
                    run_phase(device, "toolow", common + fill(UNAVAILABLE, name="toolow", code="HOST_VERSION_TOO_LOW"), 120)
                finally:
                    device.run("install", "-r", "-g", args.plugin_apk, timeout=300)
                    print("INFO reinstalled=%s" % bool(device.shell("pm path " + PLUGIN).strip()), flush=True)
    finally:
        if not args.keep:
            device.shell("rm -r '%s'" % DEVICE_DIR)


if __name__ == "__main__":
    sys.exit(main())
