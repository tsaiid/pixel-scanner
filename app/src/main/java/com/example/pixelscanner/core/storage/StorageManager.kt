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
import com.example.pixelscanner.core.diagnostics.DebugLog
import com.example.pixelscanner.core.model.ExportDecodeFailure
import com.example.pixelscanner.core.model.ExportFailure
import com.example.pixelscanner.core.model.ExportInsufficientSpaceFailure
import com.example.pixelscanner.core.model.ExportOperationResult
import com.example.pixelscanner.core.model.ExportPermissionDeniedFailure
import com.example.pixelscanner.core.model.ExportReadFailure
import com.example.pixelscanner.core.model.ExportSourceInvalidFailure
import com.example.pixelscanner.core.model.ExportUnknownFailure
import com.example.pixelscanner.core.model.ExportWriteFailure
import com.example.pixelscanner.domain.DiskSpacePreflightRepository
import com.example.pixelscanner.domain.DiskSpacePreflightResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.io.File

class StorageManager(
    private val context: Context,
    private val diskSpacePreflightRepository: DiskSpacePreflightRepository = AndroidDiskSpacePreflightRepository(),
) {

    suspend fun copyPdfToDocuments(sourceUri: Uri, fileName: String): ExportOperationResult = withContext(Dispatchers.IO) {
        val resolvedFileName = resolveAvailableDisplayName(
            collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Files.getContentUri("external")
            },
            requestedFileName = fileName,
            relativePath = Environment.DIRECTORY_DOCUMENTS + "/PixelScanner",
        )
        when (preflightSharedStorage(
            requiredBytes = estimateBytesForSource(sourceUri),
        )) {
            is DiskSpacePreflightResult.Sufficient -> Unit
            is DiskSpacePreflightResult.Insufficient -> {
                return@withContext ExportOperationResult.Failure(ExportInsufficientSpaceFailure)
            }

            is DiskSpacePreflightResult.Unknown -> Unit
        }
        copyUriToMediaStore(
            sourceUri = sourceUri,
            fileName = resolvedFileName,
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
        val resolvedFileName = resolveAvailableDisplayName(
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            requestedFileName = fileName,
            relativePath = Environment.DIRECTORY_PICTURES + "/PixelScanner",
        )
        when (preflightSharedStorage(
            requiredBytes = estimateBytesForSource(sourceUri),
        )) {
            is DiskSpacePreflightResult.Sufficient -> Unit
            is DiskSpacePreflightResult.Insufficient -> {
                return@withContext ExportOperationResult.Failure(ExportInsufficientSpaceFailure)
            }

            is DiskSpacePreflightResult.Unknown -> Unit
        }
        copyUriToMediaStore(
            sourceUri = sourceUri,
            fileName = resolvedFileName,
            mimeType = "image/jpeg",
            relativePath = Environment.DIRECTORY_PICTURES + "/PixelScanner",
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        )
    }

    suspend fun saveUrisToPdf(sourceUris: List<Uri>, fileName: String): ExportOperationResult = withContext(Dispatchers.IO) {
        val resolvedFileName = resolveAvailableDisplayName(
            collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Files.getContentUri("external")
            },
            requestedFileName = fileName,
            relativePath = Environment.DIRECTORY_DOCUMENTS + "/PixelScanner",
        )
        when (preflightSharedStorage(
            requiredBytes = estimateBytesForSources(sourceUris),
        )) {
            is DiskSpacePreflightResult.Sufficient -> Unit
            is DiskSpacePreflightResult.Insufficient -> {
                return@withContext ExportOperationResult.Failure(ExportInsufficientSpaceFailure)
            }

            is DiskSpacePreflightResult.Unknown -> Unit
        }
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
            DebugLog.error("export_read_decode_failed", e)
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
            put(MediaStore.MediaColumns.DISPLAY_NAME, resolvedFileName)
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
                    destinationName = resolvedFileName,
                    details = "Failed to create PDF destination",
                ),
            )
        }

        try {
            resolver.openOutputStream(pdfUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            } ?: return@withContext ExportOperationResult.Failure(
                ExportWriteFailure(
                    destinationName = resolvedFileName,
                    details = "Failed to open PDF destination for writing",
                ),
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(pdfUri, contentValues, null, null)
            }
        } catch (e: Exception) {
            DebugLog.error("export_write_failed", e)
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
            DebugLog.error("export_copy_failed", e)
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
    ): ExportFailure {
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

    private fun preflightSharedStorage(requiredBytes: Long): DiskSpacePreflightResult {
        return diskSpacePreflightRepository.check(
            targetPath = sharedStorageRoot(),
            requiredBytes = requiredBytes,
        )
    }

    private fun sharedStorageRoot(): File {
        return Environment.getExternalStorageDirectory()
    }

    private fun estimateBytesForSource(sourceUri: Uri): Long {
        return ContentUriSizeEstimator.estimateBytes(context, sourceUri)
    }

    private fun estimateBytesForSources(sourceUris: List<Uri>): Long {
        return ContentUriSizeEstimator.estimateBytes(context, sourceUris)
    }

    private fun resolveAvailableDisplayName(
        collection: Uri,
        requestedFileName: String,
        relativePath: String,
    ): String {
        var candidate = requestedFileName
        var suffixIndex = 1
        while (mediaStoreFileExists(
                collection = collection,
                fileName = candidate,
                relativePath = relativePath,
            )
        ) {
            candidate = appendDuplicateSuffix(requestedFileName, suffixIndex++)
        }
        return candidate
    }

    private fun mediaStoreFileExists(
        collection: Uri,
        fileName: String,
        relativePath: String,
    ): Boolean {
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        } else {
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?"
        }
        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(fileName, relativePath)
        } else {
            arrayOf(fileName)
        }

        return context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            cursor.moveToFirst()
        } == true
    }

    private fun appendDuplicateSuffix(fileName: String, suffixIndex: Int): String {
        val extensionIndex = fileName.lastIndexOf('.')
        return if (extensionIndex > 0) {
            val baseName = fileName.substring(0, extensionIndex)
            val extension = fileName.substring(extensionIndex)
            "$baseName ($suffixIndex)$extension"
        } else {
            "$fileName ($suffixIndex)"
        }
    }
}
