package com.example.pixelscanner.core.image

import android.content.Context
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.domain.ImageProcessingRepository

class AndroidImageProcessingRepository(
    private val imageEnhancer: ImageEnhancer = ImageEnhancer(),
) : ImageProcessingRepository {
    override suspend fun enhance(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): PreviewImagePipelineResult {
        return imageEnhancer.enhance(context, request)
    }
}
