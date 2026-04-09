package com.example.pixelscanner.testing

import com.example.pixelscanner.domain.TempFileRepository
import java.io.File

class FakeTempFileRepository : TempFileRepository {
    val createdFiles = mutableListOf<File>()

    override fun createTempFile(
        cacheDir: File,
        prefix: String,
        suffix: String,
    ): File {
        val file = File(cacheDir, "$prefix$suffix")
        createdFiles += file
        return file
    }
}
