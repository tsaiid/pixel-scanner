package com.example.pixelscanner.feature.review

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.ExportOverallProgress
import com.example.pixelscanner.core.model.ExportState
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.ExportProgress
import com.example.pixelscanner.core.model.toPostActionTarget
import com.example.pixelscanner.core.model.ProcessingState
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.domain.CreateSessionFromScanResultUseCase
import com.example.pixelscanner.domain.EagerSessionIngestionUseCase
import com.example.pixelscanner.domain.DeletePageUseCase
import com.example.pixelscanner.domain.DiscardSessionUseCase
import com.example.pixelscanner.domain.ExportSessionUseCase
import com.example.pixelscanner.domain.PreviewCacheCleanupRepository
import com.example.pixelscanner.domain.ReorderPageUseCase
import com.example.pixelscanner.domain.ResumeSessionUseCase
import com.example.pixelscanner.domain.RestorePageUseCase
import com.example.pixelscanner.domain.ScanMoreUseCase
import com.example.pixelscanner.domain.ToggleEnhancementUseCase
import com.example.pixelscanner.feature.export.ui.ExportUiState
import com.example.pixelscanner.feature.review.ui.ReviewPageUiMapper
import com.example.pixelscanner.feature.review.ui.HomeNavigationReason
import com.example.pixelscanner.feature.review.ui.ReviewUiEffect
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent
import com.example.pixelscanner.feature.review.ui.ReviewUiState
import com.example.pixelscanner.feature.review.ui.ReviewStatusMessage
import com.example.pixelscanner.feature.review.ui.ScannerNavigationReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val createSessionFromScanResultUseCase: CreateSessionFromScanResultUseCase,
    private val eagerSessionIngestionUseCase: EagerSessionIngestionUseCase,
    private val toggleEnhancementUseCase: ToggleEnhancementUseCase,
    private val deletePageUseCase: DeletePageUseCase,
    private val restorePageUseCase: RestorePageUseCase,
    private val reorderPageUseCase: ReorderPageUseCase,
    private val exportSessionUseCase: ExportSessionUseCase,
    private val discardSessionUseCase: DiscardSessionUseCase,
    private val previewCacheCleanupRepository: PreviewCacheCleanupRepository,
    private val resumeSessionUseCase: ResumeSessionUseCase,
    private val scanMoreUseCase: ScanMoreUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _scanSession = MutableStateFlow<ScanSession?>(null)
    val scanSession: StateFlow<ScanSession?> = _scanSession.asStateFlow()
    private val enhancementJobs = mutableMapOf<String, Job>()
    private var statusMessageSequence = 0L

    private val _uiState = MutableStateFlow(
        ReviewUiState(
            currentPage = savedStateHandle.get<Int>(KEY_CURRENT_PAGE) ?: 0,
            export = ExportUiState(
                format = savedStateHandle.get<String>(KEY_EXPORT_FORMAT)
                    ?.let(ExportFormat::valueOf)
                    ?: ExportFormat.IMAGE,
                state = ExportState.Idle,
            ),
            isAwaitingScanMoreResult = savedStateHandle.get<Boolean>(
                KEY_IS_AWAITING_SCAN_MORE_RESULT,
            ) ?: false,
            hasSession = false,
        )
    )
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ReviewUiEffect>()
    val effects: SharedFlow<ReviewUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: ReviewUiEvent) {
        when (event) {
            is ReviewUiEvent.PageChanged -> setCurrentPage(event.pageIndex)
            is ReviewUiEvent.ChangeExportFormat -> setExportFormat(event.format)
            is ReviewUiEvent.ToggleEnhancement -> toggleEnhancement(
                index = event.pageIndex,
                context = event.context,
            )
            is ReviewUiEvent.DeletePage -> deletePage(event.pageIndex)
            is ReviewUiEvent.RestorePage -> restorePage(event.pageId)
            is ReviewUiEvent.MovePageLeft -> movePage(-1)
            is ReviewUiEvent.MovePageRight -> movePage(1)
            is ReviewUiEvent.RequestScanMore -> startScanMore()
            is ReviewUiEvent.RequestRescan -> discardAndReturnToScanner()
            is ReviewUiEvent.RequestDiscardSession -> discardAndReturnToHome()
            is ReviewUiEvent.ClearStatusMessage -> clearStatusMessage()
            is ReviewUiEvent.SaveAll -> saveAll(event.context)
        }
    }

    fun setScanResult(scanResult: ScanResultPayload) {
        if (uiState.value.isAwaitingScanMoreResult) {
            val currentSession = _scanSession.value
            if (currentSession != null) {
                clearScanMorePending()
                val appendedSession = currentSession.appendScanResult(scanResult)
                applySessionUpdate(appendedSession)
                startEagerIngestion(appendedSession, scanResult)
                return
            }
            clearScanMorePending()
        }

        val session = createSessionFromScanResultUseCase(scanResult)
        val resumedSession = resumeSessionUseCase(session).session
        clearScanMorePending()
        applySessionUpdate(resumedSession)
        startEagerIngestion(resumedSession, scanResult)
    }

    fun setRestoredSession(session: ScanSession) {
        clearScanMorePending()
        applySessionUpdate(session)
    }

    fun clearScanMorePending() {
        savedStateHandle[KEY_IS_AWAITING_SCAN_MORE_RESULT] = false
        updateUiState { currentState ->
            currentState.copy(isAwaitingScanMoreResult = false)
        }
    }

    private fun startEagerIngestion(
        session: ScanSession,
        scanResult: ScanResultPayload,
    ) {
        // Kick ingestion off after the session exists so review can render immediately.
        viewModelScope.launch {
            runCatching {
                val ingestionResult = eagerSessionIngestionUseCase(session, scanResult)
                if (ingestionResult.successfulMappingCount() > 0) {
                    val currentSession = _scanSession.value
                    if (currentSession != null && currentSession.id == session.id) {
                        applySessionUpdate(currentSession.withIngestedAssets(ingestionResult))
                    }
                }
                if (ingestionResult.hasFailures()) {
                    ingestionResult.failures.forEach { failure ->
                        println("Session ingestion warning: ${failure.typedFailure?.userMessage ?: failure.message}")
                    }
                    ingestionResult.failures.firstOrNull { it.typedFailure != null }?.typedFailure?.let { typedFailure ->
                        setStatusMessage(typedFailure.userMessage)
                    }
                }
            }.onFailure { error ->
                error.printStackTrace()
            }
        }
    }

    private fun setExportFormat(format: ExportFormat) {
        savedStateHandle[KEY_EXPORT_FORMAT] = format.name
        updateUiState { currentState ->
            currentState.copy(export = currentState.export.copy(format = format))
        }
    }

    private fun setCurrentPage(pageIndex: Int) {
        updateUiState { currentState ->
            val lastIndex = (currentState.pages.size - 1).coerceAtLeast(0)
            val coercedPage = pageIndex.coerceIn(0, lastIndex)
            savedStateHandle[KEY_CURRENT_PAGE] = coercedPage
            currentState.copy(currentPage = coercedPage)
        }
    }

    private fun toggleEnhancement(index: Int, context: Context) {
        val session = _scanSession.value ?: return
        val page = session.activePages().getOrNull(index) ?: return

        enhancementJobs.remove(page.id)?.cancel()
        applySessionUpdate(session.withPageProcessing(page.id, ProcessingState.PROCESSING))

        val job = viewModelScope.launch {
            try {
                val result = toggleEnhancementUseCase(page, context)
                val currentSession = _scanSession.value ?: return@launch
                val updatedPages = currentSession.pages.map { existingPage ->
                    if (existingPage.id == result.updatedPage.id) {
                        result.updatedPage.copy(processingState = ProcessingState.IDLE)
                    } else {
                        existingPage
                    }
                }
                applySessionUpdate(currentSession.withPages(updatedPages))
            } catch (e: CancellationException) {
                val currentSession = _scanSession.value
                if (currentSession != null && currentSession.pages.any { it.id == page.id }) {
                    applySessionUpdate(currentSession.withPageProcessing(page.id, ProcessingState.IDLE))
                }
                throw e
            } catch (e: Exception) {
                e.printStackTrace() // Handle error appropriately
                val currentSession = _scanSession.value ?: return@launch
                if (currentSession.pages.any { it.id == page.id }) {
                    applySessionUpdate(currentSession.withPageProcessing(page.id, ProcessingState.IDLE))
                }
                setStatusMessage("Enhancement failed")
            } finally {
                enhancementJobs.remove(page.id)
            }
        }
        enhancementJobs[page.id] = job
    }

    private fun deletePage(index: Int) {
        val session = _scanSession.value ?: return
        val result = deletePageUseCase(session, index) ?: return
        applySessionUpdate(result.updatedSession)
        viewModelScope.launch {
            _effects.emit(ReviewUiEffect.ShowDeletedPageUndo(result.deletedPage.id))
        }
    }

    private fun restorePage(pageId: String) {
        val session = _scanSession.value ?: return
        val page = session.pages.firstOrNull { it.id == pageId } ?: return
        applySessionUpdate(
            restorePageUseCase(session, page),
            preferredPageId = pageId,
        )
    }

    private fun movePage(offset: Int) {
        val session = _scanSession.value ?: return
        val currentPageIndex = _uiState.value.currentPage
        val targetPageIndex = currentPageIndex + offset
        val result = reorderPageUseCase(session, currentPageIndex, targetPageIndex) ?: return
        applySessionUpdate(
            result.updatedSession,
            preferredPageId = result.movedPage.id,
        )
    }

    private fun startScanMore() {
        val session = _scanSession.value ?: return
        viewModelScope.launch {
            val result = scanMoreUseCase(session)
            if (!result.preservesCurrentPages) {
                return@launch
            }
            savedStateHandle[KEY_IS_AWAITING_SCAN_MORE_RESULT] = true
            updateUiState { currentState ->
                currentState.copy(isAwaitingScanMoreResult = true)
            }
            _effects.emit(
                ReviewUiEffect.NavigateToScanner(ScannerNavigationReason.SCAN_MORE),
            )
        }
    }

    private fun discardAndReturnToScanner() {
        val session = _scanSession.value ?: return
        viewModelScope.launch {
            discardSessionUseCase(session)
            previewCacheCleanupRepository.cleanSessionPreviewCache(session.id)
            clearSessionState()
            _effects.emit(
                ReviewUiEffect.NavigateToScanner(ScannerNavigationReason.RESCAN),
            )
        }
    }

    private fun discardAndReturnToHome() {
        val session = _scanSession.value ?: return
        viewModelScope.launch {
            discardSessionUseCase(session)
            previewCacheCleanupRepository.cleanSessionPreviewCache(session.id)
            clearSessionState()
            _effects.emit(
                ReviewUiEffect.NavigateToHome(HomeNavigationReason.DISCARD_SESSION),
            )
        }
    }

    private fun saveAll(context: Context) {
        viewModelScope.launch {
            val session = _scanSession.value ?: return@launch
            var exportResult: ExportResult? = null
            val request = ExportRequest(
                sessionId = session.id,
                format = _uiState.value.export.format,
            )
            val totalSteps = when (request.format) {
                ExportFormat.IMAGE -> session.activePages().size
                ExportFormat.PDF -> 1
            }
            updateUiState { currentState ->
                currentState.copy(
                    export = currentState.export.copy(
                        state = ExportState.InProgress(
                            progress = ExportProgress(
                                overall = ExportOverallProgress(
                                    completed = 0,
                                    total = totalSteps,
                                ),
                            ),
                        ),
                    ),
                )
            }
            try {
                val result = exportSessionUseCase(
                    session = session,
                    request = request,
                    context = context,
                    onProgress = { progress ->
                        updateUiState { currentState ->
                            currentState.copy(
                                export = currentState.export.copy(
                                    state = ExportState.InProgress(progress),
                                ),
                            )
                        }
                    },
                )
                exportResult = result

                when (result) {
                    is ExportResult.Success -> {
                        result.toPostActionTarget(request.format)?.let { target ->
                            _effects.emit(ReviewUiEffect.ExportSucceeded(request.format, target))
                        }
                    }

                    is ExportResult.PartialSuccess -> {
                        result.toPostActionTarget(request.format)?.let { target ->
                            _effects.emit(ReviewUiEffect.ExportSucceeded(request.format, target))
                        }
                        setStatusMessage(result.warningMessage ?: "Export completed with some skipped items")
                    }

                    is ExportResult.Failure -> {
                        setStatusMessage(result.error.userMessage)
                    }

                    ExportResult.Cancelled -> {
                        setStatusMessage("Export cancelled")
                    }
                }
            } finally {
                updateUiState { currentState ->
                    currentState.copy(
                        export = currentState.export.copy(
                            state = exportResult?.let(ExportState::Finished) ?: ExportState.Idle,
                        ),
                )
            }
        }
    }
    }

    private fun applySessionUpdate(
        session: ScanSession,
        preferredPageId: String? = null,
    ) {
        _scanSession.value = session
        updateUiState { currentState ->
            val activePages = session.activePages().map(ReviewPageUiMapper::map)
            val lastIndex = (activePages.size - 1).coerceAtLeast(0)
            val preferredPageIndex = preferredPageId?.let { pageId ->
                activePages.indexOfFirst { it.id == pageId }.takeIf { it >= 0 }
            }
            val currentPage = (preferredPageIndex ?: currentState.currentPage).coerceIn(0, lastIndex)
            savedStateHandle[KEY_CURRENT_PAGE] = currentPage
            currentState.copy(
                pages = activePages,
                currentPage = currentPage,
                hasSession = true,
            )
        }
    }

    private fun clearSessionState() {
        cancelEnhancementJobs()
        _scanSession.value = null
        savedStateHandle[KEY_CURRENT_PAGE] = 0
        savedStateHandle[KEY_EXPORT_FORMAT] = ExportFormat.IMAGE.name
        clearScanMorePending()
        _uiState.value = ReviewUiState(
            isAwaitingScanMoreResult = false,
            hasSession = false,
            export = ExportUiState(
                state = ExportState.Idle,
            ),
            statusMessage = null,
        )
    }

    override fun onCleared() {
        cancelEnhancementJobs()
        super.onCleared()
    }

    private fun updateUiState(transform: (ReviewUiState) -> ReviewUiState) {
        _uiState.value = transform(_uiState.value)
    }

    private fun cancelEnhancementJobs() {
        enhancementJobs.values.forEach { job -> job.cancel() }
        enhancementJobs.clear()
    }

    private fun setStatusMessage(message: String) {
        updateUiState { currentState ->
            currentState.copy(
                statusMessage = ReviewStatusMessage(
                    id = ++statusMessageSequence,
                    text = message,
                ),
            )
        }
    }

    private fun clearStatusMessage() {
        updateUiState { currentState ->
            currentState.copy(statusMessage = null)
        }
    }
}

private const val KEY_CURRENT_PAGE = "review_current_page"
private const val KEY_EXPORT_FORMAT = "review_export_format"
private const val KEY_IS_AWAITING_SCAN_MORE_RESULT = "review_is_awaiting_scan_more_result"

class ReviewViewModelFactory(
    private val createSessionFromScanResultUseCase: CreateSessionFromScanResultUseCase,
    private val eagerSessionIngestionUseCase: EagerSessionIngestionUseCase,
    private val toggleEnhancementUseCase: ToggleEnhancementUseCase,
    private val deletePageUseCase: DeletePageUseCase,
    private val restorePageUseCase: RestorePageUseCase,
    private val reorderPageUseCase: ReorderPageUseCase,
    private val exportSessionUseCase: ExportSessionUseCase,
    private val discardSessionUseCase: DiscardSessionUseCase,
    private val previewCacheCleanupRepository: PreviewCacheCleanupRepository,
    private val resumeSessionUseCase: ResumeSessionUseCase,
    private val scanMoreUseCase: ScanMoreUseCase,
    owner: androidx.savedstate.SavedStateRegistryOwner,
) : androidx.lifecycle.AbstractSavedStateViewModelFactory(owner, null) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        require(modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return ReviewViewModel(
            createSessionFromScanResultUseCase = createSessionFromScanResultUseCase,
            eagerSessionIngestionUseCase = eagerSessionIngestionUseCase,
            toggleEnhancementUseCase = toggleEnhancementUseCase,
            deletePageUseCase = deletePageUseCase,
            restorePageUseCase = restorePageUseCase,
            reorderPageUseCase = reorderPageUseCase,
            exportSessionUseCase = exportSessionUseCase,
            discardSessionUseCase = discardSessionUseCase,
            previewCacheCleanupRepository = previewCacheCleanupRepository,
            resumeSessionUseCase = resumeSessionUseCase,
            scanMoreUseCase = scanMoreUseCase,
            savedStateHandle = handle,
        ) as T
    }
}
