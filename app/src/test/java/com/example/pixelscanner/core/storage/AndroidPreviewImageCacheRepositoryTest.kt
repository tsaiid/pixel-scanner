package com.example.pixelscanner.core.storage

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.core.model.PreviewImagePipelineRequest
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidPreviewImageCacheRepositoryTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val repository = AndroidPreviewImageCacheRepository()

    @Test
    fun createPreviewFile_writesPreviewIndexAndCachedLookupUsesIndex() {
        val request = previewRequest("session-preview-cache-index")
        val cacheDir = context.cacheDir

        val orphanFile = File(
            SessionStoragePaths.previewPagesDir(cacheDir, request.sessionId),
            "${request.cacheKey.asFileToken()}.jpg",
        )
        orphanFile.parentFile?.mkdirs()
        FileOutputStream(orphanFile).use { outputStream ->
            outputStream.write(byteArrayOf(1, 2, 3))
        }

        assertNull(repository.getCachedPreview(context, request))
        assertFalse(orphanFile.exists())

        val file = repository.createPreviewFile(context, request)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(byteArrayOf(4, 5, 6))
        }

        val indexFile = SessionStoragePaths.previewIndexFile(cacheDir, request.sessionId)
        assertTrue(indexFile.exists())
        assertEquals(request.sessionId, readPreviewIndex(indexFile).sessionId)
        assertEquals(request.cacheKey.value, readPreviewIndex(indexFile).entries.single())
        assertEquals(file, repository.getCachedPreview(context, request))
    }

    @Test
    fun getCachedPreview_prunesMissingFilesFromPreviewIndex() {
        val request = previewRequest("session-preview-cache-cleanup")
        val file = repository.createPreviewFile(context, request)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(byteArrayOf(9, 8, 7))
        }

        assertEquals(file, repository.getCachedPreview(context, request))
        assertTrue(SessionStoragePaths.previewIndexFile(context.cacheDir, request.sessionId).exists())

        assertTrue(file.delete())
        assertNull(repository.getCachedPreview(context, request))

        val indexFile = SessionStoragePaths.previewIndexFile(context.cacheDir, request.sessionId)
        assertFalse(indexFile.exists())
        assertFalse(SessionStoragePaths.previewPagesDir(context.cacheDir, request.sessionId).exists())
    }

    private fun previewRequest(sessionId: String): PreviewImagePipelineRequest {
        return PreviewImagePipelineRequest(
            pageId = "${sessionId}_page_0",
            sourceUri = Uri.parse("content://scanner/$sessionId/page-0.jpg"),
        )
    }

    private fun readPreviewIndex(file: File): PreviewCacheIndexSnapshot {
        val root = JSONObject(file.readText())
        val entries = root.getJSONArray("entries")
        return PreviewCacheIndexSnapshot(
            sessionId = root.getString("sessionId"),
            entries = List(entries.length()) { index ->
                entries.getJSONObject(index).getString("cacheKey")
            },
        )
    }

    private data class PreviewCacheIndexSnapshot(
        val sessionId: String,
        val entries: List<String>,
    )
}
