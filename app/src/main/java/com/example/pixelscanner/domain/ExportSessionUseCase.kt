package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportDecision
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.ExportImagePipelineResult
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.resolveDecision
import com.example.pixelscanner.core.model.ScanSession

class ExportSessionUseCase(
    private val exportRepository: ExportRepository,
    private val exportArtifactCacheRepository: ExportArtifactCacheRepository,
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
        val exportDecision = request.resolveDecision(session)
        val timestamp = System.currentTimeMillis()
        val urisToSave = pipelineRequest.sourceUris

        return when (request.format) {
            ExportFormat.PDF -> {
                val cachedArtifact = exportArtifactCacheRepository.getCachedPdfArtifact(
                    context = context,
                    request = pipelineRequest,
                )
                val cachedSourceUri = cachedArtifact?.let(Uri::fromFile)

                val outputUri = when (exportDecision) {
                    ExportDecision.ReuseOriginalPdf -> {
                        val sourceUri = cachedSourceUri ?: requireNotNull(pipelineRequest.originalPdfUri)
                        exportRepository.copyPdfToDocuments(
                            context = context,
                            sourceUri = sourceUri,
                            fileName = "PixelScanner_$timestamp.pdf",
                        )
                    }

                    ExportDecision.RebuildPdfFromEnhancedPages,
                    ExportDecision.RebuildPdfFromPages -> {
                        val sourceUri = cachedSourceUri
                        if (sourceUri != null) {
                            exportRepository.copyPdfToDocuments(
                                context = context,
                                sourceUri = sourceUri,
                                fileName = "PixelScanner_$timestamp.pdf",
                            )
                        } else {
                            exportRepository.saveUrisToPdf(
                                context = context,
                                sourceUris = urisToSave,
                                fileName = "PixelScanner_$timestamp.pdf",
                            )
                        }
                    }

                    ExportDecision.ExportJpegs -> {
                        exportRepository.saveUrisToPdf(
                            context = context,
                            sourceUris = urisToSave,
                            fileName = "PixelScanner_$timestamp.pdf",
                        )
                    }
                }

                val pipelineResult = ExportImagePipelineResult(
                    cacheKey = pipelineRequest.cacheKey,
                    outputUri = outputUri,
                    reusedOriginalPdf = exportDecision == ExportDecision.ReuseOriginalPdf && outputUri != null,
                )
                if (pipelineResult.outputUri != null) {
                    exportArtifactCacheRepository.storePdfArtifact(
                        context = context,
                        request = pipelineRequest,
                        sourceUri = pipelineResult.outputUri,
                    )
                }

                if (pipelineResult.outputUri != null) {
                    ExportResult.Success(pipelineResult.outputUri)
                } else {
                    ExportResult.Failure("Failed to export PDF")
                }
            }

            ExportFormat.IMAGE -> {
                val cachedArtifacts = exportArtifactCacheRepository.getCachedImageArtifacts(
                    context = context,
                    request = pipelineRequest,
                    pageCount = urisToSave.size,
                )
                val sourceUrisForExport = cachedArtifacts?.map(Uri::fromFile) ?: urisToSave

                val savedUris = sourceUrisForExport.mapIndexed { index, uri ->
                    exportRepository.saveUriToGallery(
                        context = context,
                        sourceUri = uri,
                        fileName = "PixelScanner_${timestamp}_$index.jpg",
                    )?.also { exportedUri ->
                        exportArtifactCacheRepository.storeImageArtifact(
                            context = context,
                            request = pipelineRequest,
                            pageIndex = index,
                            sourceUri = exportedUri,
                        )
                    }
                }

                val pipelineResult = ExportImagePipelineResult(
                    cacheKey = pipelineRequest.cacheKey,
                    outputUri = null,
                    reusedOriginalPdf = false,
                )

                val successfulUris = savedUris.filterNotNull()
                when {
                    successfulUris.isEmpty() -> {
                        ExportResult.Failure("Failed to export one or more images")
                    }

                    successfulUris.size == savedUris.size -> {
                        ExportResult.Success(successfulUris.firstOrNull())
                    }

                    else -> {
                        ExportResult.PartialSuccess(
                            outputUris = successfulUris,
                            warningMessage = "Exported ${successfulUris.size}/${savedUris.size} images",
                        )
                    }
                }
            }
        }
    }

}
