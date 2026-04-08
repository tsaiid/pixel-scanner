# ADR: Background Export Strategy

## Status

Deferred.

## Context

The product backlog requires a decision about long-running export work, but the repo does not yet contain a background worker implementation. The current export path is a foreground suspend use case with progress callbacks and cache reuse. That is sufficient for the current slice, but not a full solution for very long exports.

Supporting notes:

* [`docs/performance-baseline-measurement-and-samples.md`](/Users/tsaiid/git/pixel-scanner/docs/performance-baseline-measurement-and-samples.md)
* [`docs/di-decision-current-milestone.md`](/Users/tsaiid/git/pixel-scanner/docs/di-decision-current-milestone.md)

## Decision

Do not introduce `WorkManager` yet.

Current behavior stays as follows:

* Export runs in the existing foreground/domain flow.
* Progress is reported through the current export state model.
* Cleanup and cache handling stay in the existing session/storage layer.

Background export will only be introduced when there is a clear trigger such as:

* benchmark evidence that the foreground path is not acceptable for the target document sizes,
* a clear need to survive app backgrounding for long exports,
* or a product decision that long export jobs must continue after process or UI interruption.

## Consequences

* The current export flow stays simpler and easier to test.
* Very long exports may still be vulnerable to foreground lifecycle limits until a future implementation lands.
* The team avoids adding WorkManager complexity before the threshold and failure mode are measured.

## Deferred

* Worker design, foreground notification behavior, retry policy, and restart semantics are explicitly deferred.
* This ADR does not claim that background export is implemented.
