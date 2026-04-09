package com.example.pixelscanner.feature.export.ui

import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.ExportState
import com.example.pixelscanner.core.model.UiText

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
}
