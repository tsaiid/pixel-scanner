package com.example.pixelscanner.feature.review.ui

import android.content.Context
import com.example.pixelscanner.core.model.ExportFormat

sealed interface ReviewUiEvent {
    data class PageChanged(val pageIndex: Int) : ReviewUiEvent
    data class ChangeExportFormat(val format: ExportFormat) : ReviewUiEvent
    data class ToggleEnhancement(val pageIndex: Int, val context: Context) : ReviewUiEvent
    data class RetryExport(val context: Context) : ReviewUiEvent
    data object CancelExport : ReviewUiEvent
    data class DeletePage(val pageIndex: Int) : ReviewUiEvent
    data class RestorePage(val pageId: String) : ReviewUiEvent
    data object MovePageLeft : ReviewUiEvent
    data object MovePageRight : ReviewUiEvent
    data class RetryEnhancement(val pageId: String, val context: Context) : ReviewUiEvent
    data object RequestScanMore : ReviewUiEvent
    data object RequestRescan : ReviewUiEvent
    data object RequestDiscardSession : ReviewUiEvent
    data class SaveAll(val context: Context) : ReviewUiEvent
}
