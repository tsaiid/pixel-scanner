package com.example.pixelscanner.core.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageEnhancerTest {
    @Test
    fun calculateBackgroundDimensions_clampsTinyImageToAtLeastOnePixelPerSide() {
        val dimensions = calculateBackgroundDimensions(
            originalWidth = 1,
            originalHeight = 1,
            scaleFactor = 32,
        )

        assertEquals(1 to 1, dimensions)
    }

    @Test
    fun calculateBackgroundDimensions_handlesExtremeTallImageWithoutCollapsingWidth() {
        val dimensions = calculateBackgroundDimensions(
            originalWidth = 1,
            originalHeight = 4096,
            scaleFactor = 32,
        )

        assertEquals(1 to 128, dimensions)
    }

    @Test
    fun calculateBackgroundDimensions_handlesExtremeWideImageWithoutCollapsingHeight() {
        val dimensions = calculateBackgroundDimensions(
            originalWidth = 4096,
            originalHeight = 1,
            scaleFactor = 32,
        )

        assertEquals(128 to 1, dimensions)
    }

    @Test
    fun applyPreviewEnhancementToRow_transformsPixelsWithoutFullFrameBuffers() {
        val originalRowPixels = intArrayOf(
            rgb(20, 30, 40),
            rgb(0, 128, 255),
        )
        val backgroundRowPixels = intArrayOf(
            rgb(40, 60, 80),
            rgb(10, 128, 255),
        )
        val outputRowPixels = IntArray(originalRowPixels.size)

        applyPreviewEnhancementToRow(
            originalRowPixels = originalRowPixels,
            backgroundRowPixels = backgroundRowPixels,
            outputRowPixels = outputRowPixels,
        )

        assertEquals(rgb(124, 125, 125), outputRowPixels[0])
        assertEquals(rgb(0, 253, 254), outputRowPixels[1])
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (0xFF shl 24) or
            ((red and 0xFF) shl 16) or
            ((green and 0xFF) shl 8) or
            (blue and 0xFF)
    }
}
