package com.example.pixelscanner.core.image

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.domain.ImageProcessingRepository

class AndroidImageProcessingRepository(
    private val imageEnhancer: ImageEnhancer = ImageEnhancer(),
) : ImageProcessingRepository {
    override suspend fun enhance(context: Context, uri: Uri): Uri {
        return imageEnhancer.enhance(context, uri)
    }
}
