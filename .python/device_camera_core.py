"""Device checks of the camera core (roadmap P2.1 - P2.6, P1.2 shared memory) on one device.

    py .python/device_camera_core.py <serial> [--steps a,b,...] [--abi <abi> | --plugin-apk <file> | --no-install]
        [--host-tree <dir>] [--install-host] [--seconds 30] [--orientation portrait|landscape] [--expect-focus]
        [--screenshot <dir>]

The checks are instrumentation tests of the AutoJs6 host (app/src/androidTest/.../core/plugin/camera, runner
org.autojs.autojs6.test/androidx.test.runner.AndroidJUnitRunner): they run in the host process, bind the installed
plugin like the `camera` module does and log one summary line per test under the CameraPluginDeviceIT and
CameraFrameTransportIT tags. This script installs the APKs, runs one test per step with its arguments, reads the summary
lines and adds what only the shell sees: the CPU time of the plugin process during the perf run, the foreground
notification of the plugin around a host kill and a screenshot of the floating preview.

Steps (default: all that apply to the device; a step whose assumption fails is reported as SKIP):
- check: host, test and plugin versions, the CAMERA permission of the plugin, the SDK level.
- denied: without the CAMERA permission openSession fails with PERMISSION_DENIED (SKIP once granted).
- transport: SharedMemory[] crosses the process boundary (API 27+): 3 blocks, 10 frames, seq increasing, block reuse.
- busy: a second open fails with CAMERA_BUSY and names the holder; it succeeds after the first closes.
- perf: analysis at 640x480 for --seconds; fps, drops and the plugin CPU share (appendix F).
- capture: one picture; decoded size, EXIF orientation and, with --orientation, the expected shape.
- captureui: openCaptureUi brings the viewfinder of the plugin to the front; a back press closes it and the callback
  gets CANCELLED. The test binds the plugin itself, since the host refuses openCaptureUi until the plugin advertises
  `captureUi` (roadmap P3.1), and starts the launcher activity of AutoJs6 first, since the viewfinder needs a visible host.
- controls: torch, zoom 2x, exposure +2 against 0 by mean luminance, focusAt (asserted with --expect-focus; UNSUPPORTED
  is accepted without it, as on a camera without AF / AE / AWB regions).
- preview: floating SurfaceView preview with analysis and capture for --seconds, then an invalid surface (needs the
  overlay permission of AutoJs6; --screenshot saves preview-<serial>-<orientation>.png at PREVIEW_READY).
- front: the front preview reports previewMirrored = true (SKIP without a front camera).
- kill: a held session shows the plugin's foreground notification; after the host process is killed (`am kill`,
  else `am force-stop`, since `am kill` spares the instrumented process) the notification has to be gone within 1 s of
  the kill command returning (one `dumpsys notification` per poll bounds the resolution).

Install: the plugin debug APK of app/build/outputs/apk/debug for the first ABI of the device (or --abi / --plugin-apk);
the host and test APKs of --host-tree (default ../AutoJs6) only with --install-host, since the host APK replaces the
installed AutoJs6. Nothing here answers a system dialog: grant the camera permission through the dialog (test
requestPermissionOpensTheSystemDialog with -e prismRequestPermission true) before the camera steps.

Output: PASS / FAIL / SKIP per step and the key values only.
"""
import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

HOST = "org.autojs.autojs6"
TEST_PACKAGE = "org.autojs.autojs6.test"
RUNNER = f"{TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner"
PLUGIN = "io.github.supermonster003.autojs6.plugin.three.prism.camera"
TEST_PREFIX = "org.autojs.autojs.core.plugin.camera."
SESSION_TEST = TEST_PREFIX + "CameraPluginSessionDeviceTest"
TRANSPORT_TEST = TEST_PREFIX + "CameraFrameTransportInstrumentedTest"
TAGS = ("CameraPluginDeviceIT", "CameraFrameTransportIT")
ROOT = Path(__file__).resolve().parent.parent
ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
MIN_HOST = 6641
STEPS = ("check", "denied", "transport", "busy", "perf", "capture", "captureui", "controls", "preview", "front", "kill")
STATUS = {"0": "PASS", "-1": "FAIL", "-2": "FAIL", "-3": "SKIP", "-4": "SKIP"}


class Device:
    def __init__(self, serial):
        self.serial = serial

    def run(self, *args, timeout=120, check=False):
        result = subprocess.run(["adb", "-s", self.serial, *args], capture_output=True, timeout=timeout)
        out = result.stdout.decode("utf-8", "replace")
        if check and result.returncode != 0:
            raise RuntimeError(f"adb {' '.join(args[:2])} failed: {result.stderr.decode('utf-8', 'replace').strip()}")
        return out

    def shell(self, command, timeout=120):
        return self.run("shell", command, timeout=timeout)

    def popen(self, *args):
        return subprocess.Popen(["adb", "-s", self.serial, *args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

    def version_code(self, package):
        match = re.search(r"versionCode=(\d+)", self.shell(f"dumpsys package {package} | grep versionCode"))
        return int(match.group(1)) if match else None

    def camera_granted(self):
        return "android.permission.CAMERA: granted=true" in self.shell(f"dumpsys package {PLUGIN} | grep 'permission.CAMERA'")

    def summary_lines(self):
        out = self.run("logcat", "-d", "-v", "brief", "-s", *(f"{tag}:I" for tag in TAGS))
        return [re.sub(r"^\w/[\w.]+\(\s*\d+\):\s*", "", line).strip() for line in out.splitlines()
                if any(tag in line for tag in TAGS)]

    def pid(self, package):
        out = self.shell(f"pidof {package}").strip()
        return int(out.split()[0]) if out else None

    def plugin_notifications(self):
        return len(re.findall(rf"NotificationRecord\(\S+ pkg={re.escape(PLUGIN)} ", self.shell("dumpsys notification")))


def instrument(device, method, extras=None, background=False, prefixes=None):
    """Runs one test method; returns (status, summary lines of the given prefixes) or, with background, the process."""
    device.run("logcat", "-c")
    command = ["shell", "am", "instrument", "-w", "-r", "-e", "class", method]
    for key, value in (extras or {}).items():
        command += ["-e", key, str(value)]
    command.append(RUNNER)
    if background:
        return device.popen(*command)
    status = parse(device.run(*command, timeout=900))
    return status, [line for line in device.summary_lines() if not prefixes or line.startswith(prefixes)]


def parse(raw):
    codes = re.findall(r"INSTRUMENTATION_STATUS_CODE: (-?\d+)", raw)
    final = [code for code in codes if code != "1"]
    if not final:
        crash = re.search(r"INSTRUMENTATION_RESULT: shortMsg=(.*)", raw)
        return "FAIL", crash.group(1).strip() if crash else "no test result"
    status = STATUS.get(final[-1], "FAIL")
    stack = re.search(r"INSTRUMENTATION_STATUS: stack=(.+)", raw)
    reason = stack.group(1).strip()[:300] if stack and status != "PASS" else ""
    return status, reason


def report(step, status, lines=(), extra=""):
    print(f"{status:4} {step}{(' ' + extra) if extra else ''}", flush=True)
    for line in lines:
        print(f"     {line}", flush=True)
    return status


def cpu_ticks(device, pid):
    out = device.shell(f"cat /proc/uptime /proc/{pid}/stat").splitlines()
    uptime = float(out[0].split()[0])
    fields = out[1].rsplit(")", 1)[1].split()
    return uptime, int(fields[11]) + int(fields[12])


def wait_for_line(device, marker, timeout):
    deadline = time.time() + timeout
    while time.time() < deadline:
        lines = device.summary_lines()
        for line in lines:
            if line.startswith(marker):
                return line
        time.sleep(0.3)
    return None


def step_perf(device, seconds):
    process = instrument(device, f"{SESSION_TEST}#analysisRunsThirtySeconds", {"prismSeconds": seconds}, background=True)
    start = end = None
    if wait_for_line(device, "PERF_START", 60):
        pid = device.pid(PLUGIN)
        start = cpu_ticks(device, pid) if pid else None
        if wait_for_line(device, "PERF_END", seconds + 60) and start:
            end = cpu_ticks(device, pid)
    raw = process.communicate(timeout=seconds + 120)[0].decode("utf-8", "replace")
    status, reason = parse(raw)
    lines = [line for line in device.summary_lines() if line.startswith("PERF ")]
    extra = reason
    if start and end:
        hz = int(device.shell("getconf CLK_TCK").strip() or 100)
        share = (end[1] - start[1]) / hz / (end[0] - start[0]) * 100
        extra = f"pluginCpu={share:.1f}% (one core = 100%) window={end[0] - start[0]:.1f}s {reason}".strip()
    elif status == "PASS":
        status, extra = "FAIL", "plugin CPU was not sampled"
    return report("perf", status, lines, extra)


def step_preview(device, seconds, orientation, screenshot):
    process = instrument(device, f"{SESSION_TEST}#previewsInAFloatingWindowWithAnalysisAndCapture",
                         {"prismSeconds": seconds}, background=True)
    shot = ""
    if wait_for_line(device, "PREVIEW_READY", 60) and screenshot:
        time.sleep(1.5)
        target = Path(screenshot) / f"preview-{device.serial}-{orientation or 'natural'}.png"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(subprocess.run(["adb", "-s", device.serial, "exec-out", "screencap", "-p"],
                                          capture_output=True, timeout=60).stdout)
        shot = f"screenshot={target.name}"
    raw = process.communicate(timeout=seconds + 120)[0].decode("utf-8", "replace")
    status, reason = parse(raw)
    lines = [line for line in device.summary_lines() if line.startswith("PREVIEW")]
    return report("preview", status, lines, f"{shot} {reason}".strip())


def step_kill(device):
    process = instrument(device, f"{SESSION_TEST}#holdsASessionUntilKilled", {"prismHold": "true"}, background=True)
    if not wait_for_line(device, "HOLDING", 60):
        process.kill()
        return report("kill", "FAIL", extra="the session was not held")
    time.sleep(1.0)
    before = device.plugin_notifications()
    if before == 0:
        process.kill()
        return report("kill", "FAIL", extra="no foreground notification while the session is held")
    host = device.pid(HOST)
    # t0 before the kill, tk once the kill command returned (the host process is gone), t1 when the notification is gone;
    # one `dumpsys notification` per poll bounds the resolution. Only NotificationRecord lines count: the archive of
    # removed notifications lists the package too.
    probe = (f"t0=$(cut -d' ' -f1 /proc/uptime); {{kill}}; tk=$(cut -d' ' -f1 /proc/uptime); n=0; "
             f"while dumpsys notification | grep -q 'NotificationRecord(.* pkg={PLUGIN} '; do n=$((n+1)); [ $n -ge 100 ] && break; done; "
             f"t1=$(cut -d' ' -f1 /proc/uptime); echo \"$t0 $tk $t1 $n\"")
    out = device.shell(probe.replace("{kill}", f"am kill {HOST}")).split()
    how = "am kill"
    if device.pid(HOST) == host:
        out = device.shell(probe.replace("{kill}", f"am force-stop {HOST}")).split()
        how = "am force-stop"
    process.communicate(timeout=120)
    took, elapsed, polls = float(out[1]) - float(out[0]), float(out[2]) - float(out[1]), int(out[3])
    after = device.plugin_notifications()
    status = "PASS" if after == 0 and elapsed <= 1.0 else "FAIL"
    return report("kill", status, extra=f"{how} took={took:.2f}s notificationsBefore={before} after={after} "
                                        f"goneAfterKill={elapsed:.2f}s polls={polls}")


def install(device, args):
    if args.install_host:
        tree = Path(args.host_tree)
        abi = next(abi for abi in device.shell("getprop ro.product.cpu.abilist").strip().split(",") if abi in ABIS)
        hosts = sorted((tree / "app/build/outputs/apk/app/debug").glob(f"autojs6-v*-{abi}.apk"))
        tests = tree / "app/build/outputs/apk/androidTest/app/debug/app-app-debug-androidTest.apk"
        for apk in (hosts[-1] if hosts else None, tests):
            if not apk or not apk.exists():
                raise SystemExit(f"missing host build in {tree}")
            device.run("install", "-r", "-t", str(apk), timeout=600, check=True)
    if args.no_install:
        return None
    apk = Path(args.plugin_apk) if args.plugin_apk else None
    if apk is None:
        out = ROOT / "app/build/outputs/apk/debug"
        abis = [args.abi] if args.abi else device.shell("getprop ro.product.cpu.abilist").strip().split(",")
        candidates = [path for abi in abis for path in sorted(out.glob(f"*-{abi}.apk"))] + sorted(out.glob("*-universal.apk"))
        apk = candidates[0] if candidates else None
    if not apk or not apk.exists():
        raise SystemExit("no plugin debug APK: run gradlew :app:assembleDebug first")
    device.run("install", "-r", str(apk), timeout=600, check=True)
    return apk.name


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("serial")
    parser.add_argument("--steps", default=",".join(STEPS))
    parser.add_argument("--abi")
    parser.add_argument("--plugin-apk")
    parser.add_argument("--no-install", action="store_true")
    parser.add_argument("--host-tree", default=str(ROOT.parent / "AutoJs6"))
    parser.add_argument("--install-host", action="store_true")
    parser.add_argument("--seconds", type=int, default=30)
    parser.add_argument("--orientation", choices=("portrait", "landscape"))
    parser.add_argument("--expect-focus", action="store_true")
    parser.add_argument("--screenshot")
    args = parser.parse_args()
    steps = [step.strip() for step in args.steps.split(",") if step.strip()]
    unknown = [step for step in steps if step not in STEPS]
    if unknown:
        raise SystemExit(f"unknown steps: {', '.join(unknown)}")
    device = Device(args.serial)
    apk = install(device, args)
    sdk = int(device.shell("getprop ro.build.version.sdk").strip())
    host, test, plugin = (device.version_code(package) for package in (HOST, TEST_PACKAGE, PLUGIN))
    granted = device.camera_granted()
    results = []
    if "check" in steps:
        ok = host is not None and host >= MIN_HOST and test is not None and plugin is not None
        results.append(report("check", "PASS" if ok else "FAIL",
                              extra=f"sdk={sdk} host={host} test={test} plugin={plugin} apk={apk or '-'} camera={granted}"))
    for step in steps:
        if step == "check":
            continue
        if step == "denied":
            status, lines = instrument(device, f"{SESSION_TEST}#permissionDeniedWithoutTheCameraPermission", prefixes=("DENIED",))
            results.append(report(step, status[0], lines, status[1]))
            continue
        if not granted:
            results.append(report(step, "SKIP", extra="the plugin has no CAMERA permission"))
            continue
        if step == "transport":
            status, lines = instrument(device, TRANSPORT_TEST, prefixes=("blocks=", "client "))
            results.append(report(step, status[0], lines, status[1]))
        elif step == "busy":
            status, lines = instrument(device, f"{SESSION_TEST}#secondOpenIsBusyAndNamesTheHolder", prefixes=("BUSY",))
            results.append(report(step, status[0], lines, status[1]))
        elif step == "perf":
            results.append(step_perf(device, args.seconds))
        elif step == "capture":
            extras = {"prismOrientation": args.orientation} if args.orientation else {}
            status, lines = instrument(device, f"{SESSION_TEST}#capturesAnUprightPicture", extras, prefixes=("CAPTURE ",))
            results.append(report(step, status[0], lines, status[1]))
        elif step == "captureui":
            status, lines = instrument(device, f"{SESSION_TEST}#captureUiAnswersNullWhenClosed", prefixes=("CAPTURE_UI",))
            results.append(report(step, status[0], lines, status[1]))
        elif step == "controls":
            extras = {"prismExpectFocus": "true"} if args.expect_focus else {}
            status, lines = instrument(device, f"{SESSION_TEST}#controlsChangeTheCamera", extras, prefixes=("CONTROLS",))
            results.append(report(step, status[0], lines, status[1]))
        elif step == "preview":
            results.append(step_preview(device, args.seconds, args.orientation, args.screenshot))
        elif step == "front":
            status, lines = instrument(device, f"{SESSION_TEST}#frontPreviewIsMirrored", prefixes=("FRONT",))
            results.append(report(step, status[0], lines, status[1]))
        elif step == "kill":
            results.append(step_kill(device))
    failed = results.count("FAIL")
    print(f"summary: {results.count('PASS')} pass, {failed} fail, {results.count('SKIP')} skip")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
