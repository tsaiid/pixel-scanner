package com.example.pixelscanner.feature.review

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.ProcessingState
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.domain.CreateSessionFromScanResultUseCase
import com.example.pixelscanner.domain.DeletePageUseCase
import com.example.pixelscanner.domain.DiscardSessionUseCase
import com.example.pixelscanner.domain.ExportSessionUseCase
import com.example.pixelscanner.domain.RestorePageUseCase
import com.example.pixelscanner.domain.ToggleEnhancementUseCase
import com.example.pixelscanner.feature.export.ui.ExportUiState
import com.example.pixelscanner.feature.review.ui.ReviewPageUiModel
import com.example.pixelscanner.feature.review.ui.ReviewUiEffect
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent
import com.example.pixelscanner.feature.review.ui.ReviewUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val createSessionFromScanResultUseCase: CreateSessionFromScanResultUseCase,
    private val toggleEnhancementUseCase: ToggleEnhancementUseCase,
    private val deletePageUseCase: DeletePageUseCase,
    private val restorePageUseCase: RestorePageUseCase,
    private val exportSessionUseCase: ExportSessionUseCase,
    private val discardSessionUseCase: DiscardSessionUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _scanSession = MutableStateFlow<ScanSession?>(null)
    val scanSession: StateFlow<ScanSession?> = _scanSession.asStateFlow()

    private val _uiState = MutableStateFlow(
        ReviewUiState(
            currentPage = savedStateHandle.get<Int>(KEY_CURRENT_PAGE) ?: 0,
            export = ExportUiState(
                format = savedStateHandle.get<String>(KEY_EXPORT_FORMAT)
                    ?.let(ExportFormat::valueOf)
                    ?: ExportFormat.IMAGE,
            ),
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
            is ReviewUiEvent.RequestRescan -> discardAndReturnToScanner()
            is ReviewUiEvent.RequestDiscardSession -> discardAndReturnToHome()
            is ReviewUiEvent.SaveAll -> saveAll(event.context)
        }
    }

    fun setScanResult(scanResult: ScanResultPayload) {
        val session = createSessionFromScanResultUseCase(scanResult)
        applySessionUpdate(session)
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

        applySessionUpdate(session.withPageProcessing(page.id, ProcessingState.PROCESSING))

        viewModelScope.launch {
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
            } catch (e: Exception) {
                e.printStackTrace() // Handle error appropriately
                val currentSession = _scanSession.value ?: return@launch
                if (currentSession.pages.any { it.id == page.id }) {
                    applySessionUpdate(currentSession.withPageProcessing(page.id, ProcessingState.IDLE))
                }
            }
        }
    }

    private fun deletePage(index: Int) {
        val session = _scanSession.value ?: return
        val result = deletePageUseCase(session, index) ?: return
        applySessionUpdate(result.updatedSession)
        if (result.updatedSession.activePages().isEmpty()) {
            clearSessionState()
            viewModelScope.launch {
                _effects.emit(ReviewUiEffect.NavigateBackToScanner)
            }
            return
        }
        viewModelScope.launch {
            _effects.emit(ReviewUiEffect.ShowDeletedPageUndo(result.deletedPage.id))
        }
    }

    private fun restorePage(pageId: String) {
        val session = _scanSession.value ?: return
        val page = session.pages.firstOrNull { it.id == pageId } ?: return
        applySessionUpdate(restorePageUseCase(session, page))
    }

    private fun discardAndReturnToScanner() {
        val session = _scanSession.value ?: return
        viewModelScope.launch {
            discardSessionUseCase(session)
            clearSessionState()
            _effects.emit(ReviewUiEffect.NavigateBackToScanner)
        }
    }

    private fun discardAndReturnToHome() {
        val session = _scanSession.value ?: return
        viewModelScope.launch {
            discardSessionUseCase(session)
            clearSessionState()
            _effects.emit(ReviewUiEffect.NavigateBackToHome)
        }
    }

    private fun saveAll(context: Context) {
        viewModelScope.launch {
            val session = _scanSession.value ?: return@launch
            val request = ExportRequest(
                sessionId = session.id,
                format = _uiState.value.export.format,
            )
            updateUiState { currentState ->
                currentState.copy(export = currentState.export.copy(isExporting = true))
            }
            try {
                val result = exportSessionUseCase(
                    session = session,
                    request = request,
                    context = context,
                )

                when (result) {
                    is ExportResult.Success -> {
                        _effects.emit(ReviewUiEffect.ExportSucceeded(request.format))
                    }

                    is ExportResult.Failure -> {
                        _effects.emit(
                            ReviewUiEffect.ShowMessage(
                                result.errorMessage ?: "Export failed"
                            )
                        )
                    }
                }
            } finally {
                updateUiState { currentState ->
                    currentState.copy(export = currentState.export.copy(isExporting = false))
                }
            }
        }
    }

    private fun applySessionUpdate(session: ScanSession) {
        _scanSession.value = session
        updateUiState { currentState ->
            val activePages = session.activePages().map { it.toUiModel() }
            val lastIndex = (activePages.size - 1).coerceAtLeast(0)
            val currentPage = currentState.currentPage.coerceIn(0, lastIndex)
            savedStateHandle[KEY_CURRENT_PAGE] = currentPage
            currentState.copy(
                pages = activePages,
                currentPage = currentPage,
            )
        }
    }

    private fun clearSessionState() {
        _scanSession.value = null
        savedStateHandle[KEY_CURRENT_PAGE] = 0
        savedStateHandle[KEY_EXPORT_FORMAT] = ExportFormat.IMAGE.name
        _uiState.value = ReviewUiState()
    }

    private fun updateUiState(transform: (ReviewUiState) -> ReviewUiState) {
        _uiState.value = transform(_uiState.value)
    }
}

private fun ScannedPage.toUiModel(): ReviewPageUiModel {
    return ReviewPageUiModel(
        id = id,
        displayUri = displayUri(),
        isEnhanced = isEnhanced(),
        processingState = processingState,
    )
}

private const val KEY_CURRENT_PAGE = "review_current_page"
private const val KEY_EXPORT_FORMAT = "review_export_format"

class ReviewViewModelFactory(
    private val createSessionFromScanResultUseCase: CreateSessionFromScanResultUseCase,
    private val toggleEnhancementUseCase: ToggleEnhancementUseCase,
    private val deletePageUseCase: DeletePageUseCase,
    private val restorePageUseCase: RestorePageUseCase,
    private val exportSessionUseCase: ExportSessionUseCase,
    private val discardSessionUseCase: DiscardSessionUseCase,
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
            toggleEnhancementUseCase = toggleEnhancementUseCase,
            deletePageUseCase = deletePageUseCase,
            restorePageUseCase = restorePageUseCase,
            exportSessionUseCase = exportSessionUseCase,
            discardSessionUseCase = discardSessionUseCase,
            savedStateHandle = handle,
        ) as T
    }
}
