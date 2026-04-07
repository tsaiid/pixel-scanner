package com.example.pixelscanner.app

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelscanner.core.model.ExportFormat
import com.example.pixelscanner.feature.home.ui.HomeUiState
import com.example.pixelscanner.feature.review.ReviewViewModel
import com.example.pixelscanner.feature.review.ui.ReviewScreen
import com.example.pixelscanner.feature.scanner.ui.ScannerScreen

private enum class Screen {
    HOME,
    REVIEW,
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var homeUiState by remember { mutableStateOf(HomeUiState()) }
    val reviewViewModel: ReviewViewModel = viewModel()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    when (currentScreen) {
        Screen.HOME -> {
            ScannerScreen(
                homeUiState = homeUiState,
                onScanResult = { scanResult ->
                    reviewViewModel.setScanResult(scanResult)
                    currentScreen = Screen.REVIEW
                },
                onScanCancelled = {
                    homeUiState = homeUiState.copy(shouldAutoStartScanner = false)
                }
            )
        }

        Screen.REVIEW -> {
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
                onBack = {
                    homeUiState = homeUiState.copy(shouldAutoStartScanner = true)
                    currentScreen = Screen.HOME
                }
            )
        }
    }
}
