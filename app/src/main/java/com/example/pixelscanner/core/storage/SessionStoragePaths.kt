package com.example.pixelscanner.core.storage

import java.io.File

object SessionStoragePaths {
    private const val SESSIONS_DIR = "sessions"
    private const val ORIGINAL_DIR = "original"
    private const val PREVIEW_DIR = "preview"
    private const val EXPORT_DIR = "export"
    private const val METADATA_FILE = "metadata.json"

    fun sessionsRoot(cacheDir: File): File {
        return File(cacheDir, SESSIONS_DIR)
    }

    fun sessionRoot(cacheDir: File, sessionId: String): File {
        return File(sessionsRoot(cacheDir), sessionId)
    }

    fun originalPagesDir(cacheDir: File, sessionId: String): File {
        return File(sessionRoot(cacheDir, sessionId), ORIGINAL_DIR)
    }

    fun originalPdfFile(cacheDir: File, sessionId: String): File {
        return File(sessionRoot(cacheDir, sessionId), "original.pdf")
    }

    fun metadataFile(cacheDir: File, sessionId: String): File {
        return File(sessionRoot(cacheDir, sessionId), METADATA_FILE)
    }

    fun previewPagesDir(cacheDir: File, sessionId: String): File {
        return File(sessionRoot(cacheDir, sessionId), PREVIEW_DIR)
    }

    fun previewIndexFile(cacheDir: File, sessionId: String): File {
        return File(previewPagesDir(cacheDir, sessionId), "preview-index.json")
    }

    fun exportDir(cacheDir: File, sessionId: String): File {
        return File(sessionRoot(cacheDir, sessionId), EXPORT_DIR)
    }
}
