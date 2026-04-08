package com.example.pixelscanner.domain

import android.content.Context
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.ExportImagePipelineResult
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.ScanSession

class ExportSessionUseCase(
    private val exportRepository: ExportRepository,
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

        val pipelineRequest = ExportImagePipelineRequest(
            sessionId = session.id,
            format = request.format,
            sourceUris = pagesToExport.map { it.displayUri() },
            originalPdfUri = session.originalPdfExportSource(),
            dirtyFlags = session.dirtyFlags,
        )
        val timestamp = System.currentTimeMillis()
        val urisToSave = pipelineRequest.sourceUris

        return when (request.format) {
            ExportFormat.PDF -> {
                val outputUri = if (pipelineRequest.canReuseOriginalPdf()) {
                    exportRepository.copyPdfToDocuments(
                        context = context,
                        sourceUri = requireNotNull(pipelineRequest.originalPdfUri),
                        fileName = "PixelScanner_$timestamp.pdf",
                    )
                } else {
                    exportRepository.saveUrisToPdf(
                        context = context,
                        sourceUris = urisToSave,
                        fileName = "PixelScanner_$timestamp.pdf",
                    )
                }

                val pipelineResult = ExportImagePipelineResult(
                    cacheKey = pipelineRequest.cacheKey,
                    outputUri = outputUri,
                    reusedOriginalPdf = pipelineRequest.canReuseOriginalPdf() && outputUri != null,
                )

                if (pipelineResult.outputUri != null) {
                    ExportResult.Success(pipelineResult.outputUri)
                } else {
                    ExportResult.Failure("Failed to export PDF")
                }
            }

            ExportFormat.IMAGE -> {
                val savedUris = urisToSave.mapIndexed { index, uri ->
                    exportRepository.saveUriToGallery(
                        context = context,
                        sourceUri = uri,
                        fileName = "PixelScanner_${timestamp}_$index.jpg",
                    )
                }

                val pipelineResult = ExportImagePipelineResult(
                    cacheKey = pipelineRequest.cacheKey,
                    outputUri = null,
                    reusedOriginalPdf = false,
                )

                if (savedUris.all { it != null }) {
                    ExportResult.Success(pipelineResult.outputUri)
                } else {
                    ExportResult.Failure("Failed to export one or more images")
                }
            }
        }
    }

}
