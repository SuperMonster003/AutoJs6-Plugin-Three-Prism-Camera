"""Screenshots of the standalone screens and icons of 3-Prism Camera in light and dark (roadmap P3.1 - P3.4 evidence).

Usage: py -u .python/device_camera_screens.py <serial> [--out .bak/visual/1.0.0] [--themes light,dark]
       [--language host] [--steps plugin,system]

- plugin: for each theme the plugin's own appearance preference is set (debug build, through run-as; the previous file is
  restored afterwards) and the plugin is opened from the launcher: the viewfinder, a photo with its size (normal mode,
  not saved), the settings page (top and bottom), the lens dialog, the release history and the recents screen. Only
  controls of the plugin's own windows are tapped (found by test tag in a uiautomator dump).
  The theme `current` writes no preference (a phone of the maintainer: the plugin follows its saved setting) and
  names the shots after the device's night mode.
- system: the launcher drawer, the recents screen and the Plugin Center of AutoJs6. On an emulator the system night mode
  is switched per theme (`cmd uimode night`) and reset to "no" afterwards; on a phone the device settings are never
  changed, so only the current mode is captured and named after it.

zh-CN: 3-Prism Camera 独立界面与图标的明暗截图 (路线图 P3.1 - P3.4 证据). plugin: 每个主题先设置插件自己的外观偏好 (debug 包, 经 run-as;
结束后还原原文件), 再从启动器打开插件: 取景页, 普通模式拍下的照片与尺寸 (不保存), 设置页 (顶部与底部), 镜头对话框, 发行历史与最近任务.
只点按插件自身窗口的控件 (按测试标签在 uiautomator 转储中查找). 主题 `current` 不写偏好 (维护者的手机: 插件按已保存的设置显示),
截图按设备的夜间模式命名. system: 启动器抽屉, 最近任务与 AutoJs6 插件中心. 模拟器上按主题切换系统夜间模式
(`cmd uimode night`), 结束后恢复为 "no"; 真机不改设备设置, 只截当前模式并按其命名.
"""
import argparse
import pathlib
import re
import subprocess
import sys
import time

from device_camera_samples import DEVICE_DIR, HOST, PLUGIN, RUN_ACTIVITY, Device

PREFS = "shared_prefs/app-appearance.xml"
PLUGIN_CENTER = "org.autojs.autojs.core.plugin.center.PluginCenterActivity"
TITLE = "3-Prism Camera"


class Screens(Device):
    def __init__(self, serial, folder):
        super().__init__(serial)
        self.folder = folder

    def shot(self, name):
        time.sleep(1)
        self.screenshot(self.folder, "%s-%s.png" % (name, self.serial))

    def run_as(self, command, data=None):
        return subprocess.run(["adb", "-s", self.serial, "shell", "run-as %s sh -c '%s'" % (PLUGIN, command)], input=data,
                              capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60)

    def appearance(self, dark_mode, language):
        self.shell("am force-stop " + PLUGIN)
        xml = ("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
               "    <string name=\"language\">%s</string>\n    <string name=\"darkMode\">%s</string>\n</map>\n") % (language, dark_mode)
        self.run_as("mkdir -p shared_prefs && cat > %s" % PREFS, xml)

    def night(self):
        out = self.shell("cmd uimode night")
        return "yes" in out.lower()

    def swipe_up(self):
        size = re.search(r"(\d+)x(\d+)", self.shell("wm size"))
        width, height = (int(size.group(1)), int(size.group(2))) if size else (1080, 1920)
        self.shell("input swipe %d %d %d %d 400" % (width // 2, height * 3 // 4, width // 2, height // 4))

    def scroll_to_tag(self, tag, swipes=3):
        """Swipes the settings page up until the control is visible (a dump only lists what is on screen).
        zh-CN: 上滑设置页直到控件可见 (转储只列出屏幕上的节点)."""
        for _ in range(swipes + 1):
            if self.find_tag(tag, 3) is not None:
                return True
            self.swipe_up()
            time.sleep(1)
        return False

    def text_visible(self, package, text):
        self.shell("uiautomator dump %s/ui.xml" % DEVICE_DIR, timeout=60)
        xml = self.read("ui.xml")
        return any('package="%s"' % package in node and 'text="%s"' % text in node for node in re.findall(r"<node [^>]*>", xml))


def plugin_screens(device, theme, language):
    if theme == "current":
        device.shell("am force-stop " + PLUGIN)
        theme = "dark" if device.night() else "light"
    else:
        device.appearance(theme, language)
    launcher = device.plugin_launcher()
    device.shell("am start -n " + launcher)
    if not device.viewfinder_resumed() or device.find_tag("prism.capture.shutter", 20) is None:
        print("%s viewfinder FAIL the viewfinder did not show the shutter" % theme, flush=True)
        return
    time.sleep(2)
    device.shot("p3.1-viewfinder-%s" % theme)
    device.tap_tag("prism.capture.shutter")
    ok = device.find_tag("prism.capture.photoSize", 20) is not None
    print("%s photo %s" % (theme, "PASS" if ok else "FAIL"), flush=True)
    device.shot("p3.1-result-%s" % theme)
    device.tap_tag("prism.capture.again")
    device.find_tag("prism.capture.shutter", 20)
    device.tap_tag("prism.capture.settings")
    if device.find_tag("prism.settings", 15) is None:
        print("%s settings FAIL the settings page did not open" % theme, flush=True)
        return
    time.sleep(2)
    device.shot("p3.2-settings-top-%s" % theme)
    device.swipe_up()
    device.shot("p3.2-settings-bottom-%s" % theme)
    opened = device.scroll_to_tag("prism.settings.lenses") and device.tap_tag("prism.settings.lenses")
    if opened and device.find_tag("prism.dialog.lenses", 10) is not None:
        device.shot("p3.2-lenses-%s" % theme)
    else:
        print("%s lenses FAIL no lens dialog" % theme, flush=True)
    if opened:
        device.shell("input keyevent 4")
        time.sleep(1)
    if device.scroll_to_tag("prism.settings.releaseHistory") and device.tap_tag("prism.settings.releaseHistory"):
        time.sleep(2)
        device.shot("p3.3-release-%s" % theme)
        device.shell("input keyevent 4")
    else:
        print("%s release FAIL no release history row" % theme, flush=True)
    print("%s plugin screens done" % theme, flush=True)
    device.shell("input keyevent 4")
    time.sleep(1)
    device.shell("input keyevent KEYCODE_APP_SWITCH")
    time.sleep(2)
    device.shot("p3.4-recents-%s" % theme)
    device.shell("input keyevent 3")


def system_screens(device, theme):
    device.shell("input keyevent 3")
    time.sleep(1)
    device.swipe_up()
    time.sleep(2)
    device.shot("p3.4-launcher-%s" % theme)
    device.shell("input keyevent 3")
    # An inline script: a freshly installed host may refuse a script file before its storage access is settled.
    # zh-CN: 内联脚本: 刚安装的宿主在存储访问确定之前可能拒绝按文件路径运行脚本.
    script = "app.startActivity({ packageName: '%s', className: '%s' });" % (HOST, PLUGIN_CENTER)
    device.front()
    device.shell("am start -n %s/%s -e script \"%s\"" % (HOST, RUN_ACTIVITY, script))
    time.sleep(4)
    for _ in range(6):
        if device.text_visible(HOST, TITLE):
            break
        device.swipe_up()
        time.sleep(1)
    else:
        print("%s pluginCenter FAIL no %s row" % (theme, TITLE), flush=True)
    device.shot("p3.4-plugin-center-%s" % theme)
    device.shell("input keyevent 3")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("serial")
    parser.add_argument("--out", default=str(pathlib.Path(__file__).resolve().parents[1] / ".bak" / "visual" / "1.0.0"))
    parser.add_argument("--themes", default="light,dark")
    parser.add_argument("--language", default="host")
    parser.add_argument("--steps", default="plugin,system")
    args = parser.parse_args()
    device = Screens(args.serial, pathlib.Path(args.out))
    device.shell("mkdir -p '%s'" % DEVICE_DIR)
    themes = [t.strip() for t in args.themes.split(",") if t.strip()]
    steps = [s.strip() for s in args.steps.split(",") if s.strip()]
    writes = any(theme != "current" for theme in themes)
    saved = device.run_as("cat %s 2>/dev/null" % PREFS).stdout if writes else ""
    try:
        if "plugin" in steps:
            for theme in themes:
                plugin_screens(device, theme, args.language)
        if "system" in steps:
            if device.emulator:
                for theme in themes:
                    device.shell("cmd uimode night " + ("yes" if theme == "dark" else "no"))
                    time.sleep(2)
                    system_screens(device, theme)
            else:
                system_screens(device, "dark" if device.night() else "light")
    finally:
        device.shell("am force-stop " + PLUGIN)
        if writes and saved.strip():
            device.run_as("cat > %s" % PREFS, saved)
        elif writes:
            device.run_as("rm -f %s" % PREFS)
        if device.emulator and "system" in steps:
            device.shell("cmd uimode night no")
        device.shell("rm -r '%s'" % DEVICE_DIR)
    return 0


if __name__ == "__main__":
    sys.exit(main())
