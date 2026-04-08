package com.example.pixelscanner.domain

import android.net.Uri
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReorderPageUseCaseTest {

    @Test
    fun `moves the selected active page within the session order`() {
        val session = ScanSession(
            id = "session_reorder_test",
            pages = listOf(
                scannedPage("page-a"),
                scannedPage("page-b"),
                scannedPage("page-c"),
            ),
        )

        val result = ReorderPageUseCase().invoke(
            session = session,
            fromActivePageIndex = 2,
            toActivePageIndex = 0,
        )

        assertTrue(result != null)
        assertEquals(
            listOf("page-c", "page-a", "page-b"),
            result!!.updatedSession.pages.map { it.id },
        )
        assertTrue(result.updatedSession.dirtyFlags.hasReorderedPages)
    }

    @Test
    fun `keeps deleted pages available while reordering active pages`() {
        val session = ScanSession(
            id = "session_reorder_deleted_test",
            pages = listOf(
                scannedPage("page-a"),
                scannedPage("page-b", isDeleted = true),
                scannedPage("page-c"),
            ),
        )

        val result = ReorderPageUseCase().invoke(
            session = session,
            fromActivePageIndex = 1,
            toActivePageIndex = 0,
        )

        assertTrue(result != null)
        assertEquals(
            listOf("page-c", "page-a", "page-b"),
            result!!.updatedSession.pages.map { it.id },
        )
        assertEquals(listOf("page-c", "page-a"), result.updatedSession.activePages().map { it.id })
        assertTrue(result.updatedSession.dirtyFlags.hasReorderedPages)
    }

    private fun scannedPage(
        id: String,
        isDeleted: Boolean = false,
    ): ScannedPage {
        return ScannedPage(
            id = id,
            originalSourceUri = Uri.parse("content://scanner/$id.jpg"),
            isDeleted = isDeleted,
        )
    }
}
