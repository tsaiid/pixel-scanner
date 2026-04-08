package com.example.pixelscanner.core.storage

import android.content.Context
import com.example.pixelscanner.core.model.ScanSessionMetadata
import com.example.pixelscanner.core.model.SessionDirtyFlags
import com.example.pixelscanner.domain.SessionMetadataRepository
import org.json.JSONObject

class AndroidSessionMetadataRepository(
    private val context: Context,
) : SessionMetadataRepository {
    override suspend fun write(metadata: ScanSessionMetadata) {
        val metadataFile = SessionStoragePaths.metadataFile(context.cacheDir, metadata.sessionId)
        metadataFile.parentFile?.mkdirs()
        metadataFile.writeText(metadata.toJson().toString())
    }

    override suspend fun read(sessionId: String): ScanSessionMetadata? {
        val metadataFile = SessionStoragePaths.metadataFile(context.cacheDir, sessionId)
        if (!metadataFile.exists()) {
            return null
        }

        return metadataFile.readText().takeIf { it.isNotBlank() }?.let(::parseMetadata)
    }

    private fun ScanSessionMetadata.toJson(): JSONObject {
        return JSONObject().apply {
            put("schemaVersion", 1)
            put("sessionId", sessionId)
            put("createdAtEpochMillis", createdAtEpochMillis)
            put("pageCount", pageCount)
            put("hasOriginalPdf", hasOriginalPdf)
            put("dirtyFlags", dirtyFlags.toJson())
        }
    }

    private fun SessionDirtyFlags.toJson(): JSONObject {
        return JSONObject().apply {
            put("hasEnhancedPages", hasEnhancedPages)
            put("hasDeletedPages", hasDeletedPages)
            put("hasReorderedPages", hasReorderedPages)
            put("hasRotatedPages", hasRotatedPages)
            put("hasCroppedPages", hasCroppedPages)
        }
    }

    private fun parseMetadata(json: String): ScanSessionMetadata {
        val root = JSONObject(json)
        return ScanSessionMetadata(
            sessionId = root.getString("sessionId"),
            createdAtEpochMillis = root.getLong("createdAtEpochMillis"),
            pageCount = root.getInt("pageCount"),
            hasOriginalPdf = root.getBoolean("hasOriginalPdf"),
            dirtyFlags = root.getJSONObject("dirtyFlags").toDirtyFlags(),
        )
    }

    private fun JSONObject.toDirtyFlags(): SessionDirtyFlags {
        return SessionDirtyFlags(
            hasEnhancedPages = optBoolean("hasEnhancedPages"),
            hasDeletedPages = optBoolean("hasDeletedPages"),
            hasReorderedPages = optBoolean("hasReorderedPages"),
            hasRotatedPages = optBoolean("hasRotatedPages"),
            hasCroppedPages = optBoolean("hasCroppedPages"),
        )
    }
}
