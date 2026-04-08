package com.example.pixelscanner.domain

import java.io.File

interface TempFileRepository {
    fun createTempFile(
        cacheDir: File,
        prefix: String,
        suffix: String,
    ): File
}
