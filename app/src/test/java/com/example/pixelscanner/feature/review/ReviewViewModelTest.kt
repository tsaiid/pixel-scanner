package com.example.pixelscanner.feature.review

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.domain.CreateSessionFromScanResultUseCase
import com.example.pixelscanner.domain.DeletePageUseCase
import com.example.pixelscanner.domain.DiscardSessionUseCase
import com.example.pixelscanner.domain.EagerSessionIngestionUseCase
import com.example.pixelscanner.domain.ExportArtifactCacheRepository
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
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent
import com.example.pixelscanner.testing.FakeExportRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReviewViewModelTest {

    @Test
    fun `defaults to empty review state before a session is loaded`() {
        val viewModel = createViewModel()

        assertEquals(0, viewModel.uiState.value.currentPage)
        assertTrue(viewModel.uiState.value.pages.isEmpty())
        assertFalse(viewModel.uiState.value.hasPages)
        assertEquals(ExportFormat.IMAGE, viewModel.uiState.value.export.format)
        assertNull(viewModel.uiState.value.selectedPage)
    }

    @Test
    fun `updates selected page when page changed after restoring a session`() {
        val viewModel = createViewModel()
        val restoredSession = ScanSession(id = "session_review_test", pages = emptyList())

        viewModel.setRestoredSession(restoredSession)
        viewModel.onEvent(ReviewUiEvent.PageChanged(pageIndex = 1))

        assertEquals(0, viewModel.uiState.value.currentPage)
        assertTrue(viewModel.uiState.value.pages.isEmpty())
    }

    @Test
    fun `updates export format when change export format event is received`() {
        val viewModel = createViewModel()

        viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))

        assertEquals(ExportFormat.PDF, viewModel.uiState.value.export.format)
    }

    @Test
    fun `restores saveable state across configuration change style recreation`() {
        val savedStateHandle = SavedStateHandle()
        savedStateHandle[KEY_CURRENT_PAGE] = 1
        savedStateHandle[KEY_EXPORT_FORMAT] = ExportFormat.PDF.name
        savedStateHandle[KEY_IS_AWAITING_SCAN_MORE_RESULT] = true

        val firstViewModel = createViewModel(savedStateHandle = savedStateHandle)

        val recreatedViewModel = createViewModel(savedStateHandle = savedStateHandle)

        assertEquals(1, recreatedViewModel.uiState.value.currentPage)
        assertEquals(ExportFormat.PDF, recreatedViewModel.uiState.value.export.format)
        assertTrue(recreatedViewModel.uiState.value.isAwaitingScanMoreResult)
        assertEquals(firstViewModel.uiState.value.currentPage, recreatedViewModel.uiState.value.currentPage)
    }

    @Test
    fun `restores saveable state from a snapshot after process death style recreation`() {
        val savedStateHandle = SavedStateHandle()
        savedStateHandle[KEY_CURRENT_PAGE] = 1
        savedStateHandle[KEY_EXPORT_FORMAT] = ExportFormat.PDF.name
        savedStateHandle[KEY_IS_AWAITING_SCAN_MORE_RESULT] = true

        val firstViewModel = createViewModel(savedStateHandle = savedStateHandle)
        val snapshot = savedStateHandle.keys().associateWith { key ->
            savedStateHandle.get<Any?>(key)
        }
        val restoredViewModel = createViewModel(
            savedStateHandle = SavedStateHandle(snapshot),
        )

        assertEquals(firstViewModel.uiState.value.currentPage, restoredViewModel.uiState.value.currentPage)
        assertEquals(ExportFormat.PDF, restoredViewModel.uiState.value.export.format)
        assertTrue(restoredViewModel.uiState.value.isAwaitingScanMoreResult)
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        previewCacheCleanupRepository: RecordingPreviewCacheCleanupRepository = RecordingPreviewCacheCleanupRepository(),
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
            exportSessionUseCase = ExportSessionUseCase(
                exportRepository = FakeExportRepository(),
                exportArtifactCacheRepository = NoOpExportArtifactCacheRepository(),
            ),
            discardSessionUseCase = DiscardSessionUseCase(),
            previewCacheCleanupRepository = previewCacheCleanupRepository,
            resumeSessionUseCase = ResumeSessionUseCase(),
            scanMoreUseCase = ScanMoreUseCase(),
            savedStateHandle = savedStateHandle,
        )
    }

    private class RecordingPreviewCacheCleanupRepository : PreviewCacheCleanupRepository {
        val cleanedSessionIds = mutableListOf<String>()

        override fun cleanSessionPreviewCache(sessionId: String) {
            cleanedSessionIds += sessionId
        }
    }

    private class NoOpExportArtifactCacheRepository : ExportArtifactCacheRepository {
        override fun cleanStaleTempArtifacts(context: Context) = Unit

        override fun getCachedPdfArtifact(
            context: Context,
            request: ExportImagePipelineRequest,
        ): File? = null

        override fun storePdfArtifact(
            context: Context,
            request: ExportImagePipelineRequest,
            sourceUri: Uri,
        ): File = File(context.cacheDir, "review-test.pdf")

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
        ): File = File(context.cacheDir, "review-test-$pageIndex.jpg")
    }
}

private const val KEY_CURRENT_PAGE = "review_current_page"
private const val KEY_EXPORT_FORMAT = "review_export_format"
private const val KEY_IS_AWAITING_SCAN_MORE_RESULT = "review_is_awaiting_scan_more_result"
