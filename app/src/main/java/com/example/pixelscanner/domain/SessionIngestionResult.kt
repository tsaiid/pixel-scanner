package com.example.pixelscanner.domain

import android.net.Uri
import com.example.pixelscanner.core.model.ExportFailure

enum class SessionIngestionStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILURE,
}

enum class SessionIngestionAssetType {
    PAGE_JPEG,
    ORIGINAL_PDF,
}

data class SessionIngestionMapping(
    val sourceUri: Uri,
    val destinationPath: String,
    val destinationUri: Uri,
    val assetType: SessionIngestionAssetType,
)

data class SessionIngestionFailure(
    val sourceUri: Uri?,
    val destinationPath: String?,
    val assetType: SessionIngestionAssetType,
    val message: String,
    val typedFailure: ExportFailure? = null,
)

data class SessionIngestionResult(
    val status: SessionIngestionStatus,
    val pageMappings: List<SessionIngestionMapping> = emptyList(),
    val originalPdfMapping: SessionIngestionMapping? = null,
    val failures: List<SessionIngestionFailure> = emptyList(),
) {
    fun hasFailures(): Boolean = failures.isNotEmpty()

    fun successfulMappingCount(): Int {
        return pageMappings.size + if (originalPdfMapping != null) 1 else 0
    }
}
