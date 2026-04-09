package com.example.pixelscanner.feature.review.ui

import android.content.Context
import android.provider.Settings
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.example.pixelscanner.R
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.ExportOperationResult
import com.example.pixelscanner.core.model.ExportPermissionDeniedFailure
import com.example.pixelscanner.core.model.ExportSourceInvalidFailure
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
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.ChangeExportFormat
import com.example.pixelscanner.testing.FakeExportRepository
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReviewScreenFallbackActionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `source-missing export fallback requires rescan confirmation before navigation`() {
        val exportRepository = FakeExportRepository(
            saveUrisToPdfResult = ExportOperationResult.Failure(
                ExportSourceInvalidFailure(details = "Source missing"),
            ),
        )
        val viewModel = createViewModel(exportRepository)
        val session = buildSession(pageCount = 2)
        val scannerNavigationReasons = mutableListOf<ScannerNavigationReason>()

        viewModel.setRestoredSession(session)
        viewModel.onEvent(ChangeExportFormat(com.example.pixelscanner.core.model.ExportFormat.PDF))

        composeRule.setContent {
            ReviewScreen(
                viewModel = viewModel,
                onExportSuccess = { _, _ -> },
                onNavigateToScanner = { reason -> scannerNavigationReasons += reason },
                onNavigateToHome = { _ -> },
            )
        }

        composeRule.waitForIdle()

        val appContext = RuntimeEnvironment.getApplication()
        composeRule.onNodeWithTag("review_export_fallback_rescan").assertDoesNotExist()

        composeRule.onNodeWithText(appContext.getString(R.string.export_save_button_pdf)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("review_export_fallback_rescan").assertExists().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.review_rescan_confirm_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.review_rescan_confirm_message))
            .assertIsDisplayed()
        assertTrue(scannerNavigationReasons.isEmpty())
    }

    @Test
    fun `permission denied export fallback opens app settings`() {
        val exportRepository = FakeExportRepository(
            saveUrisToPdfResult = ExportOperationResult.Failure(
                ExportPermissionDeniedFailure,
            ),
        )
        val viewModel = createViewModel(exportRepository)
        val session = buildSession(pageCount = 2)

        viewModel.setRestoredSession(session)
        viewModel.onEvent(ChangeExportFormat(com.example.pixelscanner.core.model.ExportFormat.PDF))

        composeRule.setContent {
            ReviewScreen(
                viewModel = viewModel,
                onExportSuccess = { _, _ -> },
                onNavigateToScanner = { _ -> },
                onNavigateToHome = { _ -> },
            )
        }

        composeRule.waitForIdle()

        val appContext = RuntimeEnvironment.getApplication()
        composeRule.onNodeWithText(appContext.getString(R.string.export_save_button_pdf)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("review_export_fallback_settings").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        val startedIntent = org.robolectric.Shadows.shadowOf(appContext).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, startedIntent.action)
    }

    @Test
    fun `empty session export failure keeps scan more and discard actions visible`() {
        val exportRepository = FakeExportRepository(
            saveUrisToPdfResult = ExportOperationResult.Failure(
                com.example.pixelscanner.core.model.ExportNoPagesFailure,
            ),
        )
        val viewModel = createViewModel(exportRepository)
        val emptySession = ScanSession(
            id = "session_fallback_empty",
            pages = emptyList(),
        )

        viewModel.setRestoredSession(emptySession)
        viewModel.onEvent(ChangeExportFormat(com.example.pixelscanner.core.model.ExportFormat.PDF))

        composeRule.setContent {
            ReviewScreen(
                viewModel = viewModel,
                onExportSuccess = { _, _ -> },
                onNavigateToScanner = { _ -> },
                onNavigateToHome = { _ -> },
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("review_empty_scan_more")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("review_empty_discard_session")
            .assertIsDisplayed()
    }

    private fun createViewModel(
        exportRepository: FakeExportRepository,
    ): ReviewViewModel {
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
                    ): File = File(context.cacheDir, "review-fallback-test.pdf")

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
                    ): File = File(context.cacheDir, "review-fallback-test-$pageIndex.jpg")
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
            id = "session_fallback_test",
            pages = List(pageCount) { index ->
                ScannedPage(
                    id = "page-$index",
                    originalSourceUri = Uri.parse("content://scanner/page-$index.jpg"),
                )
            },
        )
    }
}
