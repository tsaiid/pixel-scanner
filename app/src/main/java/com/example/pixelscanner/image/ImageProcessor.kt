package com.example.pixelscanner.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object ImageProcessor {

    suspend fun enhanceImage(context: Context, uri: Uri): Uri = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) throw IllegalArgumentException("Could not decode bitmap from URI")

        // 1. Estimate background (Downscale -> Upscale)
        // Downscale significantly to get a coarse background estimation (low-pass filter)
        val scaleFactor = 32
        val backgroundWidth = originalBitmap.width / scaleFactor
        val backgroundHeight = originalBitmap.height / scaleFactor
        
        // Create scaled down bitmap (this acts as a blur)
        val backgroundSmall = Bitmap.createScaledBitmap(originalBitmap, backgroundWidth, backgroundHeight, true)
        
        // Upscale back to original size to match pixels
        val backgroundBitmap = Bitmap.createScaledBitmap(backgroundSmall, originalBitmap.width, originalBitmap.height, true)
        backgroundSmall.recycle()

        // 2. Lightness Equalization (Division)
        val width = originalBitmap.width
        val height = originalBitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val originalPixels = IntArray(width * height)
        val backgroundPixels = IntArray(width * height)
        val outputPixels = IntArray(width * height)

        originalBitmap.getPixels(originalPixels, 0, width, 0, 0, width, height)
        backgroundBitmap.getPixels(backgroundPixels, 0, width, 0, 0, width, height)
        backgroundBitmap.recycle()

        // Iterate pixels
        for (i in originalPixels.indices) {
            val origColor = originalPixels[i]
            val backColor = backgroundPixels[i]

            val rO = Color.red(origColor)
            val gO = Color.green(origColor)
            val bO = Color.blue(origColor)

            val rB = Color.red(backColor)
            val gB = Color.green(backColor)
            val bB = Color.blue(backColor)

            // Avoid division by zero, though unlikely with photos
            // Formula: Result = (Original / Background) * 255
            // We clamp to 255.
            // Adding a small epsilon to denominator to prevent divide by zero
            val rNew = min(255, (rO * 255) / (rB + 1))
            val gNew = min(255, (gO * 255) / (gB + 1))
            val bNew = min(255, (bO * 255) / (bB + 1))

            outputPixels[i] = Color.rgb(rNew, gNew, bNew)
        }
        
        originalBitmap.recycle()

        outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)

        // 3. Save to cache
        val filename = "enhanced_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, filename)
        val outStream = FileOutputStream(file)
        outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
        outStream.flush()
        outStream.close()
        outputBitmap.recycle()

        Uri.fromFile(file)
    }
}
