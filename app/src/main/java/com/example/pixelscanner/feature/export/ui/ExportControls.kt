package com.example.pixelscanner.feature.export.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.pixelscanner.R
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.asString
import com.example.pixelscanner.ui.theme.PixelScannerColorTokens
import com.example.pixelscanner.ui.theme.PixelScannerSpacingTokens
import com.example.pixelscanner.ui.theme.rememberPixelScannerTypographyTokens

@Composable
fun ExportSaveButton(
    exportState: ExportUiState,
    onSaveAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = rememberPixelScannerTypographyTokens()
    val context = LocalContext.current
    Button(
        onClick = onSaveAll,
        enabled = !exportState.isExporting,
        modifier = modifier,
    ) {
        Text(
            text = when {
                exportState.isExporting -> {
                    exportState.progressText?.asString(context)
                        ?: stringResource(id = R.string.export_save_button_saving)
                }

                exportState.canRetry && exportState.format == ExportFormat.IMAGE ->
                    stringResource(id = R.string.export_retry_button_jpeg)

                exportState.canRetry ->
                    stringResource(id = R.string.export_retry_button_pdf)

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
fun ExportCancelButton(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = rememberPixelScannerTypographyTokens()
    TextButton(
        onClick = onCancel,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(id = R.string.export_cancel_action),
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
            modifier = Modifier.testTag("review_export_format_jpeg"),
            text = { Text(stringResource(id = R.string.export_format_jpeg), style = typography.label) },
        )
        Tab(
            selected = selectedFormat == ExportFormat.PDF,
            onClick = { onSelectFormat(ExportFormat.PDF) },
            modifier = Modifier.testTag("review_export_format_pdf"),
            text = { Text(stringResource(id = R.string.export_format_pdf), style = typography.label) },
        )
    }
}
