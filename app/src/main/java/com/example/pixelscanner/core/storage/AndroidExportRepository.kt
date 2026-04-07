package com.example.pixelscanner.core.storage

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.domain.ExportRepository

class AndroidExportRepository : ExportRepository {
    override suspend fun copyPdfToDocuments(context: Context, sourceUri: Uri, fileName: String): Uri? {
        return StorageManager(context).copyPdfToDocuments(sourceUri, fileName)
    }

    override suspend fun saveUriToGallery(context: Context, sourceUri: Uri, fileName: String): Uri? {
        return StorageManager(context).saveUriToGallery(sourceUri, fileName)
    }

    override suspend fun saveUrisToPdf(context: Context, sourceUris: List<Uri>, fileName: String): Uri? {
        return StorageManager(context).saveUrisToPdf(sourceUris, fileName)
    }
}
