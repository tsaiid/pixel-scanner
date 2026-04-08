package com.example.pixelscanner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class PixelScannerColorPolicy {
    BrandFirst,
    DynamicColorWhenAvailable,
}

@Composable
fun PixelScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorPolicy: PixelScannerColorPolicy = PixelScannerColorPolicy.BrandFirst,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        shouldUseDynamicColor(
            colorPolicy = colorPolicy,
            supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        ) -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> brandColorScheme(darkTheme)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                colorScheme.primary.luminance() > 0.5f
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

internal fun shouldUseDynamicColor(
    colorPolicy: PixelScannerColorPolicy,
    supportsDynamicColor: Boolean,
): Boolean {
    return colorPolicy == PixelScannerColorPolicy.DynamicColorWhenAvailable && supportsDynamicColor
}

internal fun brandColorScheme(darkTheme: Boolean) = when (darkTheme) {
    true -> darkColorScheme(
        primary = BrandPlum,
        secondary = BrandLavender,
        tertiary = BrandSoftLavender,
        background = BrandNightBackground,
        surface = BrandNightSurface,
        onPrimary = Color.White,
        onSecondary = BrandNightOnSurface,
        onBackground = BrandNightOnSurface,
        onSurface = BrandNightOnSurface,
    )
    false -> lightColorScheme(
        primary = BrandPlum,
        secondary = BrandLavender,
        tertiary = BrandSoftLavender,
        background = BrandWarmLightBackground,
        surface = BrandWarmLightSurface,
        onPrimary = Color.White,
        onSecondary = BrandWarmLightOnSurface,
        onBackground = BrandWarmLightOnSurface,
        onSurface = BrandWarmLightOnSurface,
    )
}
