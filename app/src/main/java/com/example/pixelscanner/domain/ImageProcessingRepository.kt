package com.example.pixelscanner.domain

import android.content.Context
import android.net.Uri

interface ImageProcessingRepository {
    suspend fun enhance(context: Context, uri: Uri): Uri
}
