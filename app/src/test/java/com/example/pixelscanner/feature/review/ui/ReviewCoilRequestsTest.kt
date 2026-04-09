package com.example.pixelscanner.feature.review.ui

import android.net.Uri
import androidx.compose.ui.unit.IntSize
import coil.request.CachePolicy
import coil.size.Precision
import coil.size.Scale
import coil.size.pxOrElse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReviewCoilRequestsTest {

    @Test
    fun `preview request targets the viewport and keeps caches enabled`() = runBlocking {
        val request = buildReviewPreviewImageRequest(
            context = RuntimeEnvironment.getApplication(),
            data = Uri.parse("content://scanner/page-1.jpg"),
            viewportSize = IntSize(1440, 1920),
        )

        val size = request.sizeResolver.size()

        assertEquals(1440, size.width.pxOrElse { -1 })
        assertEquals(1920, size.height.pxOrElse { -1 })
        assertEquals(Scale.FIT, request.scale)
        assertEquals(Precision.EXACT, request.precision)
        assertEquals(CachePolicy.ENABLED, request.memoryCachePolicy)
        assertEquals(CachePolicy.ENABLED, request.diskCachePolicy)
        assertTrue(request.memoryCachePolicy.readEnabled)
        assertTrue(request.memoryCachePolicy.writeEnabled)
        assertTrue(request.diskCachePolicy.readEnabled)
        assertTrue(request.diskCachePolicy.writeEnabled)
    }

    @Test
    fun `thumbnail request targets thumbnail bounds and keeps caches enabled`() = runBlocking {
        val request = buildReviewThumbnailImageRequest(
            context = RuntimeEnvironment.getApplication(),
            data = Uri.parse("content://scanner/page-1.jpg"),
            thumbnailSize = IntSize(160, 216),
        )

        val size = request.sizeResolver.size()

        assertEquals(160, size.width.pxOrElse { -1 })
        assertEquals(216, size.height.pxOrElse { -1 })
        assertEquals(Scale.FILL, request.scale)
        assertEquals(Precision.EXACT, request.precision)
        assertEquals(CachePolicy.ENABLED, request.memoryCachePolicy)
        assertEquals(CachePolicy.ENABLED, request.diskCachePolicy)
        assertTrue(request.memoryCachePolicy.readEnabled)
        assertTrue(request.memoryCachePolicy.writeEnabled)
        assertTrue(request.diskCachePolicy.readEnabled)
        assertTrue(request.diskCachePolicy.writeEnabled)
    }
}
