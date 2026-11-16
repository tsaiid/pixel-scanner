package com.example.pixelscanner.core.storage

import com.example.pixelscanner.core.model.ScanSessionMetadata
import com.example.pixelscanner.core.model.SessionDirtyFlags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidSessionMetadataRepositoryTest {

    @Test
    fun `write and read round trips session metadata including all dirty flags`() = runBlocking {
        val repository = AndroidSessionMetadataRepository(RuntimeEnvironment.getApplication())
        val metadata = ScanSessionMetadata(
            sessionId = "session_metadata_round_trip",
            createdAtEpochMillis = 1_716_171_800_000L,
            pageCount = 7,
            hasOriginalPdf = true,
            dirtyFlags = SessionDirtyFlags(
                hasEnhancedPages = true,
                hasDeletedPages = true,
                hasReorderedPages = true,
                hasRotatedPages = true,
                hasCroppedPages = true,
                hasAddedPages = true,
            ),
        )

        try {
            repository.write(metadata)

            assertEquals(metadata, repository.read(metadata.sessionId))
        } finally {
            SessionStoragePaths.sessionRoot(
                RuntimeEnvironment.getApplication().cacheDir,
                metadata.sessionId,
            ).deleteRecursively()
        }
    }

    @Test
    fun `read returns null when metadata file does not exist`() = runBlocking {
        val repository = AndroidSessionMetadataRepository(RuntimeEnvironment.getApplication())

        assertNull(repository.read("missing_session_metadata"))
    }

    @Test
    fun `write and read preserves per-page order variant and deleted state`() = runBlocking {
        val repository = AndroidSessionMetadataRepository(RuntimeEnvironment.getApplication())
        val metadata = ScanSessionMetadata(
            sessionId = "session_metadata_pages_round_trip",
            createdAtEpochMillis = 1_716_171_900_000L,
            pageCount = 2,
            hasOriginalPdf = false,
            dirtyFlags = SessionDirtyFlags(
                hasEnhancedPages = true,
                hasDeletedPages = true,
                hasReorderedPages = true,
            ),
            pages = listOf(
                com.example.pixelscanner.core.model.ScanPageMetadata(
                    id = "page_1",
                    originalFileName = "page_2.jpg",
                    enhancedUriString = "file:///cache/enhanced_2.jpg",
                    activeVariant = com.example.pixelscanner.core.model.PageVariant.PREVIEW_ENHANCED,
                    isDeleted = false,
                ),
                com.example.pixelscanner.core.model.ScanPageMetadata(
                    id = "page_0",
                    originalFileName = "page_1.jpg",
                    enhancedUriString = null,
                    activeVariant = com.example.pixelscanner.core.model.PageVariant.ORIGINAL,
                    isDeleted = true,
                ),
            ),
        )

        try {
            repository.write(metadata)

            assertEquals(metadata, repository.read(metadata.sessionId))
        } finally {
            SessionStoragePaths.sessionRoot(
                RuntimeEnvironment.getApplication().cacheDir,
                metadata.sessionId,
            ).deleteRecursively()
        }
    }
}
