package com.example.pixelscanner.core.model

data class PreviewCacheIndex(
    val sessionId: String,
    val entries: List<PreviewCacheEntryMetadata> = emptyList(),
    val updatedAtEpochMs: Long = 0L,
)

data class PreviewCacheEntryMetadata(
    val cacheKey: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val createdAtEpochMs: Long,
    val lastAccessedAtEpochMs: Long,
)
