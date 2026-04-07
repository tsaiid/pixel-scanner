package com.example.pixelscanner.domain

import android.content.Context
import com.example.pixelscanner.core.model.ExportFormat
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

        val timestamp = System.currentTimeMillis()
        val urisToSave = pagesToExport.map { it.displayUri() }

        return when (request.format) {
            ExportFormat.PDF -> {
                val outputUri = when (val decision = decidePdfExport(session)) {
                    is PdfExportDecision.ReuseOriginalPdf -> {
                        exportRepository.copyPdfToDocuments(
                            context = context,
                            sourceUri = decision.originalPdfUri,
                            fileName = "PixelScanner_$timestamp.pdf",
                        )
                    }

                    PdfExportDecision.RebuildPdf -> {
                        exportRepository.saveUrisToPdf(
                            context = context,
                            sourceUris = urisToSave,
                            fileName = "PixelScanner_$timestamp.pdf",
                        )
                    }
                }
                if (outputUri != null) {
                    ExportResult.Success(outputUri)
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

                if (savedUris.all { it != null }) {
                    ExportResult.Success(null)
                } else {
                    ExportResult.Failure("Failed to export one or more images")
                }
            }
        }
    }

    private fun decidePdfExport(session: ScanSession): PdfExportDecision {
        val originalPdfUri = session.originalPdfUri
        return if (originalPdfUri != null && !session.dirtyFlags.isDirty()) {
            PdfExportDecision.ReuseOriginalPdf(originalPdfUri)
        } else {
            PdfExportDecision.RebuildPdf
        }
    }
}

private sealed interface PdfExportDecision {
    data class ReuseOriginalPdf(val originalPdfUri: android.net.Uri) : PdfExportDecision
    data object RebuildPdf : PdfExportDecision
}
