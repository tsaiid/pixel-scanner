package com.example.pixelscanner.core.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.diagnostics.DebugLog
import com.example.pixelscanner.core.model.ExportInsufficientSpaceFailure
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.asString
import com.example.pixelscanner.domain.DiskSpacePreflightRepository
import com.example.pixelscanner.domain.DiskSpacePreflightResult
import com.example.pixelscanner.domain.SessionMetadataRepository
import com.example.pixelscanner.domain.SessionIngestionAssetType
import com.example.pixelscanner.domain.SessionIngestionFailure
import com.example.pixelscanner.domain.SessionIngestionMapping
import com.example.pixelscanner.domain.SessionIngestionResult
import com.example.pixelscanner.domain.SessionIngestionStatus
import com.example.pixelscanner.domain.SessionIngestionRepository
import java.io.File
import java.io.FileOutputStream

class AndroidSessionIngestionRepository(
    private val context: Context,
    private val sessionMetadataRepository: SessionMetadataRepository,
    private val diskSpacePreflightRepository: DiskSpacePreflightRepository = AndroidDiskSpacePreflightRepository(),
) : SessionIngestionRepository {

    override suspend fun ingest(
        session: ScanSession,
        scanResult: ScanResultPayload,
    ): SessionIngestionResult {
        when (diskSpacePreflightRepository.check(
            targetPath = context.cacheDir,
            requiredBytes = estimateRequiredBytes(scanResult),
        )) {
            is DiskSpacePreflightResult.Sufficient -> Unit
            is DiskSpacePreflightResult.Insufficient -> {
                return SessionIngestionResult(
                    status = SessionIngestionStatus.FAILURE,
                    failures = listOf(
                        SessionIngestionFailure(
                            sourceUri = scanResult.pageUris.firstOrNull() ?: scanResult.pdfUri,
                            destinationPath = SessionStoragePaths.sessionRoot(context.cacheDir, session.id).absolutePath,
                            assetType = if (scanResult.pdfUri != null) {
                                SessionIngestionAssetType.ORIGINAL_PDF
                            } else {
                                SessionIngestionAssetType.PAGE_JPEG
                            },
                            message = ExportInsufficientSpaceFailure.userMessage.asString(context),
                            typedFailure = ExportInsufficientSpaceFailure,
                        ),
                    ),
                )
            }

            is DiskSpacePreflightResult.Unknown -> Unit
        }
        val sessionOriginalDir = SessionStoragePaths.originalPagesDir(context.cacheDir, session.id)
        sessionOriginalDir.mkdirs()
        val pageMappings = mutableListOf<SessionIngestionMapping>()
        val failures = mutableListOf<SessionIngestionFailure>()
        val appendedPageStartIndex = (session.pages.size - scanResult.pageUris.size).coerceAtLeast(0)

        scanResult.pageUris.forEachIndexed { index, sourceUri ->
            val destinationFile = File(
                sessionOriginalDir,
                "page_${appendedPageStartIndex + index + 1}.jpg",
            )
            val result = runCatching {
                copyUriToFile(sourceUri, destinationFile)
            }
            if (result.isSuccess) {
                pageMappings += SessionIngestionMapping(
                    sourceUri = sourceUri,
                    destinationPath = destinationFile.absolutePath,
                    destinationUri = Uri.fromFile(destinationFile),
                    assetType = SessionIngestionAssetType.PAGE_JPEG,
                )
            } else {
                failures += SessionIngestionFailure(
                    sourceUri = sourceUri,
                    destinationPath = destinationFile.absolutePath,
                    assetType = SessionIngestionAssetType.PAGE_JPEG,
                    message = result.exceptionOrNull()?.message ?: "Failed to ingest page image",
                )
            }
        }

        val originalPdfMapping = scanResult.pdfUri?.let { sourceUri ->
            val destinationFile = SessionStoragePaths.originalPdfFile(context.cacheDir, session.id)
            val result = runCatching {
                copyUriToFile(sourceUri, destinationFile)
            }
            if (result.isSuccess) {
                SessionIngestionMapping(
                    sourceUri = sourceUri,
                    destinationPath = destinationFile.absolutePath,
                    destinationUri = Uri.fromFile(destinationFile),
                    assetType = SessionIngestionAssetType.ORIGINAL_PDF,
                )
            } else {
                failures += SessionIngestionFailure(
                    sourceUri = sourceUri,
                    destinationPath = destinationFile.absolutePath,
                    assetType = SessionIngestionAssetType.ORIGINAL_PDF,
                    message = result.exceptionOrNull()?.message ?: "Failed to ingest original PDF",
                )
                null
            }
        }

        val status = when {
            failures.isEmpty() -> SessionIngestionStatus.SUCCESS
            pageMappings.isEmpty() && originalPdfMapping == null -> SessionIngestionStatus.FAILURE
            else -> SessionIngestionStatus.PARTIAL_SUCCESS
        }

        val ingestionResult = SessionIngestionResult(
            status = status,
            pageMappings = pageMappings,
            originalPdfMapping = originalPdfMapping,
            failures = failures,
        )

        val ingestedSession = session.withIngestedAssets(ingestionResult)
        runCatching {
            val existingMetadata = sessionMetadataRepository.read(session.id)
            val metadata = ingestedSession.toMetadata(
                createdAtEpochMillis = existingMetadata?.createdAtEpochMillis ?: System.currentTimeMillis(),
                hasOriginalPdf = originalPdfMapping != null || existingMetadata?.hasOriginalPdf == true,
            )
            sessionMetadataRepository.write(metadata)
            sessionMetadataRepository.read(session.id)
        }.onFailure { error ->
            DebugLog.error("session_metadata_warning", error)
        }

        return ingestionResult
    }

    private fun copyUriToFile(sourceUri: Uri, destinationFile: File) {
        destinationFile.parentFile?.mkdirs()
        if (sourceUri.scheme == ContentResolver.SCHEME_FILE) {
            val sourceFile = File(requireNotNull(sourceUri.path) { "Missing file path for $sourceUri" })
            sourceFile.inputStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            return
        }

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        } ?: error("Unable to open input stream for $sourceUri")
    }

    private fun estimateRequiredBytes(scanResult: ScanResultPayload): Long {
        val pagesBytes = ContentUriSizeEstimator.estimateBytes(context, scanResult.pageUris)
        val pdfBytes = scanResult.pdfUri?.let { ContentUriSizeEstimator.estimateBytes(context, it) } ?: 0L
        return pagesBytes + pdfBytes
    }
}
