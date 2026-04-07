package com.example.pixelscanner.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class ImageEnhancer {

    suspend fun enhance(context: Context, uri: Uri): Uri = withContext(Dispatchers.IO) {
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        } ?: throw IllegalArgumentException("Could not decode bitmap from URI")

        val scaleFactor = 32
        val backgroundWidth = max(1, originalBitmap.width / scaleFactor)
        val backgroundHeight = max(1, originalBitmap.height / scaleFactor)

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

        val file = File(context.cacheDir, "enhanced_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { outputStream ->
            outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }
        outputBitmap.recycle()

        Uri.fromFile(file)
    }
}
