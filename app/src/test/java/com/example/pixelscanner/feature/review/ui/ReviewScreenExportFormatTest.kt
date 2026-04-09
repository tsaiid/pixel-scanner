package com.example.pixelscanner.feature.review.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReviewScreenExportFormatTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `switching export format updates save button label`() {
        val viewModel = createViewModel()
        val session = buildSession(pageCount = 2)
        val appContext = RuntimeEnvironment.getApplication()

        viewModel.setRestoredSession(session)

        composeRule.setContent {
            ReviewScreen(
                viewModel = viewModel,
                onExportSuccess = { _, _ -> },
                onNavigateToScanner = { _ -> },
                onNavigateToHome = { _ -> },
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.export_save_button_jpeg))
            .assertIsDisplayed()

        composeRule.onNodeWithText(appContext.getString(R.string.export_format_pdf))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.export_save_button_pdf))
            .assertIsDisplayed()

        composeRule.onNodeWithText(appContext.getString(R.string.export_format_jpeg))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.export_save_button_jpeg))
            .assertIsDisplayed()
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
            reorderPageUseCase = ReorderPageUseCase(),
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
                    ): File = File(context.cacheDir, "review-export-format-test.pdf")

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
                    ): File = File(context.cacheDir, "review-export-format-test-$pageIndex.jpg")
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
            id = "session_export_format_test",
            pages = List(pageCount) { index ->
                ScannedPage(
                    id = "page-$index",
                    originalSourceUri = Uri.parse("content://scanner/page-$index.jpg"),
                )
            },
        )
    }
}
