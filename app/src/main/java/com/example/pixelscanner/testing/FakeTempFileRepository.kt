package com.example.pixelscanner.testing

import android.content.Context
import com.example.pixelscanner.domain.TempFileRepository
import java.io.File

class FakeTempFileRepository : TempFileRepository {
    val createdFiles = mutableListOf<File>()

    override fun createTempFile(
        context: Context,
        prefix: String,
        suffix: String,
    ): File {
        val file = File(context.cacheDir, "$prefix$suffix")
        createdFiles += file
        return file
    }
}
