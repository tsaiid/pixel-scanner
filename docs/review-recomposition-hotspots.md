# ReviewScreen Recomposition Hotspots

This note records the first pass of the `Compose` recomposition check for `ReviewScreen`.

## What Was Inspected

- [`app/src/main/java/com/example/pixelscanner/feature/review/ui/ReviewScreen.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/review/ui/ReviewScreen.kt)
- [`docs/remember-state-audit-phase1.md`](/Users/tsaiid/git/pixel-scanner/docs/remember-state-audit-phase1.md)

## Main Hotspots

1. `uiState.collectAsState()` at [ReviewScreen.kt:119](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/review/ui/ReviewScreen.kt#L119)
   - This is the broadest invalidation source. Any `ReviewUiState` change recomposes the whole screen tree, including the top action row, pager area, and thumbnail strip.
   - In practice, page selection, export progress, and processing state updates all flow through the same top-level state object.

2. `LaunchedEffect(uiState.currentPage, uiState.pages.size)` at [ReviewScreen.kt:162](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/review/ui/ReviewScreen.kt#L162)
   - This effect depends on the same state that drives the pager and thumbnail strip.
   - When page count or selection changes, the whole review surface is already invalidated, and this effect adds extra pager synchronization work on top.

3. The main review content column starting at [ReviewScreen.kt:173](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/review/ui/ReviewScreen.kt#L173)
   - The top action row, centered status/actions block, export controls, pager, and thumbnail strip all live under one recomposing parent.
   - `uiState.export.isExporting`, `uiState.isProcessing`, `uiState.currentPage`, and `uiState.pages` are all read in the same composition scope.

4. The pager subtree at [ReviewScreen.kt:421](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/review/ui/ReviewScreen.kt#L421)
   - The `HorizontalPager` page content reads `uiState.pages.size` and `page.processingState`, so page selection or processing changes recompose the active page subtree.
   - `AsyncImage` and the inline progress indicator are both inside that subtree, so page-level state changes remain expensive.

5. The thumbnail strip at [ReviewScreen.kt:448](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/review/ui/ReviewScreen.kt#L448)
   - It receives the full page list and current page index directly.
   - Any page mutation or selection change forces the strip to re-evaluate all visible thumbnails and selection styling.

## Conclusion

The highest-frequency recomposition source is the screen-wide `uiState` collection at the top of `ReviewScreen`. The next most expensive areas are the pager page subtree and the thumbnail strip, because they are tied to `pages`, `currentPage`, and `processingState` updates that happen during normal review interactions.

This slice intentionally stops at identification. The next step should be to split the screen into smaller state-driven subcomposables or derived state slices, but that is outside the scope of this slice.
