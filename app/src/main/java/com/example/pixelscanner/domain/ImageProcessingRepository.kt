package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult

interface ImageProcessingRepository {
    suspend fun enhance(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): PreviewImagePipelineResult

    suspend fun enhance(context: Context, uri: Uri): Uri {
        return enhance(
            context = context,
            request = PreviewImagePipelineRequest(
                pageId = "legacy",
                sourceUri = uri,
            ),
        ).outputUri
    }
}
