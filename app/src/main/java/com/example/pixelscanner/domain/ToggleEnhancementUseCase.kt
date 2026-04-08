package com.example.pixelscanner.domain

import android.content.Context
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.core.model.PipelineCacheKey
import com.example.pixelscanner.core.model.PageVariant
import com.example.pixelscanner.core.model.ScannedPage

class ToggleEnhancementUseCase(
    private val imageProcessingRepository: ImageProcessingRepository,
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

        val pipelineRequest = PreviewImagePipelineRequest(
            pageId = page.id,
            sourceUri = page.originalSourceUri,
            sourceVariant = page.activeVariant,
        )
        val pipelineResult: PreviewImagePipelineResult = imageProcessingRepository.enhance(
            context = context,
            request = pipelineRequest,
        )
        return ToggleEnhancementResult(
            updatedPage = page.copy(
                enhancedUri = pipelineResult.outputUri,
                activeVariant = PageVariant.PREVIEW_ENHANCED,
            ),
            enabled = true,
            previewCacheKey = pipelineResult.cacheKey,
        )
    }
}

data class ToggleEnhancementResult(
    val updatedPage: ScannedPage,
    val enabled: Boolean,
    val previewCacheKey: PipelineCacheKey? = null,
)
