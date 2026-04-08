package com.example.pixelscanner.core.model

import android.net.Uri

enum class ExportRetryability {
    RETRYABLE,
    NON_RETRYABLE,
}

sealed interface ExportFailure {
    val retryability: ExportRetryability
    val userMessage: String

    val isRetryable: Boolean
        get() = retryability == ExportRetryability.RETRYABLE
}

data object ExportNoPagesFailure : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.NON_RETRYABLE
    override val userMessage: String = "No pages to export"
}

data class ExportSourceInvalidFailure(
    val sourceUri: Uri? = null,
    val details: String? = null,
) : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.NON_RETRYABLE
    override val userMessage: String = details ?: "Source file is no longer available"
}

data class ExportReadFailure(
    val sourceUri: Uri? = null,
    val details: String? = null,
) : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.RETRYABLE
    override val userMessage: String = details ?: "Failed to read source file"
}

data class ExportDecodeFailure(
    val sourceUri: Uri? = null,
    val details: String? = null,
) : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.RETRYABLE
    override val userMessage: String = details ?: "Failed to decode image"
}

data class ExportWriteFailure(
    val destinationName: String? = null,
    val details: String? = null,
) : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.RETRYABLE
    override val userMessage: String = details ?: "Failed to write exported file"
}

data object ExportInsufficientSpaceFailure : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.RETRYABLE
    override val userMessage: String = "Not enough storage space"
}

data object ExportPermissionDeniedFailure : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.NON_RETRYABLE
    override val userMessage: String = "Export permission denied"
}

data class ExportUnknownFailure(
    val details: String? = null,
    override val retryability: ExportRetryability = ExportRetryability.RETRYABLE,
) : ExportFailure {
    override val userMessage: String = details ?: "Export failed"
}

sealed interface ExportOperationResult {
    data class Success(val outputUri: Uri) : ExportOperationResult
    data class Failure(val error: ExportFailure) : ExportOperationResult
}
