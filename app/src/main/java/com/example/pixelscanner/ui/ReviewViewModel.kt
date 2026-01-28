package com.example.pixelscanner.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelscanner.image.ImageProcessor
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
    val originalBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
    val isProcessed: Boolean = false,
    val isLoading: Boolean = false
)

class ReviewViewModel : ViewModel() {

    private val _scannedPages = MutableStateFlow<List<ScannedPage>>(emptyList())
    val scannedPages: StateFlow<List<ScannedPage>> = _scannedPages.asStateFlow()

    private val imageProcessor = ImageProcessor()

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
            val timestamp = System.currentTimeMillis()
            
            pages.forEachIndexed { index, page ->
                val bitmapToSave = if (page.isProcessed) page.processedBitmap else page.originalBitmap
                
                if (bitmapToSave != null) {
                    val filename = "PixelScanner_${timestamp}_$index.jpg"
                    storageManager.saveBitmapToGallery(bitmapToSave, filename)
                }
            }
            
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun toggleProcessing(index: Int) {
        val pages = _scannedPages.value.toMutableList()
        val page = pages[index]

        if (page.isProcessed) {
            pages[index] = page.copy(isProcessed = false)
            _scannedPages.value = pages
        } else {
            if (page.processedBitmap != null) {
                pages[index] = page.copy(isProcessed = true)
                _scannedPages.value = pages
            } else {
                pages[index] = page.copy(isLoading = true)
                _scannedPages.value = pages
                
                viewModelScope.launch {
                    val original = page.originalBitmap
                    if (original != null) {
                        val processed = imageProcessor.removeShadow(original)
                        _scannedPages.update { currentPages ->
                            val newPages = currentPages.toMutableList()
                            if (index < newPages.size && newPages[index].originalUri == page.originalUri) {
                                newPages[index] = newPages[index].copy(
                                    processedBitmap = processed,
                                    isProcessed = true,
                                    isLoading = false
                                )
                            }
                            newPages
                        }
                    } else {
                         _scannedPages.update { currentPages ->
                            val newPages = currentPages.toMutableList()
                            newPages[index] = newPages[index].copy(isLoading = false)
                            newPages
                        }
                    }
                }
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
