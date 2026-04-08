package com.example.pixelscanner.domain

import android.content.Context
import java.io.File

interface TempFileRepository {
    fun createTempFile(
        context: Context,
        prefix: String,
        suffix: String,
    ): File
}
