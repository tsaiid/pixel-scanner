# Review Empty and Error State Design

This note defines the empty/error states for the review and export flow without changing the current implementation.

## Scope

The review screen already has page-level enhancement failure feedback and export result messaging. This note separates the remaining empty/error cases so the UI can stay page-centric and recoverable.

## 1. No-Pages State

### When it appears

- The session exists, but the page list is empty.
- This can happen after deleting all pages or restoring into an empty session state.

### What the user sees

- A clear empty state instead of the normal preview + thumbnail layout.
- Copy that explains there are no pages in the session.
- One primary action that answers “what next?”

### Primary recovery actions

- `Scan More` if the flow can return to capture.
- `Discard Session` or leave the review flow if the user is done.
- If recovery is not possible, guide the user back to the start flow.

## 2. Single-Page Load Failure State

### When it appears

- A specific page fails to decode, render, or enhance while the rest of the session is still usable.
- This is the page-scoped failure path already used for enhancement retry, and it should cover other single-page failures in the same pattern.

### What the user sees

- The main preview stays on the failed page.
- The UI identifies the page number explicitly.
- The failure is shown as a local page problem, not a session-wide outage.

### Primary recovery actions

- `Retry` for that page.
- `Delete` or `Restore` if the page cannot be recovered.
- Move to another page via the thumbnail strip if the user wants to continue elsewhere.

## 3. Whole-Export Failure State

### When it appears

- Export fails after the user has completed review and triggered the session-level export action.
- This can be PDF rebuild failure, image export failure, or storage/write failure.

### What the user sees

- The current review state remains intact.
- Export feedback is shown as a session-level message or status card, not as a page failure.
- The message should say export failed and that the session is still available.

### Primary recovery actions

- `Retry Export`.
- Change the export format only if that is relevant to the failure path.
- Stay in review so the user can inspect or repair the pages before exporting again.

## 4. Partial-Page Failure State

### When it appears

- Some pages succeed and some fail during a session-level operation.
- The main target is future batch enhancement or other multi-page operations that do not complete uniformly.

### What the user sees

- A global progress result that distinguishes completed pages from failed pages.
- Page-level markers for the failed items so the user can identify them.
- The flow must not collapse into a single generic error message.

### Primary recovery actions

- Retry only the failed pages.
- Keep the completed pages intact.
- Allow the user to return to page-level editing without losing work.

## Design Rules

- Empty states should answer the next action, not just explain the lack of content.
- Page-scoped failures should remain page-scoped and never masquerade as a session failure.
- Session-scoped failures should keep the session available for retry or repair.
- Partial failures need both global progress context and per-page markers.

## Relationship to Current Flow

- Enhancement retry already demonstrates the page-scoped failure pattern.
- Export failure already demonstrates the session-scoped failure pattern.
- This design deliberately avoids introducing a new global error system before batch operations and large-document handling are stable.
