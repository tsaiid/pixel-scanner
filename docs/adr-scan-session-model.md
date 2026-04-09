# ADR: Scan Session Model

## Status

Accepted.

## Context

Pixel Scanner needs a session model that can survive review, recovery, cleanup, and export without depending on the ephemeral scanner result. The codebase already uses a session-owned aggregate with pages, dirty flags, metadata, and app-owned file paths.

Supporting notes:

* [`docs/adr-session-ownership.md`](/Users/tsaiid/git/pixel-scanner/docs/adr-session-ownership.md)
* [`docs/adr-export-strategy.md`](/Users/tsaiid/git/pixel-scanner/docs/adr-export-strategy.md)
* [`docs/mlkit-result-persistence-fields.md`](/Users/tsaiid/git/pixel-scanner/docs/mlkit-result-persistence-fields.md)

## Decision

Model a scan session as an app-owned aggregate:

* `ScanSession` is the source of truth for review and export.
* `ScannedPage` holds the stable page identity, original source URI, optional enhanced URI, processing state, and deleted state.
* `SessionDirtyFlags` captures whether the session has been modified in ways that affect export routing or cleanup.
* Active page order is derived from the session page list, with deleted pages filtered out.
* Session metadata should carry the session id, creation time, page count, original PDF presence, and dirty flags.

## Consequences

* The UI can reason about pages, page state, and export behavior from a single domain object instead of ad hoc lists.
* Reorder, delete, enhancement, and scan-more all become explicit session mutations instead of scattered UI state.
* Export decisions can inspect the session without re-reading scanner output.
* Recovery and stale-session cleanup can operate on a compact metadata snapshot.

## Deferred

* OCR, page history, tags, and batch-edit annotations are not part of the current session contract.
* If the session model needs to widen later, it should happen through a new ADR-backed slice rather than by implicit field growth.
