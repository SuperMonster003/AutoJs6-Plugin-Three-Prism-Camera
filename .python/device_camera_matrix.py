# -*- coding: utf-8 -*-
"""Device matrix of 3-Prism Camera (roadmap P4.2 - P4.4 and appendix D Q41 = (b)) through the host `camera` module.

Usage: py -u .python/device_camera_matrix.py <serial> [--phases smoke,perf,background,bgopen,kill,coldstart,recognize,q41]
       [--seconds 30] [--sizes 640x480,1280x720] [--allow-device-changes]

The host must be a build with the `camera` module (versionCode >= 6641) and the plugin must be installed with the camera
permission granted. Scripts run through RunIntentActivity like device_camera_samples.py (its Device and run_phase are
reused); the shell adds what only it sees. One line per check: PASS / FAIL / SKIP / INFO with the key values.

- smoke (R3): `camera.list()`, `open` + 10 frames with rising seq, `camera.capture({ path })` to a file, torch on and
  off (SKIP without a flash unit). The viewfinder photo of R3 is the viewfinder phase of device_camera_samples.py.
- perf (P4.4): per size of --sizes an analysis of --seconds: actual size and frame rate, drops, the delay from the
  sensor timestamp of a frame to the script callback (P50 / P95; an upper bound of the host's onFrame work, since it
  also holds the plugin copy and the binder hop), the CPU share (one core = 100%) and the PSS growth of both processes,
  then the delay of `session.capture()` (call to ImageWrapper, median of 3) with the session open.
- background (P4.3): a session analyzes while the host goes home for --seconds and then while the screen is off for
  --seconds; frames in each window, plus the plugin's foreground service and process capability from dumpsys.
- bgopen (P4.3): the host goes home first, then the script calls `camera.open()` and analyzes 10 s.
- kill (P4.3): a session analyzes, the host goes home and is killed with `am kill` (`am force-stop` when the process
  survives); the plugin's foreground notification and its camera client must be gone within 1 s.
- coldstart (P4.4): the plugin is force-stopped and its launcher (the viewfinder) started with `am start -W`, 3 times;
  TotalTime median.
- recognize (P4.5, with --yolo-model and the ML Kit Barcode and YOLO NCNN plugins): the calls of the OCR, barcode and
  YOLO samples on frames; a toast says what to point the back camera at; the time from the first frame to the first hit.
- q41 (Q41 = (b), a debuggable plugin and an emulator or --allow-device-changes): the plugin's settings store gets
  facing front, 320x240 and 15 fps (the values the settings page writes), `camera.open()` without options must report
  them, then the store is put back.

zh-CN: 经宿主 `camera` 模块在设备上跑 3-Prism Camera 的设备矩阵 (路线图 P4.2 - P4.4 与附录 D Q41 = (b)). 宿主须为带 `camera` 模块的构建
(versionCode >= 6641), 插件须已安装并获得相机权限. 脚本像 device_camera_samples.py 一样经 RunIntentActivity 运行 (复用其 Device 与 run_phase),
只有 shell 能看到的由本脚本补充. 每项一行 PASS / FAIL / SKIP / INFO 与关键值. smoke (R3): list, open + 10 帧, capture 到文件, 手电开关;
perf (P4.4): 每个分辨率分析 --seconds 秒, 记录实际分辨率与帧率, 丢帧, 传感器时间戳到脚本回调的延迟 P50 / P95 (宿主 onFrame 处理的上界),
两进程 CPU 与 PSS 增量, 以及会话打开时 `session.capture()` 的延迟 (3 次中位数); background (P4.3): 宿主回桌面与熄屏各 --seconds 秒的出帧数,
以及插件前台服务与进程能力; bgopen: 宿主先回桌面再 open; kill: `am kill` 宿主后插件通知与相机客户端须在 1 秒内消失; coldstart: 取景页冷启动
(`am start -W` 3 次中位数); q41: 写入设置页的默认值 (前置, 320x240, 15 fps), 不带参数的 `camera.open()` 须按其取值, 之后还原.
"""
from __future__ import annotations

import argparse
import pathlib
import re
import statistics
import subprocess
import sys
import time

from device_camera_samples import COMMON, DEVICE_DIR, HOST, PLUGIN, Device, fill, run_phase

STEPS = r"""
var steps = [];
function step(fn) { steps.push(fn); }
function next() {
    var fn = steps.shift();
    if (!fn) { out('done'); exit(); return; }
    setTimeout(function () {
        try { fn(next); } catch (e) { report('step', 'FAIL', describe(e)); next(); }
    }, 300);
}
function pct(values, p) {
    if (!values.length) return -1;
    var sorted = values.slice().sort(function (a, b) { return a - b; });
    return sorted[Math.min(sorted.length - 1, Math.floor(p * sorted.length))];
}
function delayMillis(ts) {
    // meta.timestamp is the sensor time in milliseconds, on the realtime or the monotonic clock depending on the camera.
    // zh-CN: meta.timestamp 为传感器时间 (毫秒), 取自 realtime 或 monotonic 时钟, 视镜头而定.
    var a = android.os.SystemClock.elapsedRealtimeNanos() / 1e6 - ts;
    var b = java.lang.System.nanoTime() / 1e6 - ts;
    return a >= 0 && a < 2000 ? a : (b >= 0 && b < 2000 ? b : -1);
}
function analyzeFor(session, options, seconds, done) {
    var r = { frames: 0, first: 0, last: 0, delays: [], size: '-', seqs: [] };
    session.analyze(function (img, meta) {
        var now = Date.now();
        if (!r.frames) r.first = now;
        r.last = now;
        r.frames++;
        if (r.seqs.length < 10) r.seqs.push(meta.seq);
        r.size = meta.width + 'x' + meta.height;
        var d = delayMillis(meta.timestamp);
        if (d >= 0) r.delays.push(d);
    }, options);
    setTimeout(function () {
        var state = session.state();
        session.stopAnalysis();
        r.fps = r.frames > 1 ? (r.frames - 1) * 1000 / (r.last - r.first) : 0;
        r.dropped = state.dropped;
        r.stateFps = state.fps;
        done(r);
    }, seconds * 1000);
}
function fmt(n) { return Math.round(n * 10) / 10; }
"""

SMOKE = r"""
var session = null;
step(function (next) {
    check('list', function () {
        var list = camera.list();
        assert(list.length > 0, 'no camera');
        return list.length + ' cameras: ' + list.map(function (c) { return c.facing + '/' + c.id + (c.hasFlash ? '+flash' : ''); }).join(', ');
    });
    next();
});
step(function (next) {
    session = camera.open({ facing: 'back', resolution: '640x480' });
    analyzeFor(session, {}, 4, function (r) {
        check('open10', function () {
            assert(r.seqs.length >= 10, 'only ' + r.seqs.length + ' frames');
            for (var i = 1; i < r.seqs.length; i++) assert(r.seqs[i] > r.seqs[i - 1], 'seq ' + r.seqs.join(','));
            return 'size=' + r.size + ' seq=' + r.seqs.join(',') + ' fps=' + fmt(r.fps);
        });
        next();
    });
});
step(function (next) {
    check('torch', function () {
        var info = camera.list().filter(function (c) { return c.id === session.state().id; })[0];
        if (!info || !info.hasFlash) return 'SKIP no flash unit';
        session.torch(true);
        var on = session.state().torch;
        session.torch(false);
        assert(on === true && session.state().torch === false, 'torch state ' + on);
        return 'on / off';
    });
    session.close();
    next();
});
step(function (next) {
    check('captureFile', function () {
        var path = files.join(DIR, 'smoke.jpg');
        files.remove(path);
        var t0 = Date.now();
        var img = camera.capture({ path: path });
        var dt = Date.now() - t0;
        assert(img && files.exists(path), 'no file');
        var detail = img.getWidth() + 'x' + img.getHeight() + ' bytes=' + new java.io.File(path).length() + ' oneShotMillis=' + dt;
        img.recycle();
        files.remove(path);
        return detail;
    });
    next();
});
next();
"""

PERF = r"""
var sizes = '%(sizes)s'.split(',');
var seconds = %(seconds)d;
sizes.forEach(function (size) {
    step(function (next) {
        var session = camera.open({ facing: 'back', resolution: size });
        setTimeout(function () {
            out('step:start-' + size);
            analyzeFor(session, {}, seconds, function (r) {
                out('step:end-' + size);
                report('perf-' + size, r.frames > 0 ? 'PASS' : 'FAIL', 'size=' + r.size + ' fps=' + fmt(r.fps) + ' frames=' + r.frames +
                    ' dropped=' + r.dropped + ' stateFps=' + r.stateFps + ' delayP50=' + fmt(pct(r.delays, 0.5)) + 'ms delayP95=' +
                    fmt(pct(r.delays, 0.95)) + 'ms');
                var times = [];
                try {
                    for (var i = 0; i < 3; i++) {
                        var t0 = Date.now();
                        var img = session.capture();
                        times.push(Date.now() - t0);
                        img.recycle();
                    }
                    report('capture-' + size, 'PASS', 'millis=' + times.join(',') + ' median=' + pct(times, 0.5));
                } catch (e) { report('capture-' + size, 'FAIL', describe(e)); }
                session.close();
                next();
            });
        }, 1500);
    });
});
next();
"""

BACKGROUND = r"""
var seconds = %(seconds)d;
var session = camera.open({ facing: 'back', resolution: '640x480' });
var count = 0;
session.analyze(function () { count++; });
function windowed(name, signal, next) {
    out('step:' + signal);
    setTimeout(function () {
        var c0 = count, t0 = Date.now();
        setTimeout(function () {
            var n = count - c0;
            report(name, n > 0 ? 'PASS' : 'FAIL', 'frames=' + n + ' fps=' + fmt(n * 1000 / (Date.now() - t0)) + ' dropped=' + session.state().dropped);
            next();
        }, seconds * 1000);
    }, 3000);
}
step(function (next) { windowed('background', 'home', next); });
step(function (next) { windowed('screenOff', 'screenoff', next); });
step(function (next) { out('step:screenon'); session.close(); next(); });
setTimeout(next, 2000);
"""

BGOPEN = r"""
out('step:home');
setTimeout(function () {
    var session = null;
    try {
        session = camera.open({ facing: 'back', resolution: '640x480' });
    } catch (e) {
        report('bgopen', 'INFO', 'open in the background: ' + describe(e));
        out('done');
        exit();
        return;
    }
    var count = 0, events = [];
    session.on('error', function (e) { events.push(describe(e)); });
    session.on('close', function (reason) { events.push('close ' + reason); });
    session.analyze(function () { count++; });
    setTimeout(function () {
        var state = session.state();
        report('bgopen', 'INFO', 'open in the background succeeded, frames in 10 s=' + count + ' dropped=' + state.dropped + ' open=' + state.open +
            ' events=' + (events.join('; ') || '-'));
        session.close();
        out('done');
        exit();
    }, 10000);
}, 5000);
"""

KILL = r"""
var session = camera.open({ facing: 'back', resolution: '640x480' });
var count = 0;
session.analyze(function () { count++; });
setTimeout(function () { out('step:kill'); }, 3000);
setInterval(function () {}, 1000);
"""

Q41 = r"""
// Size and frame rate are state keys only while analyzing (protocol "Open: keys left out").
// zh-CN: 分辨率与帧率只在分析期间出现在状态中 (协议 "Open: keys left out" 一节).
function probe(name, options, expect) {
    step(function (next) {
        var session = camera.open(options);
        analyzeFor(session, {}, 3, function (r) {
            check(name, function () {
                var size = r.size.split('x').map(Number).sort(function (a, b) { return b - a; }).join('x');
                var detail = 'facing=' + session.state().facing + ' frameSize=' + r.size + ' stateFps=' + r.stateFps + ' fps=' + fmt(r.fps);
                assert(session.state().facing === expect.facing && size === expect.size && (!expect.fps || r.stateFps === expect.fps), detail);
                return detail;
            });
            session.close();
            next();
        });
    });
}
probe('q41', undefined, { facing: 'front', size: '320x240', fps: 15 });
probe('q41explicit', { facing: 'back', resolution: '640x480', fps: 30 }, { facing: 'back', size: '640x480', fps: 30 });
next();
"""

# P4.5: the calls of the three recognition samples (OCR, barcode, YOLO) with the time from the first frame to the first
# hit; a toast on the device says what to point the back camera at.
# zh-CN: P4.5: 三个识别样例 (OCR, 扫码, YOLO) 的同款调用, 记录首帧到首次命中的时间; 设备上的 toast 提示后置镜头对准什么.
RECOGNIZE = r"""
var limit = %(limit)d * 1000;
function hunt(name, aim, options, recognize) {
    step(function (next) {
        toastLog('AIM: ' + aim);
        out('step:aim-' + name);
        setTimeout(function () {
            var session = null, frames = 0, first = 0, hitAt = 0, hit = null, failure = null, ms = [];
            try { session = camera.open(options); } catch (e) { report(name, 'FAIL', describe(e)); next(); return; }
            var started = Date.now();
            session.analyze(function (img) {
                if (hit !== null || failure !== null) return;
                frames++;
                if (!first) first = Date.now();
                var t0 = Date.now();
                try { hit = recognize(img); } catch (e) { failure = describe(e); }
                ms.push(Date.now() - t0);
                if (hit !== null) hitAt = Date.now();
            });
            var timer = setInterval(function () {
                if (hit === null && failure === null && Date.now() - started < limit) return;
                clearInterval(timer);
                var dropped = session.state().dropped;
                session.close();
                var detail = 'frames=' + frames + ' dropped=' + dropped + ' recognizeP50=' + pct(ms, 0.5) + 'ms';
                if (failure !== null) report(name, 'FAIL', failure + ' ' + detail);
                else if (hit === null) report(name, 'FAIL', 'no hit within ' + limit / 1000 + ' s ' + detail);
                else report(name, 'PASS', 'firstFrameToHit=' + (hitAt - first) + 'ms hitFrame=' + frames + ' hit="' + String(hit).slice(0, 60) + '" ' + detail);
                next();
            }, 200);
        }, 8000);
    });
}
hunt('ocr', 'text AutoJs6', { resolution: '1280x720' }, function (img) {
    var texts = ocr.recognizeText(img);
    for (var i = 0; i < texts.length; i++) if (String(texts[i]).toLowerCase().indexOf('autojs6') >= 0) return String(texts[i]);
    return null;
});
hunt('barcode', 'a QR code or barcode', { resolution: '1280x720', fps: 15 }, function (img) {
    var text = barcode.recognizeText(img);
    return text ? String(text) : null;
});
var detector = null;
hunt('yolo', 'a common object (cup, phone, keyboard...)', { facing: 'back', resolution: '640x480' }, function (img) {
    if (!detector) detector = yolo.load('%(model)s', { component: 'io.github.supermonster003.autojs6.plugin.yolo.ncnn/.provider.YoloProviderService' });
    var found = detector.detect(img, { confidence: 0.4, maxDetections: 20 });
    return found.length ? found.map(function (d) { return String(d.label); }).join('/') : null;
});
next();
"""

Q41_PREFS = ("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
             "    <string name=\"default_facing\">front</string>\n"
             "    <string name=\"default_analysis_size\">320x240</string>\n"
             "    <int name=\"default_fps\" value=\"15\" />\n</map>\n")
PREFS = "shared_prefs/camera_preferences.xml"


class Done(Exception):
    pass


def wait_unlocked(device, seconds=300):
    """Waits until the keyguard is gone. A secure lock screen is left to the person at the device (an UNLOCK line asks
    for it); behind the keyguard the host is "top sleeping" and a foreground service the plugin starts then gets no
    camera access, which would spoil every later phase.
    zh-CN: 等待锁屏消失. 安全锁屏由设备旁的人解锁 (输出 UNLOCK 行); 锁屏之下宿主处于 "top sleeping", 此时插件启动的前台服务没有相机权限,
    会使之后的阶段全部失效."""
    device.shell("input keyevent 224")
    device.shell("wm dismiss-keyguard")
    asked = False
    end = time.time() + seconds
    while time.time() < end:
        # isKeyguardShowing (API 31+) or mDreamingLockscreen (API 28 has no isKeyguardShowing line).
        # zh-CN: isKeyguardShowing (API 31+) 或 mDreamingLockscreen (API 28 没有 isKeyguardShowing 行).
        if "=true" not in device.shell("dumpsys window | grep -oE '(isKeyguardShowing|mDreamingLockscreen)=true'"):
            return True
        if not asked:
            print("UNLOCK the device (secure lock screen), waiting up to %d s" % seconds, flush=True)
            asked = True
        time.sleep(3)
    print("INFO the device stayed locked", flush=True)
    return False


def pid(device, package):
    return device.shell("pidof " + package).strip().split(" ")[0]


def cpu_ticks(device, process_id):
    if not process_id:
        return None
    fields = device.shell("cat /proc/%s/stat" % process_id).rsplit(")", 1)[-1].split()
    return int(fields[11]) + int(fields[12]) if len(fields) > 12 else None


def pss_kb(device, package):
    match = re.search(r"TOTAL(?: PSS)?:?\s+(\d+)", device.shell("dumpsys meminfo " + package))
    return int(match.group(1)) if match else None


def foreground_info(device):
    services = device.shell("dumpsys activity services " + PLUGIN)
    fg = re.findall(r"isForeground=\w+|foregroundServiceType=\S+|types=\S+", services)
    procs = device.shell("dumpsys activity processes | grep -A14 -F ':%s/' | grep -oE '(curCapability|mCurCapability|setCapability)=[^ ]+' | head -2" % PLUGIN)
    return "fg=%s capability=%s" % (",".join(sorted(set(fg))) or "-", ",".join(procs.split()) or "-")


def camera_held(device, plugin_pid):
    # The "Active Camera Clients" section only: the events log keeps old clients and the per-device dump of some
    # releases (Sony API 31) still says "Device 0 is open" for a client that has exited.
    # zh-CN: 只读 "Active Camera Clients" 一段: 事件日志保留旧客户端, 部分系统 (Sony API 31) 的逐设备转储对已退出的客户端仍写 "Device 0 is open".
    dump = device.shell("dumpsys media.camera")
    section = dump.split("Active Camera Clients:", 1)[-1].split("Allowed user IDs", 1)[0]
    return "PID" in section or PLUGIN in section


def notified(device):
    # The foreground service of the plugin carries its notification; the notification dump is read as well.
    # zh-CN: 插件的前台服务必带通知; 同时读取通知转储.
    # Only the active list: the archive below it keeps notifications that are gone.
    # zh-CN: 只读活动列表: 其下的归档保留已消失的通知.
    active = device.shell("dumpsys notification --noredact").split("mArchive", 1)[0]
    shown = re.search(r"NotificationRecord\([^)]*pkg=%s" % re.escape(PLUGIN), active) is not None
    return shown or "isForeground=true" in device.shell("dumpsys activity services %s | grep -F isForeground=true" % PLUGIN)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("serial")
    parser.add_argument("--phases", default="smoke,perf,background,bgopen,kill,coldstart")
    parser.add_argument("--seconds", type=int, default=30)
    parser.add_argument("--sizes", default="640x480,1280x720")
    parser.add_argument("--allow-device-changes", action="store_true")
    parser.add_argument("--yolo-model", default="", help="recognize: a directory with model.json, model.ncnn.param and model.ncnn.bin")
    parser.add_argument("--limit", type=int, default=60, help="recognize: seconds per recognizer")
    args = parser.parse_args()
    device = Device(args.serial)
    sdk = device.shell("getprop ro.build.version.sdk").strip()
    model = device.shell("getprop ro.product.model").strip()
    host_code = re.search(r"versionCode=(\d+)", device.shell("dumpsys package " + HOST))
    plugin_code = re.search(r"versionCode=(\d+)", device.shell("dumpsys package " + PLUGIN))
    granted = "android.permission.CAMERA: granted=true" in device.shell("dumpsys package " + PLUGIN)
    print("INFO model=%s sdk=%s host=%s plugin=%s pluginCameraGranted=%s" % (
        model, sdk, host_code and host_code.group(1), plugin_code and plugin_code.group(1), granted), flush=True)
    common = fill(COMMON, dir=DEVICE_DIR) + fill(STEPS)
    device.shell("rm -r '%s'; mkdir -p '%s'" % (DEVICE_DIR, DEVICE_DIR))
    phases = [p.strip() for p in args.phases.split(",") if p.strip()]
    if not wait_unlocked(device):
        return 1
    try:
        if "smoke" in phases:
            run_phase(device, "smoke", common + SMOKE, 120)
        if "perf" in phases:
            marks = {}

            def on_perf(line):
                if line.startswith("step:start-") or line.startswith("step:end-"):
                    marks[line] = {p: (cpu_ticks(device, pid(device, p)), pss_kb(device, p), time.time()) for p in (HOST, PLUGIN)}
                if line.startswith("step:end-"):
                    size = line[len("step:end-"):]
                    start = marks.get("step:start-" + size, {})
                    parts = []
                    for name, p in (("host", HOST), ("plugin", PLUGIN)):
                        a, b = start.get(p), marks[line][p]
                        if a and a[0] is not None and b[0] is not None:
                            cpu = (b[0] - a[0]) / 100.0 / (b[2] - a[2]) * 100
                            parts.append("%sCpu=%.1f%% %sPssDelta=%sKB" % (name, cpu, name, (b[1] - a[1]) if a[1] and b[1] else "-"))
                    print("INFO cpu-%s %s" % (size, " ".join(parts)), flush=True)

            run_phase(device, "perf", common + fill(PERF, sizes=args.sizes, seconds=args.seconds), 90 + len(args.sizes.split(",")) * (args.seconds + 40), on_perf)
        if "background" in phases:
            def on_background(line):
                if line == "step:home":
                    device.shell("input keyevent 3")
                    time.sleep(5)
                    print("INFO background %s" % foreground_info(device), flush=True)
                elif line == "step:screenoff":
                    device.shell("input keyevent 223")
                elif line == "step:screenon":
                    wait_unlocked(device)

            run_phase(device, "background", common + fill(BACKGROUND, seconds=args.seconds), 2 * args.seconds + 60, on_background)
        if "bgopen" in phases:
            run_phase(device, "bgopen", common + BGOPEN, 60, lambda line: line == "step:home" and device.shell("input keyevent 3"))
        if "kill" in phases:
            def on_kill(line):
                if line != "step:kill":
                    return
                device.shell("input keyevent 3")
                time.sleep(3)
                plugin_pid = pid(device, PLUGIN)
                before = "notification=%s cameraHeld=%s %s" % (notified(device), camera_held(device, plugin_pid), foreground_info(device))
                host_pid = pid(device, HOST)
                device.shell("logcat -c")
                device.shell("am kill " + HOST)
                how = "am kill"
                killed_at = float(device.shell("date +%s.%N").strip()[:14] or 0)
                if pid(device, HOST) == host_pid and host_pid:
                    device.shell("am force-stop " + HOST)
                    killed_at = float(device.shell("date +%s.%N").strip()[:14] or 0)
                    how = "am force-stop (am kill spared the process)"
                t0 = time.time()
                gone = None
                while time.time() - t0 < 10:
                    if not notified(device):
                        gone = time.time() - t0
                        break
                # The camera release is read from the camera service log (some dumpsys media.camera dumps keep exited
                # clients): the first close or disconnect line of the plugin's client after the kill.
                # zh-CN: 相机释放取自相机服务日志 (部分系统的 dumpsys media.camera 保留已退出的客户端): 击杀后插件客户端的首个关闭或断开行.
                # The plugin's own CameraX close counts as well (chatty hides it on some releases); read 2 s after the kill.
                # zh-CN: 插件自身的 CameraX 关闭行同样计入 (部分系统被 chatty 折叠); 击杀 2 秒后读取.
                time.sleep(max(0.0, 2.0 - (time.time() - t0)))
                released = None
                for line in device.shell("logcat -d -v epoch -b main,system").splitlines():
                    if re.search(r"(Camera2ClientBase|CameraService|CameraDeviceClient).*(Closed|[Dd]isconnect)", line) or (
                            " %s " % plugin_pid in line and "Closed Camera2CameraController" in line):
                        stamp = re.match(r"\s*(\d+\.\d+)", line)
                        if stamp and killed_at:
                            released = float(stamp.group(1)) - killed_at
                            break
                ok = gone is not None and gone <= 1.0 and released is not None and released <= 1.0
                print("kill %s %s; %s; notification gone after %s s (one dumpsys pair per poll); camera closed %s s after the kill (camera service log)" % (
                    "PASS" if ok else "FAIL", before, how, "%.2f" % gone if gone is not None else ">10",
                    "%.2f" % released if released is not None else "-"), flush=True)
                raise Done()

            try:
                run_phase(device, "kill", common + KILL, 40, on_kill)
            except Done:
                pass
        if "coldstart" in phases:
            launcher = device.plugin_launcher()
            times = []
            for _ in range(3):
                device.shell("am force-stop " + PLUGIN)
                time.sleep(2)
                # The launcher entry is an alias: without TotalTime (seen on API 31) WaitTime is taken.
                # zh-CN: 启动器入口是别名: 没有 TotalTime 时 (API 31 上出现) 取 WaitTime.
                answer = device.shell("am start -W -n " + launcher)
                match = re.search(r"TotalTime:\s*(\d+)", answer) or re.search(r"WaitTime:\s*(\d+)", answer)
                if match:
                    times.append(int(match.group(1)))
                time.sleep(3)
                device.shell("input keyevent 4")
            device.shell("am force-stop " + PLUGIN)
            print("coldstart %s TotalTime/WaitTime=%s median=%s ms" % ("PASS" if times else "FAIL", times, statistics.median(times) if times else "-"), flush=True)
        if "recognize" in phases:
            model = DEVICE_DIR + "/models/yolo11n"
            if args.yolo_model:
                device.shell("mkdir -p '%s'" % model)
                for name in ("model.json", "model.ncnn.param", "model.ncnn.bin"):
                    device.run("push", str(pathlib.Path(args.yolo_model) / name), "%s/%s" % (model, name), check=True)
            run_phase(device, "recognize", common + fill(RECOGNIZE, limit=args.limit, model=model), 3 * (args.limit + 30) + 60,
                      lambda line: line.startswith("step:aim-") and print("AIM " + line[len("step:aim-"):], flush=True))
        if "q41" in phases:
            if not (device.emulator or args.allow_device_changes):
                print("q41 SKIP needs an emulator or --allow-device-changes", flush=True)
            else:
                def run_as(command, data=None):
                    return subprocess.run(["adb", "-s", device.serial, "shell", "run-as %s sh -c '%s'" % (PLUGIN, command)], input=data,
                                          capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60)

                saved = run_as("cat %s 2>/dev/null" % PREFS).stdout
                device.shell("am force-stop " + PLUGIN)
                run_as("mkdir -p shared_prefs && cat > %s" % PREFS, Q41_PREFS)
                try:
                    run_phase(device, "q41", common + Q41, 90)
                finally:
                    device.shell("am force-stop " + PLUGIN)
                    if saved.strip():
                        run_as("cat > %s" % PREFS, saved)
                    else:
                        run_as("rm -f %s" % PREFS)
                    print("INFO q41 settings store put back (%s)" % ("previous file" if saved.strip() else "removed, none before"), flush=True)
    finally:
        device.shell("rm -r '%s'" % DEVICE_DIR)
    return 0


if __name__ == "__main__":
    sys.exit(main())
