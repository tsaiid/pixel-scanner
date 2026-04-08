package com.example.pixelscanner.feature.scanner.ui

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.pixelscanner.core.model.ScanResultPayload
import com.example.pixelscanner.feature.home.ui.StartScreen
import com.example.pixelscanner.feature.home.ui.HomeUiState
import com.example.pixelscanner.feature.scanner.ScannerManager
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@Composable
fun ScannerScreen(
    homeUiState: HomeUiState = HomeUiState(),
    onScanResult: (ScanResultPayload) -> Unit,
    onScanCancelled: () -> Unit = {},
    onRestoreRecoverableSession: (String) -> Unit = {},
    onDiscardRecoverableSession: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scannerManager = remember { ScannerManager(context) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val gmsResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            gmsResult?.let {
                val uris = it.pages?.map { page -> page.imageUri } ?: emptyList()
                if (uris.isNotEmpty()) {
                    onScanResult(
                        ScanResultPayload(
                            pageUris = uris,
                            pdfUri = it.pdf?.uri,
                        )
                    )
                } else {
                    onScanCancelled()
                }
            }
        } else {
            onScanCancelled()
        }
    }

    fun launchScanner() {
        try {
            val activity = context.findActivity()
            if (activity != null) {
                scannerManager.getScanner().getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                    .addOnFailureListener { error ->
                        error.printStackTrace()
                        Toast.makeText(
                            context,
                            "Error starting scanner: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        onScanCancelled()
                    }
            } else {
                Toast.makeText(context, "Activity not found", Toast.LENGTH_SHORT).show()
                onScanCancelled()
            }
        } catch (error: Exception) {
            error.printStackTrace()
            Toast.makeText(context, "Crash: ${error.message}", Toast.LENGTH_LONG).show()
            onScanCancelled()
        }
    }

    LaunchedEffect(homeUiState.shouldAutoStartScanner) {
        if (homeUiState.shouldAutoStartScanner) {
            launchScanner()
        }
    }

    if (homeUiState.shouldAutoStartScanner) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Resuming Scanner...",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    } else {
        StartScreen(
            recoverableSession = homeUiState.recoverableSession,
            onStartScanClicked = ::launchScanner,
            onRestoreRecoverableSession = onRestoreRecoverableSession,
            onDiscardRecoverableSession = onDiscardRecoverableSession,
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
