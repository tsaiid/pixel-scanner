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
        val backgroundBitmap = Bitmap.createScaledBitmap(
            backgroundSmall,
            originalBitmap.width,
            originalBitmap.height,
            true
        )
        backgroundSmall.recycle()
        maybeLogPreviewEnhancementMemory(
            stage = "background-build-complete",
            baseline = baselineMemorySnapshot,
            detail = "background=${backgroundBitmap.width}x${backgroundBitmap.height}",
        )

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
            maybeLogPreviewEnhancementMemory(
                stage = "row-processing-complete",
                baseline = baselineMemorySnapshot,
                detail = "rows=$height output=${outputBitmap.width}x${outputBitmap.height}",
            )

            val file = previewCacheRepository.createPreviewFile(context, request)
            FileOutputStream(file).use { outputStream ->
                outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
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
private const val KIB = 1024L
private const val MIB = KIB * 1024L
private const val GIB = MIB * 1024L

private fun shouldLogPreviewEnhancementMemory(): Boolean {
    return Log.isLoggable(PREVIEW_ENHANCER_MEMORY_TAG, Log.DEBUG)
}
