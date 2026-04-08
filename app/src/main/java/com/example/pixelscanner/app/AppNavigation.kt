package com.example.pixelscanner.app

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.PixelScannerApp
import com.example.pixelscanner.feature.home.HomeViewModel
import com.example.pixelscanner.feature.review.ReviewViewModel
import com.example.pixelscanner.feature.review.ui.ReviewScreen
import com.example.pixelscanner.feature.scanner.ui.ScannerScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appContainer = (context.applicationContext as PixelScannerApp).appContainer
    val homeViewModelFactory = appContainer.createHomeViewModelFactory(
        owner = context as SavedStateRegistryOwner,
    )
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
    val reviewViewModelFactory = appContainer.createReviewViewModelFactory(
        owner = context as SavedStateRegistryOwner,
    )
    val reviewViewModel: ReviewViewModel = viewModel(factory = reviewViewModelFactory)
    val homeUiState by homeViewModel.uiState.collectAsState()

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
                }
            )
        }

        composable(AppNavigationContract.reviewRoute) {
            ReviewScreen(
                viewModel = reviewViewModel,
                onExportSuccess = { format ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val message = if (format == ExportFormat.PDF) {
                        "Saved as PDF to Documents"
                    } else {
                        "Saved to Gallery"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
                onRescan = {
                    homeViewModel.setShouldAutoStartScanner(true)
                    navController.navigateResettingToHome(AppNavigationContract.homeRoute)
                },
                onDiscardSession = {
                    homeViewModel.setShouldAutoStartScanner(false)
                    navController.navigateResettingToHome(AppNavigationContract.homeRoute)
                }
            )
        }
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
