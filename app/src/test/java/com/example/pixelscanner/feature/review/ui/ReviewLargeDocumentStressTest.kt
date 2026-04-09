package com.example.pixelscanner.feature.review.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.SavedStateHandle
import com.example.pixelscanner.R
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.ExportOperationResult
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.core.model.SessionDirtyFlags
import com.example.pixelscanner.domain.CreateSessionFromScanResultUseCase
import com.example.pixelscanner.domain.DeletePageUseCase
import com.example.pixelscanner.domain.DiscardSessionUseCase
import com.example.pixelscanner.domain.EagerSessionIngestionUseCase
import com.example.pixelscanner.domain.ExportArtifactCacheRepository
import com.example.pixelscanner.domain.ExportRepository
import com.example.pixelscanner.domain.ExportSessionUseCase
import com.example.pixelscanner.domain.ImageProcessingRepository
import com.example.pixelscanner.domain.PreviewCacheCleanupRepository
import com.example.pixelscanner.domain.ReorderPageUseCase
import com.example.pixelscanner.domain.ResumeSessionUseCase
import com.example.pixelscanner.domain.RestorePageUseCase
import com.example.pixelscanner.domain.ScanMoreUseCase
import com.example.pixelscanner.domain.SessionIngestionRepository
import com.example.pixelscanner.domain.SessionIngestionResult
import com.example.pixelscanner.domain.SessionIngestionStatus
import com.example.pixelscanner.domain.ToggleEnhancementUseCase
import com.example.pixelscanner.feature.review.ReviewViewModel
import com.example.pixelscanner.testing.FakeExportRepository
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReviewLargeDocumentStressTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `50 page session supports scroll reorder and export`() {
        runStressScenario(pageCount = 50)
    }

    @Test
    fun `100 page session supports scroll reorder and export`() {
        runStressScenario(pageCount = 100)
    }

    private fun runStressScenario(pageCount: Int) {
        val exportSuccessCount = AtomicInteger(0)
        val exportRepository = FakeExportRepository(
            saveUrisToPdfResult = ExportOperationResult.Success(
                Uri.parse("content://exports/rebuilt-$pageCount.pdf"),
            ),
        )
        val viewModel = createViewModel(exportRepository)
        val session = buildSession(pageCount)
        val appContext = RuntimeEnvironment.getApplication()

        viewModel.setRestoredSession(session)
        viewModel.onEvent(ReviewUiEvent.PageChanged(0))
        viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))

        composeRule.setContent {
            ReviewScreen(
                viewModel = viewModel,
                onExportSuccess = { format, _ ->
                    if (format == ExportFormat.PDF) {
                        exportSuccessCount.incrementAndGet()
                    }
                },
                onNavigateToScanner = { _ -> },
                onNavigateToHome = { _ -> },
            )
        }

        composeRule.waitForIdle()

        val lastIndex = pageCount - 1
        composeRule.onNodeWithTag("review_thumbnail_strip")
            .performScrollToNode(hasTestTag("review_thumbnail_$lastIndex"))
        composeRule.onNodeWithTag("review_thumbnail_$lastIndex").assertIsDisplayed()
        composeRule.onNodeWithTag("review_thumbnail_$lastIndex").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(
            appContext.getString(
                R.string.review_thumbnail_selected_content_description,
                pageCount,
                pageCount,
            ),
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.review_move_page_left_content_description),
        ).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(
            appContext.getString(
                R.string.review_thumbnail_selected_content_description,
                pageCount - 1,
                pageCount,
            ),
        ).assertIsDisplayed()

        composeRule.onNodeWithTag("review_save_button").performClick()
        composeRule.waitForIdle()

        assertEquals(1, exportRepository.savedPdfRequests.size)
        assertEquals(pageCount, exportRepository.savedPdfRequests.single().sourceUris.size)
        assertEquals(1, exportSuccessCount.get())
    }

    private fun createViewModel(exportRepository: ExportRepository): ReviewViewModel {
        return ReviewViewModel(
            createSessionFromScanResultUseCase = CreateSessionFromScanResultUseCase(),
            eagerSessionIngestionUseCase = EagerSessionIngestionUseCase(
                sessionIngestionRepository = object : SessionIngestionRepository {
                    override suspend fun ingest(
                        session: ScanSession,
                        scanResult: ScanResultPayload,
                    ): SessionIngestionResult {
                        return SessionIngestionResult(status = SessionIngestionStatus.SUCCESS)
                    }
                },
            ),
            toggleEnhancementUseCase = ToggleEnhancementUseCase(
                object : ImageProcessingRepository {
                    override suspend fun enhance(
                        context: Context,
                        request: PreviewImagePipelineRequest,
                    ): PreviewImagePipelineResult {
                        throw AssertionError("Not used in this test")
                    }
                },
            ),
            deletePageUseCase = DeletePageUseCase(),
            restorePageUseCase = RestorePageUseCase(),
            reorderPageUseCase = ReorderPageUseCase(),
            exportSessionUseCase = ExportSessionUseCase(
                exportRepository = exportRepository,
                exportArtifactCacheRepository = object : ExportArtifactCacheRepository {
                    override fun cleanStaleTempArtifacts(context: Context) = Unit

                    override fun getCachedPdfArtifact(
                        context: Context,
                        request: ExportImagePipelineRequest,
                    ): File? = null

                    override fun storePdfArtifact(
                        context: Context,
                        request: ExportImagePipelineRequest,
                        sourceUri: Uri,
                    ): File = File(context.cacheDir, "review-large-document-${request.sessionId}.pdf")

                    override fun getCachedImageArtifacts(
                        context: Context,
                        request: ExportImagePipelineRequest,
                        pageCount: Int,
                    ): List<File>? = null

                    override fun storeImageArtifact(
                        context: Context,
                        request: ExportImagePipelineRequest,
                        pageIndex: Int,
                        sourceUri: Uri,
                    ): File = File(context.cacheDir, "review-large-document-$pageIndex.jpg")
                },
            ),
            discardSessionUseCase = DiscardSessionUseCase(),
            previewCacheCleanupRepository = object : PreviewCacheCleanupRepository {
                override fun cleanSessionPreviewCache(sessionId: String) = Unit
            },
            resumeSessionUseCase = ResumeSessionUseCase(),
            scanMoreUseCase = ScanMoreUseCase(),
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun buildSession(pageCount: Int): ScanSession {
        return ScanSession(
            id = "session_large_document_$pageCount",
            pages = List(pageCount) { index ->
                ScannedPage(
                    id = "page-$index",
                    originalSourceUri = Uri.parse("content://scanner/page-$index.jpg"),
                )
            },
            originalPdfUri = Uri.parse("content://scanner/original-$pageCount.pdf"),
            dirtyFlags = SessionDirtyFlags(hasReorderedPages = true),
        )
    }
}
