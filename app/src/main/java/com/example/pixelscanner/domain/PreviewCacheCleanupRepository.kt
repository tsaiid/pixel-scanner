package com.example.pixelscanner.domain

interface PreviewCacheCleanupRepository {
    fun cleanSessionPreviewCache(sessionId: String)
}
