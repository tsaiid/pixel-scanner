# Product Scope V1

This document sets the v1 scope for Pixel Scanner. It is intentionally narrow and based on the current implementation plus the active backlog.

## V1 Must-Have

These capabilities are required for v1:

* Scan with ML Kit and return to the app review flow.
* Ingest scan results into app-owned session storage.
* Recover or discard an unfinished session.
* Review pages with thumbnails and page counter.
* Delete and restore pages.
* Scan more into the current session.
* Toggle enhancement on a per-page basis.
* Export to JPEG and PDF.
* Reuse the original PDF when the session is clean and the source PDF is available.
* Show clear post-export actions such as open, share, or continue scanning.

## V1 Should-Have

These items improve the experience but are not required to ship the first version:

* Page-level status feedback for enhancement and export.
* Export progress reporting for multi-page work.
* Basic cleanup of stale session and temporary files.
* Clearer empty, error, and recovery states.
* Accessibility cleanup for labels, focus order, and touch targets.
* Better naming and copy for destructive actions.

## Not In V1

These items are explicitly out of scope for v1:

* OCR text extraction.
* Cloud sync or account-backed storage.
* Scan history or recent scans.
* Incremental PDF reassembly as a required shipping path.
* Batch operations such as apply-to-all, bulk delete, or grid-based selection.
* Full page reordering as a v1 requirement.
* Any feature that depends on a new product data model or backend service.

## Decision List

Use the following decisions for disputed items:

### OCR

Decision: not in v1.

Reason: the current product focuses on scan, review, enhancement, and export. OCR adds a separate product surface and does not unblock the core scan flow.

### Sync

Decision: not in v1.

Reason: the app currently owns local session files and local export behavior. Sync adds account, conflict, and recovery design that is outside the current backlog.

### History / Recent Scans

Decision: not in v1.

Reason: the current start flow is a single entry point plus recoverable unfinished session prompt. History can be added later as a separate product surface.

### Incremental PDF Reassembly

Decision: not required for v1.

Reason: the backlog already treats this as a separate evaluation item. v1 may continue to rely on the current export decision tree and PDF rebuild/reuse strategy.

### Batch Operations

Decision: not in v1.

Reason: apply-to-all, grid view, and bulk delete should wait until the review state model and large-document behavior are stable.

## Scope Guardrails

Do not promote a feature into v1 unless it meets all of the following:

* It is already implemented, or the backlog has a concrete near-term slice for it.
* It does not require a new backend or account model.
* It does not depend on unresolved batch or large-document interaction design.
* It can be explained in one sentence in the start, review, or export flow.
