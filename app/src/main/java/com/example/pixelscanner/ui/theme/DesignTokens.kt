package com.example.pixelscanner.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

object PixelScannerColorTokens {
    val brandPrimary = BrandPlum
    val brandAccent = BrandLavender
    val brandAccentSoft = BrandSoftLavender

    val homeSurface = BrandPlum
    val homeOnSurface = Color.White
    val homeButtonContainer = Color.White
    val homeButtonContent = BrandPlum

    val reviewSurface = Color.Black
    val reviewSurfaceRaised = Color(0xFF111111)
    val reviewThumbnailSurface = Color(0xFF1A1A1A)
    val reviewThumbnailBorder = Color(0xFF3A3A3A)
    val reviewThumbnailBorderMuted = Color(0xFF4A4A4A)
    val reviewThumbnailBadge = Color(0xAA000000)
    val reviewTextPrimary = Color.White
    val reviewTextSecondary = Color.LightGray
    val reviewDelete = Color.Red
    val reviewActionContainer = Color.White
    val reviewActionContent = BrandPlum

    val reviewFailedContainer = Color(0xFF351A1A)
    val reviewFailedContent = Color(0xFFFFC7C7)
    val reviewFailedBorder = Color(0xFFFF6B6B)
    val reviewProcessingContainer = Color(0xFF3A2B00)
    val reviewProcessingContent = Color(0xFFFFE08A)
    val reviewProcessingBorder = Color(0xFFFFC94D)
    val reviewEnhancedContainer = Color(0xFF1E1735)
    val reviewEnhancedContent = Color(0xFFE0D2FF)
    val reviewEnhancedBorder = BrandLavender
    val reviewOriginalContainer = Color(0xFF222222)
    val reviewOriginalContent = Color(0xFFD0D0D0)
    val reviewOriginalBorder = Color(0xFF4A4A4A)
}

object PixelScannerSpacingTokens {
    val xxs = 4.dp
    val xs = 6.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    val heroOffset = 80.dp
    val heroLogoSize = 280.dp
    val primaryButtonHeight = 56.dp
    val touchTarget = 48.dp

    val reviewThumbnailWidth = 64.dp
    val reviewThumbnailHeight = 86.dp
    val reviewThumbnailBadgeSize = 20.dp
    val reviewThumbnailBadgeInset = 6.dp
    val reviewThumbnailSpacing = 10.dp
    val reviewThumbnailBorderWidth = 2.dp
    val reviewPageStatusIndicatorSize = 36.dp
    val reviewPageStatusIndicatorStroke = 3.dp
    val reviewPageStatusChipIndicatorSize = 14.dp
    val reviewPageStatusChipIndicatorStroke = 2.dp
    val reviewStatusBorderWidth = 1.dp
    val reviewPageStatusIconSize = 24.dp
}

object PixelScannerRadiusTokens {
    val thumbnail = 12.dp
    val pill = 999.dp
}

object PixelScannerElevationTokens {
    val none = 0.dp
}

data class PixelScannerTypographyTokens(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val labelSmall: TextStyle,
)

@Composable
fun rememberPixelScannerTypographyTokens(): PixelScannerTypographyTokens {
    return PixelScannerTypographyTokens(
        display = MaterialTheme.typography.displayMedium,
        title = MaterialTheme.typography.titleMedium,
        body = MaterialTheme.typography.bodyMedium,
        label = MaterialTheme.typography.labelMedium,
        labelSmall = MaterialTheme.typography.labelSmall,
    )
}

object PixelScannerMotionTokens {
    val logoScaleIn: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )
}

object PixelScannerWindowInsetsTokens {
    @Composable
    fun safeDrawing(): WindowInsets = WindowInsets.safeDrawing
}

object PixelScannerShapeTokens {
    val thumbnail = RoundedCornerShape(PixelScannerRadiusTokens.thumbnail)
    val pill = RoundedCornerShape(PixelScannerRadiusTokens.pill)
}
