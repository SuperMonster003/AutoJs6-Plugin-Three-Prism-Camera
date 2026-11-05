3-Prism Camera is the camera plugin of AutoJs6. Built on Jetpack CameraX, it gives scripts photo capture, continuous frame analysis and viewfinder preview. Live frames reach the script as AutoJs6 `ImageWrapper` objects, so a script can hand them straight to existing modules such as `ocr`, `barcode` or `yolo` for live barcode scanning, text recognition and object detection.

> This is a development preview: the AutoJs6 Plugin Center recognizes the plugin and AutoJs6 can already use the camera core (opening the camera, live frames, photo capture, preview and the controls), but the capture screen and the `camera` script module are not delivered yet. The sections below describe the design goals of 1.0.0; see the roadmap for progress.

### Features

- Photos: returns an `ImageWrapper` turned upright by its EXIF orientation, or saves the original JPEG (with EXIF) to a path you choose; flash mode, quality and resolution are configurable.
- Live frames: RGBA frames at 640x480 by default and up to 1920x1080, turned upright to match the screen by default; when a script falls behind, frames are dropped and counted instead of piling up.
- Preview: hand a `SurfaceView` from a ui script or a floating window to the session to show the viewfinder, alongside live frames and photo capture.
- Controls: lens selection, resolution, target frame rate, torch, flash mode, zoom, exposure compensation and tap to focus, with readable ranges and current state.
- Capture screen: `camera.capture({ ui: true })` opens the plugin's own capture screen; the script gets the photo when the user presses the shutter, or `null` when the user cancels.
- Works in the background: a camera foreground service runs while a session is open, so frames keep coming in the background or with the screen off; the camera is released when the script exits or AutoJs6 is stopped.

Progress and design are available on the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
