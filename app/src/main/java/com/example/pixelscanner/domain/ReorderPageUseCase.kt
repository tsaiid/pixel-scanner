package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage

class ReorderPageUseCase {

    operator fun invoke(
        session: ScanSession,
        fromActivePageIndex: Int,
        toActivePageIndex: Int,
    ): ReorderPageResult? {
        val activePages = session.activePages()
        if (activePages.size < 2) return null

        val sourcePage = activePages.getOrNull(fromActivePageIndex) ?: return null
        val targetIndex = toActivePageIndex.coerceIn(0, activePages.lastIndex)
        if (fromActivePageIndex == targetIndex) return null

        val updatedPages = session.pages.toMutableList()
        val sourceFullIndex = updatedPages.indexOfFirst { it.id == sourcePage.id }
        if (sourceFullIndex < 0) return null
        updatedPages.removeAt(sourceFullIndex)

        val targetPage = activePages[targetIndex]
        val targetFullIndex = updatedPages.indexOfFirst { it.id == targetPage.id }
        if (targetFullIndex < 0) return null

        val insertionIndex = if (fromActivePageIndex < targetIndex) {
            targetFullIndex + 1
        } else {
            targetFullIndex
        }

        updatedPages.add(insertionIndex, sourcePage)

        return ReorderPageResult(
            updatedSession = session.withReorderedPages(updatedPages),
            movedPage = sourcePage,
        )
    }
}

data class ReorderPageResult(
    val updatedSession: ScanSession,
    val movedPage: ScannedPage,
)
