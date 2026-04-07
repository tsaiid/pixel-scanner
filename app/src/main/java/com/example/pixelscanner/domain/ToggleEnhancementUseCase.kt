package com.example.pixelscanner.domain

import android.content.Context
import com.example.pixelscanner.core.image.ImageEnhancer
import com.example.pixelscanner.core.model.PageVariant
import com.example.pixelscanner.core.model.ScannedPage

class ToggleEnhancementUseCase(
    private val imageEnhancer: ImageEnhancer = ImageEnhancer(),
) {

    suspend operator fun invoke(
        page: ScannedPage,
        context: Context,
    ): ToggleEnhancementResult {
        if (page.isEnhanced()) {
            return ToggleEnhancementResult(
                updatedPage = page.copy(activeVariant = PageVariant.ORIGINAL),
                enabled = false,
            )
        }

        if (page.enhancedUri != null) {
            return ToggleEnhancementResult(
                updatedPage = page.copy(activeVariant = PageVariant.PREVIEW_ENHANCED),
                enabled = true,
            )
        }

        val enhancedUri = imageEnhancer.enhance(context, page.originalSourceUri)
        return ToggleEnhancementResult(
            updatedPage = page.copy(
                enhancedUri = enhancedUri,
                activeVariant = PageVariant.PREVIEW_ENHANCED,
            ),
            enabled = true,
        )
    }
}

data class ToggleEnhancementResult(
    val updatedPage: ScannedPage,
    val enabled: Boolean,
)
