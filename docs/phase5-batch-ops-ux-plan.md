# Phase 5 Batch Operations UX Plan

This note closes the Phase 5 backlog item for batch-ops UX planning without committing the app to batch execution behavior yet.

## Decision Summary

- `Apply to All` is milestone-gated, not part of the current v1 interaction set.
- The review screen stays page-centric in v1: current page state, thumbnail strip navigation, and per-page actions remain the primary model.
- Batch enhancement and bulk delete are deferred until the review state model is stable enough to support non-blocking, multi-page operations.

## What v1 Can Support

- Page-level enhancement toggle with clear processing and failure feedback.
- Thumbnail-strip navigation for large documents.
- Per-page delete, restore, reorder, and export order preservation.

This is enough for a usable large-document review flow without introducing multi-page coordination UI.

## What Is Deferred

### Apply to All

`Apply to All` should be treated as a later milestone-gated action because it depends on a page-queue model that can touch multiple pages without freezing the review screen.

### Batch Enhancement

If batch enhancement is added later, it needs:

- A non-blocking queue that can process pages one at a time or in bounded parallelism.
- A global progress model that reports total pages, completed pages, failed pages, and cancellability.
- Page-level status objects so the user can still tell which page is processing or failed.

### Bulk Delete

Bulk delete is not a good v1 target unless there is a clear multi-select interaction model. Without that, the operation risks being ambiguous and too destructive for the current review surface.

### Grid View

Grid view is not required for v1. The current thumbnail strip already covers:

- large page-count navigation,
- direct page selection,
- local context around the current page.

Grid view only becomes worth the added complexity if the strip stops being sufficient for:

- jumping across very large documents,
- identifying ranges of pages quickly,
- selecting many pages for delete or batch enhancement.

## Required Prerequisites

These dependencies should exist before batch operations are promoted:

- Page-level processing state, including success, failure, and retry affordances.
- A non-blocking queue for long-running page work.
- Stable reorder state so batch actions do not fight with page order changes.
- Explicit export-order rules so the output sequence always matches the current session order.

## Recommendation

Keep Phase 5 focused on making the single-page review model resilient enough that batch operations can be added later without redesigning the screen. When the prerequisites above are met, `Apply to All` can move from milestone-gated to implementation-ready.
