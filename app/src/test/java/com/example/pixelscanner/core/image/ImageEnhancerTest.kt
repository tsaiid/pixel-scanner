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
}
