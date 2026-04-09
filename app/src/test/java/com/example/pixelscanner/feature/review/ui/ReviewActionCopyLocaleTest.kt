package com.example.pixelscanner.feature.review.ui

import android.content.Context
import android.content.res.Configuration
import com.example.pixelscanner.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReviewActionCopyLocaleTest {

    @Test
    fun `review and export action labels stay distinct and within length bounds in English and Traditional Chinese`() {
        assertLocaleLabels(
            locale = Locale.ENGLISH,
            maxActionLength = 16,
            maxExportLength = 12,
        )

        assertLocaleLabels(
            locale = Locale("zh", "TW"),
            maxActionLength = 9,
            maxExportLength = 8,
        )
    }

    private fun assertLocaleLabels(
        locale: Locale,
        maxActionLength: Int,
        maxExportLength: Int,
    ) {
        val context = localizedContext(locale)

        val reviewActionLabels = listOf(
            context.getString(R.string.review_action_scan_more),
            context.getString(R.string.review_action_rescan),
            context.getString(R.string.review_action_discard_session),
        )
        val confirmActionLabels = listOf(
            context.getString(R.string.review_rescan_confirm_action),
            context.getString(R.string.review_discard_confirm_action),
        )
        val exportLabels = listOf(
            context.getString(R.string.export_save_button_jpeg),
            context.getString(R.string.export_save_button_pdf),
            context.getString(R.string.export_retry_button_jpeg),
            context.getString(R.string.export_retry_button_pdf),
        )

        reviewActionLabels.forEach { label ->
            assertTrue("Label is too long for ${locale.toLanguageTag()}: $label", label.length <= maxActionLength)
        }
        confirmActionLabels.forEach { label ->
            assertTrue("Confirm label is too long for ${locale.toLanguageTag()}: $label", label.length <= maxActionLength)
        }
        exportLabels.forEach { label ->
            assertTrue("Export label is too long for ${locale.toLanguageTag()}: $label", label.length <= maxExportLength)
        }

        assertEquals(reviewActionLabels.size, reviewActionLabels.toSet().size)
        assertEquals(confirmActionLabels.size, confirmActionLabels.toSet().size)
        assertEquals(exportLabels.size, exportLabels.toSet().size)
        assertTrue(reviewActionLabels.intersect(exportLabels.toSet()).isEmpty())
        assertTrue(confirmActionLabels.intersect(exportLabels.toSet()).isEmpty())
    }

    private fun localizedContext(locale: Locale): Context {
        val baseContext = RuntimeEnvironment.getApplication()
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.setLocale(locale)
        return baseContext.createConfigurationContext(configuration)
    }
}
