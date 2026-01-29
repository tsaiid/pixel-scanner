# Pixel Scanner

![Android](https://img.shields.io/badge/Android-16-green?style=flat&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple?style=flat&logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=flat&logo=jetpackcompose)
![Pixel Ready](https://img.shields.io/badge/Pixel-10%20Pro%20XL-blue?style=flat&logo=google)

**Pixel Scanner** 是一款專為 Google Pixel 10 Pro XL (Android 16 / Tensor G5) 量身打造的原生文件掃描應用程式。

目標是復刻並超越 iOS "Evernote Scannable" 的流暢體驗，利用 Google ML Kit 的強大邊緣運算能力，實現極速啟動、自動邊緣偵測與高品質的文件數位化。

## ✨ 核心特色 (Features)

*   **極速掃描 (Instant Capture)**: 深度整合 **Google ML Kit Document Scanner API**，提供毫秒級的自動文件偵測、透視校正 (Perspective Correction) 與自動裁切。
*   **智慧影像增強 (Smart Enhancement)**:
    *   內建自定義 **Simple Lightness Equalization** 演算法，有效移除文件上的陰影並平衡亮度。
    *   提供「原圖」與「增強模式」的一鍵無縫切換預覽。
*   **隱私優先 (Privacy First)**: 所有的影像處理與 AI 運算皆在 **裝置端 (On-device)** 完成，無需上傳雲端，確保醫療或個人敏感資料的安全。
*   **批量處理 (Batch Mode)**: 支援連續拍攝多頁文件，並可事後自由排序或刪除。
*   **靈活輸出 (Flexible Export)**: 支援高解析度 JPEG 儲存至系統相簿，或合併為 PDF 文件分享。

## 🛠 技術架構 (Tech Stack)

本專案採用現代化 Android 開發規範 (MAD) 建構：

*   **語言**: Kotlin
*   **UI 框架**: Jetpack Compose (Material 3 Design System)
*   **核心引擎**: Google ML Kit Document Scanner API
*   **非同步處理**: Kotlin Coroutines & Flow
*   **圖片加載**: Coil (Memory-efficient image loading)
*   **架構模式**: MVVM (Model-View-ViewModel) + Unidirectional Data Flow
*   **儲存**: Android Scoped Storage (MediaStore API)

## 📱 開發環境 (Development Environment)

*   **IDE**: Android Studio Ladybug (or newer)
*   **Language**: Kotlin 2.0+
*   **Min SDK**: 29 (Android 10)
*   **Target SDK**: 35/36 (Android 16)
*   **Build System**: Gradle (Kotlin DSL)

## 🚀 安裝與執行 (Build & Run)

1.  Clone 此專案到本地：
    ```bash
    git clone https://github.com/tsaiid/pixel-scanner.git
    ```
2.  使用 Android Studio 開啟專案資料夾。
3.  等待 Gradle Sync 完成。
4.  連接實體 Pixel 裝置或啟動 Android Emulator (建議 API 34+)。
5.  點擊 **Run** (Shift+F10)。

> **注意**: 由於使用了 ML Kit，初次執行時若使用模擬器，請確保模擬器已安裝 Google Play Services。

## 📅 開發路徑 (Roadmap)

請參考 [todo.md](./todo.md) 查看詳細的開發進度與待辦事項。目前已完成核心 MVP 功能，包含相機掃描、自定義去陰影演算法與檔案儲存。

## 📄 授權 (License)

MIT License. See [LICENSE](./LICENSE) for more details.

---
Designed with ❤️ by [I-Ta Tsai (蔡依達)](https://github.com/tsaiid)
