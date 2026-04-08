package com.example.pixelscanner.feature.review.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pixelscanner.R
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ProcessingState
import com.example.pixelscanner.feature.export.ui.ExportFormatTabs
import com.example.pixelscanner.feature.export.ui.ExportSaveButton
import com.example.pixelscanner.feature.review.ReviewViewModel
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.ChangeExportFormat
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.DeletePage
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.PageChanged
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RequestDiscardSession
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RequestRescan
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RequestScanMore
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RestorePage
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.SaveAll
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.ToggleEnhancement
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = viewModel(),
    onExportSuccess: (ExportFormat) -> Unit,
    onRescan: () -> Unit,
    onDiscardSession: () -> Unit,
) {
    var showRescanConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardConfirmDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        showDiscardConfirmDialog = true
    }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedPage = uiState.selectedPage
    val pagerState = rememberPagerState(
        initialPage = uiState.currentPage,
        pageCount = { uiState.pages.size }
    )
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReviewUiEffect.ExportSucceeded -> onExportSuccess(effect.format)
                is ReviewUiEffect.NavigateBackToScanner -> onRescan()
                is ReviewUiEffect.NavigateBackToHome -> onDiscardSession()
                is ReviewUiEffect.ShowDeletedPageUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Page deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(RestorePage(effect.pageId))
                    }
                }

                is ReviewUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                viewModel.onEvent(PageChanged(pageIndex))
            }
    }

    LaunchedEffect(uiState.currentPage, uiState.pages.size) {
        if (uiState.hasPages && pagerState.currentPage != uiState.currentPage) {
            pagerState.scrollToPage(uiState.currentPage)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            // 1. Navigation Row (Rescan & Save All)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.onEvent(RequestScanMore) },
                        enabled = uiState.hasPages && !uiState.export.isExporting && !uiState.isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF541765)
                        )
                    ) {
                        Text(stringResource(id = R.string.review_action_scan_more))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { showRescanConfirmDialog = true },
                        enabled = !uiState.export.isExporting && !uiState.isProcessing,
                    ) {
                        Text(stringResource(id = R.string.review_action_rescan))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { showDiscardConfirmDialog = true },
                        enabled = !uiState.export.isExporting && !uiState.isProcessing,
                    ) {
                        Text(stringResource(id = R.string.review_action_discard_session))
                    }
                }

                if (uiState.hasPages) {
                    ExportSaveButton(
                        exportState = uiState.export,
                        onSaveAll = { viewModel.onEvent(SaveAll(context)) },
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }

            if (uiState.hasPages) {
                // 2, 3, 4. Centered Info & Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Review",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "(${uiState.currentPage + 1}/${uiState.pages.size})",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Magic Wand (Enhance)
                        IconButton(
                            onClick = {
                                viewModel.onEvent(
                                    ToggleEnhancement(
                                        pageIndex = uiState.currentPage,
                                        context = context,
                                    )
                                )
                            },
                            enabled = selectedPage?.processingState != ProcessingState.PROCESSING
                        ) {
                            if (selectedPage?.processingState == ProcessingState.PROCESSING) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Enhance",
                                    tint = if (uiState.selectedPage?.isEnhanced == true) Color.Yellow else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        // Delete
                        IconButton(
                            onClick = {
                                val pageToDelete = uiState.selectedPage
                                val indexToDelete = uiState.currentPage
                                if (pageToDelete != null) {
                                    viewModel.onEvent(DeletePage(indexToDelete))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Page",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }

            ExportFormatTabs(
                selectedFormat = uiState.export.format,
                onSelectFormat = { viewModel.onEvent(ChangeExportFormat(it)) },
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (uiState.hasPages) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                        // Guard against index out of bounds during deletion animation
                        if (pageIndex < uiState.pages.size) {
                            val page = uiState.pages[pageIndex]
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = page.displayUri,
                                    contentDescription = "Scanned Page $pageIndex",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                if (page.processingState == ProcessingState.PROCESSING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = Color.White,
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text("No images to review", color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    if (showRescanConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRescanConfirmDialog = false },
            title = { Text(stringResource(id = R.string.review_rescan_confirm_title)) },
            text = { Text(stringResource(id = R.string.review_rescan_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRescanConfirmDialog = false
                        viewModel.onEvent(RequestRescan)
                    }
                ) {
                    Text(stringResource(id = R.string.review_rescan_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescanConfirmDialog = false }) {
                    Text(stringResource(id = R.string.review_cancel_action))
                }
            },
        )
    }

    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = { Text(stringResource(id = R.string.review_discard_confirm_title)) },
            text = { Text(stringResource(id = R.string.review_discard_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        viewModel.onEvent(RequestDiscardSession)
                    }
                ) {
                    Text(stringResource(id = R.string.review_discard_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text(stringResource(id = R.string.review_cancel_action))
                }
            },
        )
    }
}
