# Image Enhancer Memory Strategy

Current preview enhancement still uses a full-sized background bitmap and a full-sized output bitmap, but it no longer allocates full-frame pixel arrays.

Decision for this slice:

- Choose row-based processing for the CPU preview path.
- Keep the enhancement math per pixel and process one bitmap row at a time.

Why row-based over tile-based for now:

- The current enhancement is purely local per pixel, so a row buffer is enough.
- Row buffers are simpler than tiles and do not introduce edge overlap or seam handling.
- The main transient memory win comes from removing `width * height` pixel arrays; row processing achieves that directly.
- Tile-based processing is a better candidate only if we later need parallelism or hit row I/O overhead on very large images.

Residual work:

- Add memory instrumentation / benchmark points before marking the full memory-strategy backlog item complete.

Memory measurement notes:

- Preview enhancement now emits debug logs under the `ImageEnhancerMem` tag.
- Each run records stage snapshots for `start`, `decode-complete`, `background-build-complete`, `row-processing-complete`, and `final-write-complete`.
- The log line includes used heap, free heap, total heap, max heap, native heap, and delta values relative to the start snapshot.
- Read the delta values to compare stage-to-stage pressure; use the absolute values to compare different devices or input sizes.
- If the tag is not visible yet, enable it locally with `adb shell setprop log.tag.ImageEnhancerMem DEBUG`.
