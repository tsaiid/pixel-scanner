package com.example.pixelscanner.testing

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.ExportOperationResult
import com.example.pixelscanner.domain.ExportRepository

class FakeExportRepository(
    var copyPdfResult: ExportOperationResult = ExportOperationResult.Failure(
        com.example.pixelscanner.core.model.ExportUnknownFailure("No fake PDF result configured"),
    ),
    var saveUriResult: ExportOperationResult = ExportOperationResult.Failure(
        com.example.pixelscanner.core.model.ExportUnknownFailure("No fake image result configured"),
    ),
    var saveUrisToPdfResult: ExportOperationResult = ExportOperationResult.Failure(
        com.example.pixelscanner.core.model.ExportUnknownFailure("No fake PDF export result configured"),
    ),
) : ExportRepository {
    val copiedPdfRequests = mutableListOf<ExportPdfRequest>()
    val savedUriRequests = mutableListOf<ExportImageRequest>()
    val savedPdfRequests = mutableListOf<ExportPdfBuildRequest>()

    override suspend fun copyPdfToDocuments(
        context: Context,
        sourceUri: Uri,
        fileName: String,
    ): ExportOperationResult {
        copiedPdfRequests += ExportPdfRequest(sourceUri, fileName)
        return copyPdfResult
    }

    override suspend fun saveUriToGallery(
        context: Context,
        sourceUri: Uri,
        fileName: String,
    ): ExportOperationResult {
        savedUriRequests += ExportImageRequest(sourceUri, fileName)
        return saveUriResult
    }

    override suspend fun saveUrisToPdf(
        context: Context,
        sourceUris: List<Uri>,
        fileName: String,
    ): ExportOperationResult {
        savedPdfRequests += ExportPdfBuildRequest(sourceUris, fileName)
        return saveUrisToPdfResult
    }
}

data class ExportPdfRequest(
    val sourceUri: Uri,
    val fileName: String,
)

data class ExportImageRequest(
    val sourceUri: Uri,
    val fileName: String,
)

data class ExportPdfBuildRequest(
    val sourceUris: List<Uri>,
    val fileName: String,
)
