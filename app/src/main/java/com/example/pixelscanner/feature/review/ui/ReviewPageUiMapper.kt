package com.example.pixelscanner.feature.review.ui

import com.example.pixelscanner.core.model.ScannedPage

object ReviewPageUiMapper {
    fun map(page: ScannedPage): ReviewPageUiModel {
        return ReviewPageUiModel(
            id = page.id,
            displayUri = page.displayUri(),
            isEnhanced = page.isEnhanced(),
            processingState = page.processingState,
        )
    }
}
