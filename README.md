# Pixel Scanner

![Android](https://img.shields.io/badge/Android-34-green?style=flat&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-purple?style=flat&logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=flat&logo=jetpackcompose)

Pixel Scanner is an Android document scanner built with Jetpack Compose and Google ML Kit. The current codebase is organized around session-owned files, a review-first editing flow, and explicit export decisions for JPEG and PDF output.

## What It Does

- Launches the ML Kit Document Scanner in `FULL` mode.
- Ingests scan results into app-owned session storage.
- Restores unfinished sessions when recoverable data is available.
- Supports review-time page thumbnails, delete/undo, scan more, discard/rescan, page reorder, and per-page enhancement toggles.
- Exports as JPEG or PDF with progress reporting, cache reuse, and post-export open/share/continue-scanning actions.
- Uses separate preview and export image-processing paths so review remains interactive while export favors correctness.

## Current Limits

- OCR is not implemented.
- Cloud sync is not implemented.
- Scan history and recent scans are not implemented.
- Batch actions such as apply-to-all and bulk selection are not in v1.
- Background export is deferred; long exports still run through the current foreground/domain flow.
- Large-document performance, Macrobenchmark, and Baseline Profile work are still in progress.

## Architecture

The project currently uses a single `app` module with package-level separation:

- `app/` contains app entry points, navigation, and manual dependency wiring.
- `feature/home`, `feature/scanner`, `feature/review`, and `feature/export` contain feature UI and ViewModel code.
- `domain/` contains use cases and repository interfaces.
- `core/model` contains shared domain and UI model types.
- `core/image` contains decoding and enhancement logic.
- `core/storage` contains the Android-backed repository implementations for MediaStore, session files, cleanup, and recovery.
- `core/diagnostics` contains debug logging and StrictMode helpers.
- `testing/` contains fakes used by unit tests.

The runtime entry flow is:

`MainActivity` -> `AppNavigation` -> home/scanner/review/export, with `AppContainer` providing manual constructor injection for the feature ViewModels and use cases.

## Requirements

- Android Studio with the current AGP / Kotlin toolchain in this repo.
- Android 10+ device or emulator.
- Google Play Services available on the test device or emulator for ML Kit Document Scanner.

Current toolchain values are:

- `minSdk` 29
- `compileSdk` 34
- `targetSdk` 34
- AGP `9.0.0`
- Kotlin `2.2.10`

## Build And Run

From the project root:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

To run on a connected device or emulator:

```bash
./gradlew installDebug
```

You can also open the project in Android Studio and run the `app` configuration directly.

## Roadmap

See [PRODUCTIZATION_TODO.md](./PRODUCTIZATION_TODO.md) for the active refactor backlog and [todo.md](./todo.md) for the older MVP-era checklist.

## License

MIT License. See [LICENSE](./LICENSE) for details.
