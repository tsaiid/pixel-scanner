package com.example.pixelscanner.ui

import android.content.Context
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewViewModel : ViewModel() {
    private val createSessionFromScanResultUseCase = CreateSessionFromScanResultUseCase()
    private val toggleEnhancementUseCase = ToggleEnhancementUseCase()
    private val deletePageUseCase = DeletePageUseCase()
    private val restorePageUseCase = RestorePageUseCase()
    private val exportSessionUseCase = ExportSessionUseCase()

    private val _scanSession = MutableStateFlow<ScanSession?>(null)
    val scanSession: StateFlow<ScanSession?> = _scanSession.asStateFlow()

    private val _scannedPages = MutableStateFlow<List<ScannedPage>>(emptyList())
    val scannedPages: StateFlow<List<ScannedPage>> = _scannedPages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _exportFormat = MutableStateFlow(ExportFormat.IMAGE)
    val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    fun setExportFormat(format: ExportFormat) {
        _exportFormat.value = format
    }

    fun setScanResult(scanResult: ScanResultPayload) {
        val session = createSessionFromScanResultUseCase(scanResult)
        applySessionUpdate(session)
    }

    fun toggleEnhancement(index: Int, context: Context) {
        val session = _scanSession.value ?: return
        val page = session.activePages().getOrNull(index) ?: return

        viewModelScope.launch {
            _isProcessing.value = true
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
                _isProcessing.value = false
            }
        }
    }

    fun deletePage(index: Int) {
        val session = _scanSession.value ?: return
        val result = deletePageUseCase(session, index) ?: return
        applySessionUpdate(result.updatedSession)
    }

    fun restorePage(page: ScannedPage) {
        val session = _scanSession.value ?: return
        applySessionUpdate(restorePageUseCase(session, page))
    }

    fun saveAll(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            val session = _scanSession.value ?: return@launch
            val request = ExportRequest(
                sessionId = session.id,
                format = _exportFormat.value,
            )
            val result = exportSessionUseCase(
                session = session,
                request = request,
                context = context,
            )

            if (result is ExportResult.Failure) {
                return@launch
            }
            onComplete()
        }
    }

    private fun applySessionUpdate(session: ScanSession) {
        _scanSession.value = session
        _scannedPages.value = session.activePages()
    }
}
