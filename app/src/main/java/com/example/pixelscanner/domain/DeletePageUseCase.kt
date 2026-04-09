package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage

class DeletePageUseCase {

    operator fun invoke(
        session: ScanSession,
        activePageIndex: Int,
    ): DeletePageResult? {
        val pageToDelete = session.activePages().getOrNull(activePageIndex) ?: return null
        val updatedPages = session.pages.map { page ->
            if (page.id == pageToDelete.id) {
                page.copy(isDeleted = true)
            } else {
                page
            }
        }

        return DeletePageResult(
            updatedSession = session.withPages(updatedPages),
            deletedPage = pageToDelete,
        )
    }
}

data class DeletePageResult(
    val updatedSession: ScanSession,
    val deletedPage: ScannedPage,
)
