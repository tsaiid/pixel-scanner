# Preview Bitmap Hardware Decision

Decision: do not use `Bitmap.Config.HARDWARE` for the current preview pipeline.

Reasoning:
- The preview pipeline performs CPU-side pixel inspection and transformation.
- `ImageEnhancer` needs mutable software access to source pixels and to the
  derived background/output buffers.
- The pipeline writes the transformed preview back to a session-owned cache file,
  so the software bitmap path is already dominated by decode and recomposition,
  not by copy avoidance.

Conclusion:
- Keep preview decode on `Bitmap.Config.RGB_565` for now.
- Revisit `HARDWARE` only if the preview pipeline is reworked to avoid CPU pixel
  reads and writes, or if profiling shows decode/copy cost is the bottleneck.
