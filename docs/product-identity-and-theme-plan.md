# Product Identity and Theme Plan

This note closes the Phase 6 identity/theme planning slice. It defines the v1 visual direction without replacing the current theme implementation yet.

## Brand Direction

Pixel Scanner v1 should read as a focused utility with one strong brand accent, not as a generic Material 3 demo.

### Primary Color

- Brand primary: deep plum `#541765`.
- This is already present in the current start screen and review actions, so it becomes the shared signature color across home, review, and export entry points.

### Supporting Colors

- Accent / selected state: lavender `#9B82FF`.
- Success / confirmation: soft light purple rather than a new hue family.
- Neutral surfaces: near-black on review and warm white on start, so the brand accent stays readable instead of competing with the background.

## Typography Strategy

Use the Material 3 type scale as the base system, with a restrained hierarchy:

- Large display style for the app name on home.
- Title and label styles for review headings, page state, and actions.
- Body style for helper copy and recovery prompts.

Do not introduce a separate display font family in v1. The goal is consistency and legibility across home, review, and export, not a decorative identity system.

## Icon Style

- Use filled Material icons / Material Symbols consistently.
- Keep icons single-weight and simple, with no mixed outline/fill style within the same screen.
- Use mirrored variants for directional icons where appropriate so the UI reads correctly in both layout directions.
- Reserve icon color changes for state, not decoration.

## Material 3 Token Direction

The theme should map the product identity into Material 3 tokens rather than hardcoding one-off colors in each screen.

### Token Mapping

- `primary` maps to the brand plum.
- `onPrimary` stays high-contrast white.
- `secondary` and selected-state accents map to lavender.
- `surface` and `background` support the current home/review split: warm light entry surface and dark review surface.
- `error` remains reserved for real failure states, not brand decoration.
- `onSurface` and `onBackground` stay tuned for readability first, especially for review thumbnails, page counters, and recovery copy.

### Implementation Direction

- Keep the brand palette centralized in theme tokens instead of scattering raw hex values through feature screens.
- Preserve a single identity across home and review, so the start screen and review screen feel like one product flow.
- Use Material 3 components and tokenized roles for color, typography, and iconography, even when the final tone is brand-led rather than fully dynamic.

## v1 Decision

For v1, Pixel Scanner should be brand-first and Material 3-based:

- one primary brand color,
- one accent color,
- one typography system,
- one icon style,
- and a shared token map across the app entry and review surfaces.

This is enough to unify the current product experience without committing to a full custom design system yet.
