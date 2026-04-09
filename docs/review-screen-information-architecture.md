# Review Screen Information Architecture Draft

本文件定義 review 畫面的資訊架構與互動骨架，目標是讓文件掃描的核心任務在單一畫面內清楚可讀、可切換、可完成。

## 設計目標

- 先讓使用者辨識「現在看的是哪一頁」。
- 再讓使用者理解「這一頁的狀態是什麼」。
- 最後才放出頁面操作與整批完成動作。
- 主要操作要能在單手範圍內完成，且不依賴隱性手勢。

## 區塊結構

### 1. Top App Bar

- 顯示文件情境名稱，例如 `Review`、頁數摘要、或 session 相關提示。
- 左側保留返回或離開入口，但回到哪裡要依流程語意決定，不混成單一 back 行為。
- 右側放次要動作，例如更多選單，避免把高頻文件操作塞進 overflow。

### 2. Main Preview

- 主預覽是畫面的第一焦點，永遠顯示目前選中的頁面。
- 預覽區要保留足夠視覺空間讓使用者檢查裁切、旋轉、增強效果與文件可讀性。
- 若頁面正在處理，主預覽仍保留位置，改以遮罩或 inline loading 呈現狀態，不整塊跳版。

### 3. Thumbnail Strip

- 縮圖列用來快速理解頁數、頁序與目前選中頁。
- 每個縮圖應可點選切換頁面。
- 縮圖選中態需明顯，避免多頁 session 下使用者迷失目前位置。
- 若頁數過多，縮圖列可水平捲動，並保持目前頁附近的上下文。

### 4. Page Tools

- 頁面工具只放與「目前頁」直接相關的操作。
- 建議包含增強切換、旋轉、刪除、復原入口，或其他頁級調整。
- 工具列的順序應以常用到少用排序，避免把 destructive action 放在最容易誤觸的位置。

### 5. Bottom Primary Action

- 底部主按鈕用來完成 session 層級的下一步。
- 單頁與多頁都應維持明確的 primary action，但文案可依情境不同而變化。
- 若存在多個高頻 session 動作，主按鈕只保留最核心的一個，其餘動作移至次要區或選單。

## 資訊優先順序

由高到低排序如下：

1. 目前頁面是什麼。
2. 目前頁面的內容是否可用、是否正在處理、是否有錯誤。
3. 縮圖列提供的頁序與快速切換能力。
4. 當前頁可執行的頁級操作。
5. session 層級的完成動作，例如 export、scan more、rescan、discard。

這個排序的原則是：先確保使用者知道自己在看哪一頁，再提供修改與完成的手段。

## 單頁與多頁版型差異

### 單頁模式

- 主預覽可佔據更高比例，讓使用者專注檢查單張文件品質。
- 縮圖列仍保留，但可弱化為補充資訊，不必占據過多垂直空間。
- 頁面工具的存在感可以較高，因為此時不需要頻繁跨頁切換。
- 主按鈕通常是直接完成 export，因為單頁流程最短。

### 多頁模式

- 主預覽仍為焦點，但縮圖列的重要性明顯提升。
- 縮圖列要能快速反映頁數、順序、刪除結果與目前選中頁。
- 頁面工具要避免侵佔主預覽過多空間，否則使用者會難以比較頁面內容。
- 主按鈕的文案要暗示這是一個 session 級完成動作，而不是單頁提交。

### 版型切換原則

- 當頁數從 1 變成多頁時，畫面重心應自動轉向「導覽與管理」。
- 當多頁 session 被刪到只剩單頁時，版型可以回收成單頁模式。
- 版型變化應盡量平滑，避免每次頁數改變都讓使用者覺得進入不同畫面。

## Loading / Error / Empty States

### Loading

- 初始載入或切頁處理中，主預覽保留骨架位置。
- 縮圖列可顯示漸進式載入，不阻塞整個 review 畫面。
- loading 狀態應回答「正在做什麼」，不是只顯示一個模糊 spinner。

### Error

- 單頁載入失敗時，主預覽應顯示該頁的失敗狀態與重試入口。
- 若 session 結構本身失敗，需提示使用者 session 可否恢復或是否應離開。
- 匯出失敗不應破壞目前 review 狀態，應保留頁面與操作上下文。

### Empty

- 空 session 不應強行維持 review 主流程。
- 若刪到空頁，應明確給出離開、重新掃描或放棄 session 的選項。
- Empty state 的重點是「下一步去哪裡」，不是只告知沒有內容。

## 互動流程

### Scan More

1. 使用者在 review 畫面點 `Scan More`。
2. 系統保留目前 session 與既有頁面。
3. 畫面切回 scanner，準備接下一輪掃描。
4. 新掃描結果回來後，append 到同一個 session。
5. 回到 review 時，預設仍可停在原本頁面或新加入頁面附近，避免使用者失去上下文。

### Rescan

1. 使用者選 `Rescan`。
2. 系統先確認這是破壞性操作。
3. 使用者確認後，捨棄目前 review session。
4. 流程回到 scanner，重新開始一次新的掃描。
5. 不保留原 session 的頁面、排序與暫存輸出。

### Discard Session

1. 使用者選 `Discard Session`。
2. 系統先確認這是破壞性操作。
3. 使用者確認後，清掉目前 session 的暫存與 review 狀態。
4. 流程回到 home 或 start screen。
5. 不自動重啟 scanner，讓使用者明確選擇下一步。

### Export

1. 使用者在底部主按鈕選擇 export。
2. 系統先根據 session 狀態判斷要 reuse 原始 PDF 還是重建輸出。
3. 匯出進行中時，畫面保留 current page 與頁面結構，不強制離開。
4. 匯出成功後顯示成功回饋與後續動作入口，例如 open、share、回到 scanner。
5. 匯出失敗時保留 review session，讓使用者可重試而不需要重新掃描。

## Wireframe Interaction Flow

```text
Review Screen
├─ Top App Bar
│  ├─ Back / leave
│  └─ More
├─ Main Preview
│  ├─ Current page image
│  ├─ Loading / error overlay
│  └─ Page index hint
├─ Thumbnail Strip
│  ├─ Page 1 thumbnail
│  ├─ Page 2 thumbnail
│  └─ ...
├─ Page Tools
│  ├─ Enhance toggle
│  ├─ Rotate
│  ├─ Delete
│  └─ Restore
└─ Bottom Primary Action
   ├─ Export
   ├─ Scan More
   ├─ Rescan
   └─ Discard Session
```

互動流的核心規則是：

- 主預覽永遠跟著目前頁。
- 縮圖列負責頁間移動。
- 頁面工具只影響目前頁。
- 底部主按鈕只處理 session 級下一步。

## 這一版草案的邊界

- 這份文件只定義 IA 與互動骨架，不決定最終視覺稿。
- 這份文件已足以支撐後續 wireframe、Compose layout 拆分與 review state 調整。
- 若後續加入排序、grid view 或 batch operation，應在不破壞上述資訊優先順序的前提下擴充。
