package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.ExportOperationResult
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.ExportUnknownFailure
import com.example.pixelscanner.core.model.UiText
import com.example.pixelscanner.core.model.asString
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.SessionDirtyFlags
import com.example.pixelscanner.testing.ExportImageRequest
import com.example.pixelscanner.testing.ExportPdfBuildRequest
import com.example.pixelscanner.testing.ExportPdfRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ExportSessionUseCaseTest {

    @Test
    fun `reuses original pdf and skips rebuilding when session is clean`() {
        val originalPdfUri = Uri.parse("content://scanner/original-session.pdf")
        val pageUri = Uri.parse("content://scanner/page-1.jpg")
        val expectedExportUri = Uri.parse("file:///documents/PixelScanner_clean.pdf")
        val exportRepository = RecordingExportRepository(
            copyPdfResult = ExportOperationResult.Success(expectedExportUri),
        )
        val artifactCacheRepository = RecordingExportArtifactCacheRepository()
        val useCase = ExportSessionUseCase(
            exportRepository = exportRepository,
            exportArtifactCacheRepository = artifactCacheRepository,
        )
        val session = ScanSession(
            id = "session_clean_pdf",
            pages = listOf(
                ScannedPage(
                    id = "session_clean_pdf_page_0",
                    originalSourceUri = pageUri,
                ),
            ),
            originalPdfUri = originalPdfUri,
            dirtyFlags = SessionDirtyFlags(),
        )

        val result = runBlocking {
            useCase(
                session = session,
                request = ExportRequest(sessionId = session.id, format = ExportFormat.PDF),
                context = RuntimeEnvironment.getApplication(),
            )
        }

        assertTrue(result is ExportResult.Success)
        assertEquals(1, exportRepository.copiedPdfRequests.size)
        assertEquals(originalPdfUri, exportRepository.copiedPdfRequests.single().sourceUri)
        assertEquals(0, exportRepository.savedPdfRequests.size)
        assertTrue(artifactCacheRepository.cachedPdfRequests.isNotEmpty())
        assertEquals(1, artifactCacheRepository.storedPdfRequests.size)
        assertEquals(expectedExportUri, artifactCacheRepository.storedPdfRequests.single().sourceUri)
        assertEquals(expectedExportUri, (result as ExportResult.Success).outputUri)
    }

    @Test
    fun `preserves completed page mapping when image export partially fails`() {
        val firstOutputUri = Uri.parse("file:///documents/page-1.jpg")
        val exportRepository = RecordingExportRepository(
            copyPdfResult = ExportOperationResult.Failure(
                ExportUnknownFailure(details = "PDF export not expected in this test"),
            ),
            imageResults = mutableListOf(
                ExportOperationResult.Success(firstOutputUri),
                ExportOperationResult.Failure(
                    ExportUnknownFailure(details = "second page failed"),
                ),
            ),
        )
        val artifactCacheRepository = RecordingExportArtifactCacheRepository()
        val useCase = ExportSessionUseCase(
            exportRepository = exportRepository,
            exportArtifactCacheRepository = artifactCacheRepository,
        )
        val session = ScanSession(
            id = "session_image_partial",
            pages = listOf(
                ScannedPage(
                    id = "session_image_partial_page_0",
                    originalSourceUri = Uri.parse("content://scanner/page-1.jpg"),
                ),
                ScannedPage(
                    id = "session_image_partial_page_1",
                    originalSourceUri = Uri.parse("content://scanner/page-2.jpg"),
                ),
            ),
            dirtyFlags = SessionDirtyFlags(hasAddedPages = true),
        )

        val result = runBlocking {
            useCase(
                session = session,
                request = ExportRequest(sessionId = session.id, format = ExportFormat.IMAGE),
                context = RuntimeEnvironment.getApplication(),
            )
        }

        assertTrue(result is ExportResult.PartialSuccess)
        val partialSuccess = result as ExportResult.PartialSuccess
        assertEquals(1, partialSuccess.completedPages.size)
        assertEquals(session.pages.first().id, partialSuccess.completedPages.single().pageId)
        assertEquals(firstOutputUri, partialSuccess.completedPages.single().outputUri)
        assertEquals(listOf(firstOutputUri), partialSuccess.outputUris)
        assertEquals(1, partialSuccess.failures.size)
        assertEquals(UiText.DynamicString("second page failed"), partialSuccess.failures.single().userMessage)
        assertTrue(partialSuccess.warningMessage is UiText.StringResource)
        assertEquals(
            RuntimeEnvironment.getApplication().getString(
                com.example.pixelscanner.R.string.export_warning_partial_success,
                1,
                2,
            ),
            partialSuccess.warningMessage?.asString(RuntimeEnvironment.getApplication()),
        )
        assertEquals(2, exportRepository.savedUriRequests.size)
        assertEquals(1, artifactCacheRepository.storedImageRequests.size)
    }

    @Test
    fun `exports pages in the reordered session order`() {
        val exportRepository = RecordingExportRepository(
            copyPdfResult = ExportOperationResult.Failure(
                ExportUnknownFailure(details = "PDF export not expected in this test"),
            ),
            imageResults = mutableListOf(
                ExportOperationResult.Success(Uri.parse("file:///documents/page-b.jpg")),
                ExportOperationResult.Success(Uri.parse("file:///documents/page-a.jpg")),
                ExportOperationResult.Success(Uri.parse("file:///documents/page-c.jpg")),
            ),
        )
        val artifactCacheRepository = RecordingExportArtifactCacheRepository()
        val useCase = ExportSessionUseCase(
            exportRepository = exportRepository,
            exportArtifactCacheRepository = artifactCacheRepository,
        )
        val originalSession = ScanSession(
            id = "session_reordered_export",
            pages = listOf(
                ScannedPage(
                    id = "page-a",
                    originalSourceUri = Uri.parse("content://scanner/page-a.jpg"),
                ),
                ScannedPage(
                    id = "page-b",
                    originalSourceUri = Uri.parse("content://scanner/page-b.jpg"),
                ),
                ScannedPage(
                    id = "page-c",
                    originalSourceUri = Uri.parse("content://scanner/page-c.jpg"),
                ),
            ),
        )
        val reorderedSession = ReorderPageUseCase().invoke(
            session = originalSession,
            fromActivePageIndex = 1,
            toActivePageIndex = 0,
        )!!.updatedSession

        val result = runBlocking {
            useCase(
                session = reorderedSession,
                request = ExportRequest(sessionId = reorderedSession.id, format = ExportFormat.IMAGE),
                context = RuntimeEnvironment.getApplication(),
            )
        }

        assertTrue(result is ExportResult.Success)
        assertEquals(
            listOf(
                Uri.parse("content://scanner/page-b.jpg"),
                Uri.parse("content://scanner/page-a.jpg"),
                Uri.parse("content://scanner/page-c.jpg"),
            ),
            exportRepository.savedUriRequests.map { it.sourceUri },
        )
    }

    private class RecordingExportRepository(
        private val copyPdfResult: ExportOperationResult,
        private val imageResults: MutableList<ExportOperationResult> = mutableListOf(),
    ) : ExportRepository {
        val copiedPdfRequests = mutableListOf<ExportPdfRequest>()
        val savedUriRequests = mutableListOf<ExportImageRequest>()
        val savedPdfRequests = mutableListOf<ExportPdfBuildRequest>()

        override suspend fun copyPdfToDocuments(
            context: Context,
            sourceUri: Uri,
            fileName: String,
        ): ExportOperationResult {
            copiedPdfRequests += ExportPdfRequest(sourceUri = sourceUri, fileName = fileName)
            return copyPdfResult
        }

        override suspend fun saveUriToGallery(
            context: Context,
            sourceUri: Uri,
            fileName: String,
        ): ExportOperationResult {
            savedUriRequests += ExportImageRequest(sourceUri = sourceUri, fileName = fileName)
            return if (imageResults.isNotEmpty()) {
                imageResults.removeAt(0)
            } else {
                ExportOperationResult.Failure(
                    ExportUnknownFailure(
                        details = "Image export not expected in this test",
                    ),
                )
            }
        }

        override suspend fun saveUrisToPdf(
            context: Context,
            sourceUris: List<Uri>,
            fileName: String,
        ): ExportOperationResult {
            savedPdfRequests += ExportPdfBuildRequest(sourceUris = sourceUris, fileName = fileName)
            return ExportOperationResult.Failure(
                com.example.pixelscanner.core.model.ExportUnknownFailure(
                    details = "PDF rebuild not expected in this test",
                ),
            )
        }
    }

    private class RecordingExportArtifactCacheRepository : ExportArtifactCacheRepository {
        val cachedPdfRequests = mutableListOf<ExportImagePipelineRequest>()
        val storedPdfRequests = mutableListOf<StoredPdfArtifact>()
        val cachedImageRequests = mutableListOf<CachedImageArtifactRequest>()
        val storedImageRequests = mutableListOf<StoredImageArtifact>()

        override fun cleanStaleTempArtifacts(context: Context) = Unit

        override fun getCachedPdfArtifact(
            context: Context,
            request: ExportImagePipelineRequest,
        ): File? {
            cachedPdfRequests += request
            return null
        }

        override fun storePdfArtifact(
            context: Context,
            request: ExportImagePipelineRequest,
            sourceUri: Uri,
        ): File {
            storedPdfRequests += StoredPdfArtifact(request = request, sourceUri = sourceUri)
            return File("build/test-export-session.pdf")
        }

        override fun getCachedImageArtifacts(
            context: Context,
            request: ExportImagePipelineRequest,
            pageCount: Int,
        ): List<File>? {
            cachedImageRequests += CachedImageArtifactRequest(request = request, pageCount = pageCount)
            return null
        }

        override fun storeImageArtifact(
            context: Context,
            request: ExportImagePipelineRequest,
            pageIndex: Int,
            sourceUri: Uri,
        ): File {
            storedImageRequests += StoredImageArtifact(
                request = request,
                pageIndex = pageIndex,
                sourceUri = sourceUri,
            )
            return File("build/test-export-session-$pageIndex.jpg")
        }
    }

    private data class StoredPdfArtifact(
        val request: ExportImagePipelineRequest,
        val sourceUri: Uri,
    )

    private data class CachedImageArtifactRequest(
        val request: ExportImagePipelineRequest,
        val pageCount: Int,
    )

    private data class StoredImageArtifact(
        val request: ExportImagePipelineRequest,
        val pageIndex: Int,
        val sourceUri: Uri,
    )

}
