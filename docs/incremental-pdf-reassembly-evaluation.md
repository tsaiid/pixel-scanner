# Incremental PDF Reassembly Evaluation

## Status

Accepted as a v1 evaluation decision. No incremental PDF reassembly is implemented by this document.

## Context

The current export strategy already handles modified sessions by rebuilding the PDF rather than trying to surgically edit it. That keeps the export path aligned with the current session-owned model and the existing decision tree.

Supporting notes:

* [`docs/adr-export-strategy.md`](/Users/tsaiid/git/pixel-scanner/docs/adr-export-strategy.md)
* [`docs/product-scope-v1.md`](/Users/tsaiid/git/pixel-scanner/docs/product-scope-v1.md)
* [`docs/performance-baseline-blocker.md`](/Users/tsaiid/git/pixel-scanner/docs/performance-baseline-blocker.md)

## Options Considered

### Full Rebuild

Rebuild the PDF from the current active pages.

Pros:

* simplest to keep aligned with delete, reorder, and enhancement state,
* matches the current export decision tree,
* works with the app-owned session model,
* easier to test and reason about on Android.

Cons:

* can be slower for large documents,
* may re-encode work that did not actually change,
* can lose source-PDF metadata that a rewrite library might preserve.

### Incremental PDF Manipulation

Use a library or lower-level PDF writer to edit page order or remove pages in-place or with minimal rewrite.

Pros:

* can avoid full rebuild work for some delete/reorder cases,
* may preserve more original PDF structure in some implementations.

Cons:

* higher integration and maintenance cost,
* more library-specific behavior to validate,
* Android fit varies by library and often adds binary size and compatibility risk,
* the delete/reorder benefit is not guaranteed to outweigh the complexity for v1.

### Defer to v2

Keep the current rebuild-based behavior and revisit incremental reassembly only if future benchmarks or customer needs justify it.

Pros:

* preserves the current simple and testable export path,
* avoids premature library adoption,
* lets the team prioritize session ownership, review UX, and performance measurement first.

Cons:

* delete/reorder PDFs may continue to be rebuilt rather than surgically edited,
* any performance gain from incremental editing is postponed.

## Third-Party Library Tradeoffs

If the team later revisits incremental reassembly, evaluate any third-party PDF library against these criteria:

* app size impact
* performance on real delete/reorder samples
* licensing compatibility
* Android API fit and compatibility risk
* maintenance burden and project longevity

Why this matters:

* PDF libraries often solve one narrow problem but add long-term dependency risk.
* A library that looks attractive in a spike can become a release liability if it is hard to debug or if it does not fit Android storage and export flows cleanly.
* The current repo already has a working rebuild path, so any replacement must beat that bar clearly.

## V1 Decision

Do not adopt incremental PDF reassembly for v1.

Continue with the existing rebuild-based behavior for delete and reorder scenarios, while preserving the decision tree that already reuses the original PDF when the session is clean.

## Consequences

* v1 stays aligned with the current export architecture.
* Delete/reorder cases remain deterministic even if they are not the most optimal possible implementation.
* The team keeps a clear future option to open a separate spike if real benchmarks or customer pressure justify a more advanced PDF rewrite strategy.

## Deferred

* Incremental PDF reassembly moves to a future evaluation or v2 spike if needed.
* No third-party PDF library is adopted by default from this decision.
