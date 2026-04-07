package com.example.pixelscanner.domain

import android.content.Context
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.storage.StorageManager

class ExportSessionUseCase(
    private val storageManagerFactory: (Context) -> StorageManager = ::StorageManager,
) {

    suspend operator fun invoke(
        session: ScanSession,
        request: ExportRequest,
        context: Context,
    ): ExportResult {
        val pagesToExport = session.activePages()
        if (pagesToExport.isEmpty()) {
            return ExportResult.Failure("No pages to export")
        }

        val storageManager = storageManagerFactory(context)
        val timestamp = System.currentTimeMillis()
        val urisToSave = pagesToExport.map { it.displayUri() }

        return when (request.format) {
            ExportFormat.PDF -> {
                val outputUri = storageManager.saveUrisToPdf(
                    sourceUris = urisToSave,
                    fileName = "PixelScanner_$timestamp.pdf",
                )
                if (outputUri != null) {
                    ExportResult.Success(outputUri)
                } else {
                    ExportResult.Failure("Failed to export PDF")
                }
            }

            ExportFormat.IMAGE -> {
                val savedUris = urisToSave.mapIndexed { index, uri ->
                    storageManager.saveUriToGallery(
                        sourceUri = uri,
                        fileName = "PixelScanner_${timestamp}_$index.jpg",
                    )
                }

                if (savedUris.all { it != null }) {
                    ExportResult.Success(null)
                } else {
                    ExportResult.Failure("Failed to export one or more images")
                }
            }
        }
    }
}
