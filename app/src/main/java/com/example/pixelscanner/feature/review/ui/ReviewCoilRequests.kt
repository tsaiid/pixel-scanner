package com.example.pixelscanner.feature.review.ui

import android.content.Context
import androidx.compose.ui.unit.IntSize
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale

internal fun buildReviewPreviewImageRequest(
    context: Context,
    data: Any,
    viewportSize: IntSize,
): ImageRequest {
    return ImageRequest.Builder(context)
        .data(data)
        .size(viewportSize.width, viewportSize.height)
        .scale(Scale.FIT)
        .precision(Precision.EXACT)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
}

internal fun buildReviewThumbnailImageRequest(
    context: Context,
    data: Any,
    thumbnailSize: IntSize,
): ImageRequest {
    return ImageRequest.Builder(context)
        .data(data)
        .size(thumbnailSize.width, thumbnailSize.height)
        .scale(Scale.FILL)
        .precision(Precision.EXACT)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
}
