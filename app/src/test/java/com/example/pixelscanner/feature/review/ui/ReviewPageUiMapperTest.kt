package com.example.pixelscanner.feature.review.ui

import android.net.Uri
import com.example.pixelscanner.core.model.PageVariant
import com.example.pixelscanner.core.model.ProcessingState
import com.example.pixelscanner.core.model.ScannedPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReviewPageUiMapperTest {

    @Test
    fun `maps original scanned page to review page ui model`() {
        val page = ScannedPage(
            id = "page-1",
            originalSourceUri = Uri.parse("content://scanner/page-1.jpg"),
        )

        val uiModel = ReviewPageUiMapper.map(page)

        assertEquals("page-1", uiModel.id)
        assertEquals(Uri.parse("content://scanner/page-1.jpg"), uiModel.displayUri)
        assertFalse(uiModel.isEnhanced)
        assertEquals(ProcessingState.IDLE, uiModel.processingState)
    }

    @Test
    fun `maps enhanced scanned page to review page ui model`() {
        val page = ScannedPage(
            id = "page-2",
            originalSourceUri = Uri.parse("content://scanner/page-2.jpg"),
            enhancedUri = Uri.parse("content://scanner/page-2-enhanced.jpg"),
            activeVariant = PageVariant.PREVIEW_ENHANCED,
            processingState = ProcessingState.PROCESSING,
        )

        val uiModel = ReviewPageUiMapper.map(page)

        assertEquals(Uri.parse("content://scanner/page-2-enhanced.jpg"), uiModel.displayUri)
        assertTrue(uiModel.isEnhanced)
        assertEquals(ProcessingState.PROCESSING, uiModel.processingState)
    }
}
