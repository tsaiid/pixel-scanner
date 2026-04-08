# Phase 1 Remember-State Audit

Scope audited:

- `app/src/main/java/com/example/pixelscanner/**`

Findings:

- `feature/home/ui/StartScreen.kt`
  - `startAnimation` already uses `rememberSaveable`.
  - This is intro animation state only and is not flow-critical.
- `feature/review/ui/ReviewScreen.kt`
  - `showRescanConfirmDialog` and `showDiscardConfirmDialog` already use `rememberSaveable`.
  - `snackbarHostState` and `pagerState` are ephemeral UI state and are not flow-persistence concerns.
- `feature/scanner/ui/ScannerScreen.kt`
  - `scannerManager` is created with `remember` because it is a screen-scoped helper, not saved state.

Conclusion:

- No remaining flow-critical composable `remember` state was found that needs migration.
- The flow-critical state remains covered by `HomeViewModel`, `ReviewViewModel`, and `AppFlowViewModel` via `SavedStateHandle`.
