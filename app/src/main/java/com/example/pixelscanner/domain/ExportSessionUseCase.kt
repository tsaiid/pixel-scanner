package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportDecision
import com.example.pixelscanner.core.model.ExportDecodeFailure
import com.example.pixelscanner.core.model.ExportFailure
import com.example.pixelscanner.core.model.ExportInsufficientSpaceFailure
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.ExportImagePipelineResult
import com.example.pixelscanner.core.model.ExportOverallProgress
import com.example.pixelscanner.core.model.ExportOperationResult
import com.example.pixelscanner.core.model.ExportPageProgress
import com.example.pixelscanner.core.model.ExportProgress
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.ExportResult
import com.example.pixelscanner.core.model.ExportSourceInvalidFailure
import com.example.pixelscanner.core.model.ExportUnknownFailure
import com.example.pixelscanner.core.model.ExportWriteFailure
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
        onProgress: (ExportProgress) -> Unit = {},
    ): ExportResult {
        val pagesToExport = session.activePages()
        if (pagesToExport.isEmpty()) {
            return ExportResult.Failure(com.example.pixelscanner.core.model.ExportNoPagesFailure)
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
                onProgress(
                    ExportProgress(
                        overall = ExportOverallProgress(
                            completed = 0,
                            total = 1,
                        ),
                    ),
                )
                val cachedArtifact = exportArtifactCacheRepository.getCachedPdfArtifact(
                    context = context,
                    request = pipelineRequest,
                )
                val cachedSourceUri = cachedArtifact?.let(Uri::fromFile)

                val outputResult = when (exportDecision) {
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

                val outputUri = when (outputResult) {
                    is ExportOperationResult.Success -> outputResult.outputUri
                    is ExportOperationResult.Failure -> null
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
                onProgress(
                    ExportProgress(
                        overall = ExportOverallProgress(
                            completed = 1,
                            total = 1,
                        ),
                    ),
                )

                when (outputResult) {
                    is ExportOperationResult.Success -> ExportResult.Success(pipelineResult.outputUri)
                    is ExportOperationResult.Failure -> ExportResult.Failure(outputResult.error)
                }
            }

            ExportFormat.IMAGE -> {
                val cachedArtifacts = exportArtifactCacheRepository.getCachedImageArtifacts(
                    context = context,
                    request = pipelineRequest,
                    pageCount = urisToSave.size,
                )
                val sourceUrisForExport = cachedArtifacts?.map(Uri::fromFile) ?: urisToSave
                val totalPages = sourceUrisForExport.size
                onProgress(
                    ExportProgress(
                        overall = ExportOverallProgress(
                            completed = 0,
                            total = totalPages,
                        ),
                    ),
                )

                val results = sourceUrisForExport.mapIndexed { index, uri ->
                    val exportResult = when (val result = exportRepository.saveUriToGallery(
                        context = context,
                        sourceUri = uri,
                        fileName = "PixelScanner_${timestamp}_$index.jpg",
                    )) {
                        is ExportOperationResult.Success -> {
                            exportArtifactCacheRepository.storeImageArtifact(
                                context = context,
                                request = pipelineRequest,
                                pageIndex = index,
                                sourceUri = result.outputUri,
                            )
                            result
                        }

                        is ExportOperationResult.Failure -> result
                    }
                    onProgress(
                        ExportProgress(
                            overall = ExportOverallProgress(
                                completed = index + 1,
                                total = totalPages,
                            ),
                            page = ExportPageProgress(
                                completed = index + 1,
                                total = totalPages,
                            ),
                        ),
                    )
                    exportResult
                }

                val pipelineResult = ExportImagePipelineResult(
                    cacheKey = pipelineRequest.cacheKey,
                    outputUri = null,
                    reusedOriginalPdf = false,
                )

                val successfulUris = results.mapNotNull { result ->
                    (result as? ExportOperationResult.Success)?.outputUri
                }
                val failures = results.mapNotNull { result ->
                    (result as? ExportOperationResult.Failure)?.error
                }
                when {
                    successfulUris.isEmpty() -> {
                        ExportResult.Failure(
                            failures.firstOrNull() ?: ExportUnknownFailure(
                                details = "Failed to export one or more images",
                            ),
                        )
                    }

                    successfulUris.size == results.size -> {
                        ExportResult.Success(successfulUris.firstOrNull())
                    }

                    else -> {
                        ExportResult.PartialSuccess(
                            outputUris = successfulUris,
                            failures = failures,
                            warningMessage = "Exported ${successfulUris.size}/${results.size} images",
                        )
                    }
                }
            }
        }
    }

}
