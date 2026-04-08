package com.example.pixelscanner.feature.review.ui

import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportPostActionTarget

sealed interface ReviewUiEffect {
    data class ShowDeletedPageUndo(val pageId: String) : ReviewUiEffect
    data class ExportSucceeded(
        val format: ExportFormat,
        val target: ExportPostActionTarget,
    ) : ReviewUiEffect
    data object NavigateBackToScanner : ReviewUiEffect
    data object NavigateBackToHome : ReviewUiEffect
}
