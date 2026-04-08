package com.example.pixelscanner.core.storage

import android.content.Context
import com.example.pixelscanner.core.model.PreviewCacheEntryMetadata
import com.example.pixelscanner.core.model.PreviewCacheIndex
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import com.example.pixelscanner.domain.PreviewImageCacheRepository
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class AndroidPreviewImageCacheRepository : PreviewImageCacheRepository {
    private val lock = Any()

    override fun getCachedPreview(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): File? = synchronized(lock) {
        val index = prunePreviewCache(context.cacheDir, request.sessionId)
        val entry = index.entries.firstOrNull { it.cacheKey == request.cacheKey.value } ?: return null
        val file = previewFile(context.cacheDir, request.sessionId, entry.fileName)
        if (!file.exists() || file.length() <= 0L) {
            deletePreviewEntry(context.cacheDir, request.sessionId, entry.cacheKey)
            return null
        }

        persistPreviewCacheIndex(
            context.cacheDir,
            index.touchEntry(entry.cacheKey, file.length()),
        )
        file
    }

    override fun createPreviewFile(
        context: Context,
        request: PreviewImagePipelineRequest,
    ): File = synchronized(lock) {
        val previewDir = SessionStoragePaths.previewPagesDir(context.cacheDir, request.sessionId)
        previewDir.mkdirs()

        val file = previewFile(
            context.cacheDir,
            request.sessionId,
            "${request.cacheKey.asFileToken()}.jpg",
        )
        if (file.exists() && file.length() == 0L) {
            file.delete()
        }

        val index = prunePreviewCache(context.cacheDir, request.sessionId)
        persistPreviewCacheIndex(
            context.cacheDir,
            index.upsert(
                PreviewCacheEntryMetadata(
                    cacheKey = request.cacheKey.value,
                    fileName = file.name,
                    fileSizeBytes = if (file.exists()) file.length() else 0L,
                    createdAtEpochMs = System.currentTimeMillis(),
                    lastAccessedAtEpochMs = System.currentTimeMillis(),
                ),
            ),
        )
        file
    }

    private fun prunePreviewCache(
        cacheDir: File,
        sessionId: String,
    ): PreviewCacheIndex {
        val index = readPreviewCacheIndex(cacheDir, sessionId)
        val previewDir = SessionStoragePaths.previewPagesDir(cacheDir, sessionId)
        val filesByName = previewDir.listFiles()?.associateBy { it.name }.orEmpty()
        val validEntries = mutableListOf<PreviewCacheEntryMetadata>()

        index.entries.forEach { entry ->
            val file = filesByName[entry.fileName]
            if (file == null || !file.isFile || file.length() <= 0L) {
                file?.delete()
                return@forEach
            }
            validEntries += entry.copy(
                fileSizeBytes = file.length(),
                lastAccessedAtEpochMs = System.currentTimeMillis(),
            )
        }

        val validFileNames = validEntries.map { it.fileName }.toSet()
        filesByName.values.forEach { file ->
            if (file.name == PREVIEW_INDEX_FILE_NAME || file.name in validFileNames) {
                return@forEach
            }
            if (file.isFile) {
                file.delete()
            }
        }

        val prunedIndex = PreviewCacheIndex(
            sessionId = sessionId,
            entries = validEntries,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        persistPreviewCacheIndex(cacheDir, prunedIndex)
        return prunedIndex
    }

    private fun deletePreviewEntry(
        cacheDir: File,
        sessionId: String,
        cacheKey: String,
    ) {
        val index = readPreviewCacheIndex(cacheDir, sessionId)
        val removedEntry = index.entries.firstOrNull { it.cacheKey == cacheKey } ?: return
        val file = previewFile(cacheDir, sessionId, removedEntry.fileName)
        if (file.exists()) {
            file.delete()
        }
        persistPreviewCacheIndex(cacheDir, index.without(cacheKey))
    }

    private fun readPreviewCacheIndex(
        cacheDir: File,
        sessionId: String,
    ): PreviewCacheIndex {
        val indexFile = SessionStoragePaths.previewIndexFile(cacheDir, sessionId)
        if (!indexFile.exists()) {
            return PreviewCacheIndex(sessionId = sessionId)
        }

        return runCatching {
            val root = JSONObject(indexFile.readText())
            val entries = buildList {
                val jsonEntries = root.optJSONArray(KEY_ENTRIES)
                if (jsonEntries != null) {
                    for (i in 0 until jsonEntries.length()) {
                        val entry = jsonEntries.getJSONObject(i)
                        add(
                            PreviewCacheEntryMetadata(
                                cacheKey = entry.getString(KEY_CACHE_KEY),
                                fileName = entry.getString(KEY_FILE_NAME),
                                fileSizeBytes = entry.optLong(KEY_FILE_SIZE_BYTES, 0L),
                                createdAtEpochMs = entry.optLong(KEY_CREATED_AT_EPOCH_MS, 0L),
                                lastAccessedAtEpochMs = entry.optLong(KEY_LAST_ACCESSED_AT_EPOCH_MS, 0L),
                            ),
                        )
                    }
                }
            }
            PreviewCacheIndex(
                sessionId = root.optString(KEY_SESSION_ID, sessionId),
                entries = entries,
                updatedAtEpochMs = root.optLong(KEY_UPDATED_AT_EPOCH_MS, 0L),
            )
        }.getOrElse {
            indexFile.delete()
            PreviewCacheIndex(sessionId = sessionId)
        }
    }

    private fun persistPreviewCacheIndex(
        cacheDir: File,
        index: PreviewCacheIndex,
    ) {
        val indexFile = SessionStoragePaths.previewIndexFile(cacheDir, index.sessionId)
        indexFile.parentFile?.mkdirs()
        if (index.entries.isEmpty()) {
            if (indexFile.exists()) {
                indexFile.delete()
            }
            cleanupEmptyPreviewDirectory(cacheDir, index.sessionId)
            return
        }

        val json = JSONObject().apply {
            put(KEY_SESSION_ID, index.sessionId)
            put(KEY_UPDATED_AT_EPOCH_MS, index.updatedAtEpochMs)
            put(KEY_ENTRIES, JSONArray().apply {
                index.entries.forEach { entry ->
                    put(
                        JSONObject().apply {
                            put(KEY_CACHE_KEY, entry.cacheKey)
                            put(KEY_FILE_NAME, entry.fileName)
                            put(KEY_FILE_SIZE_BYTES, entry.fileSizeBytes)
                            put(KEY_CREATED_AT_EPOCH_MS, entry.createdAtEpochMs)
                            put(KEY_LAST_ACCESSED_AT_EPOCH_MS, entry.lastAccessedAtEpochMs)
                        },
                    )
                }
            })
        }
        indexFile.writeText(json.toString())
    }

    private fun cleanupEmptyPreviewDirectory(
        cacheDir: File,
        sessionId: String,
    ) {
        val previewDir = SessionStoragePaths.previewPagesDir(cacheDir, sessionId)
        val files = previewDir.listFiles().orEmpty()
        if (files.isEmpty()) {
            previewDir.delete()
        }
    }

    private fun previewFile(
        cacheDir: File,
        sessionId: String,
        fileName: String,
    ): File {
        return File(SessionStoragePaths.previewPagesDir(cacheDir, sessionId), fileName)
    }

    private fun PreviewCacheIndex.upsert(
        entry: PreviewCacheEntryMetadata,
    ): PreviewCacheIndex {
        val existing = entries.firstOrNull { it.cacheKey == entry.cacheKey }
        val updatedEntry = entry.copy(
            createdAtEpochMs = existing?.createdAtEpochMs ?: entry.createdAtEpochMs,
            fileSizeBytes = existing?.fileSizeBytes ?: entry.fileSizeBytes,
            lastAccessedAtEpochMs = entry.lastAccessedAtEpochMs,
        )
        val updatedEntries = buildList {
            entries.forEach { existingEntry ->
                if (existingEntry.cacheKey != entry.cacheKey) {
                    add(existingEntry)
                }
            }
            add(updatedEntry)
        }
        return copy(
            entries = updatedEntries,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun PreviewCacheIndex.touchEntry(
        cacheKey: String,
        fileSizeBytes: Long,
    ): PreviewCacheIndex {
        val touchedEntries = entries.map { entry ->
            if (entry.cacheKey != cacheKey) {
                return@map entry
            }
            entry.copy(
                fileSizeBytes = fileSizeBytes,
                lastAccessedAtEpochMs = System.currentTimeMillis(),
            )
        }
        return copy(
            entries = touchedEntries,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun PreviewCacheIndex.without(cacheKey: String): PreviewCacheIndex {
        return copy(
            entries = entries.filterNot { it.cacheKey == cacheKey },
            updatedAtEpochMs = System.currentTimeMillis(),
        )
    }

    companion object {
        private const val KEY_SESSION_ID = "sessionId"
        private const val KEY_UPDATED_AT_EPOCH_MS = "updatedAtEpochMs"
        private const val KEY_ENTRIES = "entries"
        private const val KEY_CACHE_KEY = "cacheKey"
        private const val KEY_FILE_NAME = "fileName"
        private const val KEY_FILE_SIZE_BYTES = "fileSizeBytes"
        private const val KEY_CREATED_AT_EPOCH_MS = "createdAtEpochMs"
        private const val KEY_LAST_ACCESSED_AT_EPOCH_MS = "lastAccessedAtEpochMs"
        private const val PREVIEW_INDEX_FILE_NAME = "preview-index.json"
    }
}
