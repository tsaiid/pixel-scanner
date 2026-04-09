# Phase 1 Status Convergence Evidence

This note records the evidence used to close selected Phase 1 items in `PRODUCTIZATION_TODO.md`.

## Navigation

- [`app/src/main/java/com/example/pixelscanner/app/AppNavigation.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/app/AppNavigation.kt) wires `Home` and `Review` through Navigation Compose, with explicit route transitions and reset-to-home behavior.
- [`app/src/main/java/com/example/pixelscanner/app/AppNavigationContract.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/app/AppNavigationContract.kt) defines the route contract and flow transitions.
- [`app/src/test/java/com/example/pixelscanner/feature/review/ReviewViewModelTest.kt`](/Users/tsaiid/git/pixel-scanner/app/src/test/java/com/example/pixelscanner/feature/review/ReviewViewModelTest.kt) covers restore-style recreation and back-effect behavior.
- [`app/src/test/java/com/example/pixelscanner/feature/home/HomeViewModelTest.kt`](/Users/tsaiid/git/pixel-scanner/app/src/test/java/com/example/pixelscanner/feature/home/HomeViewModelTest.kt) covers saveable navigation flag restoration.
- [`app/src/test/java/com/example/pixelscanner/app/AppFlowViewModelTest.kt`](/Users/tsaiid/git/pixel-scanner/app/src/test/java/com/example/pixelscanner/app/AppFlowViewModelTest.kt) covers pending export action restore.

## Feature / Module Split

- [`app/src/main/java/com/example/pixelscanner/app/AppNavigation.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/app/AppNavigation.kt) now only composes the flow entry points.
- [`app/src/main/java/com/example/pixelscanner/feature/home/HomeViewModel.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/home/HomeViewModel.kt)
- [`app/src/main/java/com/example/pixelscanner/feature/review/ReviewViewModel.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/review/ReviewViewModel.kt)
- [`app/src/main/java/com/example/pixelscanner/feature/export/ui/ExportUiState.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/export/ui/ExportUiState.kt)
- Core packages are separated under `core/model`, `core/image`, and `core/storage`, with Android implementations kept in the corresponding data-layer packages.

## Core Domain Model

- [`app/src/main/java/com/example/pixelscanner/core/model/ScanModels.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/core/model/ScanModels.kt) defines `ScanSession`, `ScannedPage`, `PageVariant`, `ExportRequest`, `ExportResult`, `ProcessingState`, and `SessionDirtyFlags`.
- [`app/src/test/java/com/example/pixelscanner/domain/ExportRequestDecisionTest.kt`](/Users/tsaiid/git/pixel-scanner/app/src/test/java/com/example/pixelscanner/domain/ExportRequestDecisionTest.kt) verifies export decision logic from domain state.
- [`app/src/test/java/com/example/pixelscanner/domain/CreateSessionFromScanResultUseCaseTest.kt`](/Users/tsaiid/git/pixel-scanner/app/src/test/java/com/example/pixelscanner/domain/CreateSessionFromScanResultUseCaseTest.kt) verifies session creation from scan results.

## UI State Structure

- [`app/src/main/java/com/example/pixelscanner/feature/home/HomeViewModel.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/home/HomeViewModel.kt) keeps home flow flags in `HomeUiState` plus `SavedStateHandle`.
- [`app/src/main/java/com/example/pixelscanner/feature/review/ui/ReviewUiState.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/review/ui/ReviewUiState.kt) centralizes review page selection, export state, and scan-more pending state.
- [`app/src/main/java/com/example/pixelscanner/feature/review/ReviewViewModel.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/review/ReviewViewModel.kt) routes review interactions through `ReviewUiEvent` / `ReviewUiEffect`.

## Remaining Gap

- The review screen still treats errors as effect-driven snackbar/message behavior. A dedicated error state split has not been introduced yet, so the boundary row stays at ` [~] `.
