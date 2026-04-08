package com.example.pixelscanner.core.storage

import android.content.Context
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.domain.PreviewImageCacheRepository
import java.io.File

class AndroidPreviewImageCacheRepository : PreviewImageCacheRepository {
    override fun getCachedPreview(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): File? {
        val file = previewFile(context, request)
        return if (file.exists() && file.length() > 0L) file else null
    }

    override fun createPreviewFile(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): File {
        val file = previewFile(context, request)
        file.parentFile?.mkdirs()
        if (file.exists() && file.length() == 0L) {
            file.delete()
        }
        return file
    }

    private fun previewFile(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): File {
        return File(
            SessionStoragePaths.previewPagesDir(context.cacheDir, request.sessionId),
            "${request.cacheKey.asFileToken()}.jpg",
        )
    }
}
