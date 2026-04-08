package com.example.pixelscanner.domain

import android.content.Context
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import java.io.File

interface PreviewImageCacheRepository {
    fun getCachedPreview(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): File?

    fun createPreviewFile(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): File
}
