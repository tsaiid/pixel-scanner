# Pixel Scanner 產品化補強建議 (Supplemental Recommendations)

本文件補充了 `PRODUCTIZATION_TODO.md` 中未詳盡描述的關鍵技術細節，旨在提升 Pixel Scanner 在極端使用情境（如 50+ 頁掃描、長時匯出、背景運行）下的穩定性與效能。

## 1. 影像物理拷貝與 Session 穩定性 (Stability)

### 現狀與風險
目前應用程式直接引用 ML Kit 回傳的 `content://` URI。這些 URI 屬於 Google Play Services 的暫存區，具有時效性且在跨進程 (Cross-process) 存取時可能因權限失效或清理而導致圖片丟失。

### 補強建議
- **強制物理拷貝 (Eager Ingestion)**：在 `ScannerManager` 返回結果後，立即非同步將所有原始影像拷貝至 App 內部的 `cacheDir/sessions/{sessionId}/original/` 目錄。
- **URI 對映表**：建立資料模型追蹤「ML Kit URI -> 內部檔案路徑」的轉換，確保 Review 畫面讀取的檔案是由 App 自行掌控生命週期的。
- **背景清理機制**：在 `SessionManager` 中實作 LRU 或時效檢查，確保未完成或已廢棄的 Session 實體檔案不會永久占用磁碟空間。

## 2. 智慧 PDF 決策路徑 (PDF Strategy)

### 現狀與風險
目前的 `StorageManager` 採用 Canvas 繪圖方式重建 PDF，這會導致：
1. **畫質降低**：重新解碼與編碼 JPEG 進入 PDF 導致失真。
2. **遺失 OCR**：遺失了 ML Kit 原本可能生成的文字層資訊。
3. **檔案體積膨脹**：Canvas 重繪的 PDF 通常比專業庫生成的體積大。

### 補強建議
- **無損路徑 (Fast-track)**：若使用者未進行「編輯/增強/旋轉」，直接將 ML Kit 產出的原始 PDF 透過 `File.copyTo` 移至目的地。
- **增量重構 (Incremental Rebuild)**：若僅進行「刪頁/排序」，評估使用 `PdfBox-Android` 或 `itext` 等庫進行頁面操作，而非全量重繪。
- **影像增強路徑**：僅在頁面內容被修改（如啟用 Enhance）時，才啟動 Canvas 重繪管線。

## 3. 背景可靠性與 WorkManager (Reliability)

### 現狀與風險
匯出 50 頁以上的 PDF 是一項高負載任務。若使用者在匯出時切換到其他 App（如回覆訊息），`viewModelScope` 可能因 App 進入後台被暫停，導致匯出中斷且產生損壞檔案。

### 補強建議
- **匯出任務背景化**：將「PDF 生成」與「MediaStore 寫入」邏輯封裝進 `WorkManager`。
- **前台通知 (Foreground Service)**：當匯出任務啟動時，顯示一個帶進度條的 Notification，並確保在系統記憶體不足時該任務具有較高優先權。
- **續傳機制**：若匯出因系統 OOM 中斷，重新啟動 App 時應能檢查並清理未完成的殘餘檔案。

## 4. 硬體加速影像處理 (Performance)

### 現狀與風險
目前的像素處理迴圈 (Kotlin Loop) 在 Tensor G5 上雖然可行，但處理 12MP 圖片時會造成顯著發熱且耗電，尤其在多頁批量處理時效能下降明顯。

### 補強建議
- **ColorMatrix 濾鏡**：針對簡單的亮度均勻化與色彩增強，優先使用 `Android.graphics.ColorMatrixColorFilter`，這在底層由 GPU 加速，效能比手寫迴圈快一個量級。
- **RenderScript/Vulkan/Compute Shader**：若需進行複雜的背景估計 (Background Estimation)，應考慮移動到 GPU 執行。
- **Bitmap 同步優化**：利用 `Bitmap.Config.HARDWARE` 讀取預覽圖，減少 CPU 與 GPU 記憶體間的拷貝。

## 5. 批量操作 UX 與 Logic (Scalability)

### 現狀與風險
目前的 UI 設計適合 1-5 頁的小規模掃描。當面對 20 頁以上的文件時，單頁切換與逐頁點選增強的效率極低。

### 補強建議
- **一鍵全選增強 (Apply to All)**：在 Review 工具列提供「套用增強至所有頁面」的功能。
- **非同步非阻塞 (Non-blocking) 處理**：當使用者點選「全部增強」時，背景佇列自動開始處理，UI 顯示全域進度條，但使用者仍可繼續進行頁面排序或導航。
- **頁面網格預覽 (Grid View)**：提供縮圖網格模式，方便使用者在大量頁面中快速跳轉與批量選取刪除。

## 6. 錯誤分類與恢復 (Error Handling)

### 現狀與風險
目前錯誤處理主要依賴 `try-catch` 與簡單的 `Toast`，使用者無法區分是「權限不足」、「空間不足」還是「圖片損壞」。

### 補強建議
- **磁碟空間檢查**：在啟動掃描與匯出前，先檢查 `internal storage` 的可用空間，避免中斷引發的 Crash。
- **媒體掃描器同步 (MediaScanner)**：匯出後強制調用 `MediaScannerConnection.scanFile`，確保圖片能立即在 Gallery/相簿中顯示。
- **明確的權限導引**：針對 Android 11+ 的 Scoped Storage 限制，提供清晰的「文件存取權限」跳轉連結。
