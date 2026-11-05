******

### Release history

******

# v1.0.0

###### 2026/11/05

* `Hint` Development preview: the AutoJs6 Plugin Center can discover the plugin; photo capture, live frames, the viewfinder, the capture screen and the camera script module are not delivered yet
* `Hint` Requires AutoJs6 6.8.1 (6641) or later (the exact minimum build will be filled in once the host changes land)
* `Hint` The current icon is a candidate design and may still change
* `Feature` Plugin skeleton: Wake activation protocol, Plugin Center identity service and camera capability service (placeholder)
* `Feature` A single APK for every device; the native libraries that come with CameraX and Compose are aligned to 16 KB memory pages
* `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
* `Dependency` Attach Jetpack CameraX 1.6.2 (opens the camera, takes photos, delivers frames and the viewfinder)
* `Dependency` Attach Jetpack Compose BOM 2026.09.00 (plugin screens)
* `Dependency` Attach AndroidX Activity Compose 1.12.2
* `Dependency` Attach AndroidX Core KTX 1.16.0
