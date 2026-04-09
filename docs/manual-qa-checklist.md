# Manual QA Checklist

Use this checklist before a release or a tagged internal build. It is scoped to the current Pixel Scanner flow: `Start Scanning` from home, review-based editing, and export through `Save as JPEG` / `Save as PDF`.

## Home

- Launch the app from a cold start and confirm the home screen loads without layout overlap.
- Verify the primary action reads `Start Scanning`.
- If an unfinished session exists, confirm the restore dialog appears with the expected `Restore` and `Discard` actions.
- Confirm `Restore` returns to the recoverable session and `Discard` leaves no resume prompt on the next launch.

## Scanning Flow

- Start a new scan and confirm the scanner returns to `Review` with the captured pages.
- Verify `Scan More` appends pages to the current session instead of creating a new one.
- Verify `Discard & Rescan` discards the current session and restarts scanning.
- Verify `Discard Session` returns to the home screen and does not automatically reopen the scanner.

## Review Editing

- Confirm the current page is obvious in the main preview and the thumbnail strip.
- Switch between pages from the thumbnail strip and confirm the selected page updates correctly.
- Toggle enhancement on a page and confirm the page status updates to `Original`, `Processing`, or `Enhanced` as appropriate.
- Delete a page, then use `Undo` and confirm the page returns in the correct position.
- Reorder pages and confirm the page order in `Review` matches the export order.
- If a page enhancement fails, confirm the failure state and `Retry` action are visible.

## Export

- Export a clean session as PDF and confirm the result completes successfully.
- Export a modified session as PDF and confirm it uses the expected rebuilt output path.
- Export as JPEG and confirm all pages are written to the gallery flow.
- After export, confirm the success sheet shows `Open`, `Share`, and `Continue Scanning`.
- Verify the intended fallback appears for at least one failure scenario, such as empty-session export or missing source files.

## Performance / Memory

- Exercise a representative multi-page session and watch for obvious jank during page switching.
- Run at least one enhancement on a larger page and confirm the app remains responsive.
- Run a multi-page export and confirm progress updates stay visible and the app does not freeze.
- If testing on a device with limited memory, watch for process death, repeated GC thrashing, or decode failures.
- Record any regression in cold start, scan return, review responsiveness, enhancement, export, or memory footprint.

## Notes

- Prefer a real device for final release validation.
- Use the same sample set and scenario order when comparing builds.
- If a step fails, capture the exact screen, action, and file or session state before retrying.
