package com.example.pixelscanner.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.core.model.ExportPostActionTarget
import com.example.pixelscanner.PixelScannerApp
import com.example.pixelscanner.domain.RestoreSessionOutcome
import com.example.pixelscanner.feature.home.HomeViewModel
import com.example.pixelscanner.feature.review.ReviewViewModel
import com.example.pixelscanner.feature.review.ui.ReviewUiEvent.RequestScanMore
import com.example.pixelscanner.feature.review.ui.ReviewScreen
import com.example.pixelscanner.feature.scanner.ui.ScannerScreen
import kotlinx.coroutines.launch
import java.util.ArrayList

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
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

    LaunchedEffect(homeUiState.shouldAttemptRecoverableSessionRestore, homeUiState.shouldAutoStartScanner) {
        if (!homeUiState.shouldAttemptRecoverableSessionRestore || homeUiState.shouldAutoStartScanner) {
            return@LaunchedEffect
        }

        val recoverableSession = runCatching { getRecoverableSessionUseCase() }.getOrElse { error ->
            error.printStackTrace()
            null
        }

        homeViewModel.setShouldAttemptRecoverableSessionRestore(false)
        homeViewModel.setRecoverableSession(recoverableSession)
    }

    NavHost(
        navController = navController,
        startDestination = AppNavigationContract.homeRoute,
    ) {
        composable(AppNavigationContract.homeRoute) {
            ScannerScreen(
                homeUiState = homeUiState,
                onScanResult = { scanResult ->
                    reviewViewModel.setScanResult(scanResult)
                    navController.navigateResettingToHome(AppNavigationContract.reviewRoute)
                },
                onScanCancelled = {
                    homeViewModel.setShouldAutoStartScanner(false)
                    if (reviewUiState.isAwaitingScanMoreResult) {
                        reviewViewModel.clearScanMorePending()
                        navController.navigateResettingToHome(AppNavigationContract.reviewRoute)
                    }
                },
                onRestoreRecoverableSession = { sessionId ->
                    coroutineScope.launch {
                        val outcome = runCatching { restoreSessionUseCase(sessionId) }.getOrElse { error ->
                            error.printStackTrace()
                            RestoreSessionOutcome.Unavailable
                        }
                        homeViewModel.setShouldAttemptRecoverableSessionRestore(false)
                        homeViewModel.clearRecoverableSession()
                        when (outcome) {
                            is RestoreSessionOutcome.Restored -> {
                                reviewViewModel.setRestoredSession(outcome.result.session)
                                if (outcome.result.isPartial) {
                                    Toast.makeText(
                                        context,
                                        "Recovered session with some missing files",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                                navController.navigateResettingToHome(AppNavigationContract.reviewRoute)
                            }

                            RestoreSessionOutcome.Unavailable -> {
                                Toast.makeText(
                                    context,
                                    "No recoverable session found",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                },
                onDiscardRecoverableSession = { sessionId ->
                    coroutineScope.launch {
                        runCatching { discardRecoverableSessionUseCase(sessionId) }
                            .onFailure { error -> error.printStackTrace() }
                        homeViewModel.setShouldAttemptRecoverableSessionRestore(false)
                        homeViewModel.clearRecoverableSession()
                    }
                },
            )
        }

        composable(AppNavigationContract.reviewRoute) {
            ReviewScreen(
                viewModel = reviewViewModel,
                onExportSuccess = { format, target ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val message = if (format == ExportFormat.PDF) {
                        "Saved as PDF to Documents"
                    } else {
                        "Saved to Gallery"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    appFlowViewModel.setPendingExportAction(target)
                },
                onRescan = {
                    homeViewModel.setShouldAutoStartScanner(true)
                    homeViewModel.setShouldAttemptRecoverableSessionRestore(false)
                    homeViewModel.clearRecoverableSession()
                    navController.navigateResettingToHome(AppNavigationContract.homeRoute)
                },
                onDiscardSession = {
                    homeViewModel.setShouldAutoStartScanner(false)
                    homeViewModel.setShouldAttemptRecoverableSessionRestore(false)
                    homeViewModel.clearRecoverableSession()
                    navController.navigateResettingToHome(AppNavigationContract.homeRoute)
                }
            )
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
                        context.openExportTarget(action.toTarget())
                        appFlowViewModel.clearPendingExportAction()
                    }
                ) {
                    Text(text = context.getString(R.string.export_post_action_open))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        context.shareExportTarget(action.toTarget())
                        appFlowViewModel.clearPendingExportAction()
                    }
                ) {
                    Text(text = context.getString(R.string.export_post_action_share))
                }
            },
        )
    }
}

private fun NavHostController.navigateResettingToHome(route: String) {
    navigate(route) {
        popUpTo(AppNavigationContract.homeRoute) {
            inclusive = false
        }
        launchSingleTop = true
    }
}

private fun Context.openExportTarget(target: ExportPostActionTarget) {
    val primaryUri = target.primaryUri ?: return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(primaryUri, target.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(contentResolver, "export", primaryUri)
    }
    runCatching {
        startActivity(Intent.createChooser(intent, getString(R.string.export_post_action_open)))
    }.onFailure {
        Toast.makeText(
            this,
            getString(R.string.export_post_action_open_failed),
            Toast.LENGTH_SHORT,
        ).show()
    }
}

private fun Context.shareExportTarget(target: ExportPostActionTarget) {
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
        Toast.makeText(
            this,
            getString(R.string.export_post_action_share_failed),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
