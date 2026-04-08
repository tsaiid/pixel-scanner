package com.example.pixelscanner.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelScannerThemePolicyTest {

    @Test
    fun `brand-first policy never uses dynamic color`() {
        assertFalse(
            shouldUseDynamicColor(
                colorPolicy = PixelScannerColorPolicy.BrandFirst,
                supportsDynamicColor = true,
            )
        )
        assertFalse(
            shouldUseDynamicColor(
                colorPolicy = PixelScannerColorPolicy.BrandFirst,
                supportsDynamicColor = false,
            )
        )
    }

    @Test
    fun `dynamic color policy only uses dynamic color on supported devices`() {
        assertTrue(
            shouldUseDynamicColor(
                colorPolicy = PixelScannerColorPolicy.DynamicColorWhenAvailable,
                supportsDynamicColor = true,
            )
        )
        assertFalse(
            shouldUseDynamicColor(
                colorPolicy = PixelScannerColorPolicy.DynamicColorWhenAvailable,
                supportsDynamicColor = false,
            )
        )
    }

    @Test
    fun `brand color scheme stays on brand palette in both light and dark mode`() {
        val lightScheme = brandColorScheme(darkTheme = false)
        val darkScheme = brandColorScheme(darkTheme = true)

        assertEquals(BrandPlum.toArgb(), lightScheme.primary.toArgb())
        assertEquals(BrandWarmLightBackground.toArgb(), lightScheme.background.toArgb())
        assertEquals(BrandPlum.toArgb(), darkScheme.primary.toArgb())
        assertEquals(BrandNightBackground.toArgb(), darkScheme.background.toArgb())
    }
}
