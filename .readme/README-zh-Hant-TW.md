<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-prism-camera-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>透過 CameraX 拍照, 連續擷取影格與取景預覽, 影格可直接交給 OCR, 條碼與 YOLO 等辨識模組</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言 (Languages)

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ar.md)

******

### 簡介

******

3-Prism Camera 是 AutoJs6 的相機外掛, 基於 Jetpack CameraX 為指令碼提供拍照, 連續影格分析與取景預覽. 連續影格以 AutoJs6 的 `ImageWrapper` 交給指令碼, 指令碼可以直接把它交給 `ocr`, `barcode` 或 `yolo` 等現有模組, 實現即時掃碼, 文字辨識與物件偵測.

外掛持有相機並透過 Binder 與 AutoJs6 通訊: 連續影格經共用記憶體環狀緩衝傳給 AutoJs6 (Android 7.0 - 8.0 改用暫存檔), 相片以 JPEG 交給 AutoJs6 解碼或原樣儲存, 預覽直接算繪到指令碼介面中的 `SurfaceView`, 像素不經過 Binder. 工作階段開啟期間外掛執行相機前景服務, 指令碼在背景或螢幕關閉時也能繼續擷取影格.

> 目前為開發預覽版: 外掛可以被 AutoJs6 外掛中心識別, 但拍照, 連續影格, 預覽, 取景頁與 `camera` 指令碼模組尚未交付. 下文描述的是 1.0.0 的設計目標, 進度見開發路線圖.

******

### 功能亮點

******

- 拍照: 傳回依 EXIF 方向轉正的 `ImageWrapper`, 也可以把原始 JPEG (含 EXIF) 儲存到指定路徑; 可設定閃光模式, 畫質與解析度.
- 連續影格: RGBA 影格預設 640x480, 最高 1920x1080, 預設依螢幕方向轉正; 處理不及時自動捨棄影格並計數, 不會排隊積壓.
- 預覽: 把 ui 指令碼或懸浮視窗中的 `SurfaceView` 交給工作階段即可顯示取景畫面, 可與連續影格和拍照同時進行.
- 控制: 鏡頭選擇, 解析度, 目標影格率, 手電筒, 閃光模式, 變焦, 曝光補償與輕觸對焦, 並可讀取各項範圍與目前狀態.
- 取景頁: `camera.capture({ ui: true })` 開啟外掛內建的取景頁, 使用者按下快門後指令碼取得相片, 取消時傳回 `null`.
- 背景可用: 工作階段開啟期間執行相機前景服務, 指令碼切到背景或螢幕關閉後仍可擷取影格; 指令碼結束或 AutoJs6 被終止時自動釋放相機.

******

### 使用方法

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases) 下載與裝置 ABI 對應的 APK 並安裝: 大多數手機選擇 `arm64-v8a`, 不確定時選擇適用於所有裝置但檔案較大的 `universal`.
2. 開啟外掛, 依提示授予相機權限; 指令碼也可以呼叫 `camera.requestPermission()` 開啟授權頁.
3. 如要在指令碼中使用 `camera` 模組, 請將 AutoJs6 升級到內部版本號 6641 (6.8.1) 或更高版本, 並在外掛中心啟用本外掛.

> 外掛支援 Android 7.0 (API 24) 以上的裝置. 每種 ABI 各有一個安裝檔, 另有適用於所有裝置的 `universal` 安裝檔; CameraX 與 Compose 內建的少量原生程式庫 (每種 ABI 約 50 KB) 已按 16 KB 記憶體分頁對齊, 採用 16 KB 記憶體分頁的新裝置也能正常安裝.

******

### 腳本 API

******

指令碼透過全域模組 `camera` (也可寫作 `$camera`) 使用這些能力 (1.0.0 設計草案, 以發布版本的文件為準):

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

- `camera.capture(options)` 一步完成開啟, 拍照與關閉; `camera.open(options)` 開啟獨占工作階段, 工作階段物件提供 `preview`, `analyze`, `capture`, `torch`, `zoom`, `exposure`, `focusAt`, `state` 與 `close`.
- 影格的生命週期: `analyze` 回呼中的 `img` 只在回呼執行期間有效, 回呼傳回後立即回收; 如要在回呼之外使用, 請呼叫 `img.clone()`, 並在用完後自行 `recycle()`.
- `camera.list()` 列出鏡頭及其解析度, 變焦與曝光範圍; `camera.capabilities()` 傳回契約版本, 功能清單與授權狀態.
- 同一時間只有一個工作階段: 再次 `open` 會報 `CAMERA_BUSY` 並說明占用者; 指令碼結束時工作階段自動關閉.

******

### 常見問題

******

#### 為什麼相機做成外掛, 而不是內建在 AutoJs6 中?

相機依賴 CameraX 與前景服務權限. 做成外掛後, 不需要相機的使用者不必承擔這部分體積與權限, 外掛也可以獨立於 AutoJs6 更新.

#### 指令碼在背景還能擷取影格嗎?

能. 工作階段開啟期間外掛執行相機前景服務, 通知欄會顯示相機正在使用. 部分廠商系統會額外限制背景相機, 相容情況見開發路線圖中的裝置矩陣.

#### 外掛會把相片存進相簿嗎?

不會. 外掛不申請儲存權限, 也不寫入系統相簿; 只有指令碼傳入 `path` 時, AutoJs6 才把相片寫到該路徑.

******

### 權限與安全

******

外掛在設計上盡量縮小權限範圍:

- 受保護的入口: 能力服務與啟動入口受 AutoJs6 外掛權限 (`org.autojs.permission.PLUGIN`) 保護, 其他應用程式無法直接呼叫; 前景服務與透明啟動頁不匯出.
- 相機: 只申請 `CAMERA` 與相機類前景服務權限; 相機只在工作階段開啟期間使用, 工作階段關閉, 指令碼結束或 AutoJs6 被終止時立即釋放.
- 網路與儲存: 不申請網路與儲存權限, 影格與相片只在外掛和 AutoJs6 之間傳遞, 不寫入系統相簿.
- 獨占: 同一時間只有一個工作階段 (指令碼或外掛取景頁), 占用者會寫在錯誤訊息中.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者, 主程式透過這些識別碼發現外掛並完成能力協商:

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

### 開發路線圖

******

功能規劃, 設計決策與完成情況以可勾選清單維護在 ROADMAP.md 中. 未勾選的項目表示規劃意向, 不代表目前版本的能力.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/11/05_

- `提示` 開發預覽: 外掛可以被 AutoJs6 外掛中心辨識, 拍照, 連續擷取影格, 取景預覽, 拍攝頁與 camera 指令碼模組尚未交付
- `提示` 需要 AutoJs6 6.8.1 (6641) 或更高版本 (確切的最低版本號待宿主端變更完成後回填)
- `提示` 目前圖示為候選圖案, 之後可能更換
- `新增` 外掛骨架: Wake 啟用協定, 外掛中心身分服務與相機能力服務 (預留)
- `新增` 只有一個適用於所有裝置的 APK, CameraX 與 Compose 內建的原生程式庫已按 16 KB 記憶體分頁對齊
- `新增` 10 種語言的 README, 外掛中心說明與更新日誌, 由 JSON 來源檔案統一產生
- `相依性` 附加 Jetpack CameraX 版本 1.6.2 (開啟相機, 拍照, 擷取影格與取景)
- `相依性` 附加 Jetpack Compose BOM 版本 2026.09.00 (外掛介面)
- `相依性` 附加 AndroidX Activity Compose 版本 1.12.2
- `相依性` 附加 AndroidX Core KTX 版本 1.16.0

##### 更多發行歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置

******

本節面向希望從原始碼建置外掛的開發者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### 授權

******

專案程式碼使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE). 第三方元件的授權見第三方聲明.

- [第三方聲明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
