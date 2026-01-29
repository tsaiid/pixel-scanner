# Project Spec: Pixel Scanner (Pixel 10 Pro XL)

## 1. 產品概述 (Overview)
一款專為 Pixel 10 Pro XL 設計的原生 Android 文件掃描應用。利用 Google ML Kit Document Scanner API 處理核心的相機取景、自動邊緣偵測與裁切，並在後製階段加入自定義的「去陰影（亮度平衡）」演算法，提供類似 iOS Evernote Scannable 的流暢體驗。

## 2. 目標裝置與環境 (Target Environment)
* **Device:** Pixel 10 Pro XL
* **OS:** Android 16 (API Level 36)
* **Architecture:** MVVM (Model-View-ViewModel)
* **UI Framework:** Jetpack Compose (Material 3)
* **Primary API:** Google ML Kit Document Scanner API
* **Image Processing:** Google ML Kit Document Scanner API (內建自動增強與去陰影)

## 3. 核心功能規格 (Functional Requirements)

### 3.1 掃描與捕捉 (Scanning & Capture)
* **技術方案:** 調用 `GmsDocumentScanner`。
* **啟動模式:** 支援 `FULL` 模式（包含掃描、裁切、旋轉、排序的完整 Google 原生 UI）。
* **捕捉模式 (Capture Mode):** 設定為 **Batch Mode (連續拍攝)**，允許使用者連續翻拍多頁文件。
* **自動化:** 啟用 ML Kit 的自動快門與自動邊緣偵測。
* **輸出:** 接收 ML Kit 回傳的 `GmsDocumentScanningResult`，包含裁切後的 JPEG 圖片 URI 列表與 PDF URI。

### 3.2 影像後處理 (Post-Processing Pipeline)
ML Kit 雖然內建濾鏡，但為了達成「去陰影」與「還原」的精確控制，App 將在接收 ML Kit 結果後，提供第二層編輯能力。

* **演算法: 簡單亮度平衡 (Simple Lightness Equalization)**
    * **原理:** 估算圖像的背景亮度平面（Background Illumination Estimation），將原圖亮度除以背景亮度以均勻化光照。
    * **實作:** 使用 ML Kit Document Scanner 提供的多頁面掃描與濾鏡功能。
    * **觸發:** 當使用者點擊「自動修圖」或「去陰影」按鈕時執行。
* **還原機制 (Revert):**
    * 保留 ML Kit 回傳的原始裁切圖檔 (Original Cropped)。
    * 允許使用者在「處理後圖像 (Processed)」與「原始圖像 (Original)」間無縫切換。

### 3.3 儲存與輸出 (Storage & Output)
* **格式:** * 預設: JPG (存入系統相簿 `MediaStore.Images`)。
    * 選用: PDF (存入 `MediaStore.Downloads` 或 `Documents`)。
* **路徑:** 圖片應存於 `Pictures/PixelScanner` 資料夾以便管理。

## 4. UI/UX 流程 (User Flow)
1.  **Home Screen:** 顯示歷史掃描紀錄（選用），右下角巨大 FAB (懸浮按鈕)「開始掃描」。
2.  **ML Kit Activity:** * 使用者進行連續拍攝。
    * 在 ML Kit 介面中確認裁切範圍。
    * 點擊「完成」。
3.  **App Review Screen (自定義預覽頁):**
    * 顯示剛剛掃描的圖片清單 (Grid/Carousel)，使用非同步載入 (Coil) 避免記憶體溢出。
    * **編輯/管理:** 
        * 刪除單頁 (Delete Page)。
        * 單圖編輯模式 (待實作): 點擊圖片放大，下方顯示 Toggle Switch：「原圖」vs「去陰影增強」。
    * **批次動作:** 底部按鈕「全部儲存到相簿」、「分享 PDF」。

## 5. 技術限制與邊界條件 (Constraints)
* **隱私:** 所有運算必須在裝置端 (On-device) 完成，禁止上傳圖片至雲端伺服器處理。
* **效能:** 去陰影演算法處理單張 12MP 圖片的時間需控制在 1秒內 (利用 Pixel Tensor G5 效能)。
* **記憶體管理:** 
    * 嚴禁在 ViewModel 中持有全尺寸 Bitmap 列表。
    * 預覽圖需透過 Coil 等 Image Loader 非同步載入。
    * 儲存時採 Just-in-Time 載入或 Stream Copy，避免 OOM。
