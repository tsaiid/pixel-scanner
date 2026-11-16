package com.example.pixelscanner.core.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Debug
import android.util.Log
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.core.storage.AndroidPreviewImageCacheRepository
import com.example.pixelscanner.domain.PreviewImageCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.min

internal data class PreviewEnhancementMemorySnapshot(
    val usedHeapBytes: Long,
    val freeHeapBytes: Long,
    val totalHeapBytes: Long,
    val maxHeapBytes: Long,
    val nativeHeapBytes: Long,
)

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

internal fun capturePreviewEnhancementMemorySnapshot(): PreviewEnhancementMemorySnapshot {
    val runtime = Runtime.getRuntime()
    val freeHeapBytes = runtime.freeMemory()
    val totalHeapBytes = runtime.totalMemory()
    return PreviewEnhancementMemorySnapshot(
        usedHeapBytes = totalHeapBytes - freeHeapBytes,
        freeHeapBytes = freeHeapBytes,
        totalHeapBytes = totalHeapBytes,
        maxHeapBytes = runtime.maxMemory(),
        nativeHeapBytes = Debug.getNativeHeapAllocatedSize(),
    )
}

internal fun formatPreviewEnhancementMemoryReport(
    stage: String,
    snapshot: PreviewEnhancementMemorySnapshot,
    baseline: PreviewEnhancementMemorySnapshot? = null,
): String {
    val deltaUsed = baseline?.let { snapshot.usedHeapBytes - it.usedHeapBytes }
    val deltaNative = baseline?.let { snapshot.nativeHeapBytes - it.nativeHeapBytes }

    return buildString {
        append(stage)
        append(" used=").append(formatPreviewEnhancementBytes(snapshot.usedHeapBytes))
        append(" free=").append(formatPreviewEnhancementBytes(snapshot.freeHeapBytes))
        append(" total=").append(formatPreviewEnhancementBytes(snapshot.totalHeapBytes))
        append(" max=").append(formatPreviewEnhancementBytes(snapshot.maxHeapBytes))
        append(" native=").append(formatPreviewEnhancementBytes(snapshot.nativeHeapBytes))
        if (baseline != null) {
            append(" deltaUsed=").append(formatSignedPreviewEnhancementBytes(deltaUsed ?: 0L))
            append(" deltaNative=").append(formatSignedPreviewEnhancementBytes(deltaNative ?: 0L))
        }
    }
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

        val baselineMemorySnapshot = if (shouldLogPreviewEnhancementMemory()) {
            capturePreviewEnhancementMemorySnapshot()
        } else {
            null
        }

        maybeLogPreviewEnhancementMemory(
            stage = "start",
            baseline = baselineMemorySnapshot,
            detail = "source=${request.sourceUri}",
        )

        val originalBitmap = decodeHelper.decode(context, request.sourceUri)
        maybeLogPreviewEnhancementMemory(
            stage = "decode-complete",
            baseline = baselineMemorySnapshot,
            detail = "bitmap=${originalBitmap.width}x${originalBitmap.height} config=${originalBitmap.config}",
        )

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
        maybeLogPreviewEnhancementMemory(
            stage = "background-build-complete",
            baseline = baselineMemorySnapshot,
            detail = "background=${backgroundSmall.width}x${backgroundSmall.height}",
        )

        val width = originalBitmap.width
        val height = originalBitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        val chunkRows = min(ROW_CHUNK_SIZE, height).coerceAtLeast(1)
        val originalChunkPixels = IntArray(width * chunkRows)
        val outputChunkPixels = IntArray(width * chunkRows)
        val originalRowPixels = IntArray(width)
        val backgroundRowPixels = IntArray(width)
        val outputRowPixels = IntArray(width)
        val smallBackgroundPixels = IntArray(backgroundWidth * backgroundHeight)

        try {
            backgroundSmall.getPixels(
                smallBackgroundPixels,
                0,
                backgroundWidth,
                0,
                0,
                backgroundWidth,
                backgroundHeight,
            )
            var startRow = 0
            while (startRow < height) {
                val rowsInChunk = min(chunkRows, height - startRow)
                originalBitmap.getPixels(
                    originalChunkPixels,
                    0,
                    width,
                    0,
                    startRow,
                    width,
                    rowsInChunk,
                )
                for (localRow in 0 until rowsInChunk) {
                    val rowIndex = startRow + localRow
                    val rowOffset = localRow * width
                    System.arraycopy(originalChunkPixels, rowOffset, originalRowPixels, 0, width)
                    interpolatePreviewBackgroundRowBilinear(
                        smallBackgroundPixels = smallBackgroundPixels,
                        backgroundWidth = backgroundWidth,
                        backgroundHeight = backgroundHeight,
                        rowIndex = rowIndex,
                        outputHeight = height,
                        outputRowPixels = backgroundRowPixels,
                    )
                    applyPreviewEnhancementToRow(
                        originalRowPixels = originalRowPixels,
                        backgroundRowPixels = backgroundRowPixels,
                        outputRowPixels = outputRowPixels,
                    )
                    System.arraycopy(outputRowPixels, 0, outputChunkPixels, rowOffset, width)
                }
                outputBitmap.setPixels(
                    outputChunkPixels,
                    0,
                    width,
                    0,
                    startRow,
                    width,
                    rowsInChunk,
                )
                startRow += rowsInChunk
            }
            maybeLogPreviewEnhancementMemory(
                stage = "row-processing-complete",
                baseline = baselineMemorySnapshot,
                detail = "rows=$height output=${outputBitmap.width}x${outputBitmap.height}",
            )

            val file = previewCacheRepository.createPreviewFile(context, request)
            FileOutputStream(file).use { outputStream ->
                check(outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)) {
                    "Failed to compress enhanced preview bitmap"
                }
            }
            maybeLogPreviewEnhancementMemory(
                stage = "final-write-complete",
                baseline = baselineMemorySnapshot,
                detail = "fileBytes=${file.length()}",
            )
            return@withContext PreviewImagePipelineResult(
                cacheKey = request.cacheKey,
                outputUri = Uri.fromFile(file),
                reusedCachedPreview = false,
            )
        } finally {
            originalBitmap.recycle()
            backgroundSmall.recycle()
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

internal fun interpolatePreviewBackgroundRowBilinear(
    smallBackgroundPixels: IntArray,
    backgroundWidth: Int,
    backgroundHeight: Int,
    rowIndex: Int,
    outputHeight: Int,
    outputRowPixels: IntArray,
) {
    val outputWidth = outputRowPixels.size
    val yFloat = if (outputHeight > 1) {
        rowIndex.toFloat() * (backgroundHeight - 1).toFloat() / (outputHeight - 1).toFloat()
    } else {
        0f
    }
    val y0 = yFloat.toInt().coerceIn(0, backgroundHeight - 1)
    val y1 = (y0 + 1).coerceIn(0, backgroundHeight - 1)
    val yFraction = (yFloat - y0).coerceIn(0f, 1f)
    val y0Offset = y0 * backgroundWidth
    val y1Offset = y1 * backgroundWidth

    for (x in 0 until outputWidth) {
        val xFloat = if (outputWidth > 1) {
            x.toFloat() * (backgroundWidth - 1).toFloat() / (outputWidth - 1).toFloat()
        } else {
            0f
        }
        val x0 = xFloat.toInt().coerceIn(0, backgroundWidth - 1)
        val x1 = (x0 + 1).coerceIn(0, backgroundWidth - 1)
        val xFraction = (xFloat - x0).coerceIn(0f, 1f)

        val c00 = smallBackgroundPixels[y0Offset + x0]
        val c10 = smallBackgroundPixels[y0Offset + x1]
        val c01 = smallBackgroundPixels[y1Offset + x0]
        val c11 = smallBackgroundPixels[y1Offset + x1]

        val r = bilinearChannel(red(c00), red(c10), red(c01), red(c11), xFraction, yFraction)
        val g = bilinearChannel(green(c00), green(c10), green(c01), green(c11), xFraction, yFraction)
        val b = bilinearChannel(blue(c00), blue(c10), blue(c01), blue(c11), xFraction, yFraction)
        outputRowPixels[x] = rgb(r, g, b)
    }
}

private fun bilinearChannel(
    c00: Int,
    c10: Int,
    c01: Int,
    c11: Int,
    xFraction: Float,
    yFraction: Float,
): Int {
    val top = c00 + (c10 - c00) * xFraction
    val bottom = c01 + (c11 - c01) * xFraction
    return (top + (bottom - top) * yFraction).toInt().coerceIn(0, 255)
}

internal fun scalePreviewBackgroundRow(
    sourceRowPixels: IntArray,
    outputRowPixels: IntArray,
) {
    require(sourceRowPixels.isNotEmpty()) { "Source row must not be empty" }
    require(outputRowPixels.isNotEmpty()) { "Output row must not be empty" }

    val sourceWidth = sourceRowPixels.size
    val outputWidth = outputRowPixels.size
    for (index in 0 until outputWidth) {
        val sourceIndex = (index * sourceWidth / outputWidth).coerceIn(0, sourceWidth - 1)
        outputRowPixels[index] = sourceRowPixels[sourceIndex]
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

private fun maybeLogPreviewEnhancementMemory(
    stage: String,
    baseline: PreviewEnhancementMemorySnapshot?,
    detail: String,
) {
    if (!shouldLogPreviewEnhancementMemory() || baseline == null) {
        return
    }

    val snapshot = capturePreviewEnhancementMemorySnapshot()
    Log.d(
        PREVIEW_ENHANCER_MEMORY_TAG,
        formatPreviewEnhancementMemoryReport(stage, snapshot, baseline) + " $detail",
    )
}

private fun formatPreviewEnhancementBytes(bytes: Long): String {
    val absBytes = kotlin.math.abs(bytes)
    val unit = when {
        absBytes >= GIB -> "GiB"
        absBytes >= MIB -> "MiB"
        absBytes >= KIB -> "KiB"
        else -> "B"
    }
    val divisor = when (unit) {
        "GiB" -> GIB
        "MiB" -> MIB
        "KiB" -> KIB
        else -> 1L
    }
    return if (divisor == 1L) {
        "$bytes B"
    } else {
        String.format(
            Locale.US,
            "%.1f %s",
            bytes.toDouble() / divisor.toDouble(),
            unit,
        )
    }
}

private fun formatSignedPreviewEnhancementBytes(bytes: Long): String {
    val prefix = if (bytes >= 0) "+" else "-"
    return prefix + formatPreviewEnhancementBytes(kotlin.math.abs(bytes))
}

private const val PREVIEW_ENHANCER_MEMORY_TAG = "ImageEnhancerMem"
private const val ROW_CHUNK_SIZE = 64
private const val KIB = 1024L
private const val MIB = KIB * 1024L
private const val GIB = MIB * 1024L

private fun shouldLogPreviewEnhancementMemory(): Boolean {
    return Log.isLoggable(PREVIEW_ENHANCER_MEMORY_TAG, Log.DEBUG)
}
