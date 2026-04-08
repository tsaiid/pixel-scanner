package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.core.model.ExportImagePipelineResult
import com.example.pixelscanner.core.model.ExportRequest
import com.example.pixelscanner.core.model.ExportResult
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
        val timestamp = System.currentTimeMillis()
        val urisToSave = pipelineRequest.sourceUris

        return when (request.format) {
            ExportFormat.PDF -> {
                val cachedArtifact = exportArtifactCacheRepository.getCachedPdfArtifact(
                    context = context,
                    request = pipelineRequest,
                )
                val cachedSourceUri = cachedArtifact?.let(Uri::fromFile)

                val outputUri = if (cachedSourceUri != null) {
                    exportRepository.copyPdfToDocuments(
                        context = context,
                        sourceUri = cachedSourceUri,
                        fileName = "PixelScanner_$timestamp.pdf",
                    )
                } else if (pipelineRequest.canReuseOriginalPdf()) {
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
                    reusedOriginalPdf = (cachedSourceUri != null || pipelineRequest.canReuseOriginalPdf()) &&
                        outputUri != null,
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

                if (savedUris.all { it != null }) {
                    ExportResult.Success(pipelineResult.outputUri)
                } else {
                    ExportResult.Failure("Failed to export one or more images")
                }
            }
        }
    }

}
