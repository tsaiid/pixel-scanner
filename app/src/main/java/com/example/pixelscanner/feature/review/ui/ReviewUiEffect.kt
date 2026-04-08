package com.example.pixelscanner.feature.review.ui

import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportPostActionTarget

sealed interface ReviewUiEffect {
    data class ShowDeletedPageUndo(val pageId: String) : ReviewUiEffect
    data class ExportSucceeded(
        val format: ExportFormat,
        val target: ExportPostActionTarget,
    ) : ReviewUiEffect
    data class NavigateToScanner(val reason: ScannerNavigationReason) : ReviewUiEffect
    data class NavigateToHome(val reason: HomeNavigationReason) : ReviewUiEffect
}

enum class ScannerNavigationReason {
    RESCAN,
    SCAN_MORE,
}

enum class HomeNavigationReason {
    DISCARD_SESSION,
}
