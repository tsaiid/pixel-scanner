package com.example.pixelscanner.feature.review.ui

import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReviewAccessibilityContrastTest {

    @Test
    fun `review palette meets basic contrast thresholds`() {
        assertContrast("title on black", 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 4.5)
        assertContrast("counter on black", 0xFFD3D3D3.toInt(), 0xFF000000.toInt(), 4.5)
        assertContrast("scan more button", 0xFF541765.toInt(), 0xFFFFFFFF.toInt(), 4.5)
        assertContrast("original chip", 0xFFD0D0D0.toInt(), 0xFF222222.toInt(), 4.5)
        assertContrast("enhanced chip", 0xFFE0D2FF.toInt(), 0xFF1E1735.toInt(), 4.5)
        assertContrast("processing chip", 0xFFFFE08A.toInt(), 0xFF3A2B00.toInt(), 4.5)
        assertContrast("failure chip", 0xFFFFC7C7.toInt(), 0xFF351A1A.toInt(), 4.5)
    }

    private fun assertContrast(
        label: String,
        foregroundArgb: Int,
        backgroundArgb: Int,
        minimumContrast: Double,
    ) {
        val contrast = ColorUtils.calculateContrast(foregroundArgb, backgroundArgb)
        assertTrue("$label contrast was $contrast", contrast >= minimumContrast)
    }
}
