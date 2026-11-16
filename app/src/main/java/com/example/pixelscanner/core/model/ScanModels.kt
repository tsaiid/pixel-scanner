package com.example.pixelscanner.core.model

import android.net.Uri
import com.example.pixelscanner.domain.SessionIngestionAssetType
import com.example.pixelscanner.domain.SessionIngestionResult

data class ScanResultPayload(
    val pageUris: List<Uri>,
    val pdfUri: Uri? = null,
)

data class ScanPageMetadata(
    val id: String,
    val originalFileName: String,
    val enhancedUriString: String? = null,
    val activeVariant: PageVariant = PageVariant.ORIGINAL,
    val isDeleted: Boolean = false,
)

data class ScanSessionMetadata(
    val sessionId: String,
    val createdAtEpochMillis: Long,
    val pageCount: Int,
    val hasOriginalPdf: Boolean,
    val dirtyFlags: SessionDirtyFlags,
    val pages: List<ScanPageMetadata> = emptyList(),
)

enum class ProcessingState {
    IDLE,
    PROCESSING,
}

data class SessionDirtyFlags(
    val hasEnhancedPages: Boolean = false,
    val hasDeletedPages: Boolean = false,
    val hasReorderedPages: Boolean = false,
    val hasRotatedPages: Boolean = false,
    val hasCroppedPages: Boolean = false,
    val hasAddedPages: Boolean = false,
) {
    /**
     * Dirty means the exported document can no longer be treated as the untouched
     * scanner output. Today that is driven by enhancement, delete, and scan-more
     * append operations. Reorder/rotate/crop remain reserved for the corresponding
     * future actions.
     */
    fun isDirty(): Boolean {
        return hasEnhancedPages ||
            hasDeletedPages ||
            hasReorderedPages ||
            hasRotatedPages ||
            hasCroppedPages ||
            hasAddedPages
    }

    fun cacheSignature(): String {
        return listOf(
            hasEnhancedPages,
            hasDeletedPages,
            hasReorderedPages,
            hasRotatedPages,
            hasCroppedPages,
            hasAddedPages,
        ).joinToString(separator = ":") { flag ->
            if (flag) "1" else "0"
        }
    }

    companion object {
        fun fromPages(pages: List<ScannedPage>): SessionDirtyFlags {
            return SessionDirtyFlags(
                hasEnhancedPages = pages.any { it.isEnhanced() },
                hasDeletedPages = pages.any { it.isDeleted },
            )
        }
    }
}

enum class PageVariant {
    ORIGINAL,
    PREVIEW_ENHANCED,
}

data class ScannedPage(
    val id: String,
    val originalSourceUri: Uri,
    val enhancedUri: Uri? = null,
    val activeVariant: PageVariant = PageVariant.ORIGINAL,
    val processingState: ProcessingState = ProcessingState.IDLE,
    val isDeleted: Boolean = false,
    val enhancementErrorMessage: UiText? = null,
) {
    fun displayUri(): Uri {
        return if (activeVariant == PageVariant.PREVIEW_ENHANCED && enhancedUri != null) {
            enhancedUri
        } else {
            originalSourceUri
        }
    }

    fun isEnhanced(): Boolean {
        return activeVariant == PageVariant.PREVIEW_ENHANCED && enhancedUri != null
    }

    fun isProcessing(): Boolean {
        return processingState == ProcessingState.PROCESSING
    }
}

data class ScanSession(
    val id: String,
    val pages: List<ScannedPage>,
    val originalPdfUri: Uri? = null,
    val dirtyFlags: SessionDirtyFlags = SessionDirtyFlags(),
) {
    fun activePages(): List<ScannedPage> = pages.filterNot { it.isDeleted }

    fun toMetadata(
        createdAtEpochMillis: Long,
        hasOriginalPdf: Boolean = originalPdfUri != null,
    ): ScanSessionMetadata {
        val pageMetadata = pages.mapIndexed { index, page ->
            val fileName = page.originalSourceUri.lastPathSegment
                ?.takeIf { page.originalSourceUri.scheme == "file" && it.isNotBlank() }
                ?: "page_${index + 1}.jpg"
            ScanPageMetadata(
                id = page.id,
                originalFileName = fileName,
                enhancedUriString = page.enhancedUri?.toString(),
                activeVariant = page.activeVariant,
                isDeleted = page.isDeleted,
            )
        }
        return ScanSessionMetadata(
            sessionId = id,
            createdAtEpochMillis = createdAtEpochMillis,
            pageCount = pages.size,
            hasOriginalPdf = hasOriginalPdf,
            dirtyFlags = dirtyFlags,
            pages = pageMetadata,
        )
    }

    fun isDirty(): Boolean {
        return dirtyFlags.isDirty()
    }

    fun canReuseOriginalPdfForExport(): Boolean {
        return originalPdfUri != null && !isDirty()
    }

    fun originalPdfExportSource(): Uri? {
        return if (canReuseOriginalPdfForExport()) originalPdfUri else null
    }

    fun withPageProcessing(pageId: String, processingState: ProcessingState): ScanSession {
        return withPages(
            pages.map { page ->
                if (page.id == pageId) {
                    page.copy(
                        processingState = processingState,
                        enhancementErrorMessage = if (processingState == ProcessingState.PROCESSING) null else page.enhancementErrorMessage,
                    )
                } else {
                    page
                }
            }
        )
    }

    fun withPageEnhancementFailure(pageId: String, errorMessage: UiText?): ScanSession {
        return withPages(
            pages.map { page ->
                if (page.id == pageId) {
                    page.copy(
                        processingState = ProcessingState.IDLE,
                        enhancementErrorMessage = errorMessage,
                    )
                } else {
                    page
                }
            }
        )
    }

    fun withIngestedAssets(ingestionResult: SessionIngestionResult): ScanSession {
        val ingestedPageUriBySource = ingestionResult.pageMappings
            .filter { it.assetType == SessionIngestionAssetType.PAGE_JPEG }
            .associateBy({ it.sourceUri }, { it.destinationUri })

        val updatedPages = pages.map { page ->
            val ingestedUri = ingestedPageUriBySource[page.originalSourceUri]
            if (ingestedUri != null) {
                page.copy(originalSourceUri = ingestedUri)
            } else {
                page
            }
        }

        val updatedOriginalPdfUri = ingestionResult.originalPdfMapping?.destinationUri ?: originalPdfUri
        return copy(
            pages = updatedPages,
            originalPdfUri = updatedOriginalPdfUri,
            dirtyFlags = SessionDirtyFlags.fromPages(updatedPages).copy(
                hasAddedPages = dirtyFlags.hasAddedPages,
                hasReorderedPages = dirtyFlags.hasReorderedPages,
                hasRotatedPages = dirtyFlags.hasRotatedPages,
                hasCroppedPages = dirtyFlags.hasCroppedPages,
            ),
        )
    }

    fun appendScanResult(scanResult: ScanResultPayload): ScanSession {
        val nextPageIndex = pages.size
        val appendedPages = scanResult.pageUris.mapIndexed { index, uri ->
            ScannedPage(
                id = "${id}_page_${nextPageIndex + index}",
                originalSourceUri = uri,
            )
        }

        return copy(
            pages = pages + appendedPages,
            dirtyFlags = dirtyFlags.copy(hasAddedPages = true),
        )
    }

    fun withPages(updatedPages: List<ScannedPage>): ScanSession {
        return copy(
            pages = updatedPages,
            dirtyFlags = SessionDirtyFlags.fromPages(updatedPages).copy(
                hasAddedPages = dirtyFlags.hasAddedPages,
                hasReorderedPages = dirtyFlags.hasReorderedPages,
                hasRotatedPages = dirtyFlags.hasRotatedPages,
                hasCroppedPages = dirtyFlags.hasCroppedPages,
            ),
        )
    }

    fun withReorderedPages(updatedPages: List<ScannedPage>): ScanSession {
        return copy(
            pages = updatedPages,
            dirtyFlags = SessionDirtyFlags.fromPages(updatedPages).copy(
                hasAddedPages = dirtyFlags.hasAddedPages,
                hasReorderedPages = true,
                hasRotatedPages = dirtyFlags.hasRotatedPages,
                hasCroppedPages = dirtyFlags.hasCroppedPages,
            ),
        )
    }
}

data class ExportRequest(
    val sessionId: String,
    val format: ExportFormat,
)

sealed interface ExportState {
    data object Idle : ExportState
    data class InProgress(
        val progress: ExportProgress = ExportProgress(),
    ) : ExportState
    data class Finished(val result: ExportResult) : ExportState
}

sealed interface ExportDecision {
    data object ReuseOriginalPdf : ExportDecision
    data object RebuildPdfFromEnhancedPages : ExportDecision
    data object RebuildPdfFromPages : ExportDecision
    data object ExportJpegs : ExportDecision
}

sealed interface ExportExecutionRoute {
    data object Foreground : ExportExecutionRoute
    data object Background : ExportExecutionRoute
}

fun ExportRequest.resolveExecutionRoute(session: ScanSession): ExportExecutionRoute {
    // Background export is deferred by ADR, so current exports stay on the
    // foreground path while preserving an explicit routing boundary.
    return ExportExecutionRoute.Foreground
}

fun ExportRequest.resolveDecision(session: ScanSession): ExportDecision {
    return resolveDecision(
        dirtyFlags = session.dirtyFlags,
        hasOriginalPdf = session.originalPdfUri != null,
    )
}

fun ExportRequest.resolveDecision(
    dirtyFlags: SessionDirtyFlags,
    hasOriginalPdf: Boolean,
): ExportDecision {
    return when (format) {
        ExportFormat.IMAGE -> ExportDecision.ExportJpegs
        ExportFormat.PDF -> when {
            hasOriginalPdf && !dirtyFlags.isDirty() -> ExportDecision.ReuseOriginalPdf
            dirtyFlags.hasEnhancedPages -> ExportDecision.RebuildPdfFromEnhancedPages
            else -> ExportDecision.RebuildPdfFromPages
        }
    }
}

sealed interface ExportResult {
    data class Success(
        val outputUri: Uri?,
        val outputUris: List<Uri> = listOfNotNull(outputUri),
    ) : ExportResult
    data class CompletedExportPage(
        val pageId: String,
        val outputUri: Uri,
    )
    data class PartialSuccess(
        val completedPages: List<CompletedExportPage>,
        val failures: List<ExportFailure> = emptyList(),
        val warningMessage: UiText? = null,
    ) : ExportResult {
        val outputUris: List<Uri>
            get() = completedPages.map { it.outputUri }
    }
    data class Failure(val error: ExportFailure) : ExportResult {
        val errorMessage: UiText
            get() = error.userMessage
    }
    data object Cancelled : ExportResult
}

enum class ExportFormat {
    IMAGE,
    PDF,
}
