package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSession

data class ResumeSessionResult(
    val session: ScanSession,
)

class ResumeSessionUseCase {

    operator fun invoke(session: ScanSession): ResumeSessionResult {
        return ResumeSessionResult(session = session)
    }
}
