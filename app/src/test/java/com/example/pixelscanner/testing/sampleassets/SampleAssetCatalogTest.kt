package com.example.pixelscanner.testing.sampleassets

import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SampleAssetCatalogTest {

    @Test
    fun `suite generates the expected integration sample coverage`() {
        val context = RuntimeEnvironment.getApplication()
        val suite = SampleAssetCatalog.suite(context)

        assertAsset(suite.singlePageClean.name, 1600, 2200, suite.singlePageClean.file.path)
        assertEquals(6, suite.multiPageSix.pageCount)
        assertEquals(12, suite.multiPageTwelve.pageCount)
        assertEquals(24, suite.multiPageTwentyFour.pageCount)
        assertTrue(suite.multiPageSix.pages.all { it.file.exists() })
        assertTrue(suite.multiPageTwelve.pages.all { it.file.exists() })
        assertTrue(suite.multiPageTwentyFour.pages.all { it.file.exists() })
        assertAsset(suite.lowLightPage.name, 1200, 1600, suite.lowLightPage.file.path)
        assertAsset(suite.shadowPage.name, 1200, 1600, suite.shadowPage.file.path)
        assertAsset(suite.rotatedPage.name, 1200, 1600, suite.rotatedPage.file.path)
        assertAsset(suite.highResPage.name, 2400, 3600, suite.highResPage.file.path)
        assertEquals(48, suite.longDocumentFortyEight.pageCount)
        assertEquals(72, suite.longDocumentSeventyTwo.pageCount)
        assertTrue(suite.longDocumentFortyEight.pages.all { it.file.exists() })
        assertTrue(suite.longDocumentSeventyTwo.pages.all { it.file.exists() })

        val stressSession = SampleAssetCatalog.reviewStressSession(context, pageCount = 100)
        assertEquals(100, stressSession.pages.size)
        assertTrue(stressSession.isDirty())
        assertTrue(stressSession.pages.all { it.originalSourceUri.toString().startsWith("file:") })
    }

    private fun assertAsset(
        name: String,
        expectedWidth: Int,
        expectedHeight: Int,
        path: String,
    ) {
        val bitmap = BitmapFactory.decodeFile(path)
        assertNotNull("Failed to decode $name", bitmap)
        assertEquals(expectedWidth, bitmap!!.width)
        assertEquals(expectedHeight, bitmap.height)
    }
}

