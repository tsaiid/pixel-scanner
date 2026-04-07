package com.example.pixelscanner.core.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageManager(private val context: Context) {

    suspend fun copyPdfToDocuments(sourceUri: Uri, fileName: String): Uri? = withContext(Dispatchers.IO) {
        copyUriToMediaStore(
            sourceUri = sourceUri,
            fileName = fileName,
            mimeType = "application/pdf",
            relativePath = Environment.DIRECTORY_DOCUMENTS + "/PixelScanner",
            collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Files.getContentUri("external")
            },
        )
    }

    suspend fun saveUriToGallery(sourceUri: Uri, fileName: String): Uri? = withContext(Dispatchers.IO) {
        copyUriToMediaStore(
            sourceUri = sourceUri,
            fileName = fileName,
            mimeType = "image/jpeg",
            relativePath = Environment.DIRECTORY_PICTURES + "/PixelScanner",
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        )
    }

    suspend fun saveUrisToPdf(sourceUris: List<Uri>, fileName: String): Uri? = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val resolver = context.contentResolver

        try {
            sourceUris.forEachIndexed { index, uri ->
                val inputStream = resolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return@withContext null
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/PixelScanner")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val pdfUri = resolver.insert(collection, contentValues)

        pdfUri?.let { uri ->
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                resolver.delete(uri, null, null)
                pdfDocument.close()
                return@withContext null
            }
        } ?: pdfDocument.close()

        return@withContext pdfUri
    }

    private fun copyUriToMediaStore(
        sourceUri: Uri,
        fileName: String,
        mimeType: String,
        relativePath: String,
        collection: Uri,
    ): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val destinationUri = resolver.insert(collection, contentValues)

        destinationUri?.let { destUri ->
            try {
                resolver.openInputStream(sourceUri)?.use { inputStream ->
                    resolver.openOutputStream(destUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    } ?: return null
                } ?: return null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(destUri, contentValues, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                resolver.delete(destUri, null, null)
                return null
            }
        }

        return destinationUri
    }
}
