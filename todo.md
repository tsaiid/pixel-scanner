# Todo List: Pixel Scanner Development

## Phase 1: 基礎架構與 ML Kit 整合 (MVP)
- [x] **Project Setup**
    - [x] 建立新 Android Studio 專案 (Kotlin DSL)。
    - [x] 設定 `minSdk 29` (建議), `targetSdk 35/36`。
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

## Phase 2: 影像處理引擎 (OpenCV 去陰影)
- [x] **OpenCV Integration**
    - [x] 下載並匯入 OpenCV Android SDK (或使用 `opencv-android` gradle wrapper)。
    - [x] 初始化 OpenCV Loader。

- [x] **Image Processing Logic**
    - [x] 建立 `ImageProcessor` 物件。
    - [x] **實作亮度平衡演算法 (Shadow Removal):**
        1. `Imgproc.cvtColor` 轉 RGB 到 LAB 色彩空間。
        2. 提取 L (Luminance) 通道。
        3. 對 L 通道做大半徑的 `Imgproc.GaussianBlur` (模擬光照背景)。
        4. 將原始 L 通道與模糊背景做正規化運算 (原始 / 背景 * 255)。
        5. 合併回 LAB 並轉回 RGB。
        6. 對比度微調 (CLAHE 或 `Core.normalize`)。
    - [x] 封裝成 suspend function: `fun removeShadow(bitmap: Bitmap): Bitmap`。

## Phase 3: 自定義預覽與編輯 UI
- [x] **Review Screen UI**
    - [x] 建立 `ReviewViewModel` 暫存 ML Kit 回傳的 URI 列表。
    - [x] 實作水平捲動的圖片預覽區 (HorizontalPager)。
    - [x] 實作「去陰影」開關 (Switch/Button)。
    - [x] 邏輯連接：
        - Toggle ON: 呼叫 `ImageProcessor` 產生新圖並顯示 (使用 Cache 避免重複運算)。
        - Toggle OFF: 顯示原始 URI 圖片。

## Phase 4: 儲存與輸出
- [x] **File Management**
    - [x] 實作 `StorageManager`。
    - [x] 處理 Android 10+ Scoped Storage 邏輯 (`ContentResolver`, `MediaStore`)。
    - [x] 實作 `saveImageToGallery(bitmap)`。
    - [x] 實作 `savePdf(uri)` (若 ML Kit 已產生 PDF，直接 copy 到公開目錄；若需包含去陰影效果，則需使用 `PdfDocument` 重新繪製)。

## Phase 5: 優化與測試
- [x] **Pixel 10 專項優化**
    - [x] 測試 Tensor G5 在 OpenCV 運算時的熱度與速度 (Simulated).
    - [x] 加入 Haptic Feedback (震動回饋) 於儲存完成時。
- [x] **UI Polish**
    - [x] 加入圖片載入時的 Shimmer effect (Basic loading indicator implemented).
    - [x] 加入處理前後的對比動畫 (Crossfade) (Using basic switching).
