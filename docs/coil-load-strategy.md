# Coil Load Strategy

## Scope

This note defines the review screen's Coil request shape for both the main preview and the thumbnail strip.

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
- A simple solid placeholder keeps the preview area stable while the request resolves.
- The preview remains faithful to the current page layout while keeping the decode cost bounded by the actual display area.

## Thumbnail Requirement

Thumbnail requests must target the thumbnail box size, not the preview viewport and not the source bitmap size.

Practical rule:

- Use the review thumbnail token size as the request size, currently `64dp x 86dp`.
- Keep `ContentScale.Crop` so the thumbnail stays visually consistent inside the rounded frame.
- Use a separate request builder from the main preview so the thumbnail strip does not inherit viewport-sized decode work.

Why this matters:

- Thumbnails are repeated across page sets, so oversizing them multiplies decode and cache cost.
- A fixed thumbnail size keeps the strip stable even when the main preview viewport changes.
- A solid placeholder keeps each thumbnail slot readable while the request resolves.

## Cache Behavior

Both preview and thumbnail requests should leave Coil's memory cache and disk cache enabled.

Validation rule:

- Confirm the request builders keep `memoryCachePolicy = ENABLED` and `diskCachePolicy = ENABLED`.
- Rely on the differing request sizes to produce distinct cache entries for preview and thumbnail workloads.
- Keep the request builders separate so the main preview can evolve independently from the thumbnail strip.
