package com.example.pixelscanner.core.model

import android.net.Uri
import com.example.pixelscanner.R

enum class ExportRetryability {
    RETRYABLE,
    NON_RETRYABLE,
}

sealed interface ExportFailure {
    val retryability: ExportRetryability
    val userMessage: UiText

    val isRetryable: Boolean
        get() = retryability == ExportRetryability.RETRYABLE
}

data object ExportNoPagesFailure : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.NON_RETRYABLE
    override val userMessage: UiText = uiTextResource(R.string.export_error_no_pages)
}

data class ExportSourceInvalidFailure(
    val sourceUri: Uri? = null,
    val details: String? = null,
) : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.NON_RETRYABLE
    override val userMessage: UiText = details?.let(UiText::DynamicString)
        ?: uiTextResource(R.string.export_error_source_missing)
}

data class ExportReadFailure(
    val sourceUri: Uri? = null,
    val details: String? = null,
) : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.RETRYABLE
    override val userMessage: UiText = details?.let(UiText::DynamicString)
        ?: uiTextResource(R.string.export_error_read)
}

data class ExportDecodeFailure(
    val sourceUri: Uri? = null,
    val details: String? = null,
) : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.RETRYABLE
    override val userMessage: UiText = details?.let(UiText::DynamicString)
        ?: uiTextResource(R.string.export_error_decode)
}

data class ExportWriteFailure(
    val destinationName: String? = null,
    val details: String? = null,
) : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.RETRYABLE
    override val userMessage: UiText = details?.let(UiText::DynamicString)
        ?: uiTextResource(R.string.export_error_write)
}

data object ExportInsufficientSpaceFailure : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.RETRYABLE
    override val userMessage: UiText = uiTextResource(R.string.export_error_space)
}

data object ExportPermissionDeniedFailure : ExportFailure {
    override val retryability: ExportRetryability = ExportRetryability.NON_RETRYABLE
    override val userMessage: UiText = uiTextResource(R.string.export_error_permission)
}

data class ExportUnknownFailure(
    val details: String? = null,
    override val retryability: ExportRetryability = ExportRetryability.RETRYABLE,
) : ExportFailure {
    override val userMessage: UiText = details?.let(UiText::DynamicString)
        ?: uiTextResource(R.string.export_error_unknown)
}

sealed interface ExportOperationResult {
    data class Success(val outputUri: Uri) : ExportOperationResult
    data class Failure(val error: ExportFailure) : ExportOperationResult
}
