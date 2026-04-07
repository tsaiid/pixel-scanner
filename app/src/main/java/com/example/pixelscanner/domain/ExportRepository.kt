package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri

interface ExportRepository {
    suspend fun copyPdfToDocuments(context: Context, sourceUri: Uri, fileName: String): Uri?
    suspend fun saveUriToGallery(context: Context, sourceUri: Uri, fileName: String): Uri?
    suspend fun saveUrisToPdf(context: Context, sourceUris: List<Uri>, fileName: String): Uri?
}
