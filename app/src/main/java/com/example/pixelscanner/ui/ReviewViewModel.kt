package com.example.pixelscanner.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelscanner.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.pixelscanner.image.ImageProcessor

data class ScannedPage(
    val originalUri: Uri,
    val enhancedUri: Uri? = null,
    val isEnhanced: Boolean = false
) {
    fun getDisplayUri(): Uri {
        return if (isEnhanced && enhancedUri != null) enhancedUri else originalUri
    }
}

enum class ExportFormat {
    IMAGE, PDF
}

class ReviewViewModel : ViewModel() {

    private val _scannedPages = MutableStateFlow<List<ScannedPage>>(emptyList())
    val scannedPages: StateFlow<List<ScannedPage>> = _scannedPages.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _exportFormat = MutableStateFlow(ExportFormat.IMAGE)
    val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    fun setExportFormat(format: ExportFormat) {
        _exportFormat.value = format
    }

    fun setScannedUris(uris: List<Uri>, context: Context) {
        // Simply map URIs to ScannedPage, no heavy loading here
        _scannedPages.value = uris.map { ScannedPage(originalUri = it) }
    }
    
    fun toggleEnhancement(index: Int, context: Context) {
        val currentList = _scannedPages.value.toMutableList()
        if (index !in currentList.indices) return
        
        val page = currentList[index]
        
        viewModelScope.launch {
            if (page.isEnhanced) {
                // Revert to original
                currentList[index] = page.copy(isEnhanced = false)
                _scannedPages.value = currentList
            } else {
                // Switch to enhanced
                if (page.enhancedUri != null) {
                    // Already processed, just switch
                    currentList[index] = page.copy(isEnhanced = true)
                    _scannedPages.value = currentList
                } else {
                    // Need to process
                    _isProcessing.value = true
                    try {
                        val enhanced = ImageProcessor.enhanceImage(context, page.originalUri)
                        currentList[index] = page.copy(enhancedUri = enhanced, isEnhanced = true)
                        _scannedPages.value = currentList
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
        val currentList = _scannedPages.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _scannedPages.value = currentList
        }
    }

    fun restorePage(index: Int, page: ScannedPage) {
        val currentList = _scannedPages.value.toMutableList()
        if (index >= 0 && index <= currentList.size) {
            currentList.add(index, page)
            _scannedPages.value = currentList
        }
    }
    
    fun saveAll(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            val storageManager = StorageManager(context)
            val pages = _scannedPages.value
            val format = _exportFormat.value
            val timestamp = System.currentTimeMillis()
            
            // Determine which URIs to save (Original vs Enhanced)
            val urisToSave = pages.map { it.getDisplayUri() }
            
            if (format == ExportFormat.PDF) {
                if (urisToSave.isNotEmpty()) {
                    storageManager.saveUrisToPdf(urisToSave, "PixelScanner_$timestamp.pdf")
                }
            } else {
                urisToSave.forEachIndexed { index, uri ->
                    val filename = "PixelScanner_${timestamp}_$index.jpg"
                    storageManager.saveUriToGallery(uri, filename)
                }
            }
            
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
