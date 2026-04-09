package com.example.pixelscanner.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.pixelscanner.R
import com.example.pixelscanner.core.diagnostics.DebugLog
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportPostActionTarget
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.core.model.UiText
import com.example.pixelscanner.core.model.asString
import com.example.pixelscanner.core.model.uiTextResource
import com.example.pixelscanner.PixelScannerApp
import com.example.pixelscanner.domain.RestoreSessionOutcome
import com.example.pixelscanner.feature.review.ui.HomeNavigationReason
import com.example.pixelscanner.feature.home.HomeViewModel
import com.example.pixelscanner.feature.review.ReviewViewModel
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RequestScanMore
import com.example.pixelscanner.feature.review.ui.ReviewScreen
import com.example.pixelscanner.feature.review.ui.ScannerNavigationReason
import com.example.pixelscanner.feature.scanner.ui.ScannerScreen
import kotlinx.coroutines.launch
import java.util.ArrayList

@Composable
fun AppNavigation(
    startInBenchmarkReviewMode: Boolean = false,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val appContainer = (context.applicationContext as PixelScannerApp).appContainer
    val appFlowViewModel: AppFlowViewModel = viewModel(
        factory = AppFlowViewModelFactory(owner = context as SavedStateRegistryOwner),
    )
    val getRecoverableSessionUseCase = appContainer.getRecoverableSessionUseCase
    val restoreSessionUseCase = appContainer.restoreSessionUseCase
    val discardRecoverableSessionUseCase = appContainer.discardRecoverableSessionUseCase
    val homeViewModelFactory = appContainer.createHomeViewModelFactory(
        owner = context as SavedStateRegistryOwner,
    )
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
    val reviewViewModelFactory = appContainer.createReviewViewModelFactory(
        owner = context as SavedStateRegistryOwner,
    )
    val reviewViewModel: ReviewViewModel = viewModel(factory = reviewViewModelFactory)
    val appFlowUiState by appFlowViewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val reviewUiState by reviewViewModel.uiState.collectAsState()
    val benchmarkReviewSession = remember(startInBenchmarkReviewMode) {
        if (startInBenchmarkReviewMode) {
            createBenchmarkReviewSession(context)
        } else {
            null
        }
    }
    val startDestination = if (startInBenchmarkReviewMode) {
        AppNavigationContract.reviewRoute
    } else {
        AppNavigationContract.homeRoute
    }
    val showSnackbarMessage: (UiText) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message.asString(context))
        }
    }

    fun logScannerReturn(
        status: String,
        startedAtMs: Long,
        scanResult: ScanResultPayload? = null,
    ) {
        DebugLog.event(
            name = "scanner_return",
            "status" to status,
            "durationMs" to (SystemClock.elapsedRealtime() - startedAtMs),
            "pageCount" to (scanResult?.pageUris?.size ?: 0),
            "hasPdf" to (scanResult?.pdfUri != null),
        )
    }

    LaunchedEffect(homeUiState.shouldAttemptRecoverableSessionRestore, homeUiState.shouldAutoStartScanner) {
        if (!homeUiState.shouldAttemptRecoverableSessionRestore || homeUiState.shouldAutoStartScanner) {
            return@LaunchedEffect
        }

        val recoverableSession = runCatching { getRecoverableSessionUseCase() }.getOrElse { error ->
            DebugLog.error("recoverable_session_lookup_failed", error)
            null
        }

        homeViewModel.setShouldAttemptRecoverableSessionRestore(false)
        homeViewModel.setRecoverableSession(recoverableSession)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                composable(AppNavigationContract.homeRoute) {
                    ScannerScreen(
                        homeUiState = homeUiState,
                        onScanResult = { scanResult ->
                            val startedAtMs = SystemClock.elapsedRealtime()
                            reviewViewModel.setScanResult(scanResult)
                            navController.navigateResettingToHome(AppNavigationContract.reviewRoute)
                            logScannerReturn("success", startedAtMs, scanResult)
                        },
                        onScanCancelled = {
                            val startedAtMs = SystemClock.elapsedRealtime()
                            homeViewModel.setShouldAutoStartScanner(false)
                            if (reviewUiState.isAwaitingScanMoreResult) {
                                reviewViewModel.clearScanMorePending()
                                navController.navigateResettingToHome(AppNavigationContract.reviewRoute)
                            }
                            logScannerReturn("cancelled", startedAtMs)
                        },
                        onFeedbackMessage = showSnackbarMessage,
                        onRestoreRecoverableSession = { sessionId ->
                            coroutineScope.launch {
                                val outcome = runCatching { restoreSessionUseCase(sessionId) }.getOrElse { error ->
                                    DebugLog.error("restore_session_failed", error)
                                    RestoreSessionOutcome.Unavailable
                                }
                                homeViewModel.setShouldAttemptRecoverableSessionRestore(false)
                                homeViewModel.clearRecoverableSession()
                                when (outcome) {
                                    is RestoreSessionOutcome.Restored -> {
                                        reviewViewModel.setRestoredSession(outcome.result.session)
                                        if (outcome.result.isPartial) {
                                            showSnackbarMessage(
                                                uiTextResource(R.string.restore_session_partial_success_toast),
                                            )
                                        }
                                        navController.navigateResettingToHome(AppNavigationContract.reviewRoute)
                                    }

                                    RestoreSessionOutcome.Unavailable -> {
                                        showSnackbarMessage(
                                            uiTextResource(R.string.restore_session_unavailable_toast),
                                        )
                                    }
                                }
                            }
                        },
                        onDiscardRecoverableSession = { sessionId ->
                            coroutineScope.launch {
                                runCatching { discardRecoverableSessionUseCase(sessionId) }
                                    .onFailure { error -> DebugLog.error("discard_recoverable_session_failed", error) }
                                homeViewModel.setShouldAttemptRecoverableSessionRestore(false)
                                homeViewModel.clearRecoverableSession()
                            }
                        }
                    )
                }

                composable(AppNavigationContract.reviewRoute) {
                    if (benchmarkReviewSession != null && !reviewUiState.hasSession) {
                        LaunchedEffect(benchmarkReviewSession.id) {
                            reviewViewModel.setRestoredSession(benchmarkReviewSession)
                        }
                    }
                    ReviewScreen(
                        viewModel = reviewViewModel,
                        onExportSuccess = { format, target ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showSnackbarMessage(
                                if (format == ExportFormat.PDF) {
                                    uiTextResource(R.string.export_saved_pdf_toast)
                                } else {
                                    uiTextResource(R.string.export_saved_gallery_toast)
                                },
                            )
                            appFlowViewModel.setPendingExportAction(target)
                        },
                        onNavigateToScanner = { reason ->
                            when (reason) {
                                ScannerNavigationReason.RESCAN,
                                ScannerNavigationReason.SCAN_MORE -> {
                                    homeViewModel.setShouldAutoStartScanner(true)
                                    homeViewModel.setShouldAttemptRecoverableSessionRestore(false)
                                    homeViewModel.clearRecoverableSession()
                                    navController.navigateResettingToHome(AppNavigationContract.homeRoute)
                                }
                            }
                        },
                        onNavigateToHome = { reason ->
                            when (reason) {
                                HomeNavigationReason.DISCARD_SESSION -> {
                                    homeViewModel.setShouldAutoStartScanner(false)
                                    homeViewModel.setShouldAttemptRecoverableSessionRestore(false)
                                    homeViewModel.clearRecoverableSession()
                                    navController.navigateResettingToHome(AppNavigationContract.homeRoute)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    appFlowUiState.pendingExportAction?.let { action ->
        AlertDialog(
            onDismissRequest = { appFlowViewModel.clearPendingExportAction() },
            title = { Text(text = context.getString(R.string.export_post_action_title)) },
            text = {
                Column {
                    Text(text = context.getString(R.string.export_post_action_message))
                    TextButton(
                        onClick = {
                            appFlowViewModel.clearPendingExportAction()
                            reviewViewModel.onEvent(RequestScanMore)
                        }
                    ) {
                        Text(text = context.getString(R.string.export_post_action_continue))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.openExportTarget(action.toTarget(), showSnackbarMessage)
                        appFlowViewModel.clearPendingExportAction()
                    }
                ) {
                    Text(text = context.getString(R.string.export_post_action_open))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        context.shareExportTarget(action.toTarget(), showSnackbarMessage)
                        appFlowViewModel.clearPendingExportAction()
                    }
                ) {
                    Text(text = context.getString(R.string.export_post_action_share))
                }
            },
        )
    }
}

private fun createBenchmarkReviewSession(context: Context): ScanSession {
    val pageUri = Uri.parse(
        "android.resource://${context.packageName}/${R.mipmap.ic_launcher_foreground}",
    )
    return ScanSession(
        id = "benchmark_review_session",
        pages = List(6) { index ->
            ScannedPage(
                id = "benchmark_review_page_$index",
                originalSourceUri = pageUri,
            )
        },
    )
}

private fun NavHostController.navigateResettingToHome(route: String) {
    navigate(route) {
        popUpTo(AppNavigationContract.homeRoute) {
            inclusive = false
        }
        launchSingleTop = true
    }
}

private fun Context.openExportTarget(
    target: ExportPostActionTarget,
    onErrorMessage: (UiText) -> Unit,
) {
    val primaryUri = target.primaryUri ?: return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(primaryUri, target.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(contentResolver, "export", primaryUri)
    }
    runCatching {
        startActivity(Intent.createChooser(intent, getString(R.string.export_post_action_open)))
    }.onFailure {
        onErrorMessage(uiTextResource(R.string.export_post_action_open_failed))
    }
}

private fun Context.shareExportTarget(
    target: ExportPostActionTarget,
    onErrorMessage: (UiText) -> Unit,
) {
    val primaryUri = target.primaryUri ?: return
    val intent = if (target.outputUris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = target.mimeType
            putExtra(Intent.EXTRA_STREAM, primaryUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(contentResolver, "export", primaryUri)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = target.mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(target.outputUris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(contentResolver, "export", primaryUri)
        }
    }
    runCatching {
        startActivity(Intent.createChooser(intent, getString(R.string.export_post_action_share)))
    }.onFailure {
        onErrorMessage(uiTextResource(R.string.export_post_action_share_failed))
    }
}
