package com.example.pixelscanner.feature.export.ui

import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportState

data class ExportUiState(
    val format: ExportFormat = ExportFormat.IMAGE,
    val state: ExportState = ExportState.Idle,
) {
    val isExporting: Boolean
        get() = state is ExportState.InProgress

    val lastResult: ExportState.Finished?
        get() = state as? ExportState.Finished
}
