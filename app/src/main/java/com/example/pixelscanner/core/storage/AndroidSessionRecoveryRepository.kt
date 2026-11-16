package com.example.pixelscanner.core.storage

import android.content.Context
import com.example.pixelscanner.domain.RecoverableSessionSnapshot
import com.example.pixelscanner.domain.SessionMetadataRepository
import com.example.pixelscanner.domain.SessionRecoveryRepository
import java.io.File

class AndroidSessionRecoveryRepository(
    private val context: Context,
    private val sessionMetadataRepository: SessionMetadataRepository,
) : SessionRecoveryRepository {

    override suspend fun listRecoverableSessions(): List<RecoverableSessionSnapshot> {
        val sessionsRoot = SessionStoragePaths.sessionsRoot(context.cacheDir)
        val sessionDirs = sessionsRoot.listFiles()?.filter { it.isDirectory } ?: return emptyList()

        return sessionDirs.mapNotNull { sessionDir ->
            loadRecoverableSession(sessionDir.name)
        }.sortedByDescending { it.metadata.createdAtEpochMillis }
    }

    override suspend fun loadRecoverableSession(sessionId: String): RecoverableSessionSnapshot? {
        val metadata = sessionMetadataRepository.read(sessionId) ?: return null
        if (metadata.pages.isNotEmpty() && metadata.pages.all { it.isDeleted } && !metadata.hasOriginalPdf) {
            return null
        }
        val originalPagesDir = SessionStoragePaths.originalPagesDir(context.cacheDir, sessionId)
        val expectedPageFiles = (1..metadata.pageCount).map { pageIndex ->
            File(originalPagesDir, "page_$pageIndex.jpg")
        }
        val pageFiles = expectedPageFiles.filter(File::exists)
        val missingPageFileNames = expectedPageFiles.filterNot(File::exists).map(File::getName)
        val originalPdfFile = SessionStoragePaths.originalPdfFile(context.cacheDir, sessionId)
            .takeIf(File::exists)

        if (pageFiles.isEmpty() && originalPdfFile == null) {
            return null
        }

        return RecoverableSessionSnapshot(
            metadata = metadata,
            pageFiles = pageFiles,
            originalPdfFile = originalPdfFile,
            missingPageFileNames = missingPageFileNames,
            missingOriginalPdf = metadata.hasOriginalPdf && originalPdfFile == null,
        )
    }

    override suspend fun discardRecoverableSession(sessionId: String) {
        SessionStoragePaths.sessionRoot(context.cacheDir, sessionId).deleteRecursively()
    }
}
