package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import java.util.UUID

class CreateSessionFromScanResultUseCase {

    operator fun invoke(scanResult: ScanResultPayload): ScanSession {
        val sessionId = "session_${UUID.randomUUID()}"
        val pages = scanResult.pageUris.mapIndexed { index, uri ->
            ScannedPage(
                id = "${sessionId}_page_$index",
                originalSourceUri = uri,
            )
        }

        return ScanSession(
            id = sessionId,
            pages = pages,
            originalPdfUri = scanResult.pdfUri,
        )
    }
}
