package com.example.pixelscanner.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidTempFileRepositoryTest {

    @Test
    fun createTempFile_usesProvidedCacheDir() {
        val cacheDir = RuntimeEnvironment.getApplication().cacheDir
        val repository = AndroidTempFileRepository()

        val file = repository.createTempFile(
            cacheDir = cacheDir,
            prefix = "scan",
            suffix = ".tmp",
        )

        assertEquals(cacheDir, file.parentFile)
        assertTrue(file.name.startsWith("scan"))
        assertTrue(file.name.endsWith(".tmp"))
        file.delete()
    }
}
