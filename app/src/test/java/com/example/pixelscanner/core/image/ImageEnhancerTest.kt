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

    @Test
    fun scalePreviewBackgroundRow_expandsSmallBackgroundAcrossOutputWidth() {
        val scaledRow = IntArray(4)

        scalePreviewBackgroundRow(
            sourceRowPixels = intArrayOf(
                rgb(10, 10, 10),
                rgb(20, 20, 20),
            ),
            outputRowPixels = scaledRow,
        )

        assertEquals(rgb(10, 10, 10), scaledRow[0])
        assertEquals(rgb(10, 10, 10), scaledRow[1])
        assertEquals(rgb(20, 20, 20), scaledRow[2])
        assertEquals(rgb(20, 20, 20), scaledRow[3])
    }

    @Test
    fun formatPreviewEnhancementMemoryReport_includesStageAndDeltaInformation() {
        val baseline = PreviewEnhancementMemorySnapshot(
            usedHeapBytes = 1 * MIB,
            freeHeapBytes = 2 * MIB,
            totalHeapBytes = 3 * MIB,
            maxHeapBytes = 4 * MIB,
            nativeHeapBytes = 5 * MIB,
        )
        val snapshot = PreviewEnhancementMemorySnapshot(
            usedHeapBytes = 3 * MIB,
            freeHeapBytes = 4 * MIB,
            totalHeapBytes = 7 * MIB,
            maxHeapBytes = 8 * MIB,
            nativeHeapBytes = 9 * MIB,
        )

        val report = formatPreviewEnhancementMemoryReport(
            stage = "decode-complete",
            snapshot = snapshot,
            baseline = baseline,
        )

        assertEquals(
            "decode-complete used=3.0 MiB free=4.0 MiB total=7.0 MiB max=8.0 MiB native=9.0 MiB " +
                "deltaUsed=+2.0 MiB deltaNative=+4.0 MiB",
            report,
        )
    }

    @Test
    fun interpolatePreviewBackgroundRowBilinear_smoothlyInterpolatesBetweenPixels() {
        val smallBackground = intArrayOf(
            rgb(0, 0, 0),
            rgb(100, 100, 100),
            rgb(100, 100, 100),
            rgb(200, 200, 200),
        )
        val outputRow = IntArray(3)

        interpolatePreviewBackgroundRowBilinear(
            smallBackgroundPixels = smallBackground,
            backgroundWidth = 2,
            backgroundHeight = 2,
            rowIndex = 1,
            outputHeight = 3,
            outputRowPixels = outputRow,
        )

        assertEquals(rgb(50, 50, 50), outputRow[0])
        assertEquals(rgb(100, 100, 100), outputRow[1])
        assertEquals(rgb(150, 150, 150), outputRow[2])
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (0xFF shl 24) or
            ((red and 0xFF) shl 16) or
            ((green and 0xFF) shl 8) or
            (blue and 0xFF)
    }

    private companion object {
        const val KIB = 1024L
        const val MIB = KIB * 1024L
    }
}
