package com.example.pixelscanner.core.storage

import android.content.Context
import com.example.pixelscanner.domain.SessionCleanupRepository
import com.example.pixelscanner.domain.SessionMetadataRepository

class AndroidSessionCleanupRepository(
    private val context: Context,
    private val sessionMetadataRepository: SessionMetadataRepository,
) : SessionCleanupRepository {

    override suspend fun cleanStaleSessions() {
        val sessionsRoot = SessionStoragePaths.sessionsRoot(context.cacheDir)
        val sessionDirs = sessionsRoot.listFiles()?.filter { it.isDirectory } ?: return

        sessionDirs.forEach { sessionDir ->
            val metadata = sessionMetadataRepository.read(sessionDir.name)
            if (metadata == null || isStale(metadata.createdAtEpochMillis)) {
                sessionDir.deleteRecursively()
            }
        }
    }

    private fun isStale(createdAtEpochMillis: Long): Boolean {
        return System.currentTimeMillis() - createdAtEpochMillis > STALE_SESSION_RETENTION_MILLIS
    }

    private companion object {
        private const val STALE_SESSION_RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
