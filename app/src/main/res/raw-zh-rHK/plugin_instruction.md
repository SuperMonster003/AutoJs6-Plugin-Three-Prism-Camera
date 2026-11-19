3-Prism Camera 是 AutoJs6 的相機插件, 讓腳本可以拍照, 連續取幀並顯示取景預覽. 取得的幀可以直接交給 `ocr`, `barcode` 或 YOLO 等識別模組, 實現即時掃碼, 文字識別與目標檢測.

### 開始使用

1. 從啓動器開啟 3-Prism Camera, 在取景頁點按 "授予權限", 允許插件使用相機.
2. 點按 "拍照" 試拍一張. 相片只在 "相片預覽" 中顯示, 不會儲存.
3. 在 AutoJs6 的 "插件中心" 啟用本插件.
4. 在腳本中調用 `camera` 模組拍照或取幀, 範例見下文.

### 主要功能

- **取景頁**: 從啓動器開啟插件即進入取景頁, 可以 "切換鏡頭", 開關手電筒, 用滑桿或雙指捏合變焦, 點按畫面對焦.
- **拍照**: 腳本取得按方向轉正的圖像, 也可以把原始 JPEG 儲存到指定路徑; `camera.capture({ ui: true })` 改由使用者在取景頁拍照.
- **連續取幀**: 幀預設為 640x480, 最高 1920x1080, 並按螢幕方向轉正; 來不及處理的幀會被略過並計數, 不會排隊積壓.
- **預覽**: 把 ui 介面或懸浮窗中的 `SurfaceView` 交給會話即可顯示取景畫面, 可以與取幀和拍照同時進行.
- **相機控制**: 腳本可以控制手電筒, 閃光模式, 變焦, 曝光補償與點按對焦, 並讀取各項的範圍與目前的值.
- **相機獨佔**: 同一時間只有一個會話使用相機. 腳本佔用相機時, 取景頁顯示是哪個腳本, 點按 "停止該腳本" 即可接管相機.
- **背景取幀**: 會話開啟期間顯示 "相機使用中" 通知, AutoJs6 退到背景或熄屏後仍可取幀; 腳本結束後相機立即釋放.
- **預設值**: 在取景頁右上角的 "設定" 中可以設定 "預設鏡頭", "預設分析解像度" 與 "預設分析幀率"; 腳本指定的值總是優先.
- **診斷與更新**: 設定頁的 "診斷" 部分列出各鏡頭的能力與 "目前持有者"; "檢查更新" 在官方插件索引中查找新版本.
- **外觀**: "語言", "夜間模式" 與 "主題色" 預設跟隨 AutoJs6, 亦可以單獨設定; 還可以切換 "啓動器圖標".

### 在腳本中使用

在 AutoJs6 的 Rhino 腳本中透過全域模組 `camera` 使用相機. 下例拍一張相片, 並把 JPEG 儲存為內部儲存空間根目錄下的 prism.jpg:

```js
let img = camera.capture({ path: files.join(files.getSdcardPath(), 'prism.jpg') });
toastLog(img.getWidth() + 'x' + img.getHeight());
img.recycle();
```

下例以 1280x720 連續取幀並識別條碼, 識別到後關閉會話. 條碼識別需要在 "插件中心" 啟用 ML Kit Barcode 插件:

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

- `analyze` 回調中的幀只在回調執行期間有效; 要在回調之外使用, 先調用 `img.clone()` 取得副本, 用完後調用 `recycle()`.
- `camera.capture` 不能在 UI 線程上調用, ui 腳本的介面回調中請改用 `camera.captureAsync`.
- Node.js 腳本透過 `require('autojs6:camera')` 使用同一組方法, 傳回值為 Promise; Node.js 與 Bun 中不能顯示預覽.
- 完整用法與錯誤碼見 [AutoJs6 文件 > 相機 (Camera)](https://docs.autojs6.com/#/camera).

### 運行要求

- Android 7.0 或更高版本. 取景頁與設定頁不需要 AutoJs6.
- `camera` 模組需要 AutoJs6 6.8.1 或更高版本, 並在 "插件中心" 啟用本插件.
- 相機權限由插件自己持有, AutoJs6 的相機權限不能代替. 可以在設定頁的 "相機權限" 一行授予, 腳本亦可以調用 `camera.requestPermission()`.
- 通知權限 (Android 13 及以上) 用於顯示 "相機使用中" 通知; 網絡權限只用於檢查更新, 相機畫面與相片不會離開裝置.

### 常見問題

**開啟相機時報告 `CAMERA_DISABLED` 的原因**

Android 11 及以上, AutoJs6 在背景或手機停在鎖屏時, 系統不允許開啟相機. 讓 AutoJs6 回到前景後再開啟即可; 在前景開啟的會話退到背景或熄屏後照常取幀.

**相機被佔用 (`CAMERA_BUSY`) 時的處理**

同一時間只有一個會話能使用相機, 錯誤的 `details.holder` 指出佔用者: 腳本的路徑, 或表示取景頁的 `pluginUi`. 關閉佔用相機的會話或取景頁後再開啟; 亦可以在取景頁上點按 "停止該腳本".

**相片的儲存位置**

插件不儲存相片, 也不寫入系統相簿. 只有腳本傳入 `path` 時, AutoJs6 才把 JPEG 寫到該路徑.

各廠商系統的相容情況與權限說明見 [項目主頁](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
