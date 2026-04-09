# Coil Load Strategy

## Scope

This note only defines the main preview size requirement for the review screen.
Thumbnail sizing, request differentiation, and cache-behavior validation are deferred to later slices.

## Main Preview Requirement

The main preview request must target the actual on-screen preview viewport, not the original source bitmap size and not the thumbnail size.

Practical rule:

- Measure the drawable area of the main preview container after safe-drawing insets and surrounding chrome are accounted for.
- Use that viewport size as the Coil request size for the page preview image.
- Keep `ContentScale.Fit` behavior aligned with the viewport so Coil down-samples to the displayed area instead of decoding at source resolution.
- Never upsample purely to satisfy the preview request if the source image is smaller than the viewport.

Why this matters:

- The main preview is the highest-visibility Coil workload in `ReviewScreen`.
- Matching the request to the viewport avoids decoding and caching a larger bitmap than the user can see.
- The preview remains faithful to the current page layout while keeping the decode cost bounded by the actual display area.

## Deferred

- Thumbnail request sizing.
- Separate Coil request shapes for preview vs thumbnail.
- Memory-cache and disk-cache verification.
