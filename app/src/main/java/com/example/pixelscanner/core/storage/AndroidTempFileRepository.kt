package com.example.pixelscanner.core.storage

import com.example.pixelscanner.domain.TempFileRepository
import java.io.File

class AndroidTempFileRepository : TempFileRepository {
    override fun createTempFile(
        cacheDir: File,
        prefix: String,
        suffix: String,
    ): File {
        return File.createTempFile(prefix, suffix, cacheDir)
    }
}
