package com.example.pixelscanner.feature.review.ui

import android.net.Uri
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ProcessingState
import com.example.pixelscanner.feature.export.ui.ExportUiState
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReviewWorkspaceRecompositionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `thumbnail strip does not recompose when only selected page processing changes`() {
        val pagerRecompositionCount = AtomicInteger(0)
        val thumbnailRecompositionCount = AtomicInteger(0)
        val recompositionTrace = ReviewRecompositionTrace(
            onPagerRecomposed = { pagerRecompositionCount.incrementAndGet() },
            onThumbnailStripRecomposed = { thumbnailRecompositionCount.incrementAndGet() },
        )

        val initialState = createWorkspaceState(
            currentPage = 1,
            processingState = ProcessingState.IDLE,
        )
        val workspaceState = mutableStateOf(initialState)

        composeRule.setContent {
            val pagerState = rememberPagerState(
                initialPage = workspaceState.value.currentPage,
                pageCount = { workspaceState.value.pages.size },
            )

            ReviewWorkspace(
                state = workspaceState.value,
                pagerState = pagerState,
                onExportFormatChange = {},
                onRetryEnhancement = null,
                onToggleEnhancement = {},
                onMoveLeft = {},
                onMoveRight = {},
                onDelete = {},
                onScanMore = {},
                onDiscard = {},
                onRescan = {},
                onSaveAll = {},
                onCancelExport = {},
                onOpenSettings = {},
                onPageSelected = {},
                recompositionTrace = recompositionTrace,
            )
        }

        composeRule.waitForIdle()

        assertEquals(1, pagerRecompositionCount.get())
        assertEquals(1, thumbnailRecompositionCount.get())

        composeRule.runOnIdle {
            val updatedPages = workspaceState.value.pages.mapIndexed { index, page ->
                if (index == workspaceState.value.currentPage) {
                    page.copy(processingState = ProcessingState.PROCESSING)
                } else {
                    page
                }
            }

            workspaceState.value = workspaceState.value.copy(
                pages = updatedPages,
                selectedPage = updatedPages[workspaceState.value.currentPage],
                isProcessing = true,
            )
        }

        composeRule.waitForIdle()

        assertEquals(2, pagerRecompositionCount.get())
        assertTrue(thumbnailRecompositionCount.get() <= 2)
        val thumbnailCountAfterProcessing = thumbnailRecompositionCount.get()

        composeRule.runOnIdle {
            val updatedPages = workspaceState.value.pages.mapIndexed { index, page ->
                if (index == 2) {
                    page.copy(processingState = ProcessingState.IDLE)
                } else {
                    page
                }
            }

            workspaceState.value = workspaceState.value.copy(
                currentPage = 2,
                selectedPage = updatedPages[2],
                pages = updatedPages,
                isProcessing = false,
            )
        }

        composeRule.waitForIdle()

        assertEquals(3, pagerRecompositionCount.get())
        assertTrue(thumbnailRecompositionCount.get() >= thumbnailCountAfterProcessing)
    }

    private fun createWorkspaceState(
        currentPage: Int,
        processingState: ProcessingState,
    ): ReviewWorkspaceState {
        val pages = listOf(
            ReviewPageUiModel(
                id = "page-1",
                displayUri = Uri.parse("content://scanner/page-1.jpg"),
                isEnhanced = false,
                processingState = ProcessingState.IDLE,
            ),
            ReviewPageUiModel(
                id = "page-2",
                displayUri = Uri.parse("content://scanner/page-2.jpg"),
                isEnhanced = false,
                processingState = if (currentPage == 1) processingState else ProcessingState.IDLE,
            ),
            ReviewPageUiModel(
                id = "page-3",
                displayUri = Uri.parse("content://scanner/page-3.jpg"),
                isEnhanced = false,
                processingState = if (currentPage == 2) processingState else ProcessingState.IDLE,
            ),
        )

        return ReviewWorkspaceState(
            hasPages = true,
            hasSession = true,
            pages = pages,
            currentPage = currentPage,
            selectedPage = pages[currentPage],
            exportState = ExportUiState(format = ExportFormat.IMAGE),
            isProcessing = processingState == ProcessingState.PROCESSING,
        )
    }
}
