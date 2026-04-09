# Non-Invasive Error Stats Strategy

## Status

Accepted for the current milestone.

## Context

Pixel Scanner already has a debug-only observability policy and timing logs for the main scan, enhancement, and export paths. The remaining gap is a way to understand failure frequency without collecting user data or introducing a telemetry dependency too early.

Supporting note:

* [`docs/debug-logging-policy.md`](/Users/tsaiid/git/pixel-scanner/docs/debug-logging-policy.md)

## Decision

Keep error statistics local and debug-only for this milestone.

The app may record anonymous counters or coarse event records in memory or local debug logs, but it must not upload them or persist them as user-facing analytics data. The strategy is meant to support local diagnosis, not release analytics.

### Anonymous Event Model

Use coarse, non-identifying event buckets such as:

* `scan_failure`
* `ingestion_failure`
* `enhancement_failure`
* `export_failure`

Each event may carry only high-level classification fields such as:

* flow name
* failure category
* retryability
* format or pipeline type
* coarse page-count bucket

### Sensitive Data We Must Not Collect

Do not collect raw source URIs, filesystem paths, filenames, document contents, OCR text, user-entered annotations, or any exact per-document payload that could identify a scan session.

Do not collect stable user identifiers, account identifiers, or precise session artifacts that would let a local failure record be correlated back to a specific document.

### Telemetry Position

Telemetry is deferred.

If the product later needs aggregated failure reporting, that should be introduced as a separate, privacy-reviewed slice with an explicit transport decision. This milestone stays local-debug-only.

## Consequences

* Failure frequency can be inspected during local development without changing product scope.
* The app avoids accidental analytics coupling before there is a clear privacy and release requirement.
* Any future telemetry work will need a new ADR or strategy update instead of growing implicitly from debug logging.
