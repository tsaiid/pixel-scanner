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
