package com.example.pixelscanner.testing

import android.content.Context
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.domain.ImageProcessingRepository

class FakeImageProcessingRepository(
    var enhancedUri: android.net.Uri = android.net.Uri.parse("file:///tmp/fake-enhanced.jpg"),
) : ImageProcessingRepository {
    val requests = mutableListOf<PreviewImagePipelineRequest>()

    override suspend fun enhance(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): PreviewImagePipelineResult {
        requests += request
        return PreviewImagePipelineResult(
            cacheKey = request.cacheKey,
            outputUri = enhancedUri,
        )
    }
}
