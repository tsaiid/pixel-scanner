package com.example.pixelscanner.feature.review.ui

import com.example.pixelscanner.core.model.ExportFormat

data class ExportUiState(
    val format: ExportFormat = ExportFormat.IMAGE,
    val isExporting: Boolean = false,
)
