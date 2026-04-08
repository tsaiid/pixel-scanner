package com.example.pixelscanner.core.storage

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import com.example.pixelscanner.domain.ExportArtifactCacheRepository
import java.io.File
import java.io.FileOutputStream

class AndroidExportArtifactCacheRepository : ExportArtifactCacheRepository {
    override fun cleanStaleTempArtifacts(context: Context) {
        val sessionsRoot = SessionStoragePaths.sessionsRoot(context.cacheDir)
        val sessionDirs = sessionsRoot.listFiles()?.filter { it.isDirectory } ?: return

        sessionDirs.forEach { sessionDir ->
            val exportDir = File(sessionDir, "export")
            exportDir.deleteTempFilesRecursively()
        }
    }

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
        return storeArtifactAtomically(
            context = context,
            sourceUri = sourceUri,
            finalFile = file,
        )
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
        return storeArtifactAtomically(
            context = context,
            sourceUri = sourceUri,
            finalFile = file,
        )
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

    private fun storeArtifactAtomically(
        context: Context,
        sourceUri: Uri,
        finalFile: File,
    ): File {
        finalFile.parentFile?.mkdirs()
        val tempFile = finalFile.tempFile()
        tempFile.delete()
        try {
            copyUriToFile(context, sourceUri, tempFile)
            if (finalFile.exists() && !finalFile.delete()) {
                throw IllegalStateException("Failed to replace existing artifact: ${finalFile.name}")
            }
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }
            return finalFile
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    private fun File.tempFile(): File {
        return File(parentFile, "$name.tmp")
    }

    private fun File.deleteTempFilesRecursively() {
        if (!exists()) return
        walkTopDown()
            .filter { it.isFile && it.name.endsWith(".tmp") }
            .forEach { tempFile ->
                tempFile.delete()
            }
    }
}
