package com.example.pixelscanner.feature.review

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import com.example.pixelscanner.R
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.ExportOperationResult
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.ExportNoPagesFailure
import com.example.pixelscanner.core.model.ExportPermissionDeniedFailure
import com.example.pixelscanner.core.model.ExportSourceInvalidFailure
import com.example.pixelscanner.core.model.ExportState
import com.example.pixelscanner.core.model.ExportWriteFailure
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.core.model.ProcessingState
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.core.model.UiText
import com.example.pixelscanner.core.model.uiTextResource
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
import com.example.pixelscanner.feature.review.ui.HomeNavigationReason
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent
import com.example.pixelscanner.feature.review.ui.ReviewUiEffect
import com.example.pixelscanner.feature.review.ui.ScannerNavigationReason
import com.example.pixelscanner.feature.export.ui.ExportFallbackAction
import com.example.pixelscanner.testing.FakeExportRepository
import com.example.pixelscanner.testing.ExportImageRequest
import com.example.pixelscanner.testing.ExportPdfBuildRequest
import com.example.pixelscanner.testing.ExportPdfRequest
import java.io.File
import java.util.ArrayDeque
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
import org.junit.Assert.assertNotNull
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
                ReviewUiEffect.NavigateToScanner(ScannerNavigationReason.RESCAN),
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
                ReviewUiEffect.NavigateToHome(HomeNavigationReason.DISCARD_SESSION),
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
    fun `request scan more marks append flow and emits navigate to scanner with scan more reason`() = runBlocking {
        val viewModel = createViewModel()
        val session = loadedSession(sessionId = "session_review_scan_more")

        viewModel.setRestoredSession(session)

        val firstEffect = CompletableDeferred<ReviewUiEffect>()
        val effectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.collect { effect ->
                if (!firstEffect.isCompleted) {
                    firstEffect.complete(effect)
                }
            }
        }

        try {
            viewModel.onEvent(ReviewUiEvent.RequestScanMore)
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(
                ReviewUiEffect.NavigateToScanner(ScannerNavigationReason.SCAN_MORE),
                withTimeout(1_000) { firstEffect.await() },
            )
            assertTrue(viewModel.uiState.value.isAwaitingScanMoreResult)
            assertEquals(session.pages.size, viewModel.uiState.value.pages.size)
            assertEquals(session, viewModel.scanSession.value)
        } finally {
            effectJob.cancel()
        }
    }

    @Test
    fun `deleting the last page keeps the review session available for undo`() = runBlocking {
        val viewModel = createViewModel()
        val session = ScanSession(
            id = "session_review_delete_last_page",
            pages = listOf(
                ScannedPage(
                    id = "session_review_delete_last_page_page_0",
                    originalSourceUri = Uri.parse("content://scanner/page-0.jpg"),
                ),
            ),
        )

        viewModel.setRestoredSession(session)

        val firstEffect = CompletableDeferred<ReviewUiEffect>()
        val effectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.collect { effect ->
                if (!firstEffect.isCompleted) {
                    firstEffect.complete(effect)
                }
            }
        }

        try {
            viewModel.onEvent(ReviewUiEvent.DeletePage(pageIndex = 0))
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(
                ReviewUiEffect.ShowDeletedPageUndo(session.pages.single().id),
                withTimeout(1_000) { firstEffect.await() },
            )
            assertNotNull(viewModel.scanSession.value)
            assertTrue(viewModel.uiState.value.pages.isEmpty())
            assertFalse(viewModel.uiState.value.hasPages)
            assertTrue(viewModel.uiState.value.hasSession)
            assertEquals(0, viewModel.uiState.value.currentPage)

            viewModel.onEvent(ReviewUiEvent.RestorePage(pageId = session.pages.single().id))
            shadowOf(Looper.getMainLooper()).idle()

            assertTrue(viewModel.uiState.value.hasPages)
            assertEquals(1, viewModel.uiState.value.pages.size)
            assertEquals(session.pages.single().id, viewModel.uiState.value.selectedPage?.id)
        } finally {
            effectJob.cancel()
        }
    }

    @Test
    fun `restoring a deleted page reselects that page and keeps current page in sync`() = runBlocking {
        val viewModel = createViewModel()
        val session = ScanSession(
            id = "session_review_restore_selection",
            pages = listOf(
                ScannedPage(
                    id = "session_review_restore_selection_page_0",
                    originalSourceUri = Uri.parse("content://scanner/page-0.jpg"),
                ),
                ScannedPage(
                    id = "session_review_restore_selection_page_1",
                    originalSourceUri = Uri.parse("content://scanner/page-1.jpg"),
                ),
                ScannedPage(
                    id = "session_review_restore_selection_page_2",
                    originalSourceUri = Uri.parse("content://scanner/page-2.jpg"),
                ),
            ),
        )

        viewModel.setRestoredSession(session)
        viewModel.onEvent(ReviewUiEvent.PageChanged(pageIndex = 1))

        val firstEffect = CompletableDeferred<ReviewUiEffect>()
        val effectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.collect { effect ->
                if (!firstEffect.isCompleted) {
                    firstEffect.complete(effect)
                }
            }
        }

        try {
            viewModel.onEvent(ReviewUiEvent.DeletePage(pageIndex = 1))
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(
                ReviewUiEffect.ShowDeletedPageUndo(session.pages[1].id),
                withTimeout(1_000) { firstEffect.await() },
            )
            assertEquals(1, viewModel.uiState.value.currentPage)
            assertEquals(session.pages[2].id, viewModel.uiState.value.selectedPage?.id)

            viewModel.onEvent(ReviewUiEvent.RestorePage(pageId = session.pages[1].id))
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(1, viewModel.uiState.value.currentPage)
            assertEquals(session.pages[1].id, viewModel.uiState.value.selectedPage?.id)
            assertEquals(session.pages[1].id, viewModel.uiState.value.pages[1].id)
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
        val firstEffect = CompletableDeferred<ReviewUiEffect>()
        val effectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.collect { effect ->
                if (!firstEffect.isCompleted) {
                    firstEffect.complete(effect)
                }
            }
        }

        try {
            viewModel.setRestoredSession(session)
            viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))
            val context = RuntimeEnvironment.getApplication()
            viewModel.onEvent(ReviewUiEvent.SaveAll(context))
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(
                ReviewUiEffect.ShowStatusMessage(
                    UiText.DynamicString("Failed to write review export"),
                ),
                withTimeout(1_000) { firstEffect.await() },
            )
            assertTrue(viewModel.uiState.value.export.lastResult is ExportState.Finished)
        } finally {
            effectJob.cancel()
        }
    }

    @Test
    fun `failed export can be retried explicitly and succeeds on the second attempt`() = runBlocking {
        val exportRepository = SequencedPdfExportRepository(
            results = ArrayDeque(
                listOf(
                    ExportOperationResult.Failure(
                        ExportWriteFailure(details = "Failed to write review export"),
                    ),
                    ExportOperationResult.Success(
                        outputUri = Uri.parse("content://exports/retry-success.pdf"),
                    ),
                ),
            ),
        )
        val viewModel = createViewModel(exportRepository = exportRepository)
        val session = loadedSession(sessionId = "session_review_export_retry")
        val effectEvents = mutableListOf<ReviewUiEffect>()
        val retrySuccessEffect = CompletableDeferred<ReviewUiEffect.ExportSucceeded>()
        val effectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.collect { effect ->
                effectEvents += effect
                if (effect is ReviewUiEffect.ExportSucceeded && !retrySuccessEffect.isCompleted) {
                    retrySuccessEffect.complete(effect)
                }
            }
        }

        try {
            viewModel.setRestoredSession(session)
            viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))
            val context = RuntimeEnvironment.getApplication()

            viewModel.onEvent(ReviewUiEvent.SaveAll(context))
            shadowOf(Looper.getMainLooper()).idle()

            assertTrue(viewModel.uiState.value.export.canRetry)
            assertTrue(viewModel.uiState.value.export.lastResult?.result is ExportResult.Failure)

            viewModel.onEvent(ReviewUiEvent.RetryExport(context))
            shadowOf(Looper.getMainLooper()).idle()

            val successEffect = withTimeout(1_000) { retrySuccessEffect.await() }
            assertEquals(2, exportRepository.savedPdfRequests.size)
            assertFalse(viewModel.uiState.value.export.canRetry)
            assertTrue(viewModel.uiState.value.export.lastResult?.result is ExportResult.Success)
            assertEquals(
                ReviewUiEffect.ExportSucceeded(
                    ExportFormat.PDF,
                    com.example.pixelscanner.core.model.ExportPostActionTarget(
                        outputUris = listOf(Uri.parse("content://exports/retry-success.pdf")),
                        mimeType = "application/pdf",
                    ),
                ),
                successEffect,
            )
        } finally {
            effectJob.cancel()
        }
    }

    @Test
    fun `explicit export cancel stops in-flight export and leaves state consistent`() = runBlocking {
        val exportRepository = HangingPdfExportRepository()
        val viewModel = createViewModel(exportRepository = exportRepository)
        val session = loadedSession(sessionId = "session_review_export_cancel")
        val firstEffect = CompletableDeferred<ReviewUiEffect>()
        val effectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.collect { effect ->
                if (!firstEffect.isCompleted) {
                    firstEffect.complete(effect)
                }
            }
        }

        try {
            viewModel.setRestoredSession(session)
            viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))
            val context = RuntimeEnvironment.getApplication()

            viewModel.onEvent(ReviewUiEvent.SaveAll(context))
            shadowOf(Looper.getMainLooper()).idle()

            assertTrue(exportRepository.exportStarted.await(1, TimeUnit.SECONDS))
            assertTrue(viewModel.uiState.value.export.isExporting)

            viewModel.onEvent(ReviewUiEvent.CancelExport)
            shadowOf(Looper.getMainLooper()).idle()

            assertTrue(exportRepository.exportCancelled.await(1, TimeUnit.SECONDS))
            assertEquals(
                ReviewUiEffect.ShowStatusMessage(uiTextResource(R.string.review_status_export_cancelled)),
                withTimeout(1_000) { firstEffect.await() },
            )
            assertFalse(viewModel.uiState.value.export.isExporting)
            assertTrue(viewModel.uiState.value.export.lastResult?.result == ExportResult.Cancelled)
        } finally {
            effectJob.cancel()
        }
    }

    @Test
    fun `non-retryable export failure does not expose retry or rerun export`() = runBlocking {
        val exportRepository = SequencedPdfExportRepository(
            results = ArrayDeque(
                listOf(
                    ExportOperationResult.Failure(
                        ExportSourceInvalidFailure(details = "Source missing"),
                    ),
                ),
            ),
        )
        val viewModel = createViewModel(exportRepository = exportRepository)
        val session = loadedSession(sessionId = "session_review_export_non_retryable")

        viewModel.setRestoredSession(session)
        viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))
        val context = RuntimeEnvironment.getApplication()

        viewModel.onEvent(ReviewUiEvent.SaveAll(context))
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(viewModel.uiState.value.export.canRetry)
        assertTrue(viewModel.uiState.value.export.lastResult?.result is ExportResult.Failure)
        assertEquals(
            ExportFallbackAction.DISCARD_AND_RESCAN,
            viewModel.uiState.value.export.fallbackAction,
        )
        assertEquals(1, exportRepository.savedPdfRequests.size)

        viewModel.onEvent(ReviewUiEvent.RetryExport(context))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, exportRepository.savedPdfRequests.size)
        assertFalse(viewModel.uiState.value.export.canRetry)
        assertEquals(
            ExportFallbackAction.DISCARD_AND_RESCAN,
            viewModel.uiState.value.export.fallbackAction,
        )
    }

    @Test
    fun `permission denied export failure exposes open settings fallback`() = runBlocking {
        val exportRepository = FakeExportRepository(
            saveUrisToPdfResult = ExportOperationResult.Failure(
                ExportPermissionDeniedFailure,
            ),
        )
        val viewModel = createViewModel(exportRepository = exportRepository)
        val session = loadedSession(sessionId = "session_review_export_permission")

        viewModel.setRestoredSession(session)
        viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))
        val context = RuntimeEnvironment.getApplication()

        viewModel.onEvent(ReviewUiEvent.SaveAll(context))
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(viewModel.uiState.value.export.canRetry)
        assertEquals(
            ExportFallbackAction.OPEN_SETTINGS,
            viewModel.uiState.value.export.fallbackAction,
        )
    }

    @Test
    fun `empty session export failure exposes scan more fallback`() = runBlocking {
        val exportRepository = FakeExportRepository(
            saveUrisToPdfResult = ExportOperationResult.Failure(
                ExportNoPagesFailure,
            ),
        )
        val viewModel = createViewModel(exportRepository = exportRepository)
        val emptySession = ScanSession(
            id = "session_review_export_empty",
            pages = emptyList(),
        )

        viewModel.setRestoredSession(emptySession)
        viewModel.onEvent(ReviewUiEvent.ChangeExportFormat(ExportFormat.PDF))
        val context = RuntimeEnvironment.getApplication()

        viewModel.onEvent(ReviewUiEvent.SaveAll(context))
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(viewModel.uiState.value.export.canRetry)
        assertEquals(
            ExportFallbackAction.SCAN_MORE,
            viewModel.uiState.value.export.fallbackAction,
        )
        assertTrue(viewModel.uiState.value.export.lastResult?.result is ExportResult.Failure)
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

    @Test
    fun `shows enhancement failure on the page and retries successfully`() = runBlocking {
        val imageProcessingRepository = FailingThenSucceedingImageProcessingRepository()
        val viewModel = createViewModel(imageProcessingRepository = imageProcessingRepository)
        val context = RuntimeEnvironment.getApplication()
        val session = ScanSession(
            id = "session_review_enhancement_retry",
            pages = listOf(
                ScannedPage(
                    id = "session_review_enhancement_retry_page_0",
                    originalSourceUri = Uri.parse("content://scanner/page-0.jpg"),
                ),
            ),
        )

        val firstEffect = CompletableDeferred<ReviewUiEffect>()
        val effectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.collect { effect ->
                if (!firstEffect.isCompleted) {
                    firstEffect.complete(effect)
                }
            }
        }

        try {
            viewModel.setRestoredSession(session)
            viewModel.onEvent(ReviewUiEvent.ToggleEnhancement(pageIndex = 0, context = context))
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(1, imageProcessingRepository.invocationCount.get())
            assertEquals(
                ReviewUiEffect.ShowStatusMessage(uiTextResource(R.string.review_page_enhancement_failed)),
                withTimeout(1_000) { firstEffect.await() },
            )
            assertEquals(
                uiTextResource(R.string.review_page_enhancement_failed),
                viewModel.uiState.value.selectedPage?.enhancementErrorMessage,
            )

            viewModel.onEvent(
                ReviewUiEvent.RetryEnhancement(
                    pageId = session.pages.single().id,
                    context = context,
                ),
            )
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(2, imageProcessingRepository.invocationCount.get())
            assertNull(viewModel.uiState.value.selectedPage?.enhancementErrorMessage)
            assertEquals(ProcessingState.IDLE, viewModel.uiState.value.selectedPage?.processingState)
            assertTrue(viewModel.uiState.value.selectedPage?.isEnhanced == true)
        } finally {
            effectJob.cancel()
        }
    }

    @Test
    fun `reordering a page keeps delete undo order and selected page in sync`() {
        val viewModel = createViewModel()
        val session = ScanSession(
            id = "session_review_reorder_undo",
            pages = listOf(
                ScannedPage(
                    id = "session_review_reorder_undo_page_0",
                    originalSourceUri = Uri.parse("content://scanner/page-0.jpg"),
                ),
                ScannedPage(
                    id = "session_review_reorder_undo_page_1",
                    originalSourceUri = Uri.parse("content://scanner/page-1.jpg"),
                ),
                ScannedPage(
                    id = "session_review_reorder_undo_page_2",
                    originalSourceUri = Uri.parse("content://scanner/page-2.jpg"),
                ),
            ),
        )

        viewModel.setRestoredSession(session)
        viewModel.onEvent(ReviewUiEvent.PageChanged(pageIndex = 1))
        viewModel.onEvent(ReviewUiEvent.MovePageLeft)

        assertEquals("session_review_reorder_undo_page_1", viewModel.uiState.value.selectedPage?.id)
        assertEquals(
            listOf(
                "session_review_reorder_undo_page_1",
                "session_review_reorder_undo_page_0",
                "session_review_reorder_undo_page_2",
            ),
            viewModel.uiState.value.pages.map { it.id },
        )
        assertTrue(viewModel.scanSession.value?.dirtyFlags?.hasReorderedPages == true)

        viewModel.onEvent(ReviewUiEvent.DeletePage(pageIndex = 0))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            listOf(
                "session_review_reorder_undo_page_0",
                "session_review_reorder_undo_page_2",
            ),
            viewModel.uiState.value.pages.map { it.id },
        )

        viewModel.onEvent(ReviewUiEvent.RestorePage(pageId = "session_review_reorder_undo_page_1"))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            listOf(
                "session_review_reorder_undo_page_1",
                "session_review_reorder_undo_page_0",
                "session_review_reorder_undo_page_2",
            ),
            viewModel.uiState.value.pages.map { it.id },
        )
        assertEquals("session_review_reorder_undo_page_1", viewModel.uiState.value.selectedPage?.id)
        assertTrue(viewModel.scanSession.value?.dirtyFlags?.hasReorderedPages == true)
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
            reorderPageUseCase = ReorderPageUseCase(),
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

    private class FailingThenSucceedingImageProcessingRepository : ImageProcessingRepository {
        val invocationCount = AtomicInteger(0)

        override suspend fun enhance(
            context: Context,
            request: PreviewImagePipelineRequest,
        ): PreviewImagePipelineResult {
            val invocation = invocationCount.incrementAndGet()
            if (invocation == 1) {
                throw IllegalStateException("Enhancement failed")
            }
            return PreviewImagePipelineResult(
                cacheKey = com.example.pixelscanner.core.model.PipelineCacheKey("retry-${request.pageId}"),
                outputUri = Uri.parse("content://scanner/enhanced-${request.pageId}.jpg"),
            )
        }
    }

    private class SequencedPdfExportRepository(
        private val results: ArrayDeque<ExportOperationResult>,
    ) : ExportRepository {
        val copiedPdfRequests = mutableListOf<ExportPdfRequest>()
        val savedUriRequests = mutableListOf<ExportImageRequest>()
        val savedPdfRequests = mutableListOf<ExportPdfBuildRequest>()

        override suspend fun copyPdfToDocuments(
            context: Context,
            sourceUri: Uri,
            fileName: String,
        ): ExportOperationResult {
            copiedPdfRequests += ExportPdfRequest(sourceUri, fileName)
            return results.removeFirst()
        }

        override suspend fun saveUriToGallery(
            context: Context,
            sourceUri: Uri,
            fileName: String,
        ): ExportOperationResult {
            savedUriRequests += ExportImageRequest(sourceUri, fileName)
            return results.removeFirst()
        }

        override suspend fun saveUrisToPdf(
            context: Context,
            sourceUris: List<Uri>,
            fileName: String,
        ): ExportOperationResult {
            savedPdfRequests += ExportPdfBuildRequest(sourceUris, fileName)
            return results.removeFirst()
        }
    }

    private class HangingPdfExportRepository : ExportRepository {
        val exportStarted = CountDownLatch(1)
        val exportCancelled = CountDownLatch(1)

        override suspend fun copyPdfToDocuments(
            context: Context,
            sourceUri: Uri,
            fileName: String,
        ): ExportOperationResult {
            throw AssertionError("Not used in this test")
        }

        override suspend fun saveUriToGallery(
            context: Context,
            sourceUri: Uri,
            fileName: String,
        ): ExportOperationResult {
            throw AssertionError("Not used in this test")
        }

        override suspend fun saveUrisToPdf(
            context: Context,
            sourceUris: List<Uri>,
            fileName: String,
        ): ExportOperationResult {
            exportStarted.countDown()
            try {
                awaitCancellation()
            } finally {
                exportCancelled.countDown()
            }
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
