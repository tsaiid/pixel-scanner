# Design Tokens

This note records the Phase 6 design-token slice.

Pixel Scanner now centralizes the current product UI constants into shared token groups:

- `PixelScannerColorTokens` for brand and semantic surface colors.
- `PixelScannerSpacingTokens` for layout, sizing, and touch targets.
- `PixelScannerRadiusTokens` and `PixelScannerShapeTokens` for thumbnail and pill shapes.
- `PixelScannerElevationTokens` for flat surfaces that should stay visually grounded.
- `PixelScannerTypographyTokens` for the app's current type roles.
- `PixelScannerMotionTokens` for the logo entrance animation.

Current product surfaces use those tokens directly:

- Home uses the brand surface, logo sizing, spacing, typography, and motion tokens.
- Review uses the shared colors, spacing, radius, elevation, and typography tokens for the action row, page status, and thumbnail strip.
- Export controls use the shared spacing and typography tokens.

This is intentionally narrow for v1. The goal is to remove scattered UI literals from the current flow before introducing a broader design system or more semantic variants.
