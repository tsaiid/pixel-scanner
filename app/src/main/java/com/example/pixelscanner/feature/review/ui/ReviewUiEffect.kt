package com.example.pixelscanner.feature.review.ui

import com.example.pixelscanner.core.model.ExportFormat

sealed interface ReviewUiEffect {
    data class ShowDeletedPageUndo(val pageId: String) : ReviewUiEffect
    data class ShowMessage(val message: String) : ReviewUiEffect
    data class ExportSucceeded(val format: ExportFormat) : ReviewUiEffect
    data object NavigateBackToScanner : ReviewUiEffect
}
