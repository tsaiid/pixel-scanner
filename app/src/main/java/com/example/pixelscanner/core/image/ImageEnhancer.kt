package com.example.pixelscanner.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.core.storage.AndroidPreviewImageCacheRepository
import com.example.pixelscanner.domain.PreviewImageCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import kotlin.math.min

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

        val originalPixels = IntArray(width * height)
        val backgroundPixels = IntArray(width * height)
        val outputPixels = IntArray(width * height)

        originalBitmap.getPixels(originalPixels, 0, width, 0, 0, width, height)
        backgroundBitmap.getPixels(backgroundPixels, 0, width, 0, 0, width, height)
        backgroundBitmap.recycle()

        for (i in originalPixels.indices) {
            val origColor = originalPixels[i]
            val backColor = backgroundPixels[i]

            val rNew = min(255, (Color.red(origColor) * 255) / (Color.red(backColor) + 1))
            val gNew = min(255, (Color.green(origColor) * 255) / (Color.green(backColor) + 1))
            val bNew = min(255, (Color.blue(origColor) * 255) / (Color.blue(backColor) + 1))

            outputPixels[i] = Color.rgb(rNew, gNew, bNew)
        }

        originalBitmap.recycle()

        outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)

        val file = previewCacheRepository.createPreviewFile(context, request)
        FileOutputStream(file).use { outputStream ->
            outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }
        outputBitmap.recycle()

        PreviewImagePipelineResult(
            cacheKey = request.cacheKey,
            outputUri = Uri.fromFile(file),
            reusedCachedPreview = false,
        )
    }

    private fun calculateBackgroundDimensions(
        originalBitmap: Bitmap,
        scaleFactor: Int,
    ): Pair<Int, Int> {
        require(scaleFactor > 0) { "scaleFactor must be positive" }
        require(originalBitmap.width > 0 && originalBitmap.height > 0) {
            "Decoded bitmap must have positive dimensions"
        }

        val backgroundWidth = (originalBitmap.width / scaleFactor).coerceIn(1, originalBitmap.width)
        val backgroundHeight = (originalBitmap.height / scaleFactor).coerceIn(1, originalBitmap.height)

        return backgroundWidth to backgroundHeight
    }
}
