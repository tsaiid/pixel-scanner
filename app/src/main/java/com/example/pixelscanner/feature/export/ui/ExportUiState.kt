package com.example.pixelscanner.feature.export.ui

import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.ExportNoPagesFailure
import com.example.pixelscanner.core.model.ExportPermissionDeniedFailure
import com.example.pixelscanner.core.model.ExportSourceInvalidFailure
import com.example.pixelscanner.core.model.ExportState
import com.example.pixelscanner.core.model.UiText

enum class ExportFallbackAction {
    DISCARD_AND_RESCAN,
    SCAN_MORE,
    OPEN_SETTINGS,
}

data class ExportUiState(
    val format: ExportFormat = ExportFormat.IMAGE,
    val state: ExportState = ExportState.Idle,
) {
    val isExporting: Boolean
        get() = state is ExportState.InProgress

    val progressText: UiText?
        get() = (state as? ExportState.InProgress)?.progress?.displayLabel()

    val lastResult: ExportState.Finished?
        get() = state as? ExportState.Finished

    val canRetry: Boolean
        get() = (lastResult?.result as? ExportResult.Failure)?.error?.isRetryable == true

    val fallbackAction: ExportFallbackAction?
        get() = when ((lastResult?.result as? ExportResult.Failure)?.error) {
            is ExportSourceInvalidFailure -> ExportFallbackAction.DISCARD_AND_RESCAN
            is ExportNoPagesFailure -> ExportFallbackAction.SCAN_MORE
            is ExportPermissionDeniedFailure -> ExportFallbackAction.OPEN_SETTINGS
            else -> null
        }
}
