package com.example.pixelscanner.core.storage

import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.domain.SessionIngestionRepository

class AndroidSessionIngestionRepository : SessionIngestionRepository {
    override suspend fun ingest(session: ScanSession, scanResult: ScanResultPayload) {
        // Eager ingestion is wired through this boundary first.
        // File copying will be added in the next slice.
    }
}
