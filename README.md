# Pixel Scanner

![Android](https://img.shields.io/badge/Android-34-green?style=flat&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-purple?style=flat&logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=flat&logo=jetpackcompose)

**Pixel Scanner** is an Android document scanner built with Jetpack Compose and Google ML Kit. The current codebase focuses on a productized scan flow with session-owned files, review tools, and explicit export decisions.

## Current Status

### Completed

* ML Kit Document Scanner integration with `FULL` mode, JPEG and PDF results, and gallery import enabled.
* Session ingestion into app-owned storage, plus recovery for unfinished sessions.
* Review flow with page thumbnails, delete/undo, scan more, discard/rescan, and per-page enhancement toggles.
* Export flow for JPEG and PDF, including progress reporting, post-export open/share actions, and cache reuse.
* On-device image enhancement with a preview/export pipeline split.

### In Progress

* Page reordering.
* Benchmarking and memory/performance validation for large documents.
* Background export strategy for long-running jobs.
* Accessibility polishing and large-page-count UX hardening.

### Planned

* OCR.
* Cloud sync.
* Scan history / recent scans.
* Batch actions such as apply-to-all, grid selection, and bulk delete.

## Tech Stack

* Kotlin 2.2.x
* Jetpack Compose Material 3
* Google ML Kit Document Scanner API
* Kotlin Coroutines and Flow
* Coil for image loading
* Scoped Storage via MediaStore

## Development Environment

* Android Studio with the current stable Android Gradle Plugin / Kotlin toolchain
* `minSdk` 29
* `compileSdk` 34
* `targetSdk` 34
* Gradle Kotlin DSL

## Build & Run

1. Clone the repository.
2. Open the project in Android Studio.
3. Wait for Gradle sync to complete.
4. Run on an Android 10+ device or emulator with Google Play Services available for ML Kit.

## Roadmap

See [PRODUCTIZATION_TODO.md](./PRODUCTIZATION_TODO.md) for the active refactor backlog and [todo.md](./todo.md) for the older MVP-era checklist.

## License

MIT License. See [LICENSE](./LICENSE) for details.
