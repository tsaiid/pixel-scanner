package com.example.pixelscanner.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = viewModel(),
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val scannedPages by viewModel.scannedPages.collectAsState()
    val exportFormat by viewModel.exportFormat.collectAsState()
    val pagerState = rememberPagerState(pageCount = { scannedPages.size })

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Rescan") }
            Text("Review (${pagerState.currentPage + 1}/${scannedPages.size})", color = Color.White)
            Button(onClick = onSave) { Text("Save All") }
        }

        TabRow(
            selectedTabIndex = if (exportFormat == ExportFormat.IMAGE) 0 else 1,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Tab(
                selected = exportFormat == ExportFormat.IMAGE,
                onClick = { viewModel.setExportFormat(ExportFormat.IMAGE) },
                text = { Text("JPEG") }
            )
            Tab(
                selected = exportFormat == ExportFormat.PDF,
                onClick = { viewModel.setExportFormat(ExportFormat.PDF) },
                text = { Text("PDF") }
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (scannedPages.isNotEmpty()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                    val page = scannedPages[pageIndex]
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val bitmapToShow = page.originalBitmap
                        
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
                    }
                }
            } else {
                Text("No images to review", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
