package com.example.pixelscanner.core.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.core.storage.AndroidPreviewImageCacheRepository
import com.example.pixelscanner.domain.PreviewImageCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import kotlin.math.min

internal fun applyPreviewEnhancementToRow(
    originalRowPixels: IntArray,
    backgroundRowPixels: IntArray,
    outputRowPixels: IntArray,
) {
    require(originalRowPixels.size == backgroundRowPixels.size) {
        "Row buffers must have matching lengths"
    }
    require(outputRowPixels.size == originalRowPixels.size) {
        "Row buffers must have matching lengths"
    }

    for (index in originalRowPixels.indices) {
        val origColor = originalRowPixels[index]
        val backColor = backgroundRowPixels[index]

        val rNew = min(255, (red(origColor) * 255) / (red(backColor) + 1))
        val gNew = min(255, (green(origColor) * 255) / (green(backColor) + 1))
        val bNew = min(255, (blue(origColor) * 255) / (blue(backColor) + 1))

        outputRowPixels[index] = rgb(rNew, gNew, bNew)
    }
}

internal fun calculateBackgroundDimensions(
    originalWidth: Int,
    originalHeight: Int,
    scaleFactor: Int,
): Pair<Int, Int> {
    require(scaleFactor > 0) { "scaleFactor must be positive" }
    require(originalWidth > 0 && originalHeight > 0) {
        "Decoded bitmap must have positive dimensions"
    }

    val backgroundWidth = (originalWidth / scaleFactor).coerceIn(1, originalWidth)
    val backgroundHeight = (originalHeight / scaleFactor).coerceIn(1, originalHeight)

    return backgroundWidth to backgroundHeight
}

class ImageEnhancer {
    private val previewCacheRepository: PreviewImageCacheRepository =
        AndroidPreviewImageCacheRepository()
    private val decodeHelper = PreviewBitmapDecodeHelper()

    suspend fun enhance(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): PreviewImagePipelineResult = withContext(Dispatchers.IO) {
        previewCacheRepository.getCachedPreview(context, request)?.let { cachedFile ->
            return@withContext PreviewImagePipelineResult(
                cacheKey = request.cacheKey,
                outputUri = Uri.fromFile(cachedFile),
                reusedCachedPreview = true,
            )
        }

        val originalBitmap = decodeHelper.decode(context, request.sourceUri)

        val (backgroundWidth, backgroundHeight) = calculateBackgroundDimensions(
            originalBitmap = originalBitmap,
            scaleFactor = 32,
        )

        val backgroundSmall = Bitmap.createScaledBitmap(
            originalBitmap,
            backgroundWidth,
            backgroundHeight,
            true
        )
        val backgroundBitmap = Bitmap.createScaledBitmap(
            backgroundSmall,
            originalBitmap.width,
            originalBitmap.height,
            true
        )
        backgroundSmall.recycle()

        val width = originalBitmap.width
        val height = originalBitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val originalRowPixels = IntArray(width)
        val backgroundRowPixels = IntArray(width)
        val outputRowPixels = IntArray(width)

        try {
            for (rowIndex in 0 until height) {
                originalBitmap.getPixels(originalRowPixels, 0, width, 0, rowIndex, width, 1)
                backgroundBitmap.getPixels(backgroundRowPixels, 0, width, 0, rowIndex, width, 1)
                applyPreviewEnhancementToRow(
                    originalRowPixels = originalRowPixels,
                    backgroundRowPixels = backgroundRowPixels,
                    outputRowPixels = outputRowPixels,
                )
                outputBitmap.setPixels(outputRowPixels, 0, width, 0, rowIndex, width, 1)
            }

            val file = previewCacheRepository.createPreviewFile(context, request)
            FileOutputStream(file).use { outputStream ->
                outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            return@withContext PreviewImagePipelineResult(
                cacheKey = request.cacheKey,
                outputUri = Uri.fromFile(file),
                reusedCachedPreview = false,
            )
        } finally {
            originalBitmap.recycle()
            backgroundBitmap.recycle()
            outputBitmap.recycle()
        }
    }

    private fun calculateBackgroundDimensions(
        originalBitmap: Bitmap,
        scaleFactor: Int,
    ): Pair<Int, Int> {
        return calculateBackgroundDimensions(
            originalWidth = originalBitmap.width,
            originalHeight = originalBitmap.height,
            scaleFactor = scaleFactor,
        )
    }
}

private fun red(color: Int): Int = (color shr 16) and 0xFF

private fun green(color: Int): Int = (color shr 8) and 0xFF

private fun blue(color: Int): Int = color and 0xFF

private fun rgb(red: Int, green: Int, blue: Int): Int {
    return (0xFF shl 24) or
        ((red and 0xFF) shl 16) or
        ((green and 0xFF) shl 8) or
        (blue and 0xFF)
}
