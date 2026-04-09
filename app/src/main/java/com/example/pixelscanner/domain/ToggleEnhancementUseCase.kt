package com.example.pixelscanner.domain

import android.content.Context
import android.os.SystemClock
import com.example.pixelscanner.core.diagnostics.DebugLog
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
        val startedAtMs = SystemClock.elapsedRealtime()
        var mode = "compute"
        var enabled = true
        try {
            if (page.isEnhanced()) {
                mode = "disable"
                enabled = false
                return ToggleEnhancementResult(
                    updatedPage = page.copy(activeVariant = PageVariant.ORIGINAL),
                    enabled = false,
                )
            }

            if (page.enhancedUri != null) {
                mode = "cache_hit"
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
        } catch (error: Exception) {
            DebugLog.error("preview_enhancement_use_case_failed", error)
            throw error
        } finally {
            DebugLog.event(
                name = "preview_enhancement",
                "durationMs" to (SystemClock.elapsedRealtime() - startedAtMs),
                "mode" to mode,
                "enabled" to enabled,
            )
        }
    }
}

data class ToggleEnhancementResult(
    val updatedPage: ScannedPage,
    val enabled: Boolean,
    val previewCacheKey: PipelineCacheKey? = null,
)
