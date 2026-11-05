<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-prism-camera-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Photo capture, continuous frame analysis and viewfinder preview through CameraX, with frames ready for OCR, barcode, YOLO and other recognition modules</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The README.md file is currently available in the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Prism Camera is the camera plugin of AutoJs6. Built on Jetpack CameraX, it gives scripts photo capture, continuous frame analysis and viewfinder preview. Live frames reach the script as AutoJs6 `ImageWrapper` objects, so a script can hand them straight to existing modules such as `ocr`, `barcode` or `yolo` for live barcode scanning, text recognition and object detection.

The plugin owns the camera and talks to AutoJs6 over Binder: live frames travel through a shared memory ring buffer (temporary files on Android 7.0 - 8.0), photos reach AutoJs6 as JPEG to be decoded or saved unchanged, and the preview renders straight into a `SurfaceView` of the script's interface, so no pixels cross Binder. While a session is open, the plugin runs a camera foreground service, so scripts keep receiving frames in the background or with the screen off.

> This is a development preview: the AutoJs6 Plugin Center recognizes the plugin and AutoJs6 can already use the camera core (opening the camera, live frames, photo capture, preview and the controls), but the capture screen and the `camera` script module are not delivered yet. The sections below describe the design goals of 1.0.0; see the roadmap for progress.

******

### Features

******

- Photos: returns an `ImageWrapper` turned upright by its EXIF orientation, or saves the original JPEG (with EXIF) to a path you choose; flash mode, quality and resolution are configurable.
- Live frames: RGBA frames at 640x480 by default and up to 1920x1080, turned upright to match the screen by default; when a script falls behind, frames are dropped and counted instead of piling up.
- Preview: hand a `SurfaceView` from a ui script or a floating window to the session to show the viewfinder, alongside live frames and photo capture.
- Controls: lens selection, resolution, target frame rate, torch, flash mode, zoom, exposure compensation and tap to focus, with readable ranges and current state.
- Capture screen: `camera.capture({ ui: true })` opens the plugin's own capture screen; the script gets the photo when the user presses the shutter, or `null` when the user cancels.
- Works in the background: a camera foreground service runs while a session is open, so frames keep coming in the background or with the screen off; the camera is released when the script exits or AutoJs6 is stopped.

******

### Usage

******

1. Download the APK that matches the device ABI from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases) and install it: most phones need `arm64-v8a`; if unsure, choose `universal`, which works on every device but is larger.
2. Open the plugin and grant the camera permission when asked; scripts can also call `camera.requestPermission()` to open the permission screen.
3. To use the `camera` module in scripts, update AutoJs6 to build 6641 (6.8.1) or later and enable this plugin in the Plugin Center.

> The plugin supports devices running Android 7.0 (API 24) or later. There is one package per ABI and a `universal` package for every device; the few native libraries that come with CameraX and Compose (about 50 KB per ABI) are aligned to 16 KB memory pages, so newer devices with 16 KB pages install it as well.

******

### Script API

******

Scripts use these features through the global module `camera` (also available as `$camera`; a 1.0.0 design draft, the released documentation is authoritative):

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

- `camera.capture(options)` opens, captures and closes in one step; `camera.open(options)` opens an exclusive session whose object offers `preview`, `analyze`, `capture`, `torch`, `zoom`, `exposure`, `focusAt`, `state` and `close`.
- Frame lifetime: the `img` of an `analyze` callback is valid only while the callback runs and is recycled as soon as it returns; to use it afterwards, call `img.clone()` and `recycle()` the copy yourself.
- `camera.list()` lists the lenses with their resolutions, zoom and exposure ranges; `camera.capabilities()` returns the contract version, the feature list and the permission state.
- One session at a time: a second `open` fails with `CAMERA_BUSY` and names the holder; the session closes when the script exits.

******

### FAQ

******

#### Why is the camera a plugin rather than part of AutoJs6?

The camera needs CameraX and the foreground service permissions. As a plugin, users who do not need the camera carry neither the size nor the permissions, and the plugin can be updated independently of AutoJs6.

#### Can scripts receive frames in the background?

Yes. While a session is open, the plugin runs a camera foreground service and the notification shows that the camera is in use. Some vendor systems restrict background cameras further; see the device matrix in the roadmap.

#### Does the plugin save photos to the gallery?

No. The plugin requests no storage permission and never writes to the system gallery; AutoJs6 writes a photo only to the `path` a script passes.

******

### Permissions and security

******

The plugin keeps its permission surface as small as possible:

- Protected entry points: the capability service and the activation entry require the AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`), so other apps cannot call them; the foreground service and the transparent start screen are not exported.
- Camera: only `CAMERA`, the camera foreground service permissions and, on Android 13 or later, the notification permission for the camera-in-use notification are requested; the camera is used only while a session is open and is released as soon as the session closes, the script exits or AutoJs6 is stopped.
- Network and storage: no network or storage permission; frames and photos pass only between the plugin and AutoJs6 and never reach the system gallery.
- Exclusive use: one session at a time (a script or the plugin's capture screen), and the error message names the holder.

******

### Plugin interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate capabilities:

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

### Roadmap

******

Plans, design decisions and progress are tracked as a checklist in ROADMAP.md. Unchecked items are plans, not features of the current version.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/ROADMAP.md)

******

### Release history

******

#### v1.0.0

_2026/11/05_

- `Hint` Development preview: AutoJs6 can already use the camera core (opening the camera, live frames, photo capture, preview and the controls); the capture screen and the camera script module are not delivered yet
- `Hint` Requires AutoJs6 6.8.1 (6641) or later (the exact minimum build will be filled in once the host changes land)
- `Hint` The current icon is a candidate design and may still change
- `Feature` Plugin skeleton: Wake activation protocol and Plugin Center identity service
- `Feature` Camera capability service: only one session can use the camera at a time and a busy camera names its holder; a foreground notification shows while the camera is in use, and when AutoJs6 exits the camera is released at once and the notification goes away
- `Feature` Live frames: RGBA frames take turns through 3 shared memory blocks (Android 8.1 or later) or 3 temporary files (Android 7.0 - 8.0) on their way to AutoJs6, at the chosen resolution and frame rate; frames that cannot be handled in time are skipped and counted
- `Feature` Photo capture: the JPEG reaches AutoJs6 as a read-only file together with its size, EXIF orientation, byte count and whether it is mirrored; quality, resolution and flash mode can be set
- `Feature` Preview: the picture is shown on a surface provided by AutoJs6 and follows screen rotation; the front lens preview is mirrored by default, frames and photos are not; when the surface showing the preview goes away, the preview stops on its own while frames and photos go on
- `Feature` Controls: torch, flash mode, zoom (ratio or linear), exposure compensation, tap to focus and meter; items the device does not support (such as a flash on the front lens) report UNSUPPORTED
- `Feature` Camera permission: opening the camera without it reports PERMISSION_DENIED and the permission can be requested; on Android 13 or later the notification permission is requested too, for the camera-in-use notification
- `Feature` One package per ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus a universal package for every device; the native libraries that come with CameraX and Compose are aligned to 16 KB memory pages
- `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
- `Dependency` Attach Jetpack CameraX 1.6.2 (opens the camera, takes photos, delivers frames and the viewfinder)
- `Dependency` Attach Jetpack Compose BOM 2026.09.00 (plugin screens)
- `Dependency` Attach AndroidX Activity Compose 1.12.2
- `Dependency` Attach AndroidX Core KTX 1.16.0

##### For more release history, refer to

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build

******

This section targets developers who want to build the plugin from source.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### License

******

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE). See the third party notices for the licenses of third party components.

- [Third-party notices (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/THIRD_PARTY_NOTICES.md)

******

### Related links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
