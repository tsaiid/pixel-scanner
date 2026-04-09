package com.example.pixelscanner.feature.home.ui

import com.example.pixelscanner.core.model.ScanSessionMetadata
import com.example.pixelscanner.domain.RecoverableSessionSnapshot
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoverableSessionUiMapperTest {

    @Test
    fun `maps recoverable session snapshot to ui state`() {
        val snapshot = RecoverableSessionSnapshot(
            metadata = ScanSessionMetadata(
                sessionId = "session-home-mapping",
                createdAtEpochMillis = 123L,
                pageCount = 3,
                hasOriginalPdf = true,
                dirtyFlags = com.example.pixelscanner.core.model.SessionDirtyFlags(),
            ),
            pageFiles = listOf(File("page-1.jpg"), File("page-2.jpg"), File("page-3.jpg")),
            originalPdfFile = File("original.pdf"),
            missingPageFileNames = listOf("page-2.jpg"),
            missingOriginalPdf = true,
        )

        val uiState = RecoverableSessionUiMapper.map(snapshot)

        assertEquals("session-home-mapping", uiState.sessionId)
        assertEquals(3, uiState.pageCount)
        assertTrue(uiState.hasMissingAssets)
        assertEquals(2, uiState.missingAssetCount)
    }

    @Test
    fun `maps complete recoverable session snapshot without missing assets`() {
        val snapshot = RecoverableSessionSnapshot(
            metadata = ScanSessionMetadata(
                sessionId = "session-home-complete",
                createdAtEpochMillis = 456L,
                pageCount = 1,
                hasOriginalPdf = false,
                dirtyFlags = com.example.pixelscanner.core.model.SessionDirtyFlags(),
            ),
            pageFiles = listOf(File("page-1.jpg")),
            originalPdfFile = null,
        )

        val uiState = RecoverableSessionUiMapper.map(snapshot)

        assertFalse(uiState.hasMissingAssets)
        assertEquals(0, uiState.missingAssetCount)
    }
}
