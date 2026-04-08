package com.example.pixelscanner.core.storage

import android.content.Context
import com.example.pixelscanner.domain.PreviewCacheCleanupRepository

class AndroidPreviewCacheCleanupRepository(
    private val context: Context,
) : PreviewCacheCleanupRepository {
    override fun cleanSessionPreviewCache(sessionId: String) {
        SessionStoragePaths.previewPagesDir(context.cacheDir, sessionId).deleteRecursively()
    }
}
