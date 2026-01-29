package com.example.pixelscanner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // If all pages are deleted, go back automatically
    LaunchedEffect(scannedPages.size) {
        if (scannedPages.isEmpty()) {
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
                        // Top Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Rescan")
                        }
                        
                        if (scannedPages.isNotEmpty()) {                    Text(
                        "Review (${pagerState.currentPage + 1}/${scannedPages.size})",
                        color = Color.White
                    )
                    
                    Row {
                        IconButton(
                            onClick = { 
                                val pageToDelete = scannedPages.getOrNull(pagerState.currentPage)
                                val indexToDelete = pagerState.currentPage
                                if (pageToDelete != null) {
                                    viewModel.deletePage(indexToDelete)
                                    
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Page deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restorePage(indexToDelete, pageToDelete)
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Page",
                                tint = Color.Red
                            )
                        }
                        
                        Button(onClick = onSave) {
                            Text("Save All")
                        }
                    }
                }
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
                        // Guard against index out of bounds during deletion animation
                        if (pageIndex < scannedPages.size) {
                            val page = scannedPages[pageIndex]
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = page.originalUri,
                                    contentDescription = "Scanned Page $pageIndex",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                } else {
                    Text("No images to review", color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
