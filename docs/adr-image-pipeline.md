# ADR: Image Pipeline

## Status

Accepted.

## Context

Pixel Scanner already separates preview and export workloads in code and in planning docs. Preview wants low-memory, fast page switching. Export wants correctness and full output quality. The two paths should not share one undifferentiated processing model.

Supporting notes:

* [`docs/image-enhancer-memory-strategy.md`](/Users/tsaiid/git/pixel-scanner/docs/image-enhancer-memory-strategy.md)
* [`docs/preview-bitmap-hardware-decision.md`](/Users/tsaiid/git/pixel-scanner/docs/preview-bitmap-hardware-decision.md)

## Decision

Maintain two pipelines:

* Preview pipeline: optimize for interactivity, page switching, and cache reuse.
* Export pipeline: optimize for export correctness and final-file generation.
* Keep separate request/result contracts and separate cache keys for preview and export.
* Use row-based CPU processing for preview enhancement to reduce transient memory pressure.
* Keep software bitmap handling for preview processing where pixel access and cache writes are needed.

## Consequences

* The preview path can stay responsive without forcing export to inherit preview-specific shortcuts.
* Memory behavior is easier to reason about because the preview pipeline no longer needs full-frame pixel arrays.
* Export can continue to prioritize output fidelity without being constrained by preview cache requirements.

## Deferred

* GPU acceleration is not part of this decision.
* `Bitmap.Config.HARDWARE` is not adopted for the current preview path.
* Tile-based or NDK-based optimization remains a future benchmark-driven option.
