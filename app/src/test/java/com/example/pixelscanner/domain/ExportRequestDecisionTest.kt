package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ExportDecision
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.SessionDirtyFlags
import com.example.pixelscanner.core.model.resolveDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportRequestDecisionTest {

    @Test
    fun `reuses original pdf when session is clean and pdf exists`() {
        assertEquals(
            ExportDecision.ReuseOriginalPdf,
            ExportRequest(sessionId = "session_test", format = ExportFormat.PDF).resolveDecision(
                dirtyFlags = SessionDirtyFlags(),
                hasOriginalPdf = true,
            ),
        )
    }

    @Test
    fun `rebuilds pdf from enhanced pages when enhancement is dirty`() {
        assertEquals(
            ExportDecision.RebuildPdfFromEnhancedPages,
            ExportRequest(sessionId = "session_test", format = ExportFormat.PDF).resolveDecision(
                dirtyFlags = SessionDirtyFlags(hasEnhancedPages = true),
                hasOriginalPdf = true,
            ),
        )
    }

    @Test
    fun `rebuilds pdf from pages when delete reorder or rotate is dirty`() {
        listOf(
            SessionDirtyFlags(hasDeletedPages = true),
            SessionDirtyFlags(hasReorderedPages = true),
            SessionDirtyFlags(hasRotatedPages = true),
            SessionDirtyFlags(hasCroppedPages = true),
        ).forEach { dirtyFlags ->
            assertEquals(
                ExportDecision.RebuildPdfFromPages,
                ExportRequest(sessionId = "session_test", format = ExportFormat.PDF).resolveDecision(
                    dirtyFlags = dirtyFlags,
                    hasOriginalPdf = true,
                ),
            )
        }
    }

    @Test
    fun `exports jpegs when requested format is image`() {
        assertEquals(
            ExportDecision.ExportJpegs,
            ExportRequest(sessionId = "session_test", format = ExportFormat.IMAGE).resolveDecision(
                dirtyFlags = SessionDirtyFlags(),
                hasOriginalPdf = true,
            ),
        )
    }
}
