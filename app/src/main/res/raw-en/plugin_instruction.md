3-Prism Camera is the camera plugin of AutoJs6: scripts can take photos, receive live frames and show a viewfinder preview. Frames can go straight to recognition modules such as `ocr`, `barcode` or YOLO for live code scanning, text recognition and object detection.

### Getting started

1. Open 3-Prism Camera from the launcher and tap "Grant permission" on the viewfinder to let the plugin use the camera.
2. Tap "Take photo" for a test shot. The photo is only shown in "Photo preview" and is not saved.
3. Enable this plugin in the "Plugin center" of AutoJs6.
4. Call the `camera` module in a script to take photos or receive frames, as in the examples below.

### Main features

- **Viewfinder**: opening the plugin from the launcher shows the viewfinder, where you can "Switch camera", turn the flashlight on or off, zoom with the slider or a pinch, and tap the picture to focus.
- **Photos**: scripts get an image turned upright and can also save the original JPEG to a path; `camera.capture({ ui: true })` lets the user take the photo on the viewfinder instead.
- **Live frames**: frames are 640x480 by default and up to 1920x1080, turned upright to match the screen; frames a script cannot keep up with are skipped and counted instead of piling up.
- **Preview**: hand a `SurfaceView` from a ui layout or a floating window to the session to show the viewfinder picture, alongside live frames and photos.
- **Camera controls**: scripts can control the flashlight, flash mode, zoom, exposure compensation and tap to focus, and read the range and current value of each.
- **One session at a time**: only one session uses the camera at a time. When a script holds the camera, the viewfinder shows which script it is, and "Stop this script" takes the camera over.
- **Frames in the background**: a "Camera in use" notification is shown while a session is open, and frames keep coming when AutoJs6 is in the background or the screen is off; the camera is released as soon as the script exits.
- **Defaults**: in "Settings" at the top right of the viewfinder, set the "Default camera", "Default analysis size" and "Default analysis frame rate"; values passed by a script always take precedence.
- **Diagnostics and updates**: the "Diagnostics" section of the settings lists what each camera supports and the "Current holder"; "Check for updates" looks for new versions in the official plugin index.
- **Appearance**: "Language", "Night mode" and "Theme color" follow AutoJs6 by default and can also be set separately; the "Launcher icon" can be switched as well.

### Using it in scripts

Rhino scripts in AutoJs6 use the camera through the global module `camera`. The example below takes a photo and saves the JPEG as prism.jpg in the root of the internal storage:

```js
let img = camera.capture({ path: files.join(files.getSdcardPath(), 'prism.jpg') });
toastLog(img.getWidth() + 'x' + img.getHeight());
img.recycle();
```

The example below receives 1280x720 frames and recognizes barcodes, closing the session once one is read. Barcode recognition needs the ML Kit Barcode plugin enabled in the "Plugin center":

```js
let session = camera.open({ resolution: '1280x720' });
session.analyze((img) => {
    let text = barcode.recognizeText(img);
    if (text !== null) {
        toastLog(text);
        session.close();
    }
});
events.on('exit', () => session.close());
```

- A frame in the `analyze` callback is only valid while the callback runs; to use it afterwards, call `img.clone()` for a copy and call `recycle()` on it when done.
- `camera.capture` cannot be called on the UI thread; use `camera.captureAsync` in the interface callbacks of ui scripts.
- Node.js scripts use the same methods through `require('autojs6:camera')`, returning Promises; Node.js and Bun cannot show a preview.
- See [AutoJs6 documentation > Camera](https://docs.autojs6.com/#/camera) for the full usage and the error codes.

### Requirements

- Android 7.0 or later. The viewfinder and the settings do not need AutoJs6.
- The `camera` module needs AutoJs6 6.8.1 or later, with this plugin enabled in the "Plugin center".
- The camera permission is held by the plugin itself; the camera permission of AutoJs6 does not replace it. Grant it in the "Camera permission" row of the settings, or call `camera.requestPermission()` from a script.
- The notification permission (Android 13 and later) shows the "Camera in use" notification; the network permission is only used to check for updates, and camera pictures and photos never leave the device.

### FAQ

**Why opening the camera reports `CAMERA_DISABLED`**

On Android 11 and later, the system does not allow the camera while AutoJs6 is in the background or the phone is on the lock screen. Bring AutoJs6 to the foreground and open the camera again; a session opened in the foreground keeps receiving frames in the background or with the screen off.

**When the camera is busy (`CAMERA_BUSY`)**

Only one session can use the camera at a time, and `details.holder` of the error names the holder: the path of a script, or `pluginUi` for the viewfinder. Close that session or the viewfinder and open the camera again; on the viewfinder you can also tap "Stop this script".

**Where photos are saved**

The plugin does not save photos or write to the system gallery. AutoJs6 writes the JPEG only when a script passes `path`.

Compatibility with vendor systems and the permissions are described on the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
