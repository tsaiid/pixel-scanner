package com.example.pixelscanner.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeToEdgePolicyTest {

    @Test
    fun `brand dark surfaces use dark system bar icons`() {
        val style = resolveSystemBarStyle(brandColorScheme(darkTheme = true))

        assertEquals(Color.Transparent, style.statusBarColor)
        assertEquals(Color.Transparent, style.navigationBarColor)
        assertFalse(style.isAppearanceLightStatusBars)
        assertFalse(style.isAppearanceLightNavigationBars)
    }

    @Test
    fun `light surfaces use light system bar appearance for dark icons`() {
        val style = resolveSystemBarStyle(
            lightColorScheme(
                background = Color.White,
                surface = Color.White,
            )
        )

        assertTrue(style.isAppearanceLightStatusBars)
        assertTrue(style.isAppearanceLightNavigationBars)
    }
}
