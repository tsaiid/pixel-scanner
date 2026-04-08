package com.example.pixelscanner.feature.review

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.ExportOperationResult
import com.example.pixelscanner.core.model.ExportState
import com.example.pixelscanner.core.model.ExportWriteFailure
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.core.model.ProcessingState
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
import com.example.pixelscanner.domain.ResumeSessionUseCase
import com.example.pixelscanner.domain.RestorePageUseCase
import com.example.pixelscanner.domain.ScanMoreUseCase
import com.example.pixelscanner.domain.SessionIngestionRepository
import com.example.pixelscanner.domain.SessionIngestionResult
import com.example.pixelscanner.domain.SessionIngestionStatus
import com.example.pixelscanner.domain.ToggleEnhancementUseCase
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent
import com.example.pixelscanner.feature.review.ui.ReviewUiEffect
import com.example.pixelscanner.testing.FakeExportRepository
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
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
    fun `request rescan clears review session state and emits navigate back to scanner`() = runBlocking {
        val cleanupRepository = RecordingPreviewCacheCleanupRepository()
        val viewModel = createViewModel(previewCacheCleanupRepository = cleanupRepository)
        val session = loadedSession(sessionId = "session_review_rescan")

        viewModel.setRestoredSession(session)
        viewModel.onEvent(ReviewUiEvent.PageChanged(pageIndex = 1))
        viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))

        val firstEffect = CompletableDeferred<ReviewUiEffect>()
        val effectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.collect { effect ->
                if (!firstEffect.isCompleted) {
                    firstEffect.complete(effect)
                }
            }
        }

        try {
            viewModel.onEvent(ReviewUiEvent.RequestRescan)
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(
                ReviewUiEffect.NavigateBackToScanner,
                withTimeout(1_000) { firstEffect.await() },
            )
            assertEquals(listOf(session.id), cleanupRepository.cleanedSessionIds)
            assertNull(viewModel.scanSession.value)
            assertTrue(viewModel.uiState.value.pages.isEmpty())
            assertFalse(viewModel.uiState.value.hasPages)
            assertEquals(0, viewModel.uiState.value.currentPage)
            assertEquals(ExportFormat.IMAGE, viewModel.uiState.value.export.format)
        } finally {
            effectJob.cancel()
        }
    }

    @Test
    fun `request discard session clears review session state and emits navigate back to home`() = runBlocking {
        val cleanupRepository = RecordingPreviewCacheCleanupRepository()
        val viewModel = createViewModel(previewCacheCleanupRepository = cleanupRepository)
        val session = loadedSession(sessionId = "session_review_discard")

        viewModel.setRestoredSession(session)
        viewModel.onEvent(ReviewUiEvent.PageChanged(pageIndex = 1))
        viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))

        val firstEffect = CompletableDeferred<ReviewUiEffect>()
        val effectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.collect { effect ->
                if (!firstEffect.isCompleted) {
                    firstEffect.complete(effect)
                }
            }
        }

        try {
            viewModel.onEvent(ReviewUiEvent.RequestDiscardSession)
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(
                ReviewUiEffect.NavigateBackToHome,
                withTimeout(1_000) { firstEffect.await() },
            )
            assertEquals(listOf(session.id), cleanupRepository.cleanedSessionIds)
            assertNull(viewModel.scanSession.value)
            assertTrue(viewModel.uiState.value.pages.isEmpty())
            assertFalse(viewModel.uiState.value.hasPages)
            assertEquals(0, viewModel.uiState.value.currentPage)
            assertEquals(ExportFormat.IMAGE, viewModel.uiState.value.export.format)
        } finally {
            effectJob.cancel()
        }
    }

    @Test
    fun `export failure is surfaced in review state and can be cleared explicitly`() = runBlocking {
        val exportRepository = FakeExportRepository(
            saveUrisToPdfResult = ExportOperationResult.Failure(
                ExportWriteFailure(details = "Failed to write review export"),
            ),
        )
        val viewModel = createViewModel(exportRepository = exportRepository)
        val session = loadedSession(sessionId = "session_review_export_failure")

        viewModel.setRestoredSession(session)
        viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))
        viewModel.onEvent(ReviewUiEvent.SaveAll(RuntimeEnvironment.getApplication()))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            "Failed to write review export",
            viewModel.uiState.value.statusMessage?.text,
        )
        assertTrue(viewModel.uiState.value.export.lastResult is ExportState.Finished)

        viewModel.onEvent(ReviewUiEvent.ClearStatusMessage)

        assertNull(viewModel.uiState.value.statusMessage)
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

    @Test
    fun `restores page processing state to idle after repeated enhancement toggle cancellation`() {
        val imageProcessingRepository = HangingImageProcessingRepository()
        val viewModel = createViewModel(imageProcessingRepository = imageProcessingRepository)
        val context = RuntimeEnvironment.getApplication()
        val session = ScanSession(
            id = "session_review_toggle_cancel",
            pages = listOf(
                ScannedPage(
                    id = "session_review_toggle_cancel_page_0",
                    originalSourceUri = Uri.parse("content://scanner/page-0.jpg"),
                ),
            ),
        )

        viewModel.setRestoredSession(session)

        viewModel.onEvent(ReviewUiEvent.ToggleEnhancement(pageIndex = 0, context = context))
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(imageProcessingRepository.firstInvocationStarted.await(1, TimeUnit.SECONDS))
        assertEquals(1, imageProcessingRepository.invocationCount.get())
        assertEquals(
            ProcessingState.PROCESSING,
            viewModel.scanSession.value?.pages?.single()?.processingState,
        )

        viewModel.onEvent(ReviewUiEvent.ToggleEnhancement(pageIndex = 0, context = context))
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(imageProcessingRepository.firstInvocationCancelled.await(1, TimeUnit.SECONDS))
        assertEquals(2, imageProcessingRepository.invocationCount.get())
        assertEquals(
            ProcessingState.IDLE,
            viewModel.scanSession.value?.pages?.single()?.processingState,
        )
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        previewCacheCleanupRepository: RecordingPreviewCacheCleanupRepository = RecordingPreviewCacheCleanupRepository(),
        exportRepository: com.example.pixelscanner.domain.ExportRepository = FakeExportRepository(),
        imageProcessingRepository: ImageProcessingRepository = object : ImageProcessingRepository {
            override suspend fun enhance(
                context: Context,
                request: PreviewImagePipelineRequest,
            ): PreviewImagePipelineResult {
                throw AssertionError("Not used in this test")
            }
        },
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
                imageProcessingRepository,
            ),
            deletePageUseCase = DeletePageUseCase(),
            restorePageUseCase = RestorePageUseCase(),
            exportSessionUseCase = ExportSessionUseCase(
                exportRepository = exportRepository,
                exportArtifactCacheRepository = NoOpExportArtifactCacheRepository(),
            ),
            discardSessionUseCase = DiscardSessionUseCase(),
            previewCacheCleanupRepository = previewCacheCleanupRepository,
            resumeSessionUseCase = ResumeSessionUseCase(),
            scanMoreUseCase = ScanMoreUseCase(),
            savedStateHandle = savedStateHandle,
        )
    }

    private class HangingImageProcessingRepository : ImageProcessingRepository {
        val invocationCount = AtomicInteger(0)
        val firstInvocationStarted = CountDownLatch(1)
        val firstInvocationCancelled = CountDownLatch(1)

        override suspend fun enhance(
            context: Context,
            request: PreviewImagePipelineRequest,
        ): PreviewImagePipelineResult {
            val invocation = invocationCount.incrementAndGet()
            if (invocation == 1) {
                firstInvocationStarted.countDown()
                try {
                    // Keep the first coroutine suspended until it is cancelled by the repeated toggle.
                    awaitCancellation()
                } finally {
                    firstInvocationCancelled.countDown()
                }
            }
            return PreviewImagePipelineResult(
                cacheKey = request.cacheKey,
                outputUri = Uri.parse("content://scanner/enhanced-${request.pageId}.jpg"),
            )
        }
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

    private fun loadedSession(sessionId: String): ScanSession {
        return ScanSession(
            id = sessionId,
            pages = listOf(
                ScannedPage(
                    id = "${sessionId}_page_0",
                    originalSourceUri = Uri.parse("content://scanner/page-0.jpg"),
                ),
                ScannedPage(
                    id = "${sessionId}_page_1",
                    originalSourceUri = Uri.parse("content://scanner/page-1.jpg"),
                ),
            ),
        )
    }
}

private const val KEY_CURRENT_PAGE = "review_current_page"
private const val KEY_EXPORT_FORMAT = "review_export_format"
private const val KEY_IS_AWAITING_SCAN_MORE_RESULT = "review_is_awaiting_scan_more_result"
