package com.example.pixelscanner.core.model

import android.net.Uri
import java.security.MessageDigest

data class PipelineCacheKey(val value: String) {
    fun asFileToken(): String = value
}

data class PreviewImagePipelineRequest(
    val pageId: String,
    val sourceUri: Uri,
    val sourceVariant: PageVariant = PageVariant.ORIGINAL,
) {
    val sessionId: String = pageId.substringBefore("_page_", pageId)
    val cacheKey: PipelineCacheKey = PipelineCacheKeyStrategy.preview(
        pageId = pageId,
        sourceUri = sourceUri,
        sourceVariant = sourceVariant,
    )
}

data class PreviewImagePipelineResult(
    val cacheKey: PipelineCacheKey,
    val outputUri: Uri,
    val reusedCachedPreview: Boolean = false,
)

data class ExportImagePipelineRequest(
    val sessionId: String,
    val format: ExportFormat,
    val sourceUris: List<Uri>,
    val originalPdfUri: Uri? = null,
    val dirtyFlags: SessionDirtyFlags = SessionDirtyFlags(),
) {
    val cacheKey: PipelineCacheKey = PipelineCacheKeyStrategy.export(
        sessionId = sessionId,
        format = format,
        sourceUris = sourceUris,
        originalPdfUri = originalPdfUri,
        dirtyFlags = dirtyFlags,
    )

    fun canReuseOriginalPdf(): Boolean {
        return format == ExportFormat.PDF && originalPdfUri != null && !dirtyFlags.isDirty()
    }
}

data class ExportImagePipelineResult(
    val cacheKey: PipelineCacheKey,
    val outputUri: Uri?,
    val reusedOriginalPdf: Boolean,
)

object PipelineCacheKeyStrategy {
    fun preview(
        pageId: String,
        sourceUri: Uri,
        sourceVariant: PageVariant,
    ): PipelineCacheKey {
        return PipelineCacheKey(
            value = "preview-${digest(pageId, sourceUri.toString(), sourceVariant.name)}"
        )
    }

    fun export(
        sessionId: String,
        format: ExportFormat,
        sourceUris: List<Uri>,
        originalPdfUri: Uri?,
        dirtyFlags: SessionDirtyFlags,
    ): PipelineCacheKey {
        return PipelineCacheKey(
            value = "export-${digest(
                sessionId,
                format.name,
                dirtyFlags.cacheSignature(),
                originalPdfUri?.toString().orEmpty(),
                sourceUris.joinToString(separator = "|") { it.toString() },
            )}"
        )
    }

    private fun digest(vararg parts: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(parts.joinToString(separator = "|").toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }.take(16)
    }
}
