package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSession

data class ScanMoreResult(
    val discardedSessionId: String,
)

class ScanMoreUseCase(
    private val discardSessionUseCase: DiscardSessionUseCase,
) {

    operator fun invoke(session: ScanSession): ScanMoreResult {
        val discardResult = discardSessionUseCase(session)
        return ScanMoreResult(discardedSessionId = discardResult.discardedSessionId)
    }
}
