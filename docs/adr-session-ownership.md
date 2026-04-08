# ADR: Session Ownership

## Status

Accepted.

## Context

Pixel Scanner receives transient ML Kit scan results, but the app needs stable ownership over review, recovery, cleanup, and export. The current code already persists session-owned files and metadata, and the ML Kit URIs are only reliable as ingestion inputs.

Supporting notes:

* [`docs/mlkit-result-persistence-fields.md`](/Users/tsaiid/git/pixel-scanner/docs/mlkit-result-persistence-fields.md)
* [`docs/navigation-graph.md`](/Users/tsaiid/git/pixel-scanner/docs/navigation-graph.md)

## Decision

The app owns the session lifecycle after scan return:

* Copy page JPEGs into app-owned session storage during eager ingestion.
* Copy the original PDF into the session directory when ML Kit provides one.
* Treat copied session file paths, session metadata, and derived dirty flags as the source of truth.
* Keep the raw ML Kit `content://` URIs as ingestion inputs only.
* Allow recovery, discard, cleanup, and export decisions to operate on the session-owned data, not on transient scanner output.

## Consequences

* Review can survive process death and temporary URI loss better than an ephemeral URI-based flow.
* Export can choose a fast path when the session is clean and the original PDF is available.
* Cleanup becomes explicit because the app owns the session root.
* The data model is more stable, but ingestion now has to copy files before later flows can rely on them.

## Deferred

* Cross-device sync and remote storage are not part of this decision.
* History/recent scans are not part of this decision.
* Any richer provenance tracking beyond current session metadata can wait for a later slice.
