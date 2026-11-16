package com.example.pixelscanner.feature.home.ui

import com.example.pixelscanner.domain.RecoverableSessionSnapshot

object RecoverableSessionUiMapper {
    fun map(snapshot: RecoverableSessionSnapshot): RecoverableSessionUiState {
        val activePageCount = if (snapshot.metadata.pages.isNotEmpty()) {
            snapshot.metadata.pages.count { !it.isDeleted }
        } else {
            snapshot.metadata.pageCount
        }
        return RecoverableSessionUiState(
            sessionId = snapshot.metadata.sessionId,
            pageCount = activePageCount,
            hasMissingAssets = snapshot.isPartial,
            missingAssetCount = snapshot.missingPageFileNames.size +
                if (snapshot.missingOriginalPdf) 1 else 0,
        )
    }
}
