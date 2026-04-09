package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanResultPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class CreateSessionFromScanResultUseCaseTest {

    @Test
    fun `creates a session from scan result with stable page mapping`() {
        val useCase = CreateSessionFromScanResultUseCase()
        val scanResult = ScanResultPayload(pageUris = emptyList())

        val session = useCase(scanResult)

        assertTrue(session.id.startsWith("session_"))
        assertNull(session.originalPdfUri)
        assertEquals(0, session.pages.size)
        assertFalse(session.isDirty())
    }
}
