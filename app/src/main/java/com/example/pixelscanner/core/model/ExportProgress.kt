package com.example.pixelscanner.core.model

import com.example.pixelscanner.R

data class ExportProgress(
    val overall: ExportOverallProgress = ExportOverallProgress(),
    val page: ExportPageProgress? = null,
) {
    fun displayLabel(): UiText {
        return when {
            overall.total > 0 && page != null -> {
                uiTextResource(
                    R.string.export_progress_overall_with_page,
                    overall.completed,
                    overall.total,
                    page.completed,
                    page.total,
                )
            }
            overall.total > 0 -> uiTextResource(
                R.string.export_progress_overall,
                overall.completed,
                overall.total,
            )
            page != null -> uiTextResource(
                R.string.export_progress_page,
                page.completed,
                page.total,
            )
            else -> uiTextResource(R.string.export_progress_overall, 0, 0)
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
