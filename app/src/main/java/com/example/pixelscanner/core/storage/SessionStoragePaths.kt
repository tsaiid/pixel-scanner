package com.example.pixelscanner.core.storage

import java.io.File

object SessionStoragePaths {
    private const val SESSIONS_DIR = "sessions"
    private const val ORIGINAL_DIR = "original"
    private const val PREVIEW_DIR = "preview"
    private const val EXPORT_DIR = "export"

    fun sessionRoot(cacheDir: File, sessionId: String): File {
        return File(File(cacheDir, SESSIONS_DIR), sessionId)
    }

    fun originalPagesDir(cacheDir: File, sessionId: String): File {
        return File(sessionRoot(cacheDir, sessionId), ORIGINAL_DIR)
    }

    fun originalPdfFile(cacheDir: File, sessionId: String): File {
        return File(sessionRoot(cacheDir, sessionId), "original.pdf")
    }

    fun previewPagesDir(cacheDir: File, sessionId: String): File {
        return File(sessionRoot(cacheDir, sessionId), PREVIEW_DIR)
    }

    fun exportDir(cacheDir: File, sessionId: String): File {
        return File(sessionRoot(cacheDir, sessionId), EXPORT_DIR)
    }
}
