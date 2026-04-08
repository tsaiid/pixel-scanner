package com.example.pixelscanner.core.storage

import android.content.Context
import com.example.pixelscanner.core.model.ScanSessionMetadata
import com.example.pixelscanner.domain.SessionMetadataRepository
import com.example.pixelscanner.domain.SessionRecoveryRepository

class AndroidSessionRecoveryRepository(
    private val context: Context,
    private val sessionMetadataRepository: SessionMetadataRepository,
) : SessionRecoveryRepository {

    override suspend fun listRecoverableSessions(): List<ScanSessionMetadata> {
        val sessionsRoot = SessionStoragePaths.sessionsRoot(context.cacheDir)
        val sessionDirs = sessionsRoot.listFiles()?.filter { it.isDirectory } ?: return emptyList()

        return sessionDirs.mapNotNull { sessionDir ->
            sessionMetadataRepository.read(sessionDir.name)
        }.sortedByDescending { it.createdAtEpochMillis }
    }

    override suspend fun loadRecoverableSession(sessionId: String): ScanSessionMetadata? {
        return sessionMetadataRepository.read(sessionId)
    }
}
