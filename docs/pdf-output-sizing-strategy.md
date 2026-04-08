# PDF Output Sizing Strategy

## Status

Accepted as a v1 policy. No user-facing sizing picker is exposed by this decision.

## Context

The current rebuild path writes page bitmaps into `PdfDocument` pages using the bitmap's own dimensions. That means the PDF rebuild path already behaves as a fixed strategy rather than a configurable output-size system.

Supporting notes:

* [`docs/adr-export-strategy.md`](/Users/tsaiid/git/pixel-scanner/docs/adr-export-strategy.md)
* [`docs/incremental-pdf-reassembly-evaluation.md`](/Users/tsaiid/git/pixel-scanner/docs/incremental-pdf-reassembly-evaluation.md)
* [`docs/product-scope-v1.md`](/Users/tsaiid/git/pixel-scanner/docs/product-scope-v1.md)

## Decision

Use a fixed PDF sizing strategy for v1:

* Do not expose a user-facing sizing picker in v1.
* Rebuild PDF pages at the active image dimensions supplied by the export path.
* Do not add a separate PDF page scaling mode such as `original size`, `A4`, or `Letter` for v1.
* Keep the rebuild path deterministic so delete and reorder scenarios remain easy to reason about.

## Page Scaling Rules

For v1 rebuilds:

* Use the source bitmap width and height as the PDF page size.
* Do not downscale the page just to fit a paper preset.
* Do not upscale pages that are smaller than the source image.
* Preserve the page order and content as provided by the current session state.

This matches the current rebuild implementation direction and avoids introducing a second sizing policy that the UI does not yet surface.

## JPEG Quality Policy

For PDF rebuild paths, no separate JPEG compression-quality control is exposed in v1.

Reason:

* The current PDF rebuild path renders bitmaps into PDF pages directly.
* The rebuild path is a PDF composition path, not a JPEG export setting path.
* Adding a user-facing compression slider now would imply a larger product decision and a UI surface that does not exist yet.

Policy:

* Keep the PDF rebuild path fixed and policy-driven.
* If a future export path needs JPEG compression tuning, treat it as a separate decision rather than a hidden side effect of PDF rebuild.

## Tradeoff For V1

The v1 tradeoff is:

* prioritize correctness, determinism, and implementation simplicity,
* accept that file size may be larger than a more aggressively scaled or compressed strategy,
* avoid premature tuning knobs until the product has benchmark evidence and a clear user need.

This keeps the export path aligned with the current session-owned model and the rebuild-based decision tree.

## Consequences

* Users get a consistent export result without needing to choose sizing options.
* The app avoids a quality-versus-size UI that is not yet backed by measurements.
* A future v2 can add paper presets or compression controls if benchmarking shows a real need.

## Deferred

* No sizing picker is planned for v1.
* No A4/Letter/original-size choice is exposed in v1.
* Any future compression or scaling controls require a separate product decision.
