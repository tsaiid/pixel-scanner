package com.example.pixelscanner.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelscanner.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannedPage(
    val originalUri: Uri,
    val originalBitmap: Bitmap? = null
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
        viewModelScope.launch {
            val pages = uris.map { uri ->
                val bitmap = loadBitmapFromUri(context, uri)
                ScannedPage(originalUri = uri, originalBitmap = bitmap)
            }
            _scannedPages.value = pages
        }
    }
    
    fun saveAll(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            val storageManager = StorageManager(context)
            val pages = _scannedPages.value
            val format = _exportFormat.value
            val timestamp = System.currentTimeMillis()
            
            if (format == ExportFormat.PDF) {
                val bitmaps = pages.mapNotNull { it.originalBitmap }
                if (bitmaps.isNotEmpty()) {
                    storageManager.saveBitmapsToPdf(bitmaps, "PixelScanner_$timestamp.pdf")
                }
            } else {
                pages.forEachIndexed { index, page ->
                    val bitmapToSave = page.originalBitmap
                    
                    if (bitmapToSave != null) {
                        val filename = "PixelScanner_${timestamp}_$index.jpg"
                        storageManager.saveBitmapToGallery(bitmapToSave, filename)
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
