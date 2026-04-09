package com.example.pixelscanner.core.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object ContentUriSizeEstimator {
    private const val DEFAULT_FALLBACK_BYTES = 1L * 1024 * 1024

    fun estimateBytes(
        context: Context,
        sourceUri: Uri,
        fallbackBytes: Long = DEFAULT_FALLBACK_BYTES,
    ): Long {
        return when (sourceUri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                val file = sourceUri.path?.let(::File)
                file?.takeIf(File::exists)?.length() ?: fallbackBytes
            }

            else -> queryOpenableSize(context, sourceUri)
                ?: openAssetLength(context, sourceUri)
                ?: fallbackBytes
        }
    }

    fun estimateBytes(
        context: Context,
        sourceUris: List<Uri>,
        fallbackBytesPerUri: Long = DEFAULT_FALLBACK_BYTES,
    ): Long {
        return sourceUris.sumOf { estimateBytes(context, it, fallbackBytesPerUri) }
    }

    private fun queryOpenableSize(context: Context, sourceUri: Uri): Long? {
        return context.contentResolver.query(
            sourceUri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !cursor.isNull(index)) {
                    cursor.getLong(index)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    private fun openAssetLength(context: Context, sourceUri: Uri): Long? {
        return context.contentResolver.openAssetFileDescriptor(sourceUri, "r")
            ?.length
            ?.takeIf { it >= 0 }
    }
}
