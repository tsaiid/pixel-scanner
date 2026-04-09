# 效能基線量測方式與測試樣本

> 文件目的：先定義「怎麼量」與「拿什麼樣本量」，作為後續建立效能基線的共同標準。
>
> 目前狀態：
> - 已完成：量測範圍、裝置條件、重現規則、樣本規格、記錄模板
> - 尚未完成：實際跑量測、填入 baseline 數據、判定是否超出門檻

## 1. 量測目標

本文件先建立可重複的 baseline 量測方法，涵蓋以下場景：

- cold start
- scanner -> review return
- session ingestion
- single-page enhancement
- multi-page PDF export
- memory peak / OOM risk

此階段只定義量測方法與樣本規格，不把任何未實測數據視為完成。

## 2. 裝置與測試條件

### 2.1 裝置條件

量測時需固定至少以下條件：

- 同一台實體 Android 裝置
- 同一版 system image / OS 版本
- 同一版 app build
- 同一測試帳號與相同裝置狀態
- 低電量模式關閉
- 省電模式關閉
- 自動同步、背景更新與不相關通知盡量關閉

### 2.2 建議記錄欄位

每次量測至少記錄：

- 裝置型號
- Android 版本
- CPU / RAM 規格
- app 版本號與 build number
- 測試日期與時間
- 測試人員
- 是否為冷開機後第一次執行
- 當次樣本名稱

### 2.3 重現規則

為了讓 baseline 可比較，執行前請固定以下條件：

- 測試前關閉 app，必要時清除 recent apps
- 同一 scenario 每次使用相同樣本與相同操作順序
- 同一 scenario 每輪只改變一個變因
- 量測期間不切換網路狀態，除非該 scenario 明確依賴網路
- 量測前後避免執行其他高負載 app
- 若使用 debug build，需固定同一組 debug 設定
- 若量測包含檔案操作，檔案需事先放在固定路徑，避免當次下載或搬移造成干擾

## 3. 量測場景

### 3.1 Cold Start

定義：

- 從 app 完全未啟動到主畫面可互動的時間

記錄重點：

- 首畫面可見時間
- 首畫面可互動時間
- 是否伴隨明顯掉幀或卡頓

### 3.2 Scanner -> Review Return

定義：

- 從 scanner 完成掃描並返回 review 畫面的時間

記錄重點：

- 掃描完成到畫面切換完成時間
- review 第一批資料可見時間
- 若有 session 建立或檔案搬移，需註記是否同步或背景完成

### 3.3 Session Ingestion

定義：

- 掃描結果寫入 app 內部 session 目錄與 metadata 的時間

記錄重點：

- page JPEG copy 時間
- 原始 PDF copy 時間
- metadata 寫入時間
- ingestion 整體完成時間

### 3.4 Single-Page Enhancement

定義：

- 單頁執行 enhancement 並回寫預覽資產的時間

記錄重點：

- 點擊 enhancement 到結果顯示的時間
- CPU 高峰期間是否產生 UI 卡頓
- 記憶體是否有異常上升

### 3.5 Multi-Page PDF Export

定義：

- 多頁文件從 export 開始到輸出完成的時間

記錄重點：

- export 進度開始時間
- 最終輸出檔完成時間
- 匯出期間是否可持續操作 UI
- 若使用背景匯出，需註記前景與背景交界點

### 3.6 Memory Peak / OOM Risk

定義：

- 在各 scenario 中觀察峰值記憶體與 OOM 風險點

記錄重點：

- 峰值 PSS / RSS 或等效指標
- 是否出現 GC 頻繁抖動
- 是否有 bitmap decode / export 時的高峰
- 是否接近 OOM、發生 ANR、或觸發系統回收

## 4. 測試樣本規格

### 4.1 命名規則

建議樣本命名採固定格式：

`pbm-{class}-{pages}-{resolution}-{variant}.{ext}`

其中：

- `pbm` = performance baseline measurement
- `class` = `sp`、`mp`、`large`、`long`
- `pages` = 頁數或 `1p`
- `resolution` = 解析度標記，例如 `2000x3000`
- `variant` = `orig`、`rot`、`bw`、`mixed` 等
- `ext` = `jpg`、`pdf`

範例：

- `pbm-sp-1p-2000x3000-orig.jpg`
- `pbm-mp-12p-2000x3000-mixed.pdf`
- `pbm-large-1p-4000x6000-orig.jpg`
- `pbm-long-48p-1600x2400-mixed.pdf`

### 4.2 樣本分類

#### A. 單頁樣本

用途：

- 驗證單頁掃描、單頁 enhancement 與基礎 ingestion 成本

建議規格：

- 1 張 JPEG
- 建議尺寸：`2000x3000`
- 文件內容：一般文字文件、收據或 A4 文件
- 檔名：`pbm-sp-1p-2000x3000-orig.jpg`

#### B. 多頁樣本

用途：

- 驗證多頁 review、session ingestion 與 PDF export 成本

建議規格：

- 6 頁、12 頁、24 頁各一組
- 每頁建議尺寸：`2000x3000`
- 檔案格式：PDF 與對應 page JPEG 集合
- 檔名：
  - `pbm-mp-6p-2000x3000-mixed.pdf`
  - `pbm-mp-12p-2000x3000-mixed.pdf`
  - `pbm-mp-24p-2000x3000-mixed.pdf`

#### C. 大圖樣本

用途：

- 驗證 decode、preview enhancement 與 memory peak / OOM risk

建議規格：

- 1 張 JPEG
- 建議尺寸：`4000x6000` 或接近裝置容易吃緊的高解析圖
- 文件內容：高密度文字、低對比區域或大面積背景
- 檔名：`pbm-large-1p-4000x6000-orig.jpg`

#### D. 長文件樣本

用途：

- 驗證長文件匯出、頁面切換、背景處理與清理策略

建議規格：

- 48 頁或 72 頁一組
- 每頁建議尺寸：`1600x2400`
- 文件內容：連續文件、教材、表單合輯或多章節掃描
- 檔名：
  - `pbm-long-48p-1600x2400-mixed.pdf`
  - `pbm-long-72p-1600x2400-mixed.pdf`

### 4.3 樣本組合原則

- 單頁樣本優先用於冷啟動、單頁 enhancement 與基本 ingestion
- 多頁樣本優先用於 review flow 與 PDF export
- 大圖樣本優先用於 memory peak 與 OOM 風險
- 長文件樣本優先用於多頁匯出、長時間互動與穩定性觀察

## 5. 量測記錄方式

### 5.1 基本原則

- 每個 scenario 至少跑 5 次
- 前 1 次視為 warmup，不納入 baseline 平均值
- 若 scenario 變異較大，建議增加到 10 次以上
- 量測時需保留原始紀錄，不只存平均值

### 5.2 變異接受度

建議先以保守方式處理變異：

- 若單次結果偶發偏離明顯，先檢查是否有背景干擾或樣本不一致
- 若同一 scenario 的結果波動大於約 10% 到 15%，先標記為需重測，不直接當 baseline
- 若受裝置狀態影響較大，應拆分不同條件另建 baseline

### 5.3 建議記錄表

可用下列表格作為每次量測紀錄模板：

| 日期 | 裝置 | OS | App build | Scenario | Sample | Run | Warmup | Start time | End time | Duration(ms) | Peak memory | OOM risk | 備註 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-04-08 | Pixel xx | Android xx | 1.0.xx | cold start | n/a | 1 | yes |  |  |  |  |  | 首輪 warmup |
| 2026-04-08 | Pixel xx | Android xx | 1.0.xx | multi-page export | `pbm-mp-12p-2000x3000-mixed.pdf` | 2 | no |  |  |  |  |  |  |

### 5.4 建議彙整欄位

每個 scenario 最終至少彙整：

- 最小值
- 最大值
- 中位數
- 平均值
- 標準差或其他波動指標
- 是否達到可接受 variance

## 6. 已量測與待量測

### 6.1 目前狀態

- 已量測：無正式 baseline 數據
- 已定義：量測方法、樣本類型、記錄模板、重現規則

### 6.2 後續填寫原則

未來若開始填數據，請分開標示：

- `planned`：尚未量測，只是規格或預期
- `measured`：已實際跑過並填入結果
- `needs rerun`：結果變異過大或條件不一致，需要重測

不要把 planned 欄位誤寫成 measured，也不要把推測值當成 baseline。

