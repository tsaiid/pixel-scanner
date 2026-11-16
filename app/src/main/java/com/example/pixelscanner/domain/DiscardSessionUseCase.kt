package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSession

class DiscardSessionUseCase(
    private val sessionRecoveryRepository: SessionRecoveryRepository? = null,
) {

    suspend operator fun invoke(session: ScanSession): DiscardSessionResult {
        sessionRecoveryRepository?.discardRecoverableSession(session.id)
        return DiscardSessionResult(discardedSessionId = session.id)
    }
}

data class DiscardSessionResult(
    val discardedSessionId: String,
)
