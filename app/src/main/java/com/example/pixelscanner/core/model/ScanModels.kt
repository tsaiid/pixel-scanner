package com.example.pixelscanner.core.model

import android.net.Uri
import com.example.pixelscanner.domain.SessionIngestionAssetType
import com.example.pixelscanner.domain.SessionIngestionResult

data class ScanResultPayload(
    val pageUris: List<Uri>,
    val pdfUri: Uri? = null,
)

data class ScanSessionMetadata(
    val sessionId: String,
    val createdAtEpochMillis: Long,
    val pageCount: Int,
    val hasOriginalPdf: Boolean,
    val dirtyFlags: SessionDirtyFlags,
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
        return ScanSessionMetadata(
            sessionId = id,
            createdAtEpochMillis = createdAtEpochMillis,
            pageCount = pages.size,
            hasOriginalPdf = hasOriginalPdf,
            dirtyFlags = dirtyFlags,
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
                    page.copy(processingState = processingState)
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
            ),
        )
    }
}

data class ExportRequest(
    val sessionId: String,
    val format: ExportFormat,
)

sealed class ExportResult {
    data class Success(val outputUri: Uri?) : ExportResult()
    data class Failure(val errorMessage: String? = null) : ExportResult()
}

enum class ExportFormat {
    IMAGE,
    PDF,
}
