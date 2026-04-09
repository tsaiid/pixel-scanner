package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage

class RestorePageUseCase {

    operator fun invoke(
        session: ScanSession,
        page: ScannedPage,
    ): ScanSession {
        val updatedPages = session.pages.map { existingPage ->
            if (existingPage.id == page.id) {
                existingPage.copy(isDeleted = false)
            } else {
                existingPage
            }
        }

        return session.withPages(updatedPages)
    }
}
