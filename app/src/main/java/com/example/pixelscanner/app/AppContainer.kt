package com.example.pixelscanner.app

import android.content.Context
import androidx.savedstate.SavedStateRegistryOwner
import com.example.pixelscanner.core.image.AndroidImageProcessingRepository
import com.example.pixelscanner.core.storage.AndroidExportRepository
import com.example.pixelscanner.core.storage.AndroidSessionCleanupRepository
import com.example.pixelscanner.core.storage.AndroidSessionIngestionRepository
import com.example.pixelscanner.core.storage.AndroidSessionMetadataRepository
import com.example.pixelscanner.core.storage.AndroidSessionRecoveryRepository
import com.example.pixelscanner.domain.CreateSessionFromScanResultUseCase
import com.example.pixelscanner.domain.EagerSessionIngestionUseCase
import com.example.pixelscanner.domain.DeletePageUseCase
import com.example.pixelscanner.domain.DiscardSessionUseCase
import com.example.pixelscanner.domain.ExportSessionUseCase
import com.example.pixelscanner.domain.ResumeSessionUseCase
import com.example.pixelscanner.domain.RestorePageUseCase
import com.example.pixelscanner.domain.RestoreSessionUseCase
import com.example.pixelscanner.domain.ScanMoreUseCase
import com.example.pixelscanner.domain.SessionCleanupRepository
import com.example.pixelscanner.domain.SessionRecoveryRepository
import com.example.pixelscanner.domain.ToggleEnhancementUseCase
import com.example.pixelscanner.feature.home.HomeViewModelFactory
import com.example.pixelscanner.feature.review.ReviewViewModelFactory

class AppContainer(
    applicationContext: Context,
) {
    val imageProcessingRepository = AndroidImageProcessingRepository()
    val exportRepository = AndroidExportRepository()
    val sessionMetadataRepository = AndroidSessionMetadataRepository(applicationContext)
    val sessionCleanupRepository: SessionCleanupRepository = AndroidSessionCleanupRepository(
        context = applicationContext,
        sessionMetadataRepository = sessionMetadataRepository,
    )
    val sessionRecoveryRepository: SessionRecoveryRepository = AndroidSessionRecoveryRepository(
        context = applicationContext,
        sessionMetadataRepository = sessionMetadataRepository,
    )
    val sessionIngestionRepository = AndroidSessionIngestionRepository(
        context = applicationContext,
        sessionMetadataRepository = sessionMetadataRepository,
    )
    val createSessionFromScanResultUseCase = CreateSessionFromScanResultUseCase()
    val eagerSessionIngestionUseCase = EagerSessionIngestionUseCase(sessionIngestionRepository)
    val toggleEnhancementUseCase = ToggleEnhancementUseCase(imageProcessingRepository)
    val deletePageUseCase = DeletePageUseCase()
    val discardSessionUseCase = DiscardSessionUseCase()
    val resumeSessionUseCase = ResumeSessionUseCase()
    val scanMoreUseCase = ScanMoreUseCase(discardSessionUseCase)
    val restoreSessionUseCase = RestoreSessionUseCase(sessionRecoveryRepository)
    val restorePageUseCase = RestorePageUseCase()
    val exportSessionUseCase = ExportSessionUseCase(exportRepository)

    fun createReviewViewModelFactory(owner: SavedStateRegistryOwner): ReviewViewModelFactory {
        return ReviewViewModelFactory(
            createSessionFromScanResultUseCase = createSessionFromScanResultUseCase,
            eagerSessionIngestionUseCase = eagerSessionIngestionUseCase,
            toggleEnhancementUseCase = toggleEnhancementUseCase,
            deletePageUseCase = deletePageUseCase,
            discardSessionUseCase = discardSessionUseCase,
            resumeSessionUseCase = resumeSessionUseCase,
            scanMoreUseCase = scanMoreUseCase,
            restorePageUseCase = restorePageUseCase,
            exportSessionUseCase = exportSessionUseCase,
            owner = owner,
        )
    }

    fun createHomeViewModelFactory(owner: SavedStateRegistryOwner): HomeViewModelFactory {
        return HomeViewModelFactory(owner = owner)
    }
}
