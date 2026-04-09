package com.example.pixelscanner.feature.review.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pixelscanner.R
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ProcessingState
import com.example.pixelscanner.core.model.ExportPostActionTarget
import com.example.pixelscanner.feature.export.ui.ExportFormatTabs
import com.example.pixelscanner.feature.export.ui.ExportCancelButton
import com.example.pixelscanner.feature.export.ui.ExportFallbackAction
import com.example.pixelscanner.feature.export.ui.ExportSaveButton
import com.example.pixelscanner.feature.review.ReviewViewModel
import com.example.pixelscanner.core.model.asString
import com.example.pixelscanner.ui.theme.PixelScannerColorTokens
import com.example.pixelscanner.ui.theme.PixelScannerElevationTokens
import com.example.pixelscanner.ui.theme.PixelScannerShapeTokens
import com.example.pixelscanner.ui.theme.PixelScannerSpacingTokens
import com.example.pixelscanner.ui.theme.PixelScannerWindowInsetsTokens
import com.example.pixelscanner.ui.theme.rememberPixelScannerTypographyTokens
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.ChangeExportFormat
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.CancelExport
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.DeletePage
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.PageChanged
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.MovePageLeft
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.MovePageRight
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RetryExport
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RetryEnhancement
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RequestDiscardSession
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RequestRescan
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RequestScanMore
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RestorePage
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.SaveAll
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.ToggleEnhancement
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
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
    val commandBarState = ReviewCommandBarState(
        hasPages = uiState.hasPages,
        exportState = uiState.export,
        isProcessing = uiState.isProcessing,
    )
    val workspaceState = ReviewWorkspaceState(
        hasPages = uiState.hasPages,
        hasSession = uiState.hasSession,
        pages = uiState.pages,
        currentPage = uiState.currentPage,
        selectedPage = selectedPage,
        exportState = uiState.export,
        isProcessing = uiState.isProcessing,
    )
    val pagerState = rememberPagerState(
        initialPage = uiState.currentPage,
        pageCount = { uiState.pages.size }
    )
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReviewUiEffect.ShowStatusMessage -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message.asString(context),
                        duration = SnackbarDuration.Short,
                    )
                }
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
        containerColor = PixelScannerColorTokens.reviewSurface,
        contentWindowInsets = PixelScannerWindowInsetsTokens.safeDrawing(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PixelScannerColorTokens.reviewSurface)
                .semantics { testTagsAsResourceId = true }
        ) {
            ReviewCommandBar(
                state = commandBarState,
                onScanMore = { viewModel.onEvent(RequestScanMore) },
                onRescan = { showRescanConfirmDialog = true },
                onDiscard = { showDiscardConfirmDialog = true },
                onSaveAll = {
                    viewModel.onEvent(
                        if (uiState.export.canRetry) RetryExport(context) else SaveAll(context),
                    )
                },
                onCancelExport = { viewModel.onEvent(CancelExport) },
                onOpenSettings = { context.openAppSettings() },
            )

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                ReviewWorkspace(
                    state = workspaceState,
                    pagerState = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    onExportFormatChange = { viewModel.onEvent(ChangeExportFormat(it)) },
                    onRetryEnhancement = selectedPage?.let { currentPage ->
                        {
                            viewModel.onEvent(
                                RetryEnhancement(
                                    pageId = currentPage.id,
                                    context = context,
                                ),
                            )
                        }
                    },
                    onToggleEnhancement = {
                        viewModel.onEvent(
                            ToggleEnhancement(
                                pageIndex = uiState.currentPage,
                                context = context,
                            )
                        )
                    },
                    onMoveLeft = { viewModel.onEvent(MovePageLeft) },
                    onMoveRight = { viewModel.onEvent(MovePageRight) },
                    onDelete = {
                        val pageToDelete = uiState.selectedPage
                        val indexToDelete = uiState.currentPage
                        if (pageToDelete != null) {
                            viewModel.onEvent(DeletePage(indexToDelete))
                        }
                    },
                    onScanMore = { viewModel.onEvent(RequestScanMore) },
                    onDiscard = { showDiscardConfirmDialog = true },
                    onPageSelected = { pageIndex ->
                        viewModel.onEvent(PageChanged(pageIndex))
                    },
                )
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

private fun android.content.Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        startActivity(intent)
    }
}

@Immutable
private data class ReviewCommandBarState(
    val hasPages: Boolean,
    val exportState: com.example.pixelscanner.feature.export.ui.ExportUiState,
    val isProcessing: Boolean,
)

@Immutable
internal data class ReviewWorkspaceState(
    val hasPages: Boolean,
    val hasSession: Boolean,
    val pages: List<ReviewPageUiModel>,
    val currentPage: Int,
    val selectedPage: ReviewPageUiModel?,
    val exportState: com.example.pixelscanner.feature.export.ui.ExportUiState,
    val isProcessing: Boolean,
)

@Immutable
internal data class ReviewThumbnailUiModel(
    val id: String,
    val displayUri: android.net.Uri,
)

@Immutable
internal data class ReviewRecompositionTrace(
    val onPagerRecomposed: (() -> Unit)? = null,
    val onThumbnailStripRecomposed: (() -> Unit)? = null,
)

@Composable
private fun ReviewCommandBar(
    state: ReviewCommandBarState,
    onScanMore: () -> Unit,
    onRescan: () -> Unit,
    onDiscard: () -> Unit,
    onSaveAll: () -> Unit,
    onCancelExport: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = PixelScannerSpacingTokens.lg,
                vertical = PixelScannerSpacingTokens.sm,
            )
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onScanMore,
                enabled = state.hasPages && !state.exportState.isExporting && !state.isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PixelScannerColorTokens.reviewActionContainer,
                    contentColor = PixelScannerColorTokens.reviewActionContent,
                )
            ) {
                Text(stringResource(id = R.string.review_action_scan_more))
            }
            Spacer(modifier = Modifier.width(PixelScannerSpacingTokens.sm))
            TextButton(
                onClick = onRescan,
                enabled = !state.exportState.isExporting && !state.isProcessing,
            ) {
                Text(stringResource(id = R.string.review_action_rescan))
            }
            Spacer(modifier = Modifier.width(PixelScannerSpacingTokens.sm))
            TextButton(
                onClick = onDiscard,
                enabled = !state.exportState.isExporting && !state.isProcessing,
            ) {
                Text(stringResource(id = R.string.review_action_discard_session))
            }
        }

        if (state.hasPages) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExportSaveButton(
                    exportState = state.exportState,
                    onSaveAll = onSaveAll,
                    modifier = Modifier.testTag("review_save_button"),
                )
                if (state.exportState.isExporting) {
                    Spacer(modifier = Modifier.width(PixelScannerSpacingTokens.sm))
                    ExportCancelButton(onCancel = onCancelExport)
                } else if (state.exportState.fallbackAction == ExportFallbackAction.DISCARD_AND_RESCAN) {
                    Spacer(modifier = Modifier.width(PixelScannerSpacingTokens.sm))
                    TextButton(
                        onClick = onRescan,
                        modifier = Modifier.testTag("review_export_fallback_rescan"),
                    ) {
                        Text(stringResource(id = R.string.review_action_rescan))
                    }
                } else if (state.exportState.fallbackAction == ExportFallbackAction.OPEN_SETTINGS) {
                    Spacer(modifier = Modifier.width(PixelScannerSpacingTokens.sm))
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("review_export_fallback_settings"),
                    ) {
                        Text(stringResource(id = R.string.export_fallback_open_settings))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReviewWorkspace(
    state: ReviewWorkspaceState,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onExportFormatChange: (ExportFormat) -> Unit,
    onRetryEnhancement: (() -> Unit)?,
    onToggleEnhancement: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit,
    onScanMore: () -> Unit,
    onDiscard: () -> Unit,
    onPageSelected: (Int) -> Unit,
    recompositionTrace: ReviewRecompositionTrace? = null,
    modifier: Modifier = Modifier,
) {
    val typography = rememberPixelScannerTypographyTokens()
    if (state.hasPages) {
        val thumbnailListState = rememberLazyListState()
        val thumbnailPages = remember(state.pages) {
            state.pages.map { page ->
                ReviewThumbnailUiModel(
                    id = page.id,
                    displayUri = page.displayUri,
                )
            }
        }

        LaunchedEffect(state.currentPage, state.pages.size) {
            if (state.pages.isNotEmpty()) {
                val targetIndex = state.currentPage.coerceIn(0, state.pages.lastIndex)
                thumbnailListState.scrollToItem(targetIndex)
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.review_screen_title),
                color = PixelScannerColorTokens.reviewTextPrimary,
                style = typography.title,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(
                    id = R.string.review_screen_page_counter,
                    state.currentPage + 1,
                    state.pages.size,
                ),
                color = PixelScannerColorTokens.reviewTextSecondary,
                style = typography.body,
                modifier = Modifier.padding(top = PixelScannerSpacingTokens.xxs),
            )

            ReviewPageStatusChip(
                page = state.selectedPage,
                pageIndex = state.currentPage,
                onRetryEnhancement = onRetryEnhancement,
                modifier = Modifier.padding(top = PixelScannerSpacingTokens.reviewThumbnailSpacing),
            )

            Row(
                modifier = Modifier.padding(top = PixelScannerSpacingTokens.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = onToggleEnhancement,
                    enabled = state.selectedPage?.processingState != ProcessingState.PROCESSING,
                    modifier = Modifier
                        .sizeIn(
                            minWidth = PixelScannerSpacingTokens.touchTarget,
                            minHeight = PixelScannerSpacingTokens.touchTarget,
                        )
                        .testTag("review_enhance_button"),
                ) {
                    if (state.selectedPage?.processingState == ProcessingState.PROCESSING) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(PixelScannerSpacingTokens.sm)
                                .size(PixelScannerSpacingTokens.reviewPageStatusIndicatorSize),
                            color = PixelScannerColorTokens.reviewTextPrimary,
                            strokeWidth = PixelScannerSpacingTokens.reviewPageStatusIndicatorStroke,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(id = R.string.review_enhance_content_description),
                            tint = if (state.selectedPage?.isEnhanced == true) {
                                PixelScannerColorTokens.brandAccent
                            } else {
                                PixelScannerColorTokens.reviewTextPrimary
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.width(PixelScannerSpacingTokens.md))

                IconButton(
                    onClick = onMoveLeft,
                    enabled = state.currentPage > 0 && !state.exportState.isExporting && !state.isProcessing,
                    modifier = Modifier.sizeIn(
                        minWidth = PixelScannerSpacingTokens.touchTarget,
                        minHeight = PixelScannerSpacingTokens.touchTarget,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = stringResource(id = R.string.review_move_page_left_content_description),
                        tint = PixelScannerColorTokens.reviewTextPrimary,
                    )
                }

                IconButton(
                    onClick = onMoveRight,
                    enabled = state.hasPages &&
                        state.currentPage < state.pages.lastIndex &&
                        !state.exportState.isExporting &&
                        !state.isProcessing,
                    modifier = Modifier.sizeIn(
                        minWidth = PixelScannerSpacingTokens.touchTarget,
                        minHeight = PixelScannerSpacingTokens.touchTarget,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = stringResource(id = R.string.review_move_page_right_content_description),
                        tint = PixelScannerColorTokens.reviewTextPrimary,
                    )
                }

                Spacer(modifier = Modifier.width(PixelScannerSpacingTokens.xxl))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.sizeIn(
                        minWidth = PixelScannerSpacingTokens.touchTarget,
                        minHeight = PixelScannerSpacingTokens.touchTarget,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.review_delete_page_content_description),
                        tint = PixelScannerColorTokens.reviewDelete,
                    )
                }
            }

            ExportFormatTabs(
                selectedFormat = state.exportState.format,
                onSelectFormat = onExportFormatChange,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                ReviewPagerSection(
                    pages = state.pages,
                    pagerState = pagerState,
                    onPagerRecomposed = recompositionTrace?.onPagerRecomposed,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )

                ReviewThumbnailStrip(
                    thumbnails = thumbnailPages,
                    currentPage = state.currentPage,
                    listState = thumbnailListState,
                    onPageSelected = onPageSelected,
                    onThumbnailStripRecomposed = recompositionTrace?.onThumbnailStripRecomposed,
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val emptyStateMessage = if (state.hasSession) {
                    R.string.review_empty_session_message
                } else {
                    R.string.review_empty_state_message
                }
                Text(
                    stringResource(id = emptyStateMessage),
                    color = PixelScannerColorTokens.reviewTextPrimary,
                )
                if (state.hasSession) {
                    Spacer(modifier = Modifier.height(PixelScannerSpacingTokens.lg))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PixelScannerSpacingTokens.sm),
                    ) {
                        Button(
                            onClick = onScanMore,
                            enabled = !state.exportState.isExporting && !state.isProcessing,
                            modifier = Modifier.testTag("review_empty_scan_more"),
                        ) {
                            Text(stringResource(id = R.string.review_action_scan_more))
                        }
                        TextButton(
                            onClick = onDiscard,
                            enabled = !state.exportState.isExporting && !state.isProcessing,
                            modifier = Modifier.testTag("review_empty_discard_session"),
                        ) {
                            Text(stringResource(id = R.string.review_action_discard_session))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewPagerSection(
    pages: List<ReviewPageUiModel>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPagerRecomposed: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    SideEffect {
        onPagerRecomposed?.invoke()
    }

    val context = LocalContext.current
    var previewViewportSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = modifier.onSizeChanged { previewViewportSize = it },
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
            if (pageIndex < pages.size) {
                val page = pages[pageIndex]
                val previewRequest = remember(page.displayUri, previewViewportSize) {
                    val viewportSize = previewViewportSize.takeIf { it.width > 0 && it.height > 0 }
                    if (viewportSize != null) {
                        buildReviewPreviewImageRequest(
                            context = context,
                            data = page.displayUri,
                            viewportSize = viewportSize,
                        )
                    } else {
                        null
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (previewRequest != null) {
                        AsyncImage(
                            model = previewRequest,
                            placeholder = ColorPainter(PixelScannerColorTokens.reviewSurfaceRaised),
                            contentDescription = stringResource(
                                id = R.string.review_scanned_page_content_description,
                                pageIndex + 1,
                            ),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    if (page.processingState == ProcessingState.PROCESSING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(PixelScannerSpacingTokens.reviewPageStatusIndicatorSize),
                            color = PixelScannerColorTokens.reviewTextPrimary,
                            strokeWidth = PixelScannerSpacingTokens.reviewPageStatusIndicatorStroke,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewThumbnailStrip(
    thumbnails: List<ReviewThumbnailUiModel>,
    currentPage: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPageSelected: (Int) -> Unit,
    onThumbnailStripRecomposed: (() -> Unit)?,
) {
    SideEffect {
        onThumbnailStripRecomposed?.invoke()
    }

    val density = LocalDensity.current
    val thumbnailSize = remember(density) {
        with(density) {
            IntSize(
                PixelScannerSpacingTokens.reviewThumbnailWidth.roundToPx(),
                PixelScannerSpacingTokens.reviewThumbnailHeight.roundToPx(),
            )
        }
    }
    val typography = rememberPixelScannerTypographyTokens()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelScannerColorTokens.reviewSurfaceRaised)
            .padding(
                horizontal = PixelScannerSpacingTokens.lg,
                vertical = PixelScannerSpacingTokens.md,
            ),
    ) {
        Text(
            text = stringResource(id = R.string.review_thumbnail_strip_title),
            color = PixelScannerColorTokens.reviewTextSecondary,
            style = typography.label,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(PixelScannerSpacingTokens.sm))
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("review_thumbnail_strip"),
            contentPadding = PaddingValues(end = PixelScannerSpacingTokens.lg),
            horizontalArrangement = Arrangement.spacedBy(PixelScannerSpacingTokens.reviewThumbnailSpacing),
        ) {
            itemsIndexed(
                items = thumbnails,
                key = { _, page -> page.id },
            ) { pageIndex, page ->
                ReviewThumbnailItem(
                    pageIndex = pageIndex,
                    totalPages = thumbnails.size,
                    page = page,
                    thumbnailSize = thumbnailSize,
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
    page: ReviewThumbnailUiModel,
    thumbnailSize: IntSize,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val typography = rememberPixelScannerTypographyTokens()
    val thumbnailShape = PixelScannerShapeTokens.thumbnail
    val borderColor = if (selected) {
        PixelScannerColorTokens.brandAccent
    } else {
        PixelScannerColorTokens.reviewThumbnailBorder
    }
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
            .size(
                width = PixelScannerSpacingTokens.reviewThumbnailWidth,
                height = PixelScannerSpacingTokens.reviewThumbnailHeight,
            )
            .clip(thumbnailShape)
            .border(
                width = PixelScannerSpacingTokens.reviewThumbnailBorderWidth,
                color = borderColor,
                shape = thumbnailShape,
            )
            .background(PixelScannerColorTokens.reviewThumbnailSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("review_thumbnail_$pageIndex"),
        contentAlignment = Alignment.Center,
    ) {
        val thumbnailRequest = remember(page.displayUri, thumbnailSize) {
            buildReviewThumbnailImageRequest(
                context = context,
                data = page.displayUri,
                thumbnailSize = thumbnailSize,
            )
        }
        AsyncImage(
            model = thumbnailRequest,
            placeholder = ColorPainter(PixelScannerColorTokens.reviewThumbnailSurface),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(PixelScannerSpacingTokens.reviewThumbnailBadgeInset)
                .size(PixelScannerSpacingTokens.reviewThumbnailBadgeSize)
                .clip(PixelScannerShapeTokens.pill)
                .background(
                    if (selected) {
                        PixelScannerColorTokens.brandAccent
                    } else {
                        PixelScannerColorTokens.reviewThumbnailBadge
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (pageIndex + 1).toString(),
                color = PixelScannerColorTokens.reviewTextPrimary,
                style = typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ReviewPageStatusChip(
    page: ReviewPageUiModel?,
    pageIndex: Int,
    onRetryEnhancement: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (page == null) return
    val typography = rememberPixelScannerTypographyTokens()

    val statusStyle = when {
        page.enhancementErrorMessage != null -> {
            ReviewPageStatusStyle(
                labelRes = R.string.review_page_status_failed,
                containerColor = PixelScannerColorTokens.reviewFailedContainer,
                contentColor = PixelScannerColorTokens.reviewFailedContent,
                borderColor = PixelScannerColorTokens.reviewFailedBorder,
            )
        }
        page.processingState == ProcessingState.PROCESSING -> {
            ReviewPageStatusStyle(
                labelRes = R.string.review_page_status_processing,
                containerColor = PixelScannerColorTokens.reviewProcessingContainer,
                contentColor = PixelScannerColorTokens.reviewProcessingContent,
                borderColor = PixelScannerColorTokens.reviewProcessingBorder,
            )
        }

        page.isEnhanced -> {
            ReviewPageStatusStyle(
                labelRes = R.string.review_page_status_enhanced,
                containerColor = PixelScannerColorTokens.reviewEnhancedContainer,
                contentColor = PixelScannerColorTokens.reviewEnhancedContent,
                borderColor = PixelScannerColorTokens.reviewEnhancedBorder,
            )
        }

        else -> {
            ReviewPageStatusStyle(
                labelRes = R.string.review_page_status_original,
                containerColor = PixelScannerColorTokens.reviewOriginalContainer,
                contentColor = PixelScannerColorTokens.reviewOriginalContent,
                borderColor = PixelScannerColorTokens.reviewOriginalBorder,
            )
        }
    }

    Surface(
        color = statusStyle.containerColor,
        contentColor = statusStyle.contentColor,
        shape = PixelScannerShapeTokens.pill,
        tonalElevation = PixelScannerElevationTokens.none,
        shadowElevation = PixelScannerElevationTokens.none,
        border = androidx.compose.foundation.BorderStroke(
            PixelScannerSpacingTokens.reviewStatusBorderWidth,
            statusStyle.borderColor,
        ),
        modifier = modifier.testTag("review_page_status"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PixelScannerSpacingTokens.sm),
            modifier = Modifier.padding(
                horizontal = PixelScannerSpacingTokens.md,
                vertical = PixelScannerSpacingTokens.xs,
            ),
        ) {
            if (page.enhancementErrorMessage != null) {
                Text(
                    text = stringResource(
                        id = R.string.review_page_status_failed_with_page,
                        pageIndex + 1,
                    ),
                    style = typography.label,
                )
                if (onRetryEnhancement != null) {
                    TextButton(onClick = onRetryEnhancement) {
                        Text(stringResource(id = R.string.review_retry_enhancement_action))
                    }
                }
            } else if (page.processingState == ProcessingState.PROCESSING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(PixelScannerSpacingTokens.reviewPageStatusChipIndicatorSize),
                    color = statusStyle.contentColor,
                    strokeWidth = PixelScannerSpacingTokens.reviewPageStatusChipIndicatorStroke,
                )
            }
            else {
                Text(
                    text = stringResource(id = statusStyle.labelRes),
                    style = typography.label,
                )
            }
        }
    }
}

private data class ReviewPageStatusStyle(
    val labelRes: Int,
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
)
