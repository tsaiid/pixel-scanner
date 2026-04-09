# ReviewViewModel Responsibility Inventory

This note reflects the current state after the recent review-flow refactors.

## Still In ReviewViewModel

- Accept review UI events and route them to the correct action.
- Hold review screen state for:
  - current page selection
  - export format
  - exporting / processing flags
  - page list derived from the active session
- Persist saveable review state through `SavedStateHandle`.
- Convert `ScanSession` into `ReviewPageUiModel` for the UI.
- Emit UI effects for:
  - export success / failure messages
  - undo for deleted pages
  - navigation back to scanner or home
- Coordinate session mutations that are still UI-flow driven, such as:
  - page enhancement toggles
  - delete / restore
  - rescan / discard actions

## Already Extracted

- Session creation from scan results via `CreateSessionFromScanResultUseCase`.
- Enhancement toggling via `ToggleEnhancementUseCase`.
- Page deletion via `DeletePageUseCase`.
- Page restoration via `RestorePageUseCase`.
- Export orchestration via `ExportSessionUseCase`.
- Session discard via `DiscardSessionUseCase`.
- Image processing and export I/O behind repository boundaries:
  - `ImageProcessingRepository`
  - `ExportRepository`

## Still Intended For Future Extraction

- Load / resume session flow.
- Scan-more flow.
- Session lifecycle ownership outside the ViewModel.
- Broader repository boundaries for session files, temp files, cleanup, and recovery.
- Remaining error and progress state shaping once the review flow stabilizes further.
