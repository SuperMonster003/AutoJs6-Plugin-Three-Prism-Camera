******

### Release history

******

# v1.0.0

###### 2026/11/05

* `Hint` Script API: [AutoJs6 documentation > Camera](https://docs.autojs6.com/#/camera)
* `Hint` Requires AutoJs6 6.8.1 (6641) or later (the exact minimum build will be filled in once the host changes land)
* `Hint` The current icon is a candidate design and may still change
* `Feature` Plugin skeleton: Wake activation protocol and Plugin Center identity service
* `Feature` Camera capability service: only one session can use the camera at a time and a busy camera names its holder; a foreground notification shows while the camera is in use, and when AutoJs6 exits the camera is released at once and the notification goes away
* `Feature` When AutoJs6 is in the background or the phone sits at the lock screen (Android 11 and later), the system does not allow camera use: opening the camera then reports CAMERA_DISABLED (depending on the system, the open fails at once, or the session reports it first and then closes), and opening again once AutoJs6 is back in the foreground works normally; a session opened in the foreground keeps delivering frames after AutoJs6 goes to the background or the screen turns off
* `Feature` Live frames: RGBA frames take turns through 3 shared memory blocks (Android 8.1 or later) or 3 temporary files (Android 7.0 - 8.0) on their way to AutoJs6, at the chosen resolution and frame rate; frames that cannot be handled in time are skipped and counted
* `Feature` Photo capture: the JPEG reaches AutoJs6 as a read-only file together with its size, EXIF orientation, byte count and whether it is mirrored; quality, resolution and flash mode can be set; a mirrored photo only has its EXIF orientation rewritten, without re-encoding; where a camera does not write the photo orientation (for example the Android 7.0 emulator), the plugin writes the 90 / 270 degree orientation so that the photo shows as the phone was held (a missing 180 degrees cannot be detected)
* `Feature` Preview: the picture is shown on a surface provided by AutoJs6 and follows screen rotation; the front lens preview is mirrored by default, frames and photos are not; when the surface showing the preview goes away, the preview stops on its own while frames and photos go on
* `Feature` Controls: torch, flash mode, zoom (ratio or linear), exposure compensation, tap to focus and meter; items the device does not support (such as a flash on the front lens) report UNSUPPORTED
* `Feature` Camera permission: opening the camera without it reports PERMISSION_DENIED and the permission can be requested; on Android 13 or later the notification permission is requested too, for the camera-in-use notification
* `Feature` Viewfinder: opened from the launcher, it offers lens switching, the flashlight, zoom by slider or pinch and tap to focus; a photo taken there only shows the picture and its resolution and is not saved; the front lens picture is mirrored on screen, the photo is not
* `Feature` camera.capture({ ui: true }) in a script opens the same viewfinder: the photo goes straight to the script, and cancelling returns null; if the viewfinder cannot open because AutoJs6 is in the background, the call reports UI_REQUIRES_FOREGROUND after 10 seconds instead of waiting forever
* `Feature` When a script is using the camera, the viewfinder names the script and can stop it to take over the camera
* `Feature` Settings (top right of the viewfinder): appearance (language, night mode, theme color, launcher icon), camera and notification permissions, the default camera, the default analysis size and frame rate, the foreground service notification, diagnostics (CameraX version, the capabilities of each lens, who is using the camera) and the version history
* `Feature` When a script names no camera, resolution or frame rate, the defaults of the settings page apply (initially the back camera, 640x480 and the device's own frame rate); a value the script sets always wins
* `Feature` Update check: see a newer release listed in the official plugin index and its notes from the settings page, and ignore a release if you like; an automatic check runs when the settings page opens (at most once every 12 hours, can be turned off); the network permission serves only this check, and the plugin never uploads camera frames or photos
* `Feature` One package per ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus a universal package for every device; the native libraries that come with CameraX and Compose are aligned to 16 KB memory pages
* `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
* `Dependency` Attach Jetpack CameraX 1.6.2 (opens the camera, takes photos, delivers frames and the viewfinder)
* `Dependency` Attach Jetpack Compose BOM 2026.09.00 (plugin screens)
* `Dependency` Attach AndroidX Activity Compose 1.12.2
* `Dependency` Attach AndroidX Core KTX 1.16.0
* `Dependency` Attach AndroidX ExifInterface 1.4.2 (writes the EXIF orientation of photos)
* `Dependency` Attach Material Color Utilities (MaterialKolor) 4.1.1 (theme color of the plugin screens)
