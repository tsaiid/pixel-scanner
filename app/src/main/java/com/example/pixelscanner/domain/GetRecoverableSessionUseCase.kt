package com.example.pixelscanner.domain

class GetRecoverableSessionUseCase(
    private val sessionRecoveryRepository: SessionRecoveryRepository,
) {

    suspend operator fun invoke(): RecoverableSessionSnapshot? {
        return sessionRecoveryRepository.listRecoverableSessions().firstOrNull()
    }
}
