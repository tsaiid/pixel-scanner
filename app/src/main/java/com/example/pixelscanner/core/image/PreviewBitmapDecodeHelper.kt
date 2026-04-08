package com.example.pixelscanner.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

class PreviewBitmapDecodeHelper(
    private val maxDimension: Int = DEFAULT_MAX_PREVIEW_DIMENSION,
    // Keep preview bitmaps in software memory because the preview pipeline
    // reads and rewrites pixels on the CPU before caching the output.
    private val preferredConfig: Bitmap.Config = Bitmap.Config.RGB_565,
) {
    fun decode(context: Context, sourceUri: Uri): Bitmap {
        val bounds = decodeBounds(context, sourceUri)
        val sampleSize = calculateInSampleSize(
            sourceWidth = bounds.outWidth,
            sourceHeight = bounds.outHeight,
            maxWidth = maxDimension,
            maxHeight = maxDimension,
        )
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = preferredConfig
        }
        return decodeBitmap(context, sourceUri, options)
            ?: throw IllegalArgumentException("Could not decode bitmap from URI: $sourceUri")
    }

    private fun decodeBounds(context: Context, sourceUri: Uri): BitmapFactory.Options {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        } ?: throw IllegalArgumentException("Could not open bitmap bounds from URI: $sourceUri")

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw IllegalArgumentException("Invalid bitmap bounds for URI: $sourceUri")
        }
        return options
    }

    private fun decodeBitmap(
        context: Context,
        sourceUri: Uri,
        options: BitmapFactory.Options,
    ): Bitmap? {
        return context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    }

    private fun calculateInSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
    ): Int {
        var sampleSize = 1
        var halfWidth = sourceWidth / 2
        var halfHeight = sourceHeight / 2

        while (halfWidth / sampleSize >= maxWidth && halfHeight / sampleSize >= maxHeight) {
            sampleSize *= 2
        }

        return sampleSize.coerceAtLeast(1)
    }

    private companion object {
        const val DEFAULT_MAX_PREVIEW_DIMENSION = 1600
    }
}
