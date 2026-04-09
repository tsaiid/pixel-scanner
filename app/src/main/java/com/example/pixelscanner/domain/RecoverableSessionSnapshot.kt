package com.example.pixelscanner.domain

import com.example.pixelscanner.core.model.ScanSessionMetadata
import java.io.File

data class RecoverableSessionSnapshot(
    val metadata: ScanSessionMetadata,
    val pageFiles: List<File>,
    val originalPdfFile: File?,
    val missingPageFileNames: List<String> = emptyList(),
    val missingOriginalPdf: Boolean = false,
) {
    val isPartial: Boolean
        get() = missingPageFileNames.isNotEmpty() || missingOriginalPdf
}
