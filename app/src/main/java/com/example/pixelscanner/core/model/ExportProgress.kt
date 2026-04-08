package com.example.pixelscanner.core.model

data class ExportProgress(
    val overall: ExportOverallProgress = ExportOverallProgress(),
    val page: ExportPageProgress? = null,
) {
    fun displayLabel(): String {
        val overallLabel = if (overall.total > 0) {
            "${overall.completed}/${overall.total}"
        } else {
            null
        }
        val pageLabel = page?.let {
            "page ${it.completed}/${it.total}"
        }
        return when {
            overallLabel != null && pageLabel != null -> "$overallLabel ($pageLabel)"
            overallLabel != null -> overallLabel
            pageLabel != null -> pageLabel
            else -> "0/0"
        }
    }
}

data class ExportOverallProgress(
    val completed: Int = 0,
    val total: Int = 0,
)

data class ExportPageProgress(
    val completed: Int = 0,
    val total: Int = 0,
)
