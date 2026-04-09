package com.example.pixelscanner.feature.home.ui

import com.example.pixelscanner.domain.RecoverableSessionSnapshot

object RecoverableSessionUiMapper {
    fun map(snapshot: RecoverableSessionSnapshot): RecoverableSessionUiState {
        return RecoverableSessionUiState(
            sessionId = snapshot.metadata.sessionId,
            pageCount = snapshot.metadata.pageCount,
            hasMissingAssets = snapshot.isPartial,
            missingAssetCount = snapshot.missingPageFileNames.size +
                if (snapshot.missingOriginalPdf) 1 else 0,
        )
    }
}
