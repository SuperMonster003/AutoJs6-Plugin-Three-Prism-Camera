"""Skeleton smoke run of 3-Prism Camera on one device (roadmap P0.4; roadmap P4.1 and P5 extend it to the 1.0.0 checks).

    py .python/device_smoke.py <serial> [--apk <file> | --abi <abi>] [--no-install] [--screenshot <dir>]

1. host: the AutoJs6 versionCode, compared with requiresHostVersion (reported only: the INFO service answers any host,
   the `camera` module of roadmap P1.4 needs the required build).
2. install: `adb install -r` of the APK. One APK per ABI plus a universal APK (roadmap D22, appendix D Q8 = (b)): by
   default the debug APK of app/build/outputs/apk/debug for the first ABI of the device's `ro.product.cpu.abilist` that
   has one (--abi picks another, such as arm64-v8a on an x86_64 emulator with the native bridge), else the universal debug
   APK, else the newest matching APK of releases/ (debug and release builds share the signing key, so the update keeps
   data). ColorOS and MIUI / HyperOS show a confirmation screen for USB installs: the maintainer confirms it on the phone.
3. discovery: `cmd package query-services` resolves exactly one service of the plugin for org.autojs.plugin.INFO /
   three-prism-camera and for org.autojs.plugin.CAMERA / camera.
4. CameraX probe (roadmap P0.2): the enabled launcher alias opens the capture screen, whose CameraController logs
   `availableCameraInfos=<count>` under the PrismCamera tag; enumerating cameras needs no CAMERA permission, so no
   dialog appears. A device without a camera passes with a count of 0 only under --allow-no-camera.
5. activation and getInfo: an AutoJs6 script (RunIntentActivity, as in 3-Macaw Speech) starts the Wake activity (it
   needs org.autojs.permission.PLUGIN, which the shell lacks), binds the INFO service explicitly and calls getInfo(),
   whose supportedAbis must equal the ABIs whose whitelisted native libraries the installed APK carries,
   then binds the CAMERA service and calls ICameraPlugin getInfo() and getCapabilities() (roadmap P2.1): the descriptor,
   the plugin ID, the features, the permission state, the camera count, shared memory and the CameraX version. The
   Wake start is confirmed from the ActivityManager log lines since the step began (device clock, logcat -t).
6. --screenshot <dir>: the script opens AutoJs6's Plugin Center and the screen is saved as
   plugin-center-<serial>-system-<light|dark>.png. The name records the system night mode, which picks the launcher
   icon variant; the Plugin Center itself follows the AutoJs6 theme setting, so check the picture for its theme. The
   launcher differs per vendor, so its screenshot stays manual.

Output: PASS / FAIL per step and the key values; no description text, token or tool list is printed.
A secure lock screen keeps activities from starting: unlock the device first. Nothing here answers a system dialog.
"""
import argparse
import hashlib
import json
import re
import subprocess
import sys
import tempfile
import time
import zipfile
from pathlib import Path

HOST = "org.autojs.autojs6"
PLUGIN = "io.github.supermonster003.autojs6.plugin.three.prism.camera"
PLUGIN_ID = "three-prism-camera"
REMOTE = "/sdcard/Download/autojs6-prism-run"
ROOT = Path(__file__).resolve().parent.parent
DISCOVERY = (
    ("org.autojs.plugin.INFO", PLUGIN_ID, f"{PLUGIN}/.ThreePrismCameraInfoService"),
    ("org.autojs.plugin.CAMERA", "camera", f"{PLUGIN}/.ThreePrismCameraService"),
)
PROBE_TAG = "PrismCamera"
CAPABILITY_DESCRIPTOR = "org.autojs.plugin.camera.api.ICameraPlugin"
PLUGIN_CENTER = "org.autojs.autojs.core.plugin.center.PluginCenterActivity"
NATIVE_ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
NATIVE_LIBRARIES = ("libandroidx.graphics.path.so", "libimage_processing_util_jni.so", "libsurface_util_jni.so")

SCRIPT = """
var out = "%(out)s";
var pkg = "%(pkg)s";
var r = { steps: {}, errors: [] };
files.write(out + ".started", String(Date.now()));
function done() { files.write(out, JSON.stringify(r)); }
function attempt(name, fn) {
    try { r.steps[name] = fn(); } catch (e) { r.steps[name] = { error: String(e.message || e) }; }
}
var Intent = android.content.Intent, ComponentName = android.content.ComponentName;
var TimeUnit = java.util.concurrent.TimeUnit;
var bound = [];
function bind(cls) {
    var latch = new java.util.concurrent.CountDownLatch(1);
    var ref = { binder: null };
    var connection = new android.content.ServiceConnection({
        onServiceConnected: function (name, binder) { ref.binder = binder; latch.countDown(); },
        onServiceDisconnected: function (name) {}
    });
    var intent = new Intent().setComponent(new ComponentName(pkg, pkg + cls));
    if (!context.bindService(intent, connection, android.content.Context.BIND_AUTO_CREATE)) throw new Error("bindService false " + cls);
    bound.push(connection);
    if (!latch["await"](10, TimeUnit.SECONDS) || ref.binder == null) throw new Error("no binder " + cls);
    return ref.binder;
}
attempt("wake", function () {
    var t = Date.now();
    context.startActivity(new Intent().setComponent(new ComponentName(pkg, pkg + ".WakeActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    sleep(1500);
    return { ms: Date.now() - t };
});
attempt("info", function () {
    var t = Date.now();
    var binder = bind(".ThreePrismCameraInfoService");
    var descriptor = String(binder.getInterfaceDescriptor());
    var info;
    try {
        info = Packages.org.autojs.plugin.common.api.IPluginInfoProvider.Stub.asInterface(binder).getInfo();
    } catch (e) {
        // A host without the generated proxy class: the same call as a raw Parcel (getInfo is the first transaction).
        var data = android.os.Parcel.obtain(), reply = android.os.Parcel.obtain();
        data.writeInterfaceToken(descriptor);
        binder.transact(android.os.IBinder.FIRST_CALL_TRANSACTION, data, reply, 0);
        reply.readException();
        info = reply.readInt() !== 0 ? Packages.org.autojs.plugin.common.api.PluginInfo.CREATOR.createFromParcel(reply) : null;
        data.recycle(); reply.recycle();
    }
    if (info == null) throw new Error("getInfo returned null");
    var caps = info.capabilities;
    var keys = [];
    if (caps != null) { var it = caps.keySet().iterator(); while (it.hasNext()) keys.push(String(it.next())); }
    keys.sort();
    return {
        ms: Date.now() - t, descriptor: descriptor, name: String(info.name), id: String(info.id), engine: String(info.engine),
        variant: String(info.variant), versionName: String(info.versionName), versionCode: Number(info.versionCode),
        versionDate: String(info.versionDate), descriptionChars: info.description == null ? -1 : String(info.description).length,
        abis: info.supportedAbis == null ? "null" : String(java.util.Arrays.toString(info.supportedAbis)), capabilityKeys: keys.join(","),
        requiresHostVersion: caps == null ? -1 : Number(caps.getLong("requiresHostVersion", -1)),
        contractVersion: caps == null ? -1 : caps.getInt("cameraContractVersion", -1),
        features: caps == null || caps.getStringArray("cameraFeatures") == null ? -1 : caps.getStringArray("cameraFeatures").length
    };
});
attempt("camera", function () {
    var binder = bind(".ThreePrismCameraService");
    // ICameraPlugin 1 getInfo and 2 getCapabilities as raw Parcels, so a host without the camera contract can run them.
    function call(code, read) {
        var data = android.os.Parcel.obtain(), reply = android.os.Parcel.obtain();
        try {
            data.writeInterfaceToken("%(descriptor)s");
            if (!binder.transact(android.os.IBinder.FIRST_CALL_TRANSACTION + code, data, reply, 0)) throw new Error("transaction " + code + " not handled");
            reply.readException();
            return reply.readInt() !== 0 ? read(reply) : null;
        } finally { data.recycle(); reply.recycle(); }
    }
    var info = call(0, function (p) { return Packages.org.autojs.plugin.common.api.PluginInfo.CREATOR.createFromParcel(p); });
    var caps = call(1, function (p) { return android.os.Bundle.CREATOR.createFromParcel(p); });
    if (caps == null) throw new Error("getCapabilities returned null");
    var features = caps.getStringArray("cameraFeatures");
    return {
        descriptor: String(binder.getInterfaceDescriptor()), id: info == null ? "null" : String(info.id),
        error: caps.getString("errorCode") == null ? undefined : String(caps.getString("errorCode")),
        features: features == null ? "null" : String(java.util.Arrays.toString(features)),
        permission: caps.getBoolean("cameraPermissionGranted", false), engine: String(caps.getString("cameraEngineVersion")),
        cameras: caps.getInt("cameraCount", -1), sharedMemory: caps.getBoolean("cameraSharedMemory", false)
    };
});
bound.forEach(function (c) { try { context.unbindService(c); } catch (e) { r.errors.push("unbind " + e); } });
if (%(center)s) {
    attempt("pluginCenter", function () {
        context.startActivity(new Intent().setClassName("%(host)s", "%(center_class)s").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        return { started: true };
    });
}
done();
"""


def adb(serial, *args, check=True, timeout=120):
    return subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace",
                          check=check, timeout=timeout).stdout.strip()


def label(serial):
    return f"{adb(serial, 'shell', 'getprop', 'ro.product.model')} ({serial}, API {adb(serial, 'shell', 'getprop', 'ro.build.version.sdk')})"


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def required_host_version():
    match = re.search(r"^val requiredHostVersion = (\d+)$", (ROOT / "app" / "build.gradle.kts").read_text(encoding="utf-8"), re.M)
    return int(match.group(1))


def camerax_version():
    """The CameraX version of the version catalog, answered as cameraEngineVersion."""
    match = re.search(r'^camerax = "([^"]+)"$', (ROOT / "gradle" / "libs.versions.toml").read_text(encoding="utf-8"), re.M)
    return match.group(1)


def apk_abis(apk):
    """The ABIs whose whitelisted native libraries the APK carries, as NativeLibraryInventory reports them."""
    with zipfile.ZipFile(apk) as archive:
        names = set(archive.namelist())
    return [abi for abi in NATIVE_ABIS if all(f"lib/{abi}/{library}" in names for library in NATIVE_LIBRARIES)]


def pick_apk(serial, given, abi):
    """The APK for the device (roadmap D22, appendix D Q8 = (b)): the debug APK of its first supported ABI, else the
    universal debug APK, else the newest release APK of that ABI or the universal one."""
    if given:
        return Path(given)
    device_abis = [abi] if abi else [item for item in adb(serial, "shell", "getprop", "ro.product.cpu.abilist").split(",") if item in NATIVE_ABIS]
    debug = ROOT / "app" / "build" / "outputs" / "apk" / "debug"
    for candidate in [*device_abis, "universal"]:
        apk = debug / f"autojs6-plugin-three-prism-camera-v{version_name()}-{candidate}.apk"
        if apk.is_file():
            return apk
    for candidate in [*device_abis, "universal"]:
        apks = sorted((ROOT / "releases").glob(f"*-v*-{candidate}-*.apk"), key=lambda path: path.stat().st_mtime, reverse=True)
        if apks:
            return apks[0]
    raise SystemExit(f"no APK for {device_abis or 'this device'}: build with :app:assembleDebug or pass --apk")


def version_name():
    properties = (ROOT / "version.properties").read_text(encoding="utf-8")
    return re.search(r"^VERSION_NAME=(.+)$", properties, re.M).group(1).strip()


def installed_version(serial, package):
    match = re.search(r"versionCode=(\d+)", adb(serial, "shell", "dumpsys", "package", package, check=False))
    return int(match.group(1)) if match else None


def install(serial, apk):
    started = time.time()
    try:
        out = subprocess.run(["adb", "-s", serial, "install", "-r", str(apk)], capture_output=True, text=True, timeout=300)
        ok = out.returncode == 0 and "Success" in out.stdout
        detail = "Success" if ok else ((out.stdout + out.stderr).strip().splitlines() or ["no output"])[-1]
    except subprocess.TimeoutExpired:
        ok, detail = False, "timed out (a confirmation screen on the phone?)"
    return ok, detail, time.time() - started


def launcher(serial, package=HOST):
    """The package's own enabled launcher activity (a debug host with LeakCanary lists a second launcher entry)."""
    listed = adb(serial, "shell", "cmd", "package", "query-activities", "--brief", "-a", "android.intent.action.MAIN",
                 "-c", "android.intent.category.LAUNCHER", package)
    own = [line.strip() for line in listed.splitlines() if line.strip().startswith(package + "/") and "leakcanary" not in line]
    if own:
        return own[0]
    return adb(serial, "shell", "cmd", "package", "resolve-activity", "--brief", "-c", "android.intent.category.LAUNCHER", package).splitlines()[-1].strip()


def device_time(serial):
    """The device clock in logcat's -t format; a start time survives the CameraX log burst that pushes a marker line out
    of a small main buffer (G8441), and ActivityManager writes its START lines to the system buffer anyway."""
    return adb(serial, "shell", "date", "+%m-%d_%H:%M:%S.000").replace("_", " ")


def log_since(serial, since, *filters):
    return adb(serial, "shell", "logcat", "-d", "-b", "main", "-b", "system", "-v", "brief", "-t", f"'{since}'", *filters,
               check=False, timeout=60)


def camera_probe(serial):
    """Opens the capture screen through the enabled launcher alias and waits for the CameraController probe line."""
    adb(serial, "shell", "am", "force-stop", PLUGIN, check=False)
    since = device_time(serial)
    component = launcher(serial, PLUGIN)
    started = time.time()
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    adb(serial, "shell", "am", "start", "-n", component, check=False)
    deadline = started + 20
    while time.time() < deadline:
        match = re.search(rf"{PROBE_TAG}\(\s*\d+\): availableCameraInfos=(\S+)(.*)",
                          log_since(serial, since, f"{PROBE_TAG}:*", "*:S"))
        if match:
            return component, match.group(1), match.group(2).strip(), time.time() - started
        time.sleep(1)
    return component, None, "no probe line within 20 s", time.time() - started


def discover(serial, action, category):
    listed = adb(serial, "shell", "cmd", "package", "query-services", "--brief", "-a", action, "-c", category, check=False)
    return [line.strip() for line in listed.splitlines() if line.strip().startswith(PLUGIN + "/")]


def night_mode(serial):
    return "dark" if adb(serial, "shell", "cmd", "uimode", "night", check=False).strip().endswith("yes") else "light"


def run_script(serial, center):
    values = {"out": f"{REMOTE}/smoke.json", "pkg": PLUGIN, "descriptor": CAPABILITY_DESCRIPTOR, "center": "true" if center else "false",
              "host": HOST, "center_class": PLUGIN_CENTER}
    local = Path(tempfile.gettempdir()) / f"prism-smoke-{serial.replace(':', '_')}.js"
    local.write_text(SCRIPT % values, encoding="utf-8")
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    adb(serial, "push", str(local), f"{REMOTE}/smoke.js")
    local.unlink(missing_ok=True)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "am", "start", "-n", launcher(serial))
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/smoke.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    deadline = time.time() + 60
    result = None
    while time.time() < deadline:
        raw = adb(serial, "shell", "cat", f"{REMOTE}/smoke.json", check=False)
        if raw.startswith("{"):
            result = json.loads(raw)
            break
        time.sleep(1)
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    return result


def run(serial, args):
    name = label(serial)
    ok = True

    def report(step, passed, text):
        nonlocal ok
        ok = ok and passed
        print(f"    {step}: {'PASS' if passed else 'FAIL'} {text}")

    required = required_host_version()
    host = installed_version(serial, HOST)
    print(f"{name}: AutoJs6 versionCode {host}, plugin requires {required} ({'met' if host and host >= required else 'below, the camera module stays unavailable'})")
    if not host:
        print(f"{name}: SMOKE FAIL (AutoJs6 is not installed)")
        return False
    apk = pick_apk(serial, args.apk, args.abi)
    expected_abis = "[" + ", ".join(apk_abis(apk)) + "]"
    if args.no_install:
        print(f"    install: SKIPPED (--no-install), installed versionCode {installed_version(serial, PLUGIN)}")
    else:
        passed, detail, seconds = install(serial, apk)
        report("install", passed, f"{apk.name} ({apk.stat().st_size} bytes, SHA-256 {sha256(apk)[:16]}...) in {seconds:.1f} s, "
                                  f"versionCode {installed_version(serial, PLUGIN)}; {detail}")
        if not passed:
            return False
    for action, category, expected in DISCOVERY:
        found = discover(serial, action, category)
        report(f"discovery {action.rsplit('.', 1)[-1]}", found == [expected], f"{action} / {category}: {len(found)} service(s) {found}")
    component, count, detail, seconds = camera_probe(serial)
    cameras = int(count) if count and count.isdigit() else None
    report("CameraX probe", cameras is not None and (cameras > 0 or args.allow_no_camera),
           f"{component.split('/', 1)[-1]}: availableCameraInfos={count} {detail} in {seconds:.1f} s".replace("  ", " ").rstrip())
    adb(serial, "shell", "am", "force-stop", PLUGIN, check=False)
    since = device_time(serial)
    mode = night_mode(serial)
    r = run_script(serial, bool(args.screenshot))
    if r is None:
        print(f"{name}: SMOKE FAIL (no script result: unlock the device; AutoJs6 needs all-files access)")
        return False
    steps = r.get("steps", {})
    wake_started = bool(re.search(rf"START .*cmp={re.escape(PLUGIN)}/\.WakeActivity", log_since(serial, since)))
    wake = steps.get("wake", {})
    report("wake", not wake.get("error") and wake_started,
           f"Wake activity start {'logged' if wake_started else 'not found in the log'} {wake.get('error') or ''}".rstrip())
    info = steps.get("info", {})
    expected_info = info.get("id") == PLUGIN_ID and info.get("engine") == "camera" and info.get("variant") == "default"
    report("getInfo", not info.get("error") and expected_info and info.get("abis") == expected_abis and info.get("requiresHostVersion") == required,
           f"{info.get('name')} {info.get('id')} / {info.get('engine')} / {info.get('variant')}, {info.get('versionName')} ({info.get('versionCode')}, "
           f"{info.get('versionDate')}), description {info.get('descriptionChars')} chars, ABIs {info.get('abis')} (APK {expected_abis}), requiresHostVersion "
           f"{info.get('requiresHostVersion')}, contract {info.get('contractVersion')}, features {info.get('features')}, capability keys "
           f"[{info.get('capabilityKeys')}], {info.get('ms')} ms {info.get('error') or ''}".rstrip())
    camera = steps.get("camera", {})
    report("camera binder", not camera.get("error") and camera.get("descriptor") == CAPABILITY_DESCRIPTOR and camera.get("id") == PLUGIN_ID
           and camera.get("engine") == camerax_version() and camera.get("features", "null") != "null",
           f"descriptor {camera.get('descriptor')}, getInfo id {camera.get('id')}, features {camera.get('features')}, permission "
           f"{camera.get('permission')}, cameras {camera.get('cameras')}, sharedMemory {camera.get('sharedMemory')}, engine "
           f"{camera.get('engine')} {camera.get('error') or ''}".rstrip())
    if r.get("errors"):
        report("script", False, f"errors {[e[:160] for e in r['errors']]}")
    if args.screenshot:
        center = steps.get("pluginCenter", {})
        time.sleep(4)
        folder = Path(args.screenshot)
        folder.mkdir(parents=True, exist_ok=True)
        target = folder / f"plugin-center-{serial.replace(':', '_')}-system-{mode}.png"
        with open(target, "wb") as stream:
            subprocess.run(["adb", "-s", serial, "exec-out", "screencap", "-p"], stdout=stream, check=False, timeout=60)
        report("screenshot", not center.get("error") and target.stat().st_size > 0, f"{target.name} ({target.stat().st_size} bytes, system {mode}) {center.get('error') or ''}".rstrip())
    print(f"{name}: SMOKE {'PASS' if ok else 'FAIL'} ({apk.name})")
    return ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--apk", help="APK to install (default: the debug APK of the device's ABI, else the universal one, else a release)")
    parser.add_argument("--abi", choices=NATIVE_ABIS, help="install the APK of this ABI instead of the device's first one")
    parser.add_argument("--no-install", action="store_true", help="keep the installed build")
    parser.add_argument("--screenshot", metavar="DIR", help="open the Plugin Center afterwards and save the screen into DIR")
    parser.add_argument("--allow-no-camera", action="store_true", help="accept availableCameraInfos=0 (a device or AVD without cameras)")
    arguments = parser.parse_args(argv)
    return 0 if run(arguments.serial, arguments) else 1


if __name__ == "__main__":
    sys.exit(main())
