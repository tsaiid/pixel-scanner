package com.example.pixelscanner.feature.export.ui

import com.example.pixelscanner.core.model.ExportFormat

data class ExportUiState(
    val format: ExportFormat = ExportFormat.IMAGE,
    val isExporting: Boolean = false,
)
