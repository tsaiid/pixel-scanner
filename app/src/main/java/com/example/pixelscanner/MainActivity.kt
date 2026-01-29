package com.example.pixelscanner

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelscanner.scanner.ScannerManager
import com.example.pixelscanner.ui.ReviewScreen
import com.example.pixelscanner.ui.ReviewViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class Screen { HOME, REVIEW }

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
                onScanResult = { uris ->
                    autoStartScanner = false
                    reviewViewModel.setScannedUris(uris, context)
                    currentScreen = Screen.REVIEW
                },
                onScanCancelled = {
                    autoStartScanner = false
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
                        val message = if (format == com.example.pixelscanner.ui.ExportFormat.PDF) 
                            "Saved as PDF to Documents" 
                        else 
                            "Saved to Gallery"
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

@Composable
fun ScannerScreen(
    shouldAutoStart: Boolean = false,
    onScanResult: (List<Uri>) -> Unit,
    onScanCancelled: () -> Unit = {}
) {
    val context = LocalContext.current
    val scannerManager = remember { ScannerManager(context) }
    var statusMessage by remember { mutableStateOf("Ready to Scan") }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val gmsResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            gmsResult?.let {
                val uris = it.pages?.map { page -> page.imageUri } ?: emptyList()
                if (uris.isNotEmpty()) {
                    onScanResult(uris)
                } else {
                    statusMessage = "No pages scanned"
                    onScanCancelled()
                }
            }
        } else {
            statusMessage = "Scan cancelled or failed"
            onScanCancelled()
        }
    }

    val launchScanner = {
        try {
            val activity = context.findActivity()
            if (activity != null) {
                scannerManager.getScanner().getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                    .addOnFailureListener { e ->
                        statusMessage = "Error: ${e.message}"
                        e.printStackTrace()
                        Toast.makeText(context, "Error starting scanner: ${e.message}", Toast.LENGTH_LONG).show()
                        onScanCancelled()
                    }
            } else {
                Toast.makeText(context, "Activity not found", Toast.LENGTH_SHORT).show()
                onScanCancelled()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            statusMessage = "Crash detected: ${e.message}"
            Toast.makeText(context, "Crash: ${e.message}", Toast.LENGTH_LONG).show()
            onScanCancelled()
        }
    }

    LaunchedEffect(shouldAutoStart) {
        if (shouldAutoStart) {
            launchScanner()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (shouldAutoStart) {
            Text(text = "Resuming Scanner...", style = MaterialTheme.typography.headlineSmall)
        } else {
            Text(text = statusMessage, style = MaterialTheme.typography.headlineSmall)

            Button(onClick = { launchScanner() }) {
                Text("Start Scanning")
            }
        }
    }
}
