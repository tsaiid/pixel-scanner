package com.example.pixelscanner.core.storage

import android.content.Context
import com.example.pixelscanner.core.model.PageVariant
import com.example.pixelscanner.core.model.ScanPageMetadata
import com.example.pixelscanner.core.model.ScanSessionMetadata
import com.example.pixelscanner.core.model.SessionDirtyFlags
import com.example.pixelscanner.domain.SessionMetadataRepository
import org.json.JSONArray
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
            put("schemaVersion", 2)
            put("sessionId", sessionId)
            put("createdAtEpochMillis", createdAtEpochMillis)
            put("pageCount", pageCount)
            put("hasOriginalPdf", hasOriginalPdf)
            put("dirtyFlags", dirtyFlags.toJson())
            if (pages.isNotEmpty()) {
                put(
                    "pages",
                    JSONArray().apply {
                        pages.forEach { page ->
                            put(page.toJson())
                        }
                    },
                )
            }
        }
    }

    private fun ScanPageMetadata.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("originalFileName", originalFileName)
            if (enhancedUriString != null) {
                put("enhancedUriString", enhancedUriString)
            }
            put("activeVariant", activeVariant.name)
            put("isDeleted", isDeleted)
        }
    }

    private fun SessionDirtyFlags.toJson(): JSONObject {
        return JSONObject().apply {
            put("hasEnhancedPages", hasEnhancedPages)
            put("hasDeletedPages", hasDeletedPages)
            put("hasReorderedPages", hasReorderedPages)
            put("hasRotatedPages", hasRotatedPages)
            put("hasCroppedPages", hasCroppedPages)
            put("hasAddedPages", hasAddedPages)
        }
    }

    private fun parseMetadata(json: String): ScanSessionMetadata {
        val root = JSONObject(json)
        val parsedPages = buildList {
            val pagesArray = root.optJSONArray("pages")
            if (pagesArray != null) {
                for (index in 0 until pagesArray.length()) {
                    val pageObj = pagesArray.getJSONObject(index)
                    val variantName = pageObj.optString("activeVariant", PageVariant.ORIGINAL.name)
                    val activeVariant = runCatching { PageVariant.valueOf(variantName) }
                        .getOrDefault(PageVariant.ORIGINAL)
                    add(
                        ScanPageMetadata(
                            id = pageObj.getString("id"),
                            originalFileName = pageObj.getString("originalFileName"),
                            enhancedUriString = pageObj.optString("enhancedUriString", "").takeIf { it.isNotBlank() },
                            activeVariant = activeVariant,
                            isDeleted = pageObj.optBoolean("isDeleted", false),
                        ),
                    )
                }
            }
        }
        return ScanSessionMetadata(
            sessionId = root.getString("sessionId"),
            createdAtEpochMillis = root.getLong("createdAtEpochMillis"),
            pageCount = root.getInt("pageCount"),
            hasOriginalPdf = root.getBoolean("hasOriginalPdf"),
            dirtyFlags = root.getJSONObject("dirtyFlags").toDirtyFlags(),
            pages = parsedPages,
        )
    }

    private fun JSONObject.toDirtyFlags(): SessionDirtyFlags {
        return SessionDirtyFlags(
            hasEnhancedPages = optBoolean("hasEnhancedPages"),
            hasDeletedPages = optBoolean("hasDeletedPages"),
            hasReorderedPages = optBoolean("hasReorderedPages"),
            hasRotatedPages = optBoolean("hasRotatedPages"),
            hasCroppedPages = optBoolean("hasCroppedPages"),
            hasAddedPages = optBoolean("hasAddedPages"),
        )
    }
}
