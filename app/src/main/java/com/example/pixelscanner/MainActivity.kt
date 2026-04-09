package com.example.pixelscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.pixelscanner.app.AppNavigation
import com.example.pixelscanner.ui.theme.PixelScannerColorPolicy
import com.example.pixelscanner.ui.theme.PixelScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startInBenchmarkReviewMode = intent.getBooleanExtra(
            EXTRA_START_IN_BENCHMARK_REVIEW_MODE,
            false,
        )
        val startInBenchmarkIngestionMode = intent.getBooleanExtra(
            EXTRA_START_IN_BENCHMARK_INGESTION_MODE,
            false,
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            PixelScannerTheme(colorPolicy = PixelScannerColorPolicy.BrandFirst) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        startInBenchmarkReviewMode = startInBenchmarkReviewMode,
                        startInBenchmarkIngestionMode = startInBenchmarkIngestionMode,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_START_IN_BENCHMARK_REVIEW_MODE =
            "com.example.pixelscanner.extra.START_IN_BENCHMARK_REVIEW_MODE"
        const val EXTRA_START_IN_BENCHMARK_INGESTION_MODE =
            "com.example.pixelscanner.extra.START_IN_BENCHMARK_INGESTION_MODE"
    }
}
