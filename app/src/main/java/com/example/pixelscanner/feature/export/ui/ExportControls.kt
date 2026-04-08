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
import com.example.pixelscanner.R
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.ui.theme.PixelScannerColorTokens
import com.example.pixelscanner.ui.theme.PixelScannerSpacingTokens
import com.example.pixelscanner.ui.theme.rememberPixelScannerTypographyTokens
import androidx.compose.ui.res.stringResource

@Composable
fun ExportSaveButton(
    exportState: ExportUiState,
    onSaveAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = rememberPixelScannerTypographyTokens()
    Button(
        onClick = onSaveAll,
        enabled = !exportState.isExporting,
        modifier = modifier,
    ) {
        Text(
            text = when {
                exportState.isExporting -> {
                    exportState.progressText ?: stringResource(id = R.string.export_save_button_saving)
                }

                exportState.format == ExportFormat.IMAGE ->
                    stringResource(id = R.string.export_save_button_jpeg)

                else ->
                    stringResource(id = R.string.export_save_button_pdf)
            },
            style = typography.label,
        )
    }
}

@Composable
fun ExportFormatTabs(
    selectedFormat: ExportFormat,
    onSelectFormat: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = rememberPixelScannerTypographyTokens()
    TabRow(
        selectedTabIndex = if (selectedFormat == ExportFormat.IMAGE) 0 else 1,
        containerColor = Color.Transparent,
        contentColor = PixelScannerColorTokens.reviewTextPrimary,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = PixelScannerSpacingTokens.lg,
                vertical = PixelScannerSpacingTokens.sm,
            ),
    ) {
        Tab(
            selected = selectedFormat == ExportFormat.IMAGE,
            onClick = { onSelectFormat(ExportFormat.IMAGE) },
            text = { Text(stringResource(id = R.string.export_format_jpeg), style = typography.label) },
        )
        Tab(
            selected = selectedFormat == ExportFormat.PDF,
            onClick = { onSelectFormat(ExportFormat.PDF) },
            text = { Text(stringResource(id = R.string.export_format_pdf), style = typography.label) },
        )
    }
}
