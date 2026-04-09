package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSessionMetadata

interface SessionMetadataRepository {
    suspend fun write(metadata: ScanSessionMetadata)

    suspend fun read(sessionId: String): ScanSessionMetadata?
}
