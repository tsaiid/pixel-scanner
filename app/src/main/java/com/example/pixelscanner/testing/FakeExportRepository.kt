package com.example.pixelscanner.testing

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.domain.ExportRepository

class FakeExportRepository(
    var copyPdfResult: Uri? = null,
    var saveUriResult: Uri? = null,
    var saveUrisToPdfResult: Uri? = null,
) : ExportRepository {
    val copiedPdfRequests = mutableListOf<ExportPdfRequest>()
    val savedUriRequests = mutableListOf<ExportImageRequest>()
    val savedPdfRequests = mutableListOf<ExportPdfBuildRequest>()

    override suspend fun copyPdfToDocuments(
        context: Context,
        sourceUri: Uri,
        fileName: String,
    ): Uri? {
        copiedPdfRequests += ExportPdfRequest(sourceUri, fileName)
        return copyPdfResult
    }

    override suspend fun saveUriToGallery(
        context: Context,
        sourceUri: Uri,
        fileName: String,
    ): Uri? {
        savedUriRequests += ExportImageRequest(sourceUri, fileName)
        return saveUriResult
    }

    override suspend fun saveUrisToPdf(
        context: Context,
        sourceUris: List<Uri>,
        fileName: String,
    ): Uri? {
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
