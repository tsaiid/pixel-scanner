# Advanced Hardware Acceleration Evaluation

## Status

Accepted as an evaluation decision. No GPU or NDK acceleration is implemented by this document.

## Context

Pixel Scanner currently uses a CPU-first preview enhancement path with row-based processing and a separate export path. The product already prefers a simpler software path unless the CPU route clearly fails to meet the target interaction model.

Supporting notes:

* [`docs/image-enhancer-memory-strategy.md`](/Users/tsaiid/git/pixel-scanner/docs/image-enhancer-memory-strategy.md)
* [`docs/preview-bitmap-hardware-decision.md`](/Users/tsaiid/git/pixel-scanner/docs/preview-bitmap-hardware-decision.md)
* [`docs/performance-baseline-blocker.md`](/Users/tsaiid/git/pixel-scanner/docs/performance-baseline-blocker.md)

## Decision

Keep the CPU path as the default implementation. Consider higher-cost acceleration only when the CPU path cannot satisfy the product gates below on real benchmark data.

### CPU Path Gates

The CPU path is acceptable if it can do all of the following:

* keep a single-page enhancement responsive enough that the user does not experience a blocked review flow,
* process a normal review session without forcing the UI into sustained jank or repeated cancellation,
* avoid OOM or runaway memory growth on large-image inputs,
* remain simple enough that the preview/export split stays maintainable,
* continue to work with the current session-owned cache model.

These are gates, not claimed measurements. The concrete numbers still depend on the blocked performance baseline work.

### ColorMatrix Evaluation

`ColorMatrix` can only cover simplified global transforms such as brightness, contrast, or coarse saturation-style changes.

It is not sufficient for the current enhancement algorithm because:

* the current algorithm is based on background estimation and per-pixel correction, not a single global color transform,
* document shadow reduction needs spatially varying correction rather than uniform matrix math,
* the existing CPU pipeline already depends on reading and writing pixels in software,
* using `ColorMatrix` alone would change the visual model enough that it would no longer be the same enhancement behavior.

Conclusion: `ColorMatrix` may be useful only as a limited fallback or for a future simplified mode, not as a replacement for the current enhancement path.

### Spike Trigger Conditions

Open a GPU, NDK, or other acceleration spike only if one or more of these becomes true after real measurement:

* CPU enhancement is too slow on the target device class for ordinary review use.
* CPU enhancement causes unacceptable memory pressure or OOM risk on representative samples.
* Large documents make the current CPU path too costly even with row-based processing and caching.
* The product needs a materially faster preview loop that cannot be achieved with decode, caching, or CPU algorithm tuning.

### Why Not Adopt High-Maintenance Paths by Default

Do not adopt GPU/NDK/obsolete acceleration paths by default because:

* they increase build, test, and release complexity,
* they introduce device- and ABI-specific failure modes,
* they are harder to keep aligned with the current Compose/session architecture,
* they risk solving a problem that may be addressable with the current CPU path plus measurement-driven tuning,
* older approaches such as deprecated rendering stacks are not worth introducing unless a benchmark-backed regression forces the issue.

## Consequences

* The default path remains easy to reason about and test.
* The team keeps the option to open a spike later without prematurely committing to a native or GPU architecture.
* The decision remains reversible once real benchmark data exists.

## Deferred

* No GPU, NDK, or shader-based acceleration is selected by this ADR.
* No performance numbers are asserted here because the measurement baseline is still blocked.
