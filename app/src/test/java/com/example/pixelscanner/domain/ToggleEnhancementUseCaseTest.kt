package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.PageVariant
import com.example.pixelscanner.core.model.PipelineCacheKey
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.core.model.PreviewImagePipelineResult
import com.example.pixelscanner.core.model.ScannedPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ToggleEnhancementUseCaseTest {

    @Test
    fun `uses cached enhanced uri without invoking repository`() {
        val imageProcessingRepository = RecordingImageProcessingRepository(
            result = PreviewImagePipelineResult(
                cacheKey = PipelineCacheKey("unused"),
                outputUri = Uri.parse("content://scanner/unused-enhanced.jpg"),
            ),
        )
        val useCase = ToggleEnhancementUseCase(imageProcessingRepository)
        val cachedEnhancedUri = Uri.parse("content://scanner/page-0-enhanced.jpg")
        val page = ScannedPage(
            id = "session_toggle_use_case_page_0",
            originalSourceUri = Uri.parse("content://scanner/page-0.jpg"),
            enhancedUri = cachedEnhancedUri,
            activeVariant = PageVariant.ORIGINAL,
        )

        val result = invokeUseCase(useCase, page)

        assertEquals(0, imageProcessingRepository.invocationCount)
        assertTrue(result.enabled)
        assertEquals(PageVariant.PREVIEW_ENHANCED, result.updatedPage.activeVariant)
        assertEquals(cachedEnhancedUri, result.updatedPage.enhancedUri)
        assertNull(result.previewCacheKey)
    }

    @Test
    fun `invokes repository and returns preview cache key when enhanced uri is missing`() {
        val expectedCacheKey = PipelineCacheKey("preview-cache-hit")
        val expectedEnhancedUri = Uri.parse("content://scanner/page-1-enhanced.jpg")
        val imageProcessingRepository = RecordingImageProcessingRepository(
            result = PreviewImagePipelineResult(
                cacheKey = expectedCacheKey,
                outputUri = expectedEnhancedUri,
            ),
        )
        val useCase = ToggleEnhancementUseCase(imageProcessingRepository)
        val page = ScannedPage(
            id = "session_toggle_use_case_page_1",
            originalSourceUri = Uri.parse("content://scanner/page-1.jpg"),
        )

        val result = invokeUseCase(useCase, page)

        assertEquals(1, imageProcessingRepository.invocationCount)
        assertEquals(page.id, imageProcessingRepository.lastRequest?.pageId)
        assertEquals(page.originalSourceUri, imageProcessingRepository.lastRequest?.sourceUri)
        assertTrue(result.enabled)
        assertEquals(PageVariant.PREVIEW_ENHANCED, result.updatedPage.activeVariant)
        assertEquals(expectedEnhancedUri, result.updatedPage.enhancedUri)
        assertEquals(expectedCacheKey, result.previewCacheKey)
    }

    private fun invokeUseCase(
        useCase: ToggleEnhancementUseCase,
        page: ScannedPage,
    ): ToggleEnhancementResult {
        return kotlinx.coroutines.runBlocking {
            useCase(
                page = page,
                context = RuntimeEnvironment.getApplication(),
            )
        }
    }

    private class RecordingImageProcessingRepository(
        private val result: PreviewImagePipelineResult,
    ) : ImageProcessingRepository {
        var invocationCount: Int = 0
            private set
        var lastRequest: PreviewImagePipelineRequest? = null
            private set

        override suspend fun enhance(
            context: Context,
            request: PreviewImagePipelineRequest,
        ): PreviewImagePipelineResult {
            invocationCount += 1
            lastRequest = request
            return result
        }
    }
}
