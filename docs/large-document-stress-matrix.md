# Large Document Stress Matrix

This note records the repeatable synthetic stress scenarios used for the review flow when exercising long sessions.

## Scope

The goal is to keep a 50-page and 100-page session reproducible in Robolectric Compose tests so the review surface can be checked for scroll, reorder, and export regressions without depending on a physical device.

This is not a replacement for device-side ANR or memory validation. It only confirms that the current review flow still behaves correctly with large session counts.

## Scenarios

### 50-page session

- Build a synthetic `ScanSession` with 50 active pages and a dirty PDF export path.
- Scroll the thumbnail strip to the last page.
- Select the last thumbnail and confirm the selection label updates.
- Reorder the current page one step to the left.
- Export as PDF and confirm the export request includes all 50 active pages.

### 100-page session

- Build a synthetic `ScanSession` with 100 active pages and a dirty PDF export path.
- Scroll the thumbnail strip to the last page.
- Select the last thumbnail and confirm the selection label updates.
- Reorder the current page one step to the left.
- Export as PDF and confirm the export request includes all 100 active pages.

## Current Evidence

- [`ReviewLargeDocumentStressTest.kt`](/Users/tsaiid/git/pixel-scanner/app/src/test/java/com/example/pixelscanner/feature/review/ui/ReviewLargeDocumentStressTest.kt)
- [`ReviewThumbnailStripLargePageCountTest.kt`](/Users/tsaiid/git/pixel-scanner/app/src/test/java/com/example/pixelscanner/feature/review/ui/ReviewThumbnailStripLargePageCountTest.kt)

## Remaining Gap

- Physical-device confirmation for ANR, GC thrash, and peak-memory behavior is still blocked by the same execution limits called out in [`docs/performance-baseline-blocker.md`](/Users/tsaiid/git/pixel-scanner/docs/performance-baseline-blocker.md).
