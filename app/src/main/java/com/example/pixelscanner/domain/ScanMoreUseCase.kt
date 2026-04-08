package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSession

data class ScanMoreResult(
    val session: ScanSession,
    val preservesCurrentPages: Boolean = true,
)

class ScanMoreUseCase {

    operator fun invoke(session: ScanSession): ScanMoreResult {
        return ScanMoreResult(session = session)
    }
}
