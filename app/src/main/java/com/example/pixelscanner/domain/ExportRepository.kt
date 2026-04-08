package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.ExportOperationResult

interface ExportRepository {
    suspend fun copyPdfToDocuments(context: Context, sourceUri: Uri, fileName: String): ExportOperationResult
    suspend fun saveUriToGallery(context: Context, sourceUri: Uri, fileName: String): ExportOperationResult
    suspend fun saveUrisToPdf(context: Context, sourceUris: List<Uri>, fileName: String): ExportOperationResult
}
