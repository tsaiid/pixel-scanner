package com.example.pixelscanner.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = viewModel(),
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val scannedPages by viewModel.scannedPages.collectAsState()
    val pagerState = rememberPagerState(pageCount = { scannedPages.size })

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Back") }
            Text("Review (${pagerState.currentPage + 1}/${scannedPages.size})", color = Color.White)
            Button(onClick = onSave) { Text("Save All") }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (scannedPages.isNotEmpty()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                    val page = scannedPages[pageIndex]
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val bitmapToShow = if (page.isProcessed) page.processedBitmap else page.originalBitmap
                        
                        if (bitmapToShow != null) {
                            Image(
                                bitmap = bitmapToShow.asImageBitmap(),
                                contentDescription = "Scanned Page $pageIndex",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("Error loading image", color = Color.Red)
                        }

                        if (page.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            } else {
                Text("No images to review", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Remove Shadows")
                Spacer(modifier = Modifier.padding(8.dp))
                Switch(
                    checked = scannedPages.getOrNull(pagerState.currentPage)?.isProcessed == true,
                    onCheckedChange = { 
                        viewModel.toggleProcessing(pagerState.currentPage)
                    }
                )
            }
        }
    }
}
