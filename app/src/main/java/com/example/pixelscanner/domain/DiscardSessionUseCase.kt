package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSession

class DiscardSessionUseCase {

    operator fun invoke(session: ScanSession): DiscardSessionResult {
        return DiscardSessionResult(discardedSessionId = session.id)
    }
}

data class DiscardSessionResult(
    val discardedSessionId: String,
)
