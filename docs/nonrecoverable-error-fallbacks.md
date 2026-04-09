# Nonrecoverable Error Fallbacks

This note tracks the current nonrecoverable error cases in the review/export flow and the expected fallback CTA for each case.

## Current Cases

- `ExportSourceInvalidFailure`
  - Meaning: one or more session-owned assets are no longer readable, so the current export cannot be repaired in-place.
  - User-facing fallback: `Discard & Rescan`.
  - Current status: implemented in review export controls as a session-level CTA.

- `ExportPermissionDeniedFailure`
  - Meaning: the destination write path cannot proceed because storage permission or resolver access is denied.
  - User-facing fallback: remain in review and surface a permission-specific recovery action once the destination policy is finalized.
  - Current status: not yet implemented.

- `ExportNoPagesFailure`
  - Meaning: the session has no active pages left to export.
  - User-facing fallback: return to scanning or discard the empty session.
  - Current status: review empty-state handling exists, but no dedicated export fallback CTA is exposed.

## Design Constraints

- Nonrecoverable errors should not offer a blind retry CTA.
- The fallback must make the destructive cost clear before discarding session data.
- Session-scoped fallbacks should keep the user in review unless the only valid recovery is to restart scanning.
