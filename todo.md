# Todo List: Pixel Scanner Development

## Phase 1: 基礎架構與 ML Kit 整合 (MVP)
- [x] **Project Setup**
    - [x] 建立新 Android Studio 專案 (Kotlin DSL)。
    - [x] 設定 `minSdk 29`，並與目前 build config 對齊 `compileSdk 35` / `targetSdk 35`。
    - [x] 加入 dependencies: Jetpack Compose, ML Kit Document Scanner, Coil (圖片加載)。
    - [x] 設定 `AndroidManifest.xml` 權限 (Camera, Internet - ML Kit 下載模型用)。

- [x] **ML Kit Scanner Integration**
    - [x] 建立 `ScannerManager` class 封裝 `GmsDocumentScannerOptions`。
    - [x] 設定 Scanner Options:
        - `setGalleryImportAllowed(true)`
        - `setPageLimit(100)` (必須 >= 1)
        - `setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)`
        - `setScannerMode(SCANNER_MODE_FULL)`
    - [x] 在 Compose UI 建立一個按鈕，透過 `ActivityResultLauncher` 啟動 Scanner。
    - [x] 實作 Callback 接收 `GmsDocumentScanningResult`。
    - [x] 驗證：能成功啟動相機、自動抓邊、裁切，並取得圖片 URI。

## Phase 2: 影像處理引擎 (ML Kit 內建)
- [x] **ML Kit Integration**
- [x] 配置 `GmsDocumentScannerOptions`。
- [x] 啟用 `setGalleryImportAllowed` 與 `setResultFormats`。
- [x] 測試 Tensor G5 在 ML Kit 運算時的熱度與速度 (Simulated).

## Phase 3: 自定義預覽與編輯 UI (MVP)
- [x] **Review Screen UI**
    - [x] 建立 `ReviewViewModel` 暫存 ML Kit 回傳的 URI 列表。
    - [x] 實作水平捲動的圖片預覽區 (HorizontalPager)。
    - [x] 實作「去陰影」開關 (Switch/Button)。

## Phase 4: 儲存與輸出 (MVP)
- [x] **File Management**
    - [x] 實作 `StorageManager`。
    - [x] 處理 Android 10+ Scoped Storage 邏輯 (`ContentResolver`, `MediaStore`)。
    - [x] 實作 `saveImageToGallery(bitmap)`。
    - [x] 實作 `savePdf(uri)`。
    - [x] **新增：支援選擇 JPEG 或 PDF 輸出格式並在預覽頁面切換。**

## Phase 5: 優化與測試 (Current Focus)
- [x] **UX Polish**
    - [x] Add "Delete Page" functionality in Review Screen.
    - [x] Improve Review Screen UI (Toolbar, Layout).
    - [x] **Add README.md for project documentation.**
- [x] **Memory Optimization (High Priority)**

## Phase 6: 進階影像處理 (Complete)
- [x] **Image Processing Pipeline**
    - [x] Implement `ImageProcessor` class (Simple Lightness Equalization).
    - [x] Implement Lightness Equalization algorithm (Background Estimation & Division).
    - [x] Connect "Enhanced Mode" toggle in Review Screen.

## Phase 7: 維護與穩定性修正
- [x] **Build & Tooling**
    - [x] 升級 AGP 至 `9.5.1` 與 Gradle Wrapper 至 `9.6.0`（含 Daemon JVM Toolchain 與 Foojay Resolver 設定）。
    - [x] 更新 Gradle Wrapper 至 `9.9.0`（同步更新 `gradle-wrapper.jar`、`gradlew`、`gradlew.bat` 與 `gradle-wrapper.properties` 重試設定）。
- [x] **Bug Fixes & Tests**
    - [x] 修正 `ReviewPagerSection` 在刪除與復原頁面時 `pagerKey` 發生 `IndexOutOfBoundsException` 的問題。
    - [x] 調整 `ReviewActionCopyLocaleTest` 繁體中文匯出按鈕檢查，對齊精簡「存檔」文案並驗證所有單元測試通過。
    - [x] 修正放棄（Discard）與重掃（Rescan）時 `DiscardSessionUseCase` 未實際刪除 `sessions/{sessionId}` 目錄與 `session.json` 的問題，並在放棄前取消進行中的 `ingestionJob`。
    - [x] 擴充 `ScanSessionMetadata` 與 `AndroidSessionMetadataRepository` 持久化每頁狀態（排序、刪頁、增強圖檔），並在 `ReviewViewModel` 狀態變更時同步更新 `session.json`、於 `RestoreSessionUseCase` 完整還原。
    - [x] 修正多張圖片（JPEG）匯出成功時 `ExportResult.Success` 僅保留第一張 URI 導致分享動作無法分享全部圖片的問題。
- [x] **Performance & UX Enhancements**
    - [x] 優化 `ImageEnhancer` 背景估計與像素處理：改用 2D 雙線性插值（`interpolatePreviewBackgroundRowBilinear`）消除方塊偽影，並以 64 列批次區塊讀寫大幅降低 JNI 呼叫開銷。
    - [x] 優化 `ReviewScreen` 體驗：新增預覽頁面雙擊/雙指縮放平移手勢（`reviewPageZoomGesture`）、精簡直向標題列釋放預覽空間，以及於縮圖顯示影像增強狀態標記。

