package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage

class CreateSessionFromScanResultUseCase {
    private val sessionIdGenerator = SessionIdGenerator()

    operator fun invoke(scanResult: ScanResultPayload): ScanSession {
        val sessionId = sessionIdGenerator.generate()
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
