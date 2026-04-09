package com.example.pixelscanner.core.storage

import android.net.Uri
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScanSessionMetadata
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.domain.DiskSpacePreflightRepository
import com.example.pixelscanner.domain.DiskSpacePreflightResult
import com.example.pixelscanner.domain.SessionMetadataRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidSessionIngestionRepositoryTest {

    @Test
    fun `ingest appends scan more pages without overwriting existing originals`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val sessionId = "session_ingestion_append_preserves_existing_pages"
        val repository = AndroidSessionIngestionRepository(
            context = context,
            sessionMetadataRepository = InMemorySessionMetadataRepository(),
            diskSpacePreflightRepository = SufficientDiskSpacePreflightRepository(),
        )
        val originalDir = SessionStoragePaths.originalPagesDir(context.cacheDir, sessionId)
        originalDir.mkdirs()
        val existingPageOne = File(originalDir, "page_1.jpg").apply { writeText("existing-page-1") }
        File(originalDir, "page_2.jpg").apply { writeText("existing-page-2") }
        val newScanFile = File(context.cacheDir, "session-ingestion-new-page.jpg").apply {
            writeText("new-page-3")
        }
        val session = ScanSession(
            id = sessionId,
            pages = listOf(
                ScannedPage(
                    id = "${sessionId}_page_0",
                    originalSourceUri = Uri.fromFile(existingPageOne),
                ),
                ScannedPage(
                    id = "${sessionId}_page_1",
                    originalSourceUri = Uri.fromFile(File(originalDir, "page_2.jpg")),
                ),
                ScannedPage(
                    id = "${sessionId}_page_2",
                    originalSourceUri = Uri.fromFile(newScanFile),
                ),
            ),
        )

        try {
            val result = repository.ingest(
                session = session,
                scanResult = ScanResultPayload(pageUris = listOf(Uri.fromFile(newScanFile))),
            )

            assertEquals("existing-page-1", existingPageOne.readText())
            val appendedFile = File(originalDir, "page_3.jpg")
            assertTrue(appendedFile.exists())
            assertEquals("new-page-3", appendedFile.readText())
            assertEquals(Uri.fromFile(appendedFile), result.pageMappings.single().destinationUri)
        } finally {
            SessionStoragePaths.sessionRoot(context.cacheDir, sessionId).deleteRecursively()
            newScanFile.delete()
        }
    }
}

private class InMemorySessionMetadataRepository : SessionMetadataRepository {
    private val values = mutableMapOf<String, ScanSessionMetadata>()

    override suspend fun write(metadata: ScanSessionMetadata) {
        values[metadata.sessionId] = metadata
    }

    override suspend fun read(sessionId: String): ScanSessionMetadata? = values[sessionId]
}

private class SufficientDiskSpacePreflightRepository : DiskSpacePreflightRepository {
    override fun check(
        targetPath: File,
        requiredBytes: Long,
    ): DiskSpacePreflightResult {
        return DiskSpacePreflightResult.Sufficient(availableBytes = requiredBytes + 1)
    }
}
