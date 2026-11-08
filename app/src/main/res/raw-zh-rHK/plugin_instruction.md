3-Prism Camera 是 AutoJs6 的相機插件, 基於 Jetpack CameraX 為腳本提供拍照, 連續幀分析與取景預覽. 連續幀以 AutoJs6 的 `ImageWrapper` 交給腳本, 腳本可以直接把它交給 `ocr`, `barcode` 或 `yolo` 等現有模組, 實現即時掃碼, 文字識別與目標偵測.

> 1.0.0 是首個正式版本. 腳本使用的 `camera` 模組需要 AutoJs6 6.8.1 (內部版本號 6641) 或更高版本; 插件的取景頁與設定頁不依賴 AutoJs6.

### 功能亮點

- 拍照: 傳回按 EXIF 方向轉正的 `ImageWrapper`, 也可以把原始 JPEG (含 EXIF) 儲存到指定路徑; 可設定閃光模式, 畫質與解像度.
- 連續幀: RGBA 幀預設 640x480 (可在設定頁修改), 最高 1920x1080, 預設按螢幕方向轉正; 處理不及時自動丟幀並計數, 不會排隊積壓.
- 預覽: 把 ui 腳本或懸浮窗中的 `SurfaceView` 交給會話即可顯示取景畫面, 可與連續幀和拍照同時進行.
- 控制: 鏡頭選擇, 解像度, 目標幀率, 電筒, 閃光模式, 變焦, 曝光補償與點按對焦, 並可讀取各項範圍與目前狀態.
- 取景頁: 從啓動器開啟插件即進入取景頁, 可切換鏡頭, 開關手電筒, 變焦與輕按對焦, 拍下的相片只顯示不儲存, 右上角進入設定頁; `camera.capture({ ui: true })` 開啟同一個取景頁, 使用者按下快門後腳本取得相片, 取消時傳回 `null`.
- 背景可用: 會話開啟期間執行相機前景服務, 腳本切到背景或熄屏後仍可取幀; 腳本結束或 AutoJs6 被終止時自動釋放相機.

腳本 API 見 [AutoJs6 文件 > 相機 (Camera)](https://docs.autojs6.com/#/camera), 開發進度與設計見 [項目主頁](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
