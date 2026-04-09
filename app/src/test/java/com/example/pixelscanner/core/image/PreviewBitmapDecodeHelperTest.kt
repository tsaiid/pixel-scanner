package com.example.pixelscanner.core.image

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.example.pixelscanner.core.image.calculateInSampleSize
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PreviewBitmapDecodeHelperTest {

    @Test
    fun calculateInSampleSize_downscalesExtremeTallImageWhenOnlyHeightExceedsLimit() {
        val sampleSize = calculateInSampleSize(
            sourceWidth = 1,
            sourceHeight = 4096,
            maxWidth = 1600,
            maxHeight = 1600,
        )

        assertEquals(4, sampleSize)
    }

    @Test
    fun calculateInSampleSize_downscalesExtremeWideImageWhenOnlyWidthExceedsLimit() {
        val sampleSize = calculateInSampleSize(
            sourceWidth = 4096,
            sourceHeight = 1,
            maxWidth = 1600,
            maxHeight = 1600,
        )

        assertEquals(4, sampleSize)
    }

    @Test
    fun calculateInSampleSize_keepsTinyImageAtSampleSizeOne() {
        val sampleSize = calculateInSampleSize(
            sourceWidth = 1,
            sourceHeight = 1,
            maxWidth = 1600,
            maxHeight = 1600,
        )

        assertEquals(1, sampleSize)
    }

    @Test
    fun decode_usesRgb565AsThePreferredConfig() {
        val context = RuntimeEnvironment.getApplication()
        val sourceUri = Uri.fromFile(createOpaqueJpegFile())

        val decodedBitmap = PreviewBitmapDecodeHelper().decode(context, sourceUri)

        assertEquals(Bitmap.Config.RGB_565, decodedBitmap.config)
        assertEquals(8, decodedBitmap.width)
        assertEquals(8, decodedBitmap.height)
    }

    private fun createOpaqueJpegFile(): File {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }
        val file = File.createTempFile("preview-bitmap-decode-helper", ".jpg")
        FileOutputStream(file).use { outputStream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream))
        }
        bitmap.recycle()
        file.deleteOnExit()
        return file
    }
}
