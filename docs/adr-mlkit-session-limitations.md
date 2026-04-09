# ADR: ML Kit Session Limitations

## Status

Accepted.

## Context

Pixel Scanner uses the ML Kit Document Scanner as a transient capture source, but the app cannot rely on ML Kit URIs or result metadata as long-lived product state. The current scanner API surface is intentionally small, and the app already copies scan results into session-owned storage during ingestion.

Supporting notes:

* [`docs/mlkit-result-persistence-fields.md`](/Users/tsaiid/git/pixel-scanner/docs/mlkit-result-persistence-fields.md)
* [`docs/adr-session-ownership.md`](/Users/tsaiid/git/pixel-scanner/docs/adr-session-ownership.md)

## Decision

Treat ML Kit scan output as input only:

* Use the returned page JPEG URIs and optional PDF URI as ingestion sources.
* Copy scan results into app-owned session storage before review, recovery, or export depend on them.
* Persist only the fields the app actually uses today: page order, copied file paths, original PDF path when present, and derived session metadata.
* Do not depend on extra ML Kit metadata that is not exposed in the current API surface.

## Consequences

* Review and export no longer depend on transient scanner URIs.
* Session recovery can work from app-owned files even if the original scanner output disappears.
* The app keeps a smaller and more predictable persistence surface, which reduces product drift when ML Kit changes.

## Deferred

* OCR text, page polygons, crop geometry, confidence scores, and other richer scanner metadata are not part of the current product contract.
* Any future scanner metadata usage must be justified by an actual app flow, not by API availability alone.
