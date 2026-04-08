package com.example.pixelscanner.feature.export.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pixelscanner.core.model.ExportFormat

@Composable
fun ExportSaveButton(
    exportState: ExportUiState,
    onSaveAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onSaveAll,
        enabled = !exportState.isExporting,
        modifier = modifier,
    ) {
        Text(
            when {
                exportState.isExporting -> {
                    exportState.progressText ?: "Saving..."
                }

                else -> "Save All"
            }
        )
    }
}

@Composable
fun ExportFormatTabs(
    selectedFormat: ExportFormat,
    onSelectFormat: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = if (selectedFormat == ExportFormat.IMAGE) 0 else 1,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Tab(
            selected = selectedFormat == ExportFormat.IMAGE,
            onClick = { onSelectFormat(ExportFormat.IMAGE) },
            text = { Text("JPEG") },
        )
        Tab(
            selected = selectedFormat == ExportFormat.PDF,
            onClick = { onSelectFormat(ExportFormat.PDF) },
            text = { Text("PDF") },
        )
    }
}
