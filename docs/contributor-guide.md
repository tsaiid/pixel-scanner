# Contributor Guide

This guide is for changes in the current Pixel Scanner repository state. The project is still a single `app` module, so contributions should follow the existing package boundaries instead of introducing new modules by default.

## Repository Shape

- `app/src/main/java/com/example/pixelscanner/app` contains app entry points, navigation, and manual wiring.
- `feature/home`, `feature/scanner`, `feature/review`, and `feature/export` contain feature UI and ViewModel code.
- `domain` contains use cases and repository interfaces.
- `core/model` contains shared domain and UI model types.
- `core/image` contains decoding and enhancement logic.
- `core/storage` contains Android-backed repository implementations for MediaStore, session files, cleanup, and recovery.
- `core/diagnostics` contains debug logging and StrictMode helpers.
- `testing` contains fakes used by unit tests.

If a change crosses boundaries, prefer moving the behavior to the lowest layer that can own it instead of adding more logic to UI code or a ViewModel.

## Naming Conventions

- Use intent-revealing names for use cases and repositories, such as `CreateSessionFromScanResultUseCase` or `AndroidSessionMetadataRepository`.
- Keep feature UI state names explicit, such as `HomeUiState` and `ReviewUiState`.
- Use `UiEvent` for user-driven inputs and `UiEffect` for one-off side effects.
- Use `ScanSession`, `ScannedPage`, and `ExportRequest` for domain-level work instead of ad hoc lists or raw `Uri` collections.
- Keep file names aligned with the primary class or function they contain.

## Pull Request Expectations

- Keep each PR scoped to one backlog slice or one clearly bounded refactor.
- Do not mix unrelated cleanup, feature work, and documentation changes in the same PR unless they are required for the slice.
- Update `PRODUCTIZATION_TODO.md` when the slice changes status or finishes.
- If a change alters user-visible behavior, update the relevant docs such as `README.md`, ADRs, or release notes at the same time.
- Prefer small, reviewable diffs over broad mechanical rewrites.

## Minimum Testing Expectations

- Run `./gradlew testDebugUnitTest` for host-side logic changes.
- Run the most targeted test task that covers the touched area, such as a focused `--tests` filter for unit work.
- Run `./gradlew lintDebug` when code changes touch production sources.
- If the change affects Android instrumentation behavior, run the relevant `androidTest` compile task and `connectedDebugAndroidTest` when a device or emulator is available.
- Add or update tests for the behavior you changed instead of relying only on manual verification.

## Practical Rules

- Keep UI behavior in feature packages; ViewModels own state and effects, while composables stay focused on rendering and user interaction.
- Keep Android framework dependencies in the Android implementation layer, not in shared model or domain code.
- Preserve the session-owned file model; do not reintroduce flows that depend on transient scanner URIs for review or export.
- Treat documentation as part of the change when it explains a decision, a constraint, or a release requirement.
