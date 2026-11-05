# -*- coding: utf-8 -*-
"""Run :app:connectedDebugAndroidTest on the CI emulator of Build integrity, once more after an emulator failure.

Trimmed from 3-Macaw Speech's script of the same name (roadmap D35, P0.4), whose CI runs showed why: emulators can lose
the device right after boot, restart system_server while Gradle installs, or fail `package install-commit`, and an
`adb` call without a time limit then hangs the job until it is cancelled. 3-Prism Camera runs one image (API 33
google_apis x86_64 with `-camera-back virtualscene -camera-front emulated`, see .github/workflows/build.yml), so the
three-button navigation workaround of the API 37.1 image is not carried over.

The script builds the APKs first, waits for a settled emulator, turns the animations off and hides system error
dialogs (emulators only), runs the suite with a time limit and, when it fails with one of the emulator signatures below, after a
system_server restart or when adb lost the device, settles again (bounded), keeps the first attempt's reports in
app/build/reports/androidTests-attempt-1 and runs the suite once more. A failure that repeats stays a failure; a test
failure without such a cause is never retried. The logcat of each attempt goes to app/build/reports/androidTests/.

Settled means: the device answers, sys.boot_completed is 1, user 0 is RUNNING_UNLOCKED and the system_server start count
has not changed for 10 seconds.

Usage (inside reactivecircus/android-emulator-runner, from the repository root; on a workstation pin one device with
ANDROID_SERIAL, other sessions share the devices):
  python3 .python/ci_connected_tests.py [--classes ClassA,ClassB]
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEST_PACKAGE = "io.github.supermonster003.autojs6.plugin.three.prism.camera"
GRADLE_FLAGS = ["-Pautojs.gradle.build.number.auto.increment.enabled=false",
                "-Pautojs.gradle.build.time.update.enabled=false", "--console=plain", "--stacktrace"]
# gradlew.bat lets the script run on a Windows workstation too (with ANDROID_SERIAL pinning one device).
GRADLEW = str(ROOT / "gradlew.bat") if os.name == "nt" else "./gradlew"
BUILD = [GRADLEW, ":app:assembleDebug", ":app:assembleDebugAndroidTest", *GRADLE_FLAGS]
SUITE = [GRADLEW, ":app:connectedDebugAndroidTest", *GRADLE_FLAGS]
BUILD_TIMEOUT = 30 * 60
SUITE_TIMEOUT = 25 * 60
SETTLE_TIMEOUT = 300
SETTLE_QUIET = 10
USER_STATE_SDK = 26
EMULATOR_FAILURE = re.compile("|".join([
    r"No compatible devices connected",
    r"System has crashed",
    r"DeadSystemException",
    r"Failed to commit install session",
    r"DELETE_FAILED_INTERNAL_ERROR",
    r"Failure calling service package",
    r"device offline",
    r"device '[^']*' not found",
    r"ShellCommandUnresponsiveException",
]))
REPORTS = ROOT / "app" / "build" / "reports" / "androidTests"
RESULTS = ROOT / "app" / "build" / "outputs" / "androidTest-results" / "connected"


def log(message):
    print(f"[ci_connected_tests] {message}", flush=True)


def adb(*command, timeout=60):
    try:
        return subprocess.run(["adb", *command], capture_output=True, text=True, encoding="utf-8", errors="replace",
                              timeout=timeout).stdout.strip()
    except subprocess.TimeoutExpired:
        return ""


def start_count():
    """sys.system_server.start_count, empty on images without it."""
    return adb("shell", "getprop", "sys.system_server.start_count", timeout=20)


def settle(stage):
    """Waits (bounded) for a settled emulator; returns whether it settled."""
    began = time.time()
    last, steady_since = None, None
    while time.time() - began < SETTLE_TIMEOUT:
        ready = adb("get-state", timeout=20) == "device" and adb("shell", "getprop", "sys.boot_completed", timeout=20) == "1"
        count = start_count() if ready else None
        if ready:
            sdk = adb("shell", "getprop", "ro.build.version.sdk", timeout=20)
            if sdk.isdigit() and int(sdk) >= USER_STATE_SDK:
                ready = adb("shell", "am", "get-started-user-state", "0", timeout=20) == "RUNNING_UNLOCKED"
        if ready and count == last:
            steady_since = steady_since or time.time()
            if time.time() - steady_since >= SETTLE_QUIET:
                log(f"settled before {stage}: system_server start count {count or 'n/a'}, waited {time.time() - began:.0f} s")
                return True
        else:
            steady_since = None
        last = count
        time.sleep(2)
    log(f"the emulator did not settle before {stage} within {SETTLE_TIMEOUT} s (start count {start_count() or 'n/a'})")
    return False


def prepare_settings():
    """No animations and no system error dialogs; settings needs a running system_server, so only after settling.
    Only emulators are changed: a pinned workstation phone keeps the maintainer's settings."""
    if "1" not in (adb("shell", "getprop", "ro.kernel.qemu", timeout=20), adb("shell", "getprop", "ro.boot.qemu", timeout=20)):
        log("a real device keeps its animation and error dialog settings")
        return
    for name, value in (("window_animation_scale", "0.0"), ("transition_animation_scale", "0.0"),
                        ("animator_duration_scale", "0.0"), ("hide_error_dialogs", "1")):
        adb("shell", "settings", "put", "global", name, value, timeout=30)


def gradle(command, timeout, label):
    log(f"{label}: {' '.join(command)}")
    output = []
    process = subprocess.Popen(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                               encoding="utf-8", errors="replace")
    started = time.time()
    try:
        for line in process.stdout:
            sys.stdout.write(line)
            output.append(line)
            if time.time() - started > timeout:
                log(f"{label} exceeded {timeout} s, stopping Gradle")
                process.kill()
                break
        status = process.wait(timeout=60)
    except subprocess.TimeoutExpired:
        process.kill()
        status = 1
    return status, "".join(output)


def run_suite(attempt, suite):
    """Runs one settled attempt; returns its status and the emulator cause of a failure, if any."""
    if not settle(f"attempt {attempt}"):
        return 1, "the emulator did not settle"
    prepare_settings()
    before = start_count()
    status, output = gradle(suite, SUITE_TIMEOUT, f"attempt {attempt}")
    REPORTS.mkdir(parents=True, exist_ok=True)
    logcat = adb("logcat", "-d", "-b", "main", "-b", "system", "-b", "crash", timeout=120)
    (REPORTS / f"logcat-attempt-{attempt}.txt").write_text(logcat, encoding="utf-8")
    if status == 0:
        return 0, None
    signature = EMULATOR_FAILURE.search(output)
    if signature:
        return status, signature.group(0)
    # A restarted system_server raises the start count only once it runs, so compare after settling.
    if not settle(f"the check of attempt {attempt}"):
        adb("reconnect", timeout=30)
        return status, "the device stopped answering"
    after = start_count()
    if after != before:
        return status, f"system_server restarted (start count {before} -> {after})"
    return status, None


def main(argv):
    parser = argparse.ArgumentParser(description="Connected tests of Build integrity with one rerun after an emulator failure.")
    parser.add_argument("--classes", default="", help="comma-separated simple names of the only test classes to run")
    args = parser.parse_args(argv)
    suite = list(SUITE)
    classes = [name.strip() for name in args.classes.split(",") if name.strip()]
    if classes:
        suite.append("-Pandroid.testInstrumentationRunnerArguments.class=" + ",".join(f"{TEST_PACKAGE}.{name}" for name in classes))
        log(f"only these test classes: {', '.join(classes)}")

    status, _ = gradle(BUILD, BUILD_TIMEOUT, "build")
    if status != 0:
        return status

    status, cause = run_suite(1, suite)
    if status == 0:
        return 0
    if not cause:
        log("test failure without an emulator cause: no retry")
        return status
    log(f"emulator failure ({cause}); retrying once")
    for source in (REPORTS, RESULTS):
        if source.exists():
            target = source.with_name(source.name + "-attempt-1")
            shutil.rmtree(target, ignore_errors=True)
            shutil.move(str(source), str(target))
    status, cause = run_suite(2, suite)
    if status != 0 and cause:
        log(f"the second attempt failed too ({cause})")
    return status


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
