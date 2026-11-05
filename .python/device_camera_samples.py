"""Runs the host `camera` script module against 3-Prism Camera on a device (roadmap P1.4 / P1.6 / P2.5 integration).

Usage: py -u .python/device_camera_samples.py <serial> [--host-tree ../AutoJs6] [--phases main,ui,background,denied,missing]
       [--seconds 30] [--screenshot <dir>] [--plugin-apk <apk>] [--allow-device-changes] [--keep]

The host must already be a build with the `camera` module (versionCode >= 6641) and the plugin must be installed with the
camera permission granted. Each phase pushes one generated script, brings the host launcher activity to the front, runs
the script through RunIntentActivity and reads its result file (never logcat: OPD2413 drops console lines). One line per
check: PASS / FAIL / SKIP / INFO with the key values.

- main: module members, a second `camera.open()` gets CAMERA_BUSY naming the holder, `camera.capture({ ui: true })`
  while the script holds a session gets CAMERA_BUSY (appendix D Q22), and three samples of
  `assets-app/sample/3-Prism Camera`: "take a picture" (a file and its size), "recognize text in frames" (frames reach
  the callback; the target text is not expected in front of the camera) and "viewfinder capture" (UNSUPPORTED until
  roadmap P3.1 advertises `captureUi`). The barcode and object detection samples need other plugins and are listed
  as SKIP.
- ui: a ui script shows the preview in a `<SurfaceView>`, analyzes 640x480 frames and takes a picture every 10 s for
  --seconds; halfway through an emulator is turned to landscape (a phone: the person at the device turns it when the
  ROTATE line appears). Screenshots go to --screenshot.
- background (an emulator or --allow-device-changes): the host is sent home and `camera.capture({ ui: true })` must
  fail with UI_REQUIRES_FOREGROUND.
- denied (an emulator or --allow-device-changes): the plugin's camera permission is revoked, `camera.open()` must fail
  with PERMISSION_DENIED, then the permission is granted again.
- missing (an emulator or --allow-device-changes, with --plugin-apk): the plugin is uninstalled, `camera.isAvailable()`
  must be false and `camera.open()` must fail with PLUGIN_NOT_INSTALLED, then the APK is installed again and granted.

zh-CN: 在设备上用宿主 `camera` 脚本模块驱动 3-Prism Camera (路线图 P1.4 / P1.6 / P2.5 联调). 宿主须为带 `camera` 模块的构建 (versionCode >= 6641),
插件须已安装并获得相机权限. 每个阶段推送一个生成的脚本, 把宿主启动器活动切到前台, 经 RunIntentActivity 运行, 读取其结果文件 (不读 logcat:
OPD2413 会丢 console 行). 每项检查一行 PASS / FAIL / SKIP / INFO 与关键值. main: 模块成员, 第二次 `camera.open()` 得到 CAMERA_BUSY 并写明占用者,
持有会话时 `camera.capture({ ui: true })` 得到 CAMERA_BUSY (附录 D Q22), 以及三个样例 ("拍一张存文件", "连续帧识别文字" 只要求帧到达回调,
"取景页拍照" 在 P3.1 声明 `captureUi` 之前应为 UNSUPPORTED); 扫码与物体检测样例需要其他插件, 记 SKIP. ui: ui 脚本在 `<SurfaceView>` 中预览,
同时分析 640x480 帧并每 10 秒拍一张, 持续 --seconds 秒; 过半时模拟器转为横屏 (真机在出现 ROTATE 行时由设备旁的人转动), 截图存入 --screenshot.
background / denied / missing 只在模拟器或给出 --allow-device-changes 时运行: 宿主回到桌面后 `camera.capture({ ui: true })` 应为
UI_REQUIRES_FOREGROUND; 撤销插件相机权限后 `camera.open()` 应为 PERMISSION_DENIED, 之后重新授予; 卸载插件后 `camera.isAvailable()` 为 false 且
`camera.open()` 为 PLUGIN_NOT_INSTALLED, 之后用 --plugin-apk 重装并授权.
"""
import argparse
import pathlib
import subprocess
import sys
import time

HOST = "org.autojs.autojs6"
PLUGIN = "io.github.supermonster003.autojs6.plugin.three.prism.camera"
RUN_ACTIVITY = "org.autojs.autojs.external.open.RunIntentActivity"
DEVICE_DIR = "/sdcard/Download/aj6-prism-check"
SAMPLE_DIR = "app/src/main/assets-app/sample/3-Prism Camera"
SHOT = "拍一张存文件 [v6.8.1+].js"
OCR = "连续帧识别文字 [v6.8.1+].js"
VIEWFINDER = "取景页拍照 [v6.8.1+].js"
BARCODE = "实时扫码 [v6.8.1+].js"
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
check('sampleViewfinder', function () {
    var s = runSample('%(viewfinder)s', { file: files.join(DIR, 'viewfinder.jpg') }, 60000);
    assert(s.code === 'UNSUPPORTED' || s.taken === false, JSON.stringify(s));
    return s.code ? 'code=' + s.code + ' (captureUi is P3.1)' : 'cancelled';
});
report('sampleBarcode', 'SKIP', 'needs the ML Kit Barcode plugin and a code in front of the camera');
report('sampleDetect', 'SKIP', 'needs the YOLO NCNN plugin, a model directory and the overlay permission');
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

    def read(self, name):
        return self.shell("cat '%s/%s' 2>/dev/null" % (DEVICE_DIR, name))

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
            if line in ("ready", "done"):
                if on_line:
                    on_line(line)
            else:
                print(line, flush=True)
        seen = len(lines)
        if "done" in lines:
            return True
    print("%s FAIL no result within %d s" % (name, timeout), flush=True)
    return False


def fill(template, **values):
    text = template
    for key, value in values.items():
        text = text.replace("%(" + key + ")s", str(value)).replace("%(" + key + ")d", str(value))
    return text


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("serial")
    parser.add_argument("--host-tree", default=str(pathlib.Path(__file__).resolve().parents[2] / "AutoJs6"))
    parser.add_argument("--phases", default="main,ui,background,denied,missing")
    parser.add_argument("--seconds", type=int, default=30)
    parser.add_argument("--screenshot", default="")
    parser.add_argument("--plugin-apk", default="")
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
        if not changes:
            for name in ("background", "denied", "missing"):
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
    finally:
        if not args.keep:
            device.shell("rm -r '%s'" % DEVICE_DIR)


if __name__ == "__main__":
    sys.exit(main())
