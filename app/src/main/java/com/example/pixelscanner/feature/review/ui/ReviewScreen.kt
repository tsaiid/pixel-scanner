package com.example.pixelscanner.feature.review.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pixelscanner.R
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ProcessingState
import com.example.pixelscanner.core.model.ExportPostActionTarget
import com.example.pixelscanner.feature.export.ui.ExportFormatTabs
import com.example.pixelscanner.feature.export.ui.ExportSaveButton
import com.example.pixelscanner.feature.review.ReviewViewModel
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.ClearStatusMessage
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
    onExportSuccess: (ExportFormat, ExportPostActionTarget) -> Unit,
    onNavigateToScanner: (ScannerNavigationReason) -> Unit,
    onNavigateToHome: (HomeNavigationReason) -> Unit,
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
                is ReviewUiEffect.ExportSucceeded -> onExportSuccess(effect.format, effect.target)
                is ReviewUiEffect.NavigateToScanner -> onNavigateToScanner(effect.reason)
                is ReviewUiEffect.NavigateToHome -> onNavigateToHome(effect.reason)
                is ReviewUiEffect.ShowDeletedPageUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.review_deleted_page_message),
                        actionLabel = context.getString(R.string.review_deleted_page_undo_action),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(RestorePage(effect.pageId))
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.statusMessage?.id) {
        val statusMessage = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = statusMessage.text,
            duration = SnackbarDuration.Short,
        )
        viewModel.onEvent(ClearStatusMessage)
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
                        text = stringResource(id = R.string.review_screen_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(
                            id = R.string.review_screen_page_counter,
                            uiState.currentPage + 1,
                            uiState.pages.size
                        ),
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
                                        contentDescription = stringResource(id = R.string.review_enhance_content_description),
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
                                contentDescription = stringResource(id = R.string.review_delete_page_content_description),
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

            if (uiState.hasPages) {
                val thumbnailListState = rememberLazyListState()

                LaunchedEffect(uiState.currentPage, uiState.pages.size) {
                    if (uiState.pages.isNotEmpty()) {
                        val targetIndex = uiState.currentPage.coerceIn(0, uiState.pages.lastIndex)
                        thumbnailListState.scrollToItem(targetIndex)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                            // Guard against index out of bounds during deletion animation
                            if (pageIndex < uiState.pages.size) {
                                val page = uiState.pages[pageIndex]
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    AsyncImage(
                                        model = page.displayUri,
                                        contentDescription = stringResource(
                                            id = R.string.review_scanned_page_content_description,
                                            pageIndex + 1
                                        ),
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
                    }

                    ReviewThumbnailStrip(
                        pages = uiState.pages,
                        currentPage = uiState.currentPage,
                        listState = thumbnailListState,
                        onPageSelected = { pageIndex ->
                            viewModel.onEvent(PageChanged(pageIndex))
                        },
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(id = R.string.review_empty_state_message),
                        color = Color.White,
                    )
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

@Composable
private fun ReviewThumbnailStrip(
    pages: List<ReviewPageUiModel>,
    currentPage: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPageSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.review_thumbnail_strip_title),
            color = Color.LightGray,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("review_thumbnail_strip"),
            contentPadding = PaddingValues(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(
                items = pages,
                key = { _, page -> page.id },
            ) { pageIndex, page ->
                ReviewThumbnailItem(
                    pageIndex = pageIndex,
                    totalPages = pages.size,
                    page = page,
                    selected = pageIndex == currentPage,
                    onClick = { onPageSelected(pageIndex) },
                )
            }
        }
    }
}

@Composable
private fun ReviewThumbnailItem(
    pageIndex: Int,
    totalPages: Int,
    page: ReviewPageUiModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val thumbnailShape = RoundedCornerShape(12.dp)
    val borderColor = if (selected) Color(0xFF9B82FF) else Color(0xFF3A3A3A)
    val contentDescription = if (selected) {
        stringResource(
            id = R.string.review_thumbnail_selected_content_description,
            pageIndex + 1,
            totalPages,
        )
    } else {
        stringResource(
            id = R.string.review_thumbnail_content_description,
            pageIndex + 1,
            totalPages,
        )
    }

    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 86.dp)
            .clip(thumbnailShape)
            .border(width = 2.dp, color = borderColor, shape = thumbnailShape)
            .background(Color(0xFF1A1A1A))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("review_thumbnail_$pageIndex"),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = page.displayUri,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFF9B82FF) else Color(0xAA000000)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (pageIndex + 1).toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
