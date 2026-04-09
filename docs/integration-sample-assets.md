# Integration Sample Assets

This repository uses a deterministic, generated sample asset catalog for integration-style tests.

## Why This Exists

- Keep the repo lean instead of checking in a large photo corpus.
- Give tests stable page counts and file names that can be referenced directly.
- Cover the main capture shapes used by review, export, and stress scenarios without relying on network downloads.

## Catalog Entry Points

- [`SampleAssetCatalog.suite(context)`](/Users/tsaiid/git/pixel-scanner/app/src/test/java/com/example/pixelscanner/testing/sampleassets/SampleAssetCatalog.kt)
- [`SampleAssetCatalog.reviewStressSession(context, pageCount)`](/Users/tsaiid/git/pixel-scanner/app/src/test/java/com/example/pixelscanner/testing/sampleassets/SampleAssetCatalog.kt)

## Generated Coverage

- Single clean page: `pbm-sp-1p-1600x2200-orig.jpg`
- Multi-page mixed sets: `pbm-mp-6p-1200x1600-mixed`, `pbm-mp-12p-1200x1600-mixed`, `pbm-mp-24p-1200x1600-mixed`
- Low-light page: `pbm-lowlight-1p-1200x1600-low.jpg`
- Shadow page: `pbm-shadow-1p-1200x1600-shadow.jpg`
- Rotated page: `pbm-rot-1p-1200x1600-rot.jpg`
- High-resolution page: `pbm-large-1p-2400x3600-orig.jpg`
- Long-document sessions: `pbm-long-48p-900x1200-mixed`, `pbm-long-72p-900x1200-mixed`

## Storage Convention

- Assets are generated under `cacheDir/integration-sample-assets`.
- Multi-page sets reuse a deterministic rendered cover page so page counts remain stable without requiring a large binary corpus.
- Tests should reference the catalog helper instead of hardcoding ad hoc file generation.

## Scope

- These fixtures are for integration tests and repeatable stress scenarios.
- They are not a substitute for real-device performance measurements.

