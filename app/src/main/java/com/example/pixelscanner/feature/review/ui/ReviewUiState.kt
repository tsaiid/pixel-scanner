package com.example.pixelscanner.feature.review.ui

import android.net.Uri
import com.example.pixelscanner.feature.export.ui.ExportUiState
import com.example.pixelscanner.core.model.ProcessingState

data class ReviewUiState(
    val pages: List<ReviewPageUiModel> = emptyList(),
    val currentPage: Int = 0,
    val export: ExportUiState = ExportUiState(),
    val isAwaitingScanMoreResult: Boolean = false,
    val statusMessage: ReviewStatusMessage? = null,
) {
    val hasPages: Boolean
        get() = pages.isNotEmpty()

    val selectedPage: ReviewPageUiModel?
        get() = pages.getOrNull(currentPage)

    val isProcessing: Boolean
        get() = selectedPage?.processingState == ProcessingState.PROCESSING
}

data class ReviewStatusMessage(
    val id: Long,
    val text: String,
)

data class ReviewPageUiModel(
    val id: String,
    val displayUri: Uri,
    val isEnhanced: Boolean,
    val processingState: ProcessingState = ProcessingState.IDLE,
)
