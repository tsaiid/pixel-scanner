package com.example.pixelscanner.core.model

import com.example.pixelscanner.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ExportProgressTest {

    @Test
    fun `display label resolves to overall progress resource`() {
        val progress = ExportProgress(
            overall = ExportOverallProgress(completed = 3, total = 5),
        )

        val label = progress.displayLabel()

        assertTrue(label is UiText.StringResource)
        assertEquals("3/5", label.asString(RuntimeEnvironment.getApplication()))
        assertEquals(
            R.string.export_progress_overall,
            (label as UiText.StringResource).resId,
        )
    }

    @Test
    fun `display label resolves to combined overall and page progress resource`() {
        val progress = ExportProgress(
            overall = ExportOverallProgress(completed = 2, total = 4),
            page = ExportPageProgress(completed = 1, total = 4),
        )

        val label = progress.displayLabel()

        assertTrue(label is UiText.StringResource)
        assertEquals("2/4 (page 1/4)", label.asString(RuntimeEnvironment.getApplication()))
        assertEquals(
            R.string.export_progress_overall_with_page,
            (label as UiText.StringResource).resId,
        )
    }
}
