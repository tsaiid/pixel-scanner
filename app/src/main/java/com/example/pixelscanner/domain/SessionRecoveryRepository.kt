package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSessionMetadata

interface SessionRecoveryRepository {
    suspend fun listRecoverableSessions(): List<RecoverableSessionSnapshot>

    suspend fun loadRecoverableSession(sessionId: String): RecoverableSessionSnapshot?

    suspend fun discardRecoverableSession(sessionId: String)
}
