package com.example.pixelscanner.core.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.pixelscanner.core.model.ExportDecodeFailure
import com.example.pixelscanner.core.model.ExportInsufficientSpaceFailure
import com.example.pixelscanner.core.model.ExportOperationResult
import com.example.pixelscanner.core.model.ExportPermissionDeniedFailure
import com.example.pixelscanner.core.model.ExportReadFailure
import com.example.pixelscanner.core.model.ExportSourceInvalidFailure
import com.example.pixelscanner.core.model.ExportUnknownFailure
import com.example.pixelscanner.core.model.ExportWriteFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException

class StorageManager(private val context: Context) {

    suspend fun copyPdfToDocuments(sourceUri: Uri, fileName: String): ExportOperationResult = withContext(Dispatchers.IO) {
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

    suspend fun saveUriToGallery(sourceUri: Uri, fileName: String): ExportOperationResult = withContext(Dispatchers.IO) {
        copyUriToMediaStore(
            sourceUri = sourceUri,
            fileName = fileName,
            mimeType = "image/jpeg",
            relativePath = Environment.DIRECTORY_PICTURES + "/PixelScanner",
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        )
    }

    suspend fun saveUrisToPdf(sourceUris: List<Uri>, fileName: String): ExportOperationResult = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val resolver = context.contentResolver

        try {
            sourceUris.forEachIndexed { index, uri ->
                val bitmap: Bitmap? = resolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                } ?: return@withContext ExportOperationResult.Failure(
                    ExportSourceInvalidFailure(
                        sourceUri = uri,
                        details = "Source image is no longer available",
                    ),
                )

                if (bitmap == null) {
                    return@withContext ExportOperationResult.Failure(
                        ExportDecodeFailure(
                            sourceUri = uri,
                            details = "Failed to decode source image",
                        ),
                    )
                }

                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return@withContext ExportOperationResult.Failure(
                classifyFailure(
                    exception = e,
                    sourceUri = sourceUris.firstOrNull(),
                    fileName = fileName,
                    phase = "read/decode",
                ),
            )
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

        if (pdfUri == null) {
            pdfDocument.close()
            return@withContext ExportOperationResult.Failure(
                ExportWriteFailure(
                    destinationName = fileName,
                    details = "Failed to create PDF destination",
                ),
            )
        }

        try {
            resolver.openOutputStream(pdfUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            } ?: return@withContext ExportOperationResult.Failure(
                ExportWriteFailure(
                    destinationName = fileName,
                    details = "Failed to open PDF destination for writing",
                ),
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(pdfUri, contentValues, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(pdfUri, null, null)
            pdfDocument.close()
            return@withContext ExportOperationResult.Failure(
                classifyFailure(
                    exception = e,
                    sourceUri = sourceUris.firstOrNull(),
                    fileName = fileName,
                    phase = "write",
                ),
            )
        } finally {
            pdfDocument.close()
        }

        return@withContext ExportOperationResult.Success(pdfUri)
    }

    private fun copyUriToMediaStore(
        sourceUri: Uri,
        fileName: String,
        mimeType: String,
        relativePath: String,
        collection: Uri,
    ): ExportOperationResult {
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

        if (destinationUri == null) {
            return ExportOperationResult.Failure(
                ExportWriteFailure(
                    destinationName = fileName,
                    details = "Failed to create destination file",
                ),
            )
        }

        return try {
            resolver.openInputStream(sourceUri)?.use { inputStream ->
                resolver.openOutputStream(destinationUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                } ?: return ExportOperationResult.Failure(
                    ExportWriteFailure(
                        destinationName = fileName,
                        details = "Failed to open destination file for writing",
                    ),
                )
            } ?: return ExportOperationResult.Failure(
                ExportSourceInvalidFailure(
                    sourceUri = sourceUri,
                    details = "Source file is no longer available",
                ),
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(destinationUri, contentValues, null, null)
            }

            ExportOperationResult.Success(destinationUri)
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(destinationUri, null, null)
            ExportOperationResult.Failure(
                classifyFailure(
                    exception = e,
                    sourceUri = sourceUri,
                    fileName = fileName,
                    phase = "copy",
                ),
            )
        }
    }

    private fun classifyFailure(
        exception: Exception,
        sourceUri: Uri?,
        fileName: String,
        phase: String,
    ): com.example.pixelscanner.core.model.ExportFailure {
        val message = exception.message?.takeIf { it.isNotBlank() }
        return when (exception) {
            is SecurityException -> ExportPermissionDeniedFailure
            is FileNotFoundException -> {
                if (phase == "read/decode" || phase == "copy") {
                    ExportSourceInvalidFailure(
                        sourceUri = sourceUri,
                        details = message ?: "Source file is no longer available",
                    )
                } else {
                    ExportWriteFailure(
                        destinationName = fileName,
                        details = message ?: "Failed to write exported file",
                    )
                }
            }

            is IOException -> {
                if (message?.contains("ENOSPC", ignoreCase = true) == true ||
                    message?.contains("No space left", ignoreCase = true) == true
                ) {
                    ExportInsufficientSpaceFailure
                } else if (phase == "read/decode") {
                    ExportReadFailure(
                        sourceUri = sourceUri,
                        details = message ?: "Failed to read source file",
                    )
                } else {
                    ExportWriteFailure(
                        destinationName = fileName,
                        details = message ?: "Failed to write exported file",
                    )
                }
            }

            else -> {
                if (phase == "read/decode") {
                    ExportUnknownFailure(
                        details = message ?: "Export failed while reading source",
                    )
                } else {
                    ExportUnknownFailure(
                        details = message ?: "Export failed while writing output",
                    )
                }
            }
        }
    }
}
