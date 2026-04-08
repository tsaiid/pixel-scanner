package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSessionMetadata

interface SessionRecoveryRepository {
    suspend fun listRecoverableSessions(): List<ScanSessionMetadata>

    suspend fun loadRecoverableSession(sessionId: String): ScanSessionMetadata?
}
