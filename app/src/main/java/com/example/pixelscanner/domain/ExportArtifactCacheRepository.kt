package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.ExportImagePipelineRequest
import java.io.File

interface ExportArtifactCacheRepository {
    fun cleanStaleTempArtifacts(context: Context)

    fun getCachedPdfArtifact(
        context: Context,
        request: ExportImagePipelineRequest,
    ): File?

    fun storePdfArtifact(
        context: Context,
        request: ExportImagePipelineRequest,
        sourceUri: Uri,
    ): File

    fun getCachedImageArtifacts(
        context: Context,
        request: ExportImagePipelineRequest,
        pageCount: Int,
    ): List<File>?

    fun storeImageArtifact(
        context: Context,
        request: ExportImagePipelineRequest,
        pageIndex: Int,
        sourceUri: Uri,
    ): File
}
