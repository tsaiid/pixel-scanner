package com.example.pixelscanner.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pixelscanner.core.model.ExportOperationResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StorageManagerMediaStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val insertedUris = mutableListOf<Uri>()
    private val tempFiles = mutableListOf<File>()

    @After
    fun tearDown() {
        insertedUris.forEach { uri ->
            context.contentResolver.delete(uri, null, null)
        }
        tempFiles.forEach { file ->
            file.delete()
        }
    }

    @Test
    fun saveUriToGallery_insertsJpegIntoPicturesPixelScanner() = runBlocking {
        val sourceFile = createSourceJpeg(
            fileName = "storage-manager-media-store-test.jpg",
        )
        val storageManager = StorageManager(context)

        val result = storageManager.saveUriToGallery(
            sourceUri = Uri.fromFile(sourceFile),
            fileName = "PixelScanner_instrumentation_test.jpg",
        )

        assertTrue(result is ExportOperationResult.Success)
        val insertedUri = (result as ExportOperationResult.Success).outputUri
        insertedUris += insertedUri

        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        context.contentResolver.query(
            insertedUri,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            assertTrue(cursor.moveToFirst())
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

            assertEquals("PixelScanner_instrumentation_test.jpg", cursor.getString(displayNameColumn))
            assertEquals("image/jpeg", cursor.getString(mimeTypeColumn))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = cursor.getString(relativePathColumn)
                assertTrue(
                    relativePath == "Pictures/PixelScanner" ||
                        relativePath == "Pictures/PixelScanner/",
                )
            }
        }
    }

    private fun createSourceJpeg(fileName: String): File {
        val file = File(context.cacheDir, fileName)
        tempFiles += file

        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.CYAN)
        }
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
        bitmap.recycle()
        return file
    }
}
