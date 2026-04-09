package com.example.pixelscanner.core.storage

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.domain.ExportRepository
import com.example.pixelscanner.core.model.ExportOperationResult

class AndroidExportRepository : ExportRepository {
    override suspend fun copyPdfToDocuments(
        context: Context,
        sourceUri: Uri,
        fileName: String,
    ): ExportOperationResult {
        return StorageManager(context).copyPdfToDocuments(sourceUri, fileName)
    }

    override suspend fun saveUriToGallery(
        context: Context,
        sourceUri: Uri,
        fileName: String,
    ): ExportOperationResult {
        return StorageManager(context).saveUriToGallery(sourceUri, fileName)
    }

    override suspend fun saveUrisToPdf(
        context: Context,
        sourceUris: List<Uri>,
        fileName: String,
    ): ExportOperationResult {
        return StorageManager(context).saveUrisToPdf(sourceUris, fileName)
    }
}
