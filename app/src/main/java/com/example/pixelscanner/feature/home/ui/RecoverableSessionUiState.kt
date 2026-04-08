package com.example.pixelscanner.feature.home.ui

data class RecoverableSessionUiState(
    val sessionId: String,
    val pageCount: Int,
    val hasMissingAssets: Boolean,
    val missingAssetCount: Int,
)
