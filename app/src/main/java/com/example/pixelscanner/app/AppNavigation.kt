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
    var autoStartScanner by remember { mutableStateOf(false) }
    val reviewViewModel: ReviewViewModel = viewModel()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    when (currentScreen) {
        Screen.HOME -> {
            ScannerScreen(
                shouldAutoStart = autoStartScanner,
                onScanResult = { scanResult ->
                    reviewViewModel.setScanResult(scanResult)
                    currentScreen = Screen.REVIEW
                },
                onScanCancelled = {
                    if (autoStartScanner) {
                        autoStartScanner = false
                    }
                }
            )
        }

        Screen.REVIEW -> {
            ReviewScreen(
                viewModel = reviewViewModel,
                onSave = {
                    val format = reviewViewModel.exportFormat.value
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    reviewViewModel.saveAll(context) {
                        val message = if (format == ExportFormat.PDF) {
                            "Saved as PDF to Documents"
                        } else {
                            "Saved to Gallery"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onBack = {
                    autoStartScanner = true
                    currentScreen = Screen.HOME
                }
            )
        }
    }
}
