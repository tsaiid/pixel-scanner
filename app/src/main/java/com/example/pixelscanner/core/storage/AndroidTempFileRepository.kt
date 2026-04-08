package com.example.pixelscanner.core.storage

import android.content.Context
import com.example.pixelscanner.domain.TempFileRepository
import java.io.File

class AndroidTempFileRepository : TempFileRepository {
    override fun createTempFile(
        context: Context,
        prefix: String,
        suffix: String,
    ): File {
        return File.createTempFile(prefix, suffix, context.cacheDir)
    }
}
