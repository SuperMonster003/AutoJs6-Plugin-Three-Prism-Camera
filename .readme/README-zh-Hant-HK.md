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

> 1.0.0 的相機核心, 取景頁與設定頁均已完成, 正在進行發佈前驗證. 腳本使用的 `camera` 模組需要 AutoJs6 6.8.1 (內部版本號 6641) 或更高版本.

******

### 功能亮點

******

- 拍照: 傳回按 EXIF 方向轉正的 `ImageWrapper`, 也可以把原始 JPEG (含 EXIF) 儲存到指定路徑; 可設定閃光模式, 畫質與解像度.
- 連續幀: RGBA 幀預設 640x480 (可在設定頁修改), 最高 1920x1080, 預設按螢幕方向轉正; 處理不及時自動丟幀並計數, 不會排隊積壓.
- 預覽: 把 ui 腳本或懸浮窗中的 `SurfaceView` 交給會話即可顯示取景畫面, 可與連續幀和拍照同時進行.
- 控制: 鏡頭選擇, 解像度, 目標幀率, 電筒, 閃光模式, 變焦, 曝光補償與點按對焦, 並可讀取各項範圍與目前狀態.
- 取景頁: 從啓動器開啟插件即進入取景頁, 可切換鏡頭, 開關手電筒, 變焦與輕按對焦, 拍下的相片只顯示不儲存, 右上角進入設定頁; `camera.capture({ ui: true })` 開啟同一個取景頁, 使用者按下快門後腳本取得相片, 取消時傳回 `null`.
- 背景可用: 會話開啟期間執行相機前景服務, 腳本切到背景或熄屏後仍可取幀; 腳本結束或 AutoJs6 被終止時自動釋放相機.

******

### 使用方法

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases) 下載與裝置 ABI 對應的 APK 並安裝: 大多數手機選擇 `arm64-v8a`, 不確定時選擇適用於全部裝置但體積較大的 `universal`.
2. 開啟插件, 按提示授予相機權限; 腳本也可以呼叫 `camera.requestPermission()` 開啟授權頁.
3. 如要在腳本中使用 `camera` 模組, 請將 AutoJs6 升級到內部版本號 6641 (6.8.1) 或更高版本, 並在插件中心啟用本插件.

> 插件支援 Android 7.0 (API 24) 或以上的裝置. 每種 ABI 各有一個安裝包, 另有適用於全部裝置的 `universal` 安裝包; CameraX 與 Compose 自帶的少量原生庫 (每種 ABI 約 50 KB) 已按 16 KB 記憶體分頁對齊, 採用 16 KB 記憶體分頁的新裝置亦能正常安裝.

******

### 相容性

******

- Android 7.0 (API 24) 及以上, 不限 ABI: arm64-v8a, armeabi-v7a, x86_64 與 x86 各有安裝包, `universal` 安裝包適用於全部裝置.
- Android 7.0 - 8.0 (API 24 - 26) 沒有共用記憶體, 幀改經臨時檔案傳給 AutoJs6, 連續幀的幀率低於較新的系統.
- 腳本使用 `camera` 模組需要 AutoJs6 6.8.1 (內部版本號 6641) 或更高版本; 較早的 AutoJs6 沒有該模組, 但仍可以從啟動器開啟插件的取景頁.
- 部分廠商系統 (例如 ColorOS) 會額外限制背景應用程式使用相機; 各裝置的驗證結果見開發路線圖附錄 F.

******

### 腳本 API

******

腳本透過全域模組 `camera` (也可寫作 `$camera`) 使用這些功能, 完整說明見 [AutoJs6 文件 > 相機 (Camera)](https://docs.autojs6.com/#/camera):

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

能, 前提是會話在 AutoJs6 位於前景時開啟. 會話開啟期間插件執行相機前景服務, 通知欄會顯示相機正在使用. AutoJs6 已在背景運行或手機停在鎖定畫面時 (Android 11 或以上), 系統不允許使用相機, 此時開啟相機會報告 CAMERA_DISABLED: 有的系統在開啟時直接報錯, 有的先開啟會話再報告並關閉. 回到前景後再開啟即可正常使用, 不需要重新啟動插件. 部分廠商系統會額外限制背景相機, 相容情況見開發路線圖中的裝置矩陣.

#### 插件會把相片存進相簿嗎?

不會. 插件不申請儲存權限, 也不寫入系統相簿; 只有腳本傳入 `path` 時, AutoJs6 才把相片寫到該路徑.

******

### 權限與安全

******

插件在設計上盡量收窄權限範圍:

- 受保護的入口: 能力服務與啟動入口受 AutoJs6 插件權限 (`org.autojs.permission.PLUGIN`) 保護, 其他應用程式無法直接呼叫; 前景服務與透明啟動頁不匯出.
- 相機: 只申請 `CAMERA`, 相機類前景服務權限, 以及 Android 13 或以上顯示相機使用通知所需的通知權限; 相機只在會話開啟期間使用, 會話關閉, 腳本結束或 AutoJs6 被終止時立即釋放.
- 網絡與儲存: 網絡權限只用於設定頁的檢查更新 (只讀取 AutoJs6 官方插件索引, 自動檢查每 12 小時最多一次, 可以關閉), 不申請儲存權限; 幀與相片只在插件和 AutoJs6 之間傳遞, 不會上載, 也不寫入系統相簿.
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
minimum host build: 6641
native libraries: libimage_processing_util_jni.so, libsurface_util_jni.so (CameraX), libandroidx.graphics.path.so (Compose); one APK per ABI plus a universal APK, stored uncompressed and 16 KB page-aligned
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

- `提示` 腳本 API 見 [AutoJs6 文件 > 相機 (Camera)](https://docs.autojs6.com/#/camera)
- `提示` 需要 AutoJs6 6.8.1 (6641) 或更高版本 (準確的最低版本號待宿主側改動落地後回填)
- `提示` 目前圖標為候選圖案, 之後可能更換
- `新增` 插件骨架: Wake 啟動協議與插件中心身份服務
- `新增` 相機能力服務: 同一時間只有一個會話能使用相機, 被佔用時報告佔用者; 使用相機期間顯示前景通知, AutoJs6 結束後相機立即釋放, 通知隨之消失
- `新增` AutoJs6 在背景運行或手機停在鎖定畫面時 (Android 11 或以上), 系統不允許使用相機, 此時開啟相機會報告 CAMERA_DISABLED (視乎系統, 開啟時直接報錯, 或會話先報告再關閉), 回到前景後再開啟即可正常使用; 在前景開啟的會話轉到背景或熄屏後照常取幀
- `新增` 連續取幀: RGBA 幀經 3 塊共享記憶體 (Android 8.1 或以上) 或 3 個臨時檔案 (Android 7.0 - 8.0) 輪流交給 AutoJs6, 可設定解像度與幀率; 來不及處理的幀會被略過並計數
- `新增` 拍照: JPEG 以唯讀檔案交給 AutoJs6, 並附帶寬高, EXIF 方向, 位元組數與是否鏡像; 可設定畫質, 解像度與閃光模式; 鏡像相片只改寫 EXIF 方向, 不重新編碼; 個別相機 (例如 Android 7.0 模擬器) 不寫入相片方向, 插件會補寫 90 / 270 度的方向, 使相片按手持方向顯示 (漏寫的 180 度無法識別)
- `新增` 預覽: 畫面顯示在 AutoJs6 提供的介面上, 旋轉螢幕後方向隨之調整; 前置鏡頭的預覽預設鏡像, 幀與相片預設不鏡像; 顯示預覽的介面關閉後自動停止預覽, 取幀與拍照照常進行
- `新增` 控制: 電筒, 閃光模式, 變焦 (倍率或線性), 曝光補償, 點按對焦與測光; 裝置不支援的項目 (例如前置鏡頭的閃光燈) 報告 UNSUPPORTED
- `新增` 相機權限: 未授權時開啟相機報告 PERMISSION_DENIED, 可以請求授權; Android 13 或以上同時請求通知權限, 以顯示相機使用中的通知
- `新增` 取景頁: 從啓動器開啟, 可切換鏡頭, 開關手電筒, 用滑桿或雙指縮放變焦, 輕按畫面對焦; 拍下的相片只顯示畫面與解像度, 不會儲存; 前置鏡頭的畫面在螢幕上鏡像顯示, 相片不鏡像
- `新增` 腳本調用 camera.capture({ ui: true }) 時開啟同一個取景頁: 相片直接交給腳本, 取消則返回 null; AutoJs6 在背景運行導致取景頁無法開啟時, 10 秒後報告 UI_REQUIRES_FOREGROUND, 不會一直等待
- `新增` 相機正被腳本使用時, 取景頁顯示是哪個腳本, 並可停止該腳本後接手相機
- `新增` 設定頁 (取景頁右上角): 外觀 (語言, 夜間模式, 主題色, 啓動器圖標), 相機與通知權限, 預設鏡頭, 預設分析解像度與幀率, 前台服務通知, 診斷 (CameraX 版本, 各鏡頭的能力, 目前使用相機的一方) 與版本記錄
- `新增` 腳本沒有指定鏡頭, 解像度或幀率時, 使用設定頁中的預設值 (初始為後置鏡頭, 640x480 與裝置本身的幀率); 腳本指定的值總是優先
- `新增` 檢查更新: 在設定頁查看官方插件索引中的新版本及其說明, 可以忽略某個版本; 開啟設定頁時自動檢查 (每 12 小時最多一次, 可以關閉); 網絡權限只用於檢查更新, 插件不會上載相機畫面與相片
- `新增` 按 ABI 分別提供安裝包 (arm64-v8a, armeabi-v7a, x86_64, x86), 另有適用於全部裝置的 universal 安裝包; CameraX 與 Compose 自帶的原生庫已按 16 KB 記憶體分頁對齊
- `新增` 10 種語言的 README, 插件中心說明與更新日誌, 由 JSON 來源檔案統一生成
- `依賴` 附加 Jetpack CameraX 版本 1.6.2 (開啟相機, 拍照, 取幀與取景)
- `依賴` 附加 Jetpack Compose BOM 版本 2026.09.00 (插件介面)
- `依賴` 附加 AndroidX Activity Compose 版本 1.12.2
- `依賴` 附加 AndroidX Core KTX 版本 1.16.0
- `依賴` 附加 AndroidX ExifInterface 版本 1.4.2 (寫入相片的 EXIF 方向)
- `依賴` 附加 Material Color Utilities (MaterialKolor) 版本 4.1.1 (插件介面的主題色)

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
