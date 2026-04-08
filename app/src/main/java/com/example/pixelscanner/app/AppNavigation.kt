package com.example.pixelscanner.app

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.pixelscanner.feature.home.ui.HomeUiState
import com.example.pixelscanner.feature.review.ReviewViewModel
import com.example.pixelscanner.feature.review.ui.ReviewScreen
import com.example.pixelscanner.feature.scanner.ui.ScannerScreen

@Composable
fun AppNavigation() {
    var shouldAutoStartScanner by rememberSaveable { mutableStateOf(false) }
    val navController = rememberNavController()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appContainer = (context.applicationContext as PixelScannerApp).appContainer
    val reviewViewModelFactory = appContainer.createReviewViewModelFactory(
        owner = context as SavedStateRegistryOwner,
    )
    val reviewViewModel: ReviewViewModel = viewModel(factory = reviewViewModelFactory)

    NavHost(
        navController = navController,
        startDestination = AppScreen.Home.route,
    ) {
        composable(AppScreen.Home.route) {
            ScannerScreen(
                homeUiState = HomeUiState(shouldAutoStartScanner = shouldAutoStartScanner),
                onScanResult = { scanResult ->
                    reviewViewModel.setScanResult(scanResult)
                    navController.navigateResettingToHome(AppScreen.Review.route)
                },
                onScanCancelled = {
                    shouldAutoStartScanner = false
                }
            )
        }

        composable(AppScreen.Review.route) {
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
                    shouldAutoStartScanner = true
                    navController.navigateResettingToHome(AppScreen.Home.route)
                },
                onDiscardSession = {
                    shouldAutoStartScanner = false
                    navController.navigateResettingToHome(AppScreen.Home.route)
                }
            )
        }
    }
}

private fun NavHostController.navigateResettingToHome(route: String) {
    navigate(route) {
        popUpTo(AppScreen.Home.route) {
            inclusive = false
        }
        launchSingleTop = true
    }
}
