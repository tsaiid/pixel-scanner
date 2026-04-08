package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.domain.SessionIngestionResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EagerSessionIngestionUseCase(
    private val sessionIngestionRepository: SessionIngestionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend operator fun invoke(
        session: ScanSession,
        scanResult: ScanResultPayload,
    ): SessionIngestionResult {
        return withContext(ioDispatcher) {
            sessionIngestionRepository.ingest(session, scanResult)
        }
    }
}
