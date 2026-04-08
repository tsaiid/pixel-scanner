package com.example.pixelscanner.feature.review.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.SavedStateHandle
import com.example.pixelscanner.R
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.domain.CreateSessionFromScanResultUseCase
import com.example.pixelscanner.domain.DeletePageUseCase
import com.example.pixelscanner.domain.DiscardSessionUseCase
import com.example.pixelscanner.domain.EagerSessionIngestionUseCase
import com.example.pixelscanner.domain.ExportArtifactCacheRepository
import com.example.pixelscanner.domain.ExportRepository
import com.example.pixelscanner.domain.ExportSessionUseCase
import com.example.pixelscanner.domain.ImageProcessingRepository
import com.example.pixelscanner.domain.PreviewCacheCleanupRepository
import com.example.pixelscanner.domain.ResumeSessionUseCase
import com.example.pixelscanner.domain.RestorePageUseCase
import com.example.pixelscanner.domain.ScanMoreUseCase
import com.example.pixelscanner.domain.SessionIngestionRepository
import com.example.pixelscanner.domain.SessionIngestionResult
import com.example.pixelscanner.domain.SessionIngestionStatus
import com.example.pixelscanner.domain.ToggleEnhancementUseCase
import com.example.pixelscanner.feature.review.ReviewViewModel
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.PageChanged
import com.example.pixelscanner.testing.FakeExportRepository
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReviewThumbnailStripLargePageCountTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `selected thumbnail remains visible in a 100 page session`() {
        val viewModel = createViewModel()
        val session = buildSession(pageCount = 100)

        viewModel.setRestoredSession(session)
        viewModel.onEvent(PageChanged(pageIndex = 99))

        composeRule.setContent {
            ReviewScreen(
                viewModel = viewModel,
                onExportSuccess = { _, _ -> },
                onNavigateToScanner = { _ -> },
                onNavigateToHome = { _ -> },
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("review_thumbnail_99").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            RuntimeEnvironment.getApplication().getString(
                R.string.review_thumbnail_selected_content_description,
                100,
                100,
            ),
        ).assertIsDisplayed()
    }

    private fun createViewModel(): ReviewViewModel {
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
            exportSessionUseCase = ExportSessionUseCase(
                exportRepository = FakeExportRepository(),
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
                    ): File = File(context.cacheDir, "review-thumbnail-strip-test.pdf")

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
                    ): File = File(context.cacheDir, "review-thumbnail-strip-test-$pageIndex.jpg")
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
            id = "session_large_thumbnail_strip",
            pages = List(pageCount) { index ->
                ScannedPage(
                    id = "page-$index",
                    originalSourceUri = Uri.parse("content://scanner/page-$index.jpg"),
                )
            },
        )
    }
}
