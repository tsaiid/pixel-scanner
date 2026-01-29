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

data class ScannedPage(
    val originalUri: Uri
)

enum class ExportFormat {
    IMAGE, PDF
}

class ReviewViewModel : ViewModel() {

    private val _scannedPages = MutableStateFlow<List<ScannedPage>>(emptyList())
    val scannedPages: StateFlow<List<ScannedPage>> = _scannedPages.asStateFlow()

    private val _exportFormat = MutableStateFlow(ExportFormat.IMAGE)
    val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    fun setExportFormat(format: ExportFormat) {
        _exportFormat.value = format
    }

    fun setScannedUris(uris: List<Uri>, context: Context) {
        // Simply map URIs to ScannedPage, no heavy loading here
        _scannedPages.value = uris.map { ScannedPage(originalUri = it) }
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
            
            if (format == ExportFormat.PDF) {
                val uris = pages.map { it.originalUri }
                if (uris.isNotEmpty()) {
                    storageManager.saveUrisToPdf(uris, "PixelScanner_$timestamp.pdf")
                }
            } else {
                pages.forEachIndexed { index, page ->
                    val filename = "PixelScanner_${timestamp}_$index.jpg"
                    storageManager.saveUriToGallery(page.originalUri, filename)
                }
            }
            
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
