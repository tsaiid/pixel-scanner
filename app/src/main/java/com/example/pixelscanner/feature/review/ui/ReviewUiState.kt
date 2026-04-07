package com.example.pixelscanner.feature.review.ui

import android.net.Uri
import com.example.pixelscanner.core.model.ExportFormat

data class ReviewUiState(
    val pages: List<ReviewPageUiModel> = emptyList(),
    val exportFormat: ExportFormat = ExportFormat.IMAGE,
    val isProcessing: Boolean = false,
) {
    val hasPages: Boolean
        get() = pages.isNotEmpty()
}

data class ReviewPageUiModel(
    val id: String,
    val displayUri: Uri,
    val isEnhanced: Boolean,
)
