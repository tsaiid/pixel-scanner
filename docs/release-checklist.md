# Release Checklist

This checklist is for the current Pixel Scanner release-prep state. It assumes the app still uses the session-owned scan flow, debug-only observability, and the current lint baseline strategy.

## Release Build Checks

- Confirm `git status` is clean except for intentional release-prep changes.
- Run `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` to confirm the main host-side build is healthy.
- Run `./gradlew lintDebug` and confirm it passes with `app/lint-baseline.xml` filtering only pre-existing debt.
- Run `./gradlew assembleRelease` if the release artifact is being prepared.
- Confirm release settings are intentional:
  - `versionCode` and `versionName` are updated.
  - Signing config is present for the intended release channel.
  - `release` minify/shrinker status matches the current release decision.
- If release minify is still disabled, record that explicitly as a release decision before shipping.

## QA Checks

- Start a fresh scan and confirm the scanner returns to review with the expected session contents.
- Verify the core review flow:
  - page selection
  - page delete and undo
  - page reorder
  - enhancement toggle
  - scan more
  - discard session
- Verify export outcomes:
  - clean session reuses the original PDF when available
  - modified session rebuilds correctly
  - JPEG export completes successfully
  - source-missing, permission-denied, and empty-session failures show the intended fallback actions
- Verify session recovery:
  - unfinished session can be restored
  - partial recovery still presents a usable session
- Verify basic device and locale behavior:
  - app opens on a real device
  - English and Traditional Chinese strings render correctly
  - export destinations appear in the expected gallery/documents locations
- Verify performance-sensitive paths at least once on a representative device:
  - session ingestion
  - preview enhancement
  - multi-page export

## Docs Update Checks

- Update README if the user-visible flow or supported behavior changed.
- Update any relevant ADR or design note if the release changed a previously documented decision.
- Update `PRODUCTIZATION_TODO.md` if any checklist item changed status during release prep.
- Update release notes or changelog with:
  - notable user-visible changes
  - known limitations
  - open follow-up work
- If a known release gap remains open, document it explicitly rather than leaving it implicit.

## Do Not Ship Until

- Host-side validation is green.
- Any intentional lint baseline debt is understood and accepted.
- Open release blockers are triaged and owned.
- The release decision does not depend on unresolved architecture work.
