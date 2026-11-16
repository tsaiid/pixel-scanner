package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import com.example.pixelscanner.R
import com.example.pixelscanner.core.diagnostics.DebugLog
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
import com.example.pixelscanner.core.model.uiTextResource

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
        val startedAtMs = SystemClock.elapsedRealtime()
        val pagesToExport = session.activePages()
        if (pagesToExport.isEmpty()) {
            val result = ExportResult.Failure(com.example.pixelscanner.core.model.ExportNoPagesFailure)
            DebugLog.event(
                name = "export",
                "durationMs" to (SystemClock.elapsedRealtime() - startedAtMs),
                "format" to request.format.name,
                "outcome" to "failure_no_pages",
                "path" to "empty_session",
                "decision" to "n/a",
                "cacheHit" to false,
                "pageCount" to 0,
                "hasPdf" to (session.originalPdfUri != null),
            )
            return result
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
        var exportOutcome = "unknown"
        var exportPath = "unknown"
        var cacheHit = false

        val result = when (request.format) {
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
                cacheHit = cachedArtifact != null
                exportPath = when (exportDecision) {
                    ExportDecision.ReuseOriginalPdf -> {
                        if (cachedSourceUri != null) "pdf_copy_cached" else "pdf_copy_original"
                    }

                    ExportDecision.RebuildPdfFromEnhancedPages,
                    ExportDecision.RebuildPdfFromPages -> {
                        if (cachedSourceUri != null) "pdf_copy_cached" else "pdf_rebuild"
                    }

                    ExportDecision.ExportJpegs -> "pdf_from_jpegs"
                }

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
                    is ExportOperationResult.Success -> {
                        exportOutcome = "success"
                        ExportResult.Success(pipelineResult.outputUri)
                    }

                    is ExportOperationResult.Failure -> {
                        exportOutcome = "failure"
                        ExportResult.Failure(outputResult.error)
                    }
                }
            }

            ExportFormat.IMAGE -> {
                val cachedArtifacts = exportArtifactCacheRepository.getCachedImageArtifacts(
                    context = context,
                    request = pipelineRequest,
                    pageCount = urisToSave.size,
                )
                val sourceUrisForExport = cachedArtifacts?.map(Uri::fromFile) ?: urisToSave
                cacheHit = cachedArtifacts != null
                exportPath = if (cacheHit) "image_cached" else "image_render"
                val totalPages = sourceUrisForExport.size
                val completedPages = mutableListOf<ExportResult.CompletedExportPage>()
                val failures = mutableListOf<ExportFailure>()
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
                            completedPages += ExportResult.CompletedExportPage(
                                pageId = pagesToExport[index].id,
                                outputUri = result.outputUri,
                            )
                            result
                        }

                        is ExportOperationResult.Failure -> {
                            failures += result.error
                            result
                        }
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

                val successfulUris = completedPages.map { it.outputUri }
                when {
                    successfulUris.isEmpty() -> {
                        exportOutcome = "failure"
                        ExportResult.Failure(
                            failures.firstOrNull() ?: ExportUnknownFailure(
                                details = context.getString(R.string.export_error_image_export_failed),
                            ),
                        )
                    }

                    successfulUris.size == results.size -> {
                        exportOutcome = "success"
                        ExportResult.Success(
                            outputUri = successfulUris.firstOrNull(),
                            outputUris = successfulUris,
                        )
                    }

                    else -> {
                        exportOutcome = "partial_success"
                        ExportResult.PartialSuccess(
                            completedPages = completedPages,
                            failures = failures,
                            warningMessage = uiTextResource(
                                R.string.export_warning_partial_success,
                                successfulUris.size,
                                results.size,
                            ),
                        )
                    }
                }
            }
        }

        DebugLog.event(
            name = "export",
            "durationMs" to (SystemClock.elapsedRealtime() - startedAtMs),
            "format" to request.format.name,
            "outcome" to exportOutcome,
            "path" to exportPath,
            "decision" to exportDecision.toString(),
            "cacheHit" to cacheHit,
            "pageCount" to pagesToExport.size,
            "hasPdf" to (session.originalPdfUri != null),
        )
        return result
    }

}
