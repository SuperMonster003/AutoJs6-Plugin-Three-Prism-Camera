3-Prism Camera 是 AutoJs6 的相機外掛, 讓指令碼可以拍照, 連續擷取影格並顯示取景預覽. 擷取的影格可以直接交給 `ocr`, `barcode` 或 YOLO 等辨識模組, 實現即時掃碼, 文字辨識與物件偵測.

### 開始使用

1. 從啟動器開啟 3-Prism Camera, 在取景頁點按 "授予權限", 允許外掛使用相機.
2. 點按 "拍照" 試拍一張. 相片只在 "相片預覽" 中顯示, 不會儲存.
3. 在 AutoJs6 的 "外掛中心" 啟用本外掛.
4. 在指令碼中呼叫 `camera` 模組拍照或擷取影格, 範例見下文.

### 主要功能

- **取景頁**: 從啟動器開啟外掛即進入取景頁, 可以 "切換鏡頭", 開關手電筒, 用滑桿或雙指縮放變焦, 點按畫面對焦.
- **拍照**: 指令碼取得依方向轉正的影像, 也可以把原始 JPEG 儲存到指定路徑; `camera.capture({ ui: true })` 改由使用者在取景頁拍照.
- **連續影格**: 影格預設為 640x480, 最高 1920x1080, 並依螢幕方向轉正; 來不及處理的影格會被略過並計數, 不會排隊積壓.
- **預覽**: 把 ui 介面或懸浮視窗中的 `SurfaceView` 交給工作階段即可顯示取景畫面, 可以與擷取影格和拍照同時進行.
- **相機控制**: 指令碼可以控制手電筒, 閃光模式, 變焦, 曝光補償與點按對焦, 並讀取各項的範圍與目前的值.
- **相機獨占**: 同一時間只有一個工作階段使用相機. 指令碼占用相機時, 取景頁顯示是哪個指令碼, 點按 "停止該指令碼" 即可接管相機.
- **背景擷取影格**: 工作階段開啟期間顯示 "相機使用中" 通知, AutoJs6 退到背景或螢幕關閉後仍可擷取影格; 指令碼結束後相機立即釋放.
- **預設值**: 在取景頁右上角的 "設定" 中可以設定 "預設鏡頭", "預設分析解析度" 與 "預設分析影格速率"; 指令碼指定的值總是優先.
- **診斷與更新**: 設定頁的 "診斷" 區段列出各鏡頭的能力與 "目前持有者"; "檢查更新" 在官方外掛索引中尋找新版本.
- **外觀**: "語言", "夜間模式" 與 "主題色" 預設跟隨 AutoJs6, 也可以單獨設定; 還可以切換 "啟動器圖示".

### 在指令碼中使用

在 AutoJs6 的 Rhino 指令碼中透過全域模組 `camera` 使用相機. 下例拍一張相片, 並把 JPEG 儲存為內部儲存空間根目錄下的 prism.jpg:

```js
let img = camera.capture({ path: files.join(files.getSdcardPath(), 'prism.jpg') });
toastLog(img.getWidth() + 'x' + img.getHeight());
img.recycle();
```

下例以 1280x720 連續擷取影格並辨識條碼, 辨識到後關閉工作階段. 條碼辨識需要在 "外掛中心" 啟用 ML Kit Barcode 外掛:

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

- `analyze` 回呼中的影格只在回呼執行期間有效; 要在回呼之外使用, 先呼叫 `img.clone()` 取得副本, 用完後呼叫 `recycle()`.
- `camera.capture` 不能在 UI 執行緒上呼叫, ui 指令碼的介面回呼中請改用 `camera.captureAsync`.
- Node.js 指令碼透過 `require('autojs6:camera')` 使用同一組方法, 傳回值為 Promise; Node.js 與 Bun 中不能顯示預覽.
- 完整用法與錯誤碼見 [AutoJs6 文件 > 相機 (Camera)](https://docs.autojs6.com/#/camera).

### 執行需求

- Android 7.0 或更高版本. 取景頁與設定頁不需要 AutoJs6.
- `camera` 模組需要 AutoJs6 6.8.1 或更高版本, 並在 "外掛中心" 啟用本外掛.
- 相機權限由外掛自己持有, AutoJs6 的相機權限不能代替. 可以在設定頁的 "相機權限" 一列授予, 指令碼也可以呼叫 `camera.requestPermission()`.
- 通知權限 (Android 13 以上) 用於顯示 "相機使用中" 通知; 網路權限只用於檢查更新, 相機畫面與相片不會離開裝置.

### 常見問題

**開啟相機時回報 `CAMERA_DISABLED` 的原因**

Android 11 以上, AutoJs6 在背景或手機停在鎖定畫面時, 系統不允許開啟相機. 讓 AutoJs6 回到前景後再開啟即可; 在前景開啟的工作階段退到背景或螢幕關閉後照常擷取影格.

**相機被占用 (`CAMERA_BUSY`) 時的處理**

同一時間只有一個工作階段能使用相機, 錯誤的 `details.holder` 指出占用者: 指令碼的路徑, 或表示取景頁的 `pluginUi`. 關閉占用相機的工作階段或取景頁後再開啟; 也可以在取景頁上點按 "停止該指令碼".

**相片的儲存位置**

外掛不儲存相片, 也不寫入系統相簿. 只有指令碼傳入 `path` 時, AutoJs6 才把 JPEG 寫到該路徑.

各廠商系統的相容情況與權限說明見 [專案首頁](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
