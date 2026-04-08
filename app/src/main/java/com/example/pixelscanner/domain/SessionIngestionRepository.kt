package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession

interface SessionIngestionRepository {
    suspend fun ingest(
        session: ScanSession,
        scanResult: ScanResultPayload,
    )
}
