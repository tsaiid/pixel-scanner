package com.example.pixelscanner.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class DesignTokensTest {

    @Test
    fun `color tokens mirror the brand palette and product surfaces`() {
        assertEquals(BrandPlum, PixelScannerColorTokens.brandPrimary)
        assertEquals(BrandLavender, PixelScannerColorTokens.brandAccent)
        assertEquals(BrandPlum, PixelScannerColorTokens.homeSurface)
        assertEquals(BrandPlum, PixelScannerColorTokens.reviewActionContent)
        assertEquals(BrandNightBackground, brandColorScheme(darkTheme = true).background)
    }

    @Test
    fun `layout tokens expose the product constants used by the current surfaces`() {
        assertEquals(16.dp, PixelScannerSpacingTokens.lg)
        assertEquals(280.dp, PixelScannerSpacingTokens.heroLogoSize)
        assertEquals(56.dp, PixelScannerSpacingTokens.primaryButtonHeight)
        assertEquals(12.dp, PixelScannerRadiusTokens.thumbnail)
        assertEquals(0.dp, PixelScannerElevationTokens.none)
    }
}
