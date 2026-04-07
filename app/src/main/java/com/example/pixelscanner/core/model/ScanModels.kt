package com.example.pixelscanner.core.model

import android.net.Uri

data class ScanResultPayload(
    val pageUris: List<Uri>,
    val pdfUri: Uri? = null,
)

data class SessionDirtyFlags(
    val hasEnhancedPages: Boolean = false,
    val hasDeletedPages: Boolean = false,
    val hasReorderedPages: Boolean = false,
    val hasRotatedPages: Boolean = false,
    val hasCroppedPages: Boolean = false,
) {
    fun isDirty(): Boolean {
        return hasEnhancedPages || hasDeletedPages || hasReorderedPages || hasRotatedPages || hasCroppedPages
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
}

data class ScanSession(
    val id: String,
    val pages: List<ScannedPage>,
    val originalPdfUri: Uri? = null,
    val dirtyFlags: SessionDirtyFlags = SessionDirtyFlags(),
) {
    fun activePages(): List<ScannedPage> = pages.filterNot { it.isDeleted }
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
