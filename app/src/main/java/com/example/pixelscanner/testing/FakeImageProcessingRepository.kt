package com.example.pixelscanner.testing

import android.content.Context
import android.net.Uri
import com.example.pixelscanner.domain.ImageProcessingRepository

class FakeImageProcessingRepository(
    var enhancedUri: Uri = Uri.parse("file:///tmp/fake-enhanced.jpg"),
) : ImageProcessingRepository {
    val requests = mutableListOf<Uri>()

    override suspend fun enhance(context: Context, uri: Uri): Uri {
        requests += uri
        return enhancedUri
    }
}
