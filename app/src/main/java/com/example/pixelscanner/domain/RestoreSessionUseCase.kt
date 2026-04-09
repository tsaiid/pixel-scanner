package com.example.pixelscanner.domain

import android.net.Uri
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import java.io.File

data class RestoreSessionResult(
    val session: ScanSession,
    val metadata: com.example.pixelscanner.core.model.ScanSessionMetadata,
    val isPartial: Boolean,
    val missingAssets: List<String> = emptyList(),
) {
    val hasMissingAssets: Boolean
        get() = missingAssets.isNotEmpty()
}

sealed interface RestoreSessionOutcome {
    data class Restored(val result: RestoreSessionResult) : RestoreSessionOutcome
    data object Unavailable : RestoreSessionOutcome
}

class RestoreSessionUseCase(
    private val sessionRecoveryRepository: SessionRecoveryRepository,
) {

    suspend operator fun invoke(sessionId: String? = null): RestoreSessionOutcome {
        val snapshot = if (sessionId == null) {
            sessionRecoveryRepository.listRecoverableSessions().firstOrNull()
        } else {
            sessionRecoveryRepository.loadRecoverableSession(sessionId)
        } ?: return RestoreSessionOutcome.Unavailable

        val pages = snapshot.pageFiles.mapIndexed { index, file ->
            ScannedPage(
                id = "${snapshot.metadata.sessionId}_page_$index",
                originalSourceUri = Uri.fromFile(file),
            )
        }

        val originalPdfUri = snapshot.originalPdfFile
            ?.takeIf(File::exists)
            ?.let(Uri::fromFile)

        if (pages.isEmpty() && originalPdfUri == null) {
            return RestoreSessionOutcome.Unavailable
        }

        val session = ScanSession(
            id = snapshot.metadata.sessionId,
            pages = pages,
            originalPdfUri = originalPdfUri,
            dirtyFlags = snapshot.metadata.dirtyFlags,
        )
        val missingAssets = buildList {
            addAll(snapshot.missingPageFileNames)
            if (snapshot.missingOriginalPdf) {
                add("original.pdf")
            }
        }

        return RestoreSessionOutcome.Restored(
            result = RestoreSessionResult(
                session = session,
                metadata = snapshot.metadata,
                isPartial = snapshot.isPartial,
                missingAssets = missingAssets,
            ),
        )
    }
}
