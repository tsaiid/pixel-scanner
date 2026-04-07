package com.example.pixelscanner.app

import com.example.pixelscanner.domain.CreateSessionFromScanResultUseCase
import com.example.pixelscanner.domain.DeletePageUseCase
import com.example.pixelscanner.domain.DiscardSessionUseCase
import com.example.pixelscanner.domain.ExportSessionUseCase
import com.example.pixelscanner.domain.RestorePageUseCase
import com.example.pixelscanner.domain.ToggleEnhancementUseCase
import com.example.pixelscanner.feature.review.ReviewViewModelFactory

class AppContainer {
    val createSessionFromScanResultUseCase = CreateSessionFromScanResultUseCase()
    val toggleEnhancementUseCase = ToggleEnhancementUseCase()
    val deletePageUseCase = DeletePageUseCase()
    val discardSessionUseCase = DiscardSessionUseCase()
    val restorePageUseCase = RestorePageUseCase()
    val exportSessionUseCase = ExportSessionUseCase()

    val reviewViewModelFactory = ReviewViewModelFactory(
        createSessionFromScanResultUseCase = createSessionFromScanResultUseCase,
        toggleEnhancementUseCase = toggleEnhancementUseCase,
        deletePageUseCase = deletePageUseCase,
        discardSessionUseCase = discardSessionUseCase,
        restorePageUseCase = restorePageUseCase,
        exportSessionUseCase = exportSessionUseCase,
    )
}
