# Project Spec: Pixel Scanner

## 1. 產品概述 (Overview)

Pixel Scanner 是一款原生 Android 文件掃描應用，核心掃描體驗建立在 Google ML Kit Document Scanner API 之上，並在掃描完成後提供 app 自主掌控的 session、影像增強、檢視、刪頁、排序與匯出流程。

本文件描述的是產品化目標規格，而不是早期 MVP 的暫時實作。設計原則如下：

- 掃描結果一旦回到 app，後續生命週期應由 app 自己掌控。
- 預覽與匯出是兩種不同工作負載，應使用不同策略。
- 匯出流程應根據 session 狀態走不同 decision path，而不是一律重建 PDF。
- 大型文件流程必須考慮穩定性、進度、清理、恢復與背景執行。

## 2. 目標環境 (Target Environment)

- **Device Class:** 以高階 Android 裝置為主要體驗目標，優先針對 Pixel 系列優化。
- **OS Target:** 以目前專案 build config 為準，`compileSdk` 與 `targetSdk` 皆為 34；後續若升級 API level，需同步更新 spec 與 README。
- **UI Framework:** Jetpack Compose（Material 3）
- **Primary Scan API:** Google ML Kit Document Scanner API
- **App Architecture Target:** 分層式架構
  - UI / ViewModel
  - UseCase / Domain
  - Repository / Data
  - Session storage / export / image processing infrastructure

## 3. 核心產品原則 (Product Principles)

- **Session-owned files first**
  - 掃描完成後，JPEG 與原始 PDF 應盡快收編到 app 自己管理的 session 目錄。
- **Strategy over hardcode**
  - 匯出需透過 decision tree 決定走原始 PDF 重用、重建 PDF 或 JPEG 匯出。
- **Preview and export are different workloads**
  - 預覽優先互動流暢與低記憶體。
  - 匯出優先正確性與完整畫質。
- **Large-document support is intentional**
  - 50-100 頁流程需有明確的背景執行、進度回報與殘檔清理策略。
- **Benchmark before advanced optimization**
  - GPU 加速、第三方 PDF 操作庫等高成本方案，須以 benchmark 或明確瓶頸為前提。

## 4. 功能規格 (Functional Requirements)

### 4.1 掃描與捕捉 (Scanning & Capture)

- **技術方案**
  - 使用 `GmsDocumentScanner` 啟動原生掃描流程。
- **啟動模式**
  - 支援 `FULL` 模式，讓使用者在 Google 提供的掃描 UI 內完成拍攝與基本頁面調整。
- **結果格式**
  - 接收 `GmsDocumentScanningResult`。
  - 須保留：
    - JPEG page result
    - 原始 PDF result（若 ML Kit 有提供）
- **掃描範圍**
  - 支援多頁掃描。
  - 頁數上限屬於可配置產品參數，不應只存在於程式碼常數中。

### 4.2 Scan Session Lifecycle

- **Session 建立**
  - 每次掃描完成後建立唯一 `ScanSession`。
- **Eager Ingestion**
  - 掃描結果返回 app 後，應盡快將 page JPEG 拷貝到 app 內部 session 目錄。
  - 若有原始 PDF，也應納入 session 管理範圍。
- **Session Metadata**
  - metadata 至少需描述：
    - session id
    - 原始頁面檔案位置
    - 原始 PDF 位置
    - 頁序
    - 刪除狀態
    - enhancement 狀態
    - export 狀態
    - dirty flags
- **Session Recovery**
  - 若 app 在 review 或 export 階段被系統回收，重新進入時應能恢復未完成 session，或讓使用者明確捨棄。
- **Session Cleanup**
  - 過期 session、失敗匯出殘檔、預覽快取與中斷工作需有明確清理策略。

### 4.3 Review 與頁面管理 (Review And Page Management)

- **Review Screen**
  - 顯示當前 session 的頁面內容與頁數資訊。
  - 主預覽不可依賴一次性全尺寸 bitmap 常駐記憶體。
- **Page Navigation**
  - 支援快速切頁。
  - 多頁文件應提供縮圖列或等效快速定位方式。
- **Delete / Restore**
  - 允許刪除頁面並提供可靠的復原入口。
- **Reorder**
  - 產品化版本應支援頁面重新排序。
- **Action Semantics**
  - 需明確區分：
    - `Scan More`
    - `Discard Session`
    - `Discard & Rescan`

### 4.4 影像後處理 (Image Processing)

- **雙 pipeline**
  - 預覽使用 preview pipeline。
  - 匯出使用 export pipeline。
- **增強能力**
  - 支援以 app 自己的處理流程切換原圖與增強圖。
  - enhancement 的 UX 必須清楚顯示目前頁面處於原圖或增強狀態。
- **快取**
  - 同頁重複切換 enhancement 時應優先命中快取，避免重算。
- **頁級處理狀態**
  - 每頁需有獨立 processing state。
  - 單頁處理中不應鎖住整個 review 流程。
- **可取消性**
  - 切換頁面、離開 session 或重複操作時，需能取消過時工作。
- **安全 decode**
  - 必須使用 bounds decode、`inSampleSize`、尺寸保護與合適的 bitmap config。
- **極端尺寸保護**
  - 長圖、寬圖、小圖與異常輸入不可造成 crash 或不受控記憶體放大。

### 4.5 匯出 (Export)

- **支援格式**
  - JPEG
  - PDF
- **Export Request / Result**
  - 匯出流程必須有明確的 request / result 模型。
  - result 至少區分：
    - success
    - partial success
    - failure
    - cancelled
- **Export Decision Tree**
  - 匯出不可一律重建 PDF。
  - 必須依照 session 狀態決定：
    - 未修改且有原始 PDF：優先重用原始 PDF
    - 頁序或頁面集合改變：走對應的 PDF 重建或其他已核准策略
    - 頁面內容被修改：走影像匯出或 PDF 重建路徑
- **Background Export**
  - 大型 PDF 匯出需支援背景執行策略。
  - 是否啟用 `WorkManager` 應由頁數、耗時或資源條件決定。
- **Progress**
  - 多頁匯出需提供整體進度與必要的頁級進度資訊。
- **Post-export Actions**
  - 匯出成功後應提供開啟、分享、回首頁或繼續掃描等明確 next step。

### 4.6 大文件與批量操作 (Large-Document And Batch Operations)

- 本產品需以 50-100 頁文件作為壓力測試範圍，而非只以 1-5 頁情境設計。
- 批量 enhancement、grid view、批量刪除等能力屬於 should-have / milestone-gated 功能。
- 若支援 `Apply to All`，必須以前置條件成立為前提：
  - 頁級 processing state 已完成
  - 非阻塞背景佇列已完成
  - review 的排序與導航 state 已穩定

## 5. 匯出與儲存規格 (Storage And Output)

- **JPEG**
  - 預期輸出至 `Pictures/PixelScanner` 或等效 MediaStore 路徑。
- **PDF**
  - 預期輸出至 `Documents/PixelScanner` 或經產品決策確認的文件儲存位置。
- **命名規則**
  - 需有一致的檔名策略與重名處理規則。
- **MediaStore**
  - 應以 Android 版本差異驗證實際行為。
  - `MediaScannerConnection` 是否需要介入，應依實際寫入路徑與系統行為明確定義，而非一律強制呼叫。

## 6. UI/UX 流程 (User Flow)

### 6.1 Home

- 首頁至少需提供明確的開始掃描入口。
- 產品化版本可逐步加入：
  - resume unfinished session
  - recent scans
  - export guidance

### 6.2 Scan

- 使用者進入 ML Kit 掃描流程完成拍攝。
- 回到 app 時建立 `ScanSession` 並開始 ingestion。

### 6.3 Review

- 顯示目前 session 的預覽、頁數、切頁入口與頁面工具。
- 允許刪頁、復原、排序與 enhancement 切換。
- UI 文案應清楚區分 destructive action 與 continuation action。

### 6.4 Export

- 使用者選擇 JPEG 或 PDF。
- 系統依 `ExportRequest` 與 session 狀態選擇對應路徑。
- 匯出中提供進度、取消與失敗回復。
- 匯出完成後提供 next actions。

## 7. 非功能需求 (Non-Functional Requirements)

### 7.1 隱私與資料處理

- 所有掃描後處理需在裝置端完成。
- 不得將掃描影像上傳至雲端進行處理。

### 7.2 效能

- 不將固定數字寫成不經驗證的承諾。
- 應建立 benchmark 與裝置基線，至少覆蓋：
  - 掃描返回 review
  - session ingestion
  - 單頁 enhancement
  - 多頁 PDF 匯出
- 優化方向需以 baseline 與 regression 比較為依據。

### 7.3 記憶體管理

- 不得在 ViewModel 長期持有全尺寸 bitmap 列表。
- 預覽圖應透過非同步 image loading 與合適尺寸 request 處理。
- 匯出與影像處理應避免一次性全量解碼整個工作集。
- 對大圖與長文件流程需有 OOM 防護與清理策略。

### 7.4 穩定性

- 長時間匯出不可因 app 進入背景而默默失敗。
- 不可恢復錯誤需提供明確 fallback，例如重新掃描、重試或放棄 session。
- app 啟動時應具備 stale session 與未完成匯出殘檔檢查能力。

### 7.5 可測試性

- domain model、use case、repository 與 export strategy 必須可被單元測試。
- MediaStore、session metadata、PDF 匯出等需有 instrumentation 或等效驗證。

## 8. 技術邊界與待決策事項 (Open Decisions)

- 是否採用純手動 DI 或導入 Hilt。
- 是否先保留單 module，只做 package 分層。
- 是否在 v1 支援頁面排序以外的批量操作。
- 是否在 v1 引入增量 PDF 重組，而不是統一重建。
- 是否需要背景匯出一律使用 `WorkManager`，或只在大型任務啟用。
- 是否需要更高成本的 GPU / NDK 影像處理優化。

以上項目若未定案，應以 ADR、benchmark 或 spike 的形式推進，不得在 spec 中假裝已拍板。
