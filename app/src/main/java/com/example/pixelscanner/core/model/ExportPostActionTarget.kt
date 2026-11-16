package com.example.pixelscanner.core.model

import android.net.Uri

data class ExportPostActionTarget(
    val outputUris: List<Uri>,
    val mimeType: String,
) {
    val primaryUri: Uri?
        get() = outputUris.firstOrNull()
}

fun ExportFormat.mimeType(): String {
    return when (this) {
        ExportFormat.IMAGE -> "image/jpeg"
        ExportFormat.PDF -> "application/pdf"
    }
}

fun ExportResult.toPostActionTarget(format: ExportFormat): ExportPostActionTarget? {
    val outputUris = when (this) {
        is ExportResult.Success -> outputUris.ifEmpty { listOfNotNull(outputUri) }
        is ExportResult.PartialSuccess -> outputUris
        is ExportResult.Failure -> emptyList()
        ExportResult.Cancelled -> emptyList()
    }

    if (outputUris.isEmpty()) {
        return null
    }

    return ExportPostActionTarget(
        outputUris = outputUris,
        mimeType = format.mimeType(),
    )
}
