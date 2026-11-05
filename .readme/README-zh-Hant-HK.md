<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-prism-camera-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>透過 CameraX 拍照, 連續取幀與取景預覽, 幀可直接交給 OCR, 條碼與 YOLO 等識別模組</p>

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
- 繁體中文 (香港) [zh-Hant-HK] # 目前
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-TW.md)
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

3-Prism Camera 是 AutoJs6 的相機插件, 基於 Jetpack CameraX 為腳本提供拍照, 連續幀分析與取景預覽. 連續幀以 AutoJs6 的 `ImageWrapper` 交給腳本, 腳本可以直接把它交給 `ocr`, `barcode` 或 `yolo` 等現有模組, 實現即時掃碼, 文字識別與目標偵測.

插件持有相機並透過 Binder 與 AutoJs6 通訊: 連續幀經共享記憶體環形緩衝傳給 AutoJs6 (Android 7.0 - 8.0 改用臨時檔案), 相片以 JPEG 交給 AutoJs6 解碼或原樣儲存, 預覽直接渲染到腳本介面中的 `SurfaceView`, 像素不經過 Binder. 會話開啟期間插件執行相機前景服務, 腳本在背景或熄屏時也能繼續取幀.

> 目前為開發預覽版: 插件可以被 AutoJs6 插件中心識別, 但拍照, 連續幀, 預覽, 取景頁與 `camera` 腳本模組尚未交付. 下文描述的是 1.0.0 的設計目標, 進度見開發路線圖.

******

### 功能亮點

******

- 拍照: 傳回按 EXIF 方向轉正的 `ImageWrapper`, 也可以把原始 JPEG (含 EXIF) 儲存到指定路徑; 可設定閃光模式, 畫質與解像度.
- 連續幀: RGBA 幀預設 640x480, 最高 1920x1080, 預設按螢幕方向轉正; 處理不及時自動丟幀並計數, 不會排隊積壓.
- 預覽: 把 ui 腳本或懸浮窗中的 `SurfaceView` 交給會話即可顯示取景畫面, 可與連續幀和拍照同時進行.
- 控制: 鏡頭選擇, 解像度, 目標幀率, 電筒, 閃光模式, 變焦, 曝光補償與點按對焦, 並可讀取各項範圍與目前狀態.
- 取景頁: `camera.capture({ ui: true })` 開啟插件內建的取景頁, 使用者按下快門後腳本取得相片, 取消時傳回 `null`.
- 背景可用: 會話開啟期間執行相機前景服務, 腳本切到背景或熄屏後仍可取幀; 腳本結束或 AutoJs6 被終止時自動釋放相機.

******

### 使用方法

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases) 下載 APK 並安裝. 安裝包只有一個, 適用於全部裝置.
2. 開啟插件, 按提示授予相機權限; 腳本也可以呼叫 `camera.requestPermission()` 開啟授權頁.
3. 如要在腳本中使用 `camera` 模組, 請將 AutoJs6 升級到內部版本號 6640 (6.8.1) 或更高版本, 並在插件中心啟用本插件.

> 插件支援 Android 7.0 (API 24) 或以上的裝置. 只有一個適用於全部裝置的安裝包; CameraX 與 Compose 自帶的少量原生庫 (共約 200 KB) 已按 16 KB 記憶體分頁對齊, 採用 16 KB 記憶體分頁的新裝置亦能正常安裝.

******

### 腳本 API

******

腳本透過全域模組 `camera` (亦可寫作 `$camera`) 使用這些能力 (1.0.0 設計草案, 以發佈版本的文件為準):

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

- `camera.capture(options)` 一步完成開啟, 拍照與關閉; `camera.open(options)` 開啟獨佔會話, 會話物件提供 `preview`, `analyze`, `capture`, `torch`, `zoom`, `exposure`, `focusAt`, `state` 與 `close`.
- 幀的生命週期: `analyze` 回呼中的 `img` 只在回呼執行期間有效, 回呼傳回後立即回收; 如要在回呼之外使用, 請呼叫 `img.clone()`, 並在用完後自行 `recycle()`.
- `camera.list()` 列出鏡頭及其解像度, 變焦與曝光範圍; `camera.capabilities()` 傳回契約版本, 功能清單與授權狀態.
- 同一時間只有一個會話: 再次 `open` 會報 `CAMERA_BUSY` 並說明佔用者; 腳本結束時會話自動關閉.

******

### 常見問題

******

#### 為甚麼相機做成插件, 而不是內建在 AutoJs6 中?

相機依賴 CameraX 與前景服務權限. 做成插件後, 不需要相機的使用者不必承擔這部分體積與權限, 插件也可以獨立於 AutoJs6 更新.

#### 腳本在背景還能取幀嗎?

能. 會話開啟期間插件執行相機前景服務, 通知欄會顯示相機正在使用. 部分廠商系統會額外限制背景相機, 相容情況見開發路線圖中的裝置矩陣.

#### 插件會把相片存進相簿嗎?

不會. 插件不申請儲存權限, 也不寫入系統相簿; 只有腳本傳入 `path` 時, AutoJs6 才把相片寫到該路徑.

******

### 權限與安全

******

插件在設計上盡量收窄權限範圍:

- 受保護的入口: 能力服務與啟動入口受 AutoJs6 插件權限 (`org.autojs.permission.PLUGIN`) 保護, 其他應用程式無法直接呼叫; 前景服務與透明啟動頁不匯出.
- 相機: 只申請 `CAMERA` 與相機類前景服務權限; 相機只在會話開啟期間使用, 會話關閉, 腳本結束或 AutoJs6 被終止時立即釋放.
- 網絡與儲存: 不申請網絡與儲存權限, 幀與相片只在插件和 AutoJs6 之間傳遞, 不寫入系統相簿.
- 獨佔: 同一時間只有一個會話 (腳本或插件取景頁), 佔用者會寫在錯誤訊息中.

******

### 外掛接口

******

以下資訊面向 AutoJs6 宿主與外掛開發者, 宿主透過這些標識發現外掛並完成能力協商:

```text
application id: io.github.supermonster003.autojs6.plugin.three.prism.camera
plugin id: three-prism-camera
engine: camera
variant: default
info service: org.autojs.plugin.INFO (category three-prism-camera)
capability service: org.autojs.plugin.CAMERA (category camera)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6640
native libraries: libimage_processing_util_jni.so, libsurface_util_jni.so (CameraX), libandroidx.graphics.path.so (Compose); one APK for every ABI, stored uncompressed and 16 KB page-aligned
```

******

### 開發路線圖

******

功能規劃, 設計決策與完成情況以可勾選清單維護在 ROADMAP.md 中. 未勾選條目表示規劃意向, 不代表目前版本能力.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/11/05_

- `提示` 開發預覽: 插件可以被 AutoJs6 插件中心識別, 拍照, 連續取幀, 取景預覽, 拍攝頁與 camera 腳本模組尚未交付
- `提示` 需要 AutoJs6 6.8.1 (6640) 或更高版本 (準確的最低版本號待宿主側改動落地後回填)
- `提示` 目前圖標為候選圖案, 之後可能更換
- `新增` 插件骨架: Wake 啟動協議, 插件中心身份服務與相機能力服務 (佔位)
- `新增` 只有一個適用於全部裝置的 APK, CameraX 與 Compose 自帶的原生庫已按 16 KB 記憶體分頁對齊
- `新增` 10 種語言的 README, 插件中心說明與更新日誌, 由 JSON 來源檔案統一生成
- `依賴` 附加 Jetpack CameraX 版本 1.6.2 (開啟相機, 拍照, 取幀與取景)
- `依賴` 附加 Jetpack Compose BOM 版本 2026.09.00 (插件介面)
- `依賴` 附加 AndroidX Activity Compose 版本 1.12.2
- `依賴` 附加 AndroidX Core KTX 版本 1.16.0

##### 更多發行歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

******

### 構建

******

本節面向希望從源碼構建外掛的開發者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### 許可

******

專案程式碼使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE). 第三方元件的授權見第三方聲明.

- [第三方聲明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相關連結

******

- AutoJs6 項目: https://github.com/SuperMonster003/AutoJs6
