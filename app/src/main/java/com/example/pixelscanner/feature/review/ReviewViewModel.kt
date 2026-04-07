package com.example.pixelscanner.feature.review

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.domain.CreateSessionFromScanResultUseCase
import com.example.pixelscanner.domain.DeletePageUseCase
import com.example.pixelscanner.domain.ExportSessionUseCase
import com.example.pixelscanner.domain.RestorePageUseCase
import com.example.pixelscanner.domain.ToggleEnhancementUseCase
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
) : ViewModel() {

    private val _scanSession = MutableStateFlow<ScanSession?>(null)
    val scanSession: StateFlow<ScanSession?> = _scanSession.asStateFlow()

    private val _uiState = MutableStateFlow(ReviewUiState())
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
            is ReviewUiEvent.SaveAll -> saveAll(event.context)
        }
    }

    fun setScanResult(scanResult: ScanResultPayload) {
        val session = createSessionFromScanResultUseCase(scanResult)
        applySessionUpdate(session)
    }

    private fun setExportFormat(format: ExportFormat) {
        updateUiState { currentState ->
            currentState.copy(export = currentState.export.copy(format = format))
        }
    }

    private fun setCurrentPage(pageIndex: Int) {
        updateUiState { currentState ->
            val lastIndex = (currentState.pages.size - 1).coerceAtLeast(0)
            currentState.copy(currentPage = pageIndex.coerceIn(0, lastIndex))
        }
    }

    private fun toggleEnhancement(index: Int, context: Context) {
        val session = _scanSession.value ?: return
        val page = session.activePages().getOrNull(index) ?: return

        viewModelScope.launch {
            updateUiState { it.copy(isProcessing = true) }
            try {
                val result = toggleEnhancementUseCase(page, context)
                val updatedPages = session.pages.map { existingPage ->
                    if (existingPage.id == result.updatedPage.id) {
                        result.updatedPage
                    } else {
                        existingPage
                    }
                }
                applySessionUpdate(session.withPages(updatedPages))
            } catch (e: Exception) {
                e.printStackTrace() // Handle error appropriately
            } finally {
                updateUiState { it.copy(isProcessing = false) }
            }
        }
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
        applySessionUpdate(restorePageUseCase(session, page))
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
            currentState.copy(
                pages = activePages,
                currentPage = currentState.currentPage.coerceIn(0, lastIndex),
            )
        }
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
    )
}

class ReviewViewModelFactory(
    private val createSessionFromScanResultUseCase: CreateSessionFromScanResultUseCase,
    private val toggleEnhancementUseCase: ToggleEnhancementUseCase,
    private val deletePageUseCase: DeletePageUseCase,
    private val restorePageUseCase: RestorePageUseCase,
    private val exportSessionUseCase: ExportSessionUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return ReviewViewModel(
            createSessionFromScanResultUseCase = createSessionFromScanResultUseCase,
            toggleEnhancementUseCase = toggleEnhancementUseCase,
            deletePageUseCase = deletePageUseCase,
            restorePageUseCase = restorePageUseCase,
            exportSessionUseCase = exportSessionUseCase,
        ) as T
    }
}
