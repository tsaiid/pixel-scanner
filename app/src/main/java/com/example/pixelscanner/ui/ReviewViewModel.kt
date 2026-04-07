package com.example.pixelscanner.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.PageVariant
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.core.model.SessionDirtyFlags
import com.example.pixelscanner.domain.CreateSessionFromScanResultUseCase
import com.example.pixelscanner.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.pixelscanner.image.ImageProcessor

class ReviewViewModel : ViewModel() {
    private val createSessionFromScanResultUseCase = CreateSessionFromScanResultUseCase()

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
        _scanSession.value = session
        _scannedPages.value = session.activePages()
    }

    fun toggleEnhancement(index: Int, context: Context) {
        val session = _scanSession.value ?: return
        val currentList = session.activePages().toMutableList()
        if (index !in currentList.indices) return

        val page = currentList[index]

        viewModelScope.launch {
            if (page.isEnhanced()) {
                // Revert to original
                currentList[index] = page.copy(activeVariant = PageVariant.ORIGINAL)
                updateSessionPages(session, currentList)
            } else {
                // Switch to enhanced
                if (page.enhancedUri != null) {
                    // Already processed, just switch
                    currentList[index] = page.copy(activeVariant = PageVariant.PREVIEW_ENHANCED)
                    updateSessionPages(
                        session = session,
                        activePages = currentList,
                        dirtyFlags = session.dirtyFlags.copy(hasEnhancedPages = true)
                    )
                } else {
                    // Need to process
                    _isProcessing.value = true
                    try {
                        val enhanced = ImageProcessor.enhanceImage(context, page.originalSourceUri)
                        currentList[index] = page.copy(
                            enhancedUri = enhanced,
                            activeVariant = PageVariant.PREVIEW_ENHANCED
                        )
                        updateSessionPages(
                            session = session,
                            activePages = currentList,
                            dirtyFlags = session.dirtyFlags.copy(hasEnhancedPages = true)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace() // Handle error appropriately
                    } finally {
                        _isProcessing.value = false
                    }
                }
            }
        }
    }

    fun deletePage(index: Int) {
        val session = _scanSession.value ?: return
        val currentList = session.activePages().toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            updateSessionPages(
                session = session,
                activePages = currentList,
                dirtyFlags = session.dirtyFlags.copy(hasDeletedPages = true)
            )
        }
    }

    fun restorePage(index: Int, page: ScannedPage) {
        val session = _scanSession.value ?: return
        val currentList = session.activePages().toMutableList()
        if (index >= 0 && index <= currentList.size) {
            currentList.add(index, page)
            updateSessionPages(session, currentList)
        }
    }

    fun saveAll(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            val session = _scanSession.value ?: return@launch
            val storageManager = StorageManager(context)
            val request = ExportRequest(
                sessionId = session.id,
                format = _exportFormat.value,
            )
            val timestamp = System.currentTimeMillis()

            // Determine which URIs to save (Original vs Enhanced)
            val urisToSave = session.activePages().map { it.displayUri() }

            val result = if (request.format == ExportFormat.PDF) {
                if (urisToSave.isNotEmpty()) {
                    ExportResult.Success(
                        storageManager.saveUrisToPdf(urisToSave, "PixelScanner_$timestamp.pdf")
                    )
                } else {
                    ExportResult.Failure("No pages to export")
                }
            } else {
                urisToSave.forEachIndexed { index, uri ->
                    val filename = "PixelScanner_${timestamp}_$index.jpg"
                    storageManager.saveUriToGallery(uri, filename)
                }
                ExportResult.Success(null)
            }

            if (result is ExportResult.Failure) {
                return@launch
            }

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private fun updateSessionPages(
        session: ScanSession,
        activePages: List<ScannedPage>,
        dirtyFlags: SessionDirtyFlags = session.dirtyFlags,
    ) {
        val updatedSession = session.copy(
            pages = activePages,
            dirtyFlags = dirtyFlags,
        )
        _scanSession.value = updatedSession
        _scannedPages.value = updatedSession.activePages()
    }
}
