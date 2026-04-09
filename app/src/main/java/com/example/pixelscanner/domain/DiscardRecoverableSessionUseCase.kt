package com.example.pixelscanner.domain

class DiscardRecoverableSessionUseCase(
    private val sessionRecoveryRepository: SessionRecoveryRepository,
) {

    suspend operator fun invoke(sessionId: String) {
        sessionRecoveryRepository.discardRecoverableSession(sessionId)
    }
}
