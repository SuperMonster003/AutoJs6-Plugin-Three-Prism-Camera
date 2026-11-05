******

### Release history

******

# v1.0.0

###### 2026/11/05

* `Hint` Development preview: AutoJs6 can already use the camera core (opening the camera, live frames, photo capture, preview and the controls); the capture screen and the camera script module are not delivered yet
* `Hint` Requires AutoJs6 6.8.1 (6641) or later (the exact minimum build will be filled in once the host changes land)
* `Hint` The current icon is a candidate design and may still change
* `Feature` Plugin skeleton: Wake activation protocol and Plugin Center identity service
* `Feature` Camera capability service: only one session can use the camera at a time and a busy camera names its holder; a foreground notification shows while the camera is in use, and when AutoJs6 exits the camera is released at once and the notification goes away
* `Feature` Live frames: RGBA frames take turns through 3 shared memory blocks (Android 8.1 or later) or 3 temporary files (Android 7.0 - 8.0) on their way to AutoJs6, at the chosen resolution and frame rate; frames that cannot be handled in time are skipped and counted
* `Feature` Photo capture: the JPEG reaches AutoJs6 as a read-only file together with its size, EXIF orientation, byte count and whether it is mirrored; quality, resolution and flash mode can be set
* `Feature` Preview: the picture is shown on a surface provided by AutoJs6 and follows screen rotation; the front lens preview is mirrored by default, frames and photos are not; when the surface showing the preview goes away, the preview stops on its own while frames and photos go on
* `Feature` Controls: torch, flash mode, zoom (ratio or linear), exposure compensation, tap to focus and meter; items the device does not support (such as a flash on the front lens) report UNSUPPORTED
* `Feature` Camera permission: opening the camera without it reports PERMISSION_DENIED and the permission can be requested; on Android 13 or later the notification permission is requested too, for the camera-in-use notification
* `Feature` One package per ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus a universal package for every device; the native libraries that come with CameraX and Compose are aligned to 16 KB memory pages
* `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
* `Dependency` Attach Jetpack CameraX 1.6.2 (opens the camera, takes photos, delivers frames and the viewfinder)
* `Dependency` Attach Jetpack Compose BOM 2026.09.00 (plugin screens)
* `Dependency` Attach AndroidX Activity Compose 1.12.2
* `Dependency` Attach AndroidX Core KTX 1.16.0
