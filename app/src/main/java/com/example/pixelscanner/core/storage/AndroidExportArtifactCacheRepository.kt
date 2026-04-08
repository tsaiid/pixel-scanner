package com.example.pixelscanner.core.storage

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.domain.ExportArtifactCacheRepository
import java.io.File
import java.io.FileOutputStream

class AndroidExportArtifactCacheRepository : ExportArtifactCacheRepository {
    override fun getCachedPdfArtifact(
        context: Context,
        request: ExportImagePipelineRequest,
    ): File? {
        val file = pdfArtifactFile(context, request)
        return if (file.exists() && file.length() > 0L) file else null
    }

    override fun storePdfArtifact(
        context: Context,
        request: ExportImagePipelineRequest,
        sourceUri: Uri,
    ): File {
        val file = pdfArtifactFile(context, request)
        file.parentFile?.mkdirs()
        copyUriToFile(context, sourceUri, file)
        return file
    }

    override fun getCachedImageArtifacts(
        context: Context,
        request: ExportImagePipelineRequest,
        pageCount: Int,
    ): List<File>? {
        val files = (0 until pageCount).map { index -> imageArtifactFile(context, request, index) }
        return if (files.all { it.exists() && it.length() > 0L }) files else null
    }

    override fun storeImageArtifact(
        context: Context,
        request: ExportImagePipelineRequest,
        pageIndex: Int,
        sourceUri: Uri,
    ): File {
        val file = imageArtifactFile(context, request, pageIndex)
        file.parentFile?.mkdirs()
        copyUriToFile(context, sourceUri, file)
        return file
    }

    private fun pdfArtifactFile(
        context: Context,
        request: ExportImagePipelineRequest,
    ): File {
        return File(
            SessionStoragePaths.exportDir(context.cacheDir, request.sessionId),
            "${request.cacheKey.asFileToken()}.pdf",
        )
    }

    private fun imageArtifactFile(
        context: Context,
        request: ExportImagePipelineRequest,
        pageIndex: Int,
    ): File {
        return File(
            SessionStoragePaths.exportDir(context.cacheDir, request.sessionId),
            "${request.cacheKey.asFileToken()}_$pageIndex.jpg",
        )
    }

    private fun copyUriToFile(
        context: Context,
        sourceUri: Uri,
        destinationFile: File,
    ) {
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalArgumentException("Could not open source URI: $sourceUri")
    }
}
