# ML Kit Result Persistence Fields

This note records which fields from `GmsDocumentScanningResult` should be persisted for Pixel Scanner, based on the current code path and the app's session/export requirements.

## Current code usage

The scanner result is read in [`app/src/main/java/com/example/pixelscanner/feature/scanner/ui/ScannerScreen.kt`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/feature/scanner/ui/ScannerScreen.kt):

- `result.pages?.map { page.imageUri }`
- `result.pdf?.uri`

Those values are passed into [`ScanResultPayload`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/core/model/ScanModels.kt) and then converted into a session in [`CreateSessionFromScanResultUseCase`](/Users/tsaiid/git/pixel-scanner/app/src/main/java/com/example/pixelscanner/domain/CreateSessionFromScanResultUseCase.kt).

The session model already persists:

- page list order
- per-page source URI in `ScannedPage.originalSourceUri`
- optional original PDF URI in `ScanSession.originalPdfUri`
- ingestion mapping to session-owned files

## Local SDK inspection

The installed ML Kit scanner API version exposed by the local Gradle cache is `com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1`.

Public result surface in this version:

- `GmsDocumentScanningResult.getPages()`
- `GmsDocumentScanningResult.getPdf()`
- `GmsDocumentScanningResult.Page.getImageUri()`
- `GmsDocumentScanningResult.Pdf.getUri()`
- `GmsDocumentScanningResult.Pdf.getPageCount()`

No other result fields are exposed in this version.

## Decision

### Must persist now

These are required for session ownership, review continuity, and export:

| ML Kit field | Persist? | Why |
| --- | --- | --- |
| `pages[*].imageUri` | Yes | This is the per-page JPEG source used by review, enhancement, delete/reorder flows, and eager ingestion into session-owned storage. Persist the copied session file path and keep the page order stable. |
| `pdf.uri` | Yes, when present | Needed for the fast path that reuses the original PDF when the session is still untouched. Persist the copied session file path, not the transient ML Kit URI. |
| `pdf.pageCount` | Store as optional metadata if available | Useful for sanity checks and diagnostics, but not required as the primary source of truth because the page list already defines page count. |

Practical rule:

- Treat the ML Kit URIs as ingestion inputs only.
- After ingestion, the app-owned session file paths are the persisted source of truth.

### Optional later

These are not required for the current product slice, but may be useful later:

- `pdf.pageCount` as a validation field for export/debug logs
- extra scanner-result metadata if a future ML Kit release adds it and the app actually uses it
- provenance/debug fields such as original scanner output ordering hashes, if we need deeper recovery tooling

### Not available in the current API surface

The current ML Kit result does not expose additional scan metadata beyond the fields above. In particular, there is no persisted:

- OCR text layer
- page bounding boxes or edge polygons
- crop/rotation settings
- image dimensions
- page thumbnails
- confidence scores or quality metrics
- per-page identifiers other than list order

## Recommended persistence shape for this project

For this codebase, persist the following session-owned data after ingestion:

- `sessionId`
- page order
- copied JPEG file path for each page
- copied original PDF file path, if ML Kit returned one
- `pageCount`
- `hasOriginalPdf`
- derived dirty flags
- source-to-destination mapping for recovery and cleanup

Do not keep the raw ML Kit `content://` URI as the long-term source of truth. It is only an input for copying into app-owned storage.

## Conclusion

For this project, the research answer is closed:

- persist page JPEGs and the original PDF now
- store `pdf.pageCount` only as optional metadata
- treat everything else as unavailable in the current ML Kit API or not needed for this refactor slice

