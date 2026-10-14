package com.odom.docreader.feature.home

import com.odom.docreader.core.data.RecentDocument
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.core.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDocumentListTest {
    private val alpha = document("alpha.pdf", DocumentType.PDF, sizeBytes = 300, lastModified = 1)
    private val bravo = document("Bravo.docx", DocumentType.DOCX, sizeBytes = 100, lastModified = 3)
    private val charlie = document("charlie.hwp", DocumentType.HWP, sizeBytes = 200, lastModified = 2)
    private val delta = document("delta.hwpx", DocumentType.HWPX, sizeBytes = 400, lastModified = 4)
    private val folder = listOf(delta, charlie, bravo, alpha)

    private fun names(
        filter: HomeFilter? = null,
        query: String = "",
        sortOrder: SortOrder = SortOrder.NAME,
        recent: List<DocumentFile> = emptyList(),
    ) = buildDocumentList(filter, query, sortOrder, folder, recent.asRecent()).map { it.file.name }

    // Newest first, as the repository returns them.
    private fun List<DocumentFile>.asRecent() =
        mapIndexed { index, document -> RecentDocument(document, openedAt = 1_000L - index) }

    @Test
    fun onlyTheRecentListCarriesTheOpenedTime() {
        val recent = listOf(charlie, alpha).asRecent()

        val underRecent = buildDocumentList(HomeFilter.RECENT, "", SortOrder.NAME, folder, recent)
        val everything = buildDocumentList(null, "", SortOrder.NAME, folder, recent)

        assertEquals(listOf(1_000L, 999L), underRecent.map { it.openedAt })
        assertTrue(everything.all { it.openedAt == null })
    }

    @Test
    fun noFilterListsEveryDocument() {
        assertEquals(listOf("alpha.pdf", "Bravo.docx", "charlie.hwp", "delta.hwpx"), names())
    }

    @Test
    fun sortsByDateNewestFirst() {
        assertEquals(
            listOf("delta.hwpx", "Bravo.docx", "charlie.hwp", "alpha.pdf"),
            names(sortOrder = SortOrder.DATE),
        )
    }

    @Test
    fun sortsBySizeLargestFirst() {
        assertEquals(
            listOf("delta.hwpx", "alpha.pdf", "charlie.hwp", "Bravo.docx"),
            names(sortOrder = SortOrder.SIZE),
        )
    }

    @Test
    fun formatFilterKeepsOnlyThatType() {
        assertEquals(listOf("alpha.pdf"), names(filter = HomeFilter.PDF))
        assertEquals(listOf("Bravo.docx"), names(filter = HomeFilter.DOCX))
    }

    @Test
    fun txtFilterShowsOnlyTextFiles() {
        val notes = document("notes.txt", DocumentType.TXT, sizeBytes = 1, lastModified = 5)
        val items = buildDocumentList(HomeFilter.TXT, "", SortOrder.NAME, folder + notes, emptyList())
        assertEquals(listOf("notes.txt"), items.map { it.file.name })
    }

    @Test
    fun hwpFilterIncludesHwpx() {
        assertEquals(listOf("charlie.hwp", "delta.hwpx"), names(filter = HomeFilter.HWP))
    }

    @Test
    fun queryMatchesFileNameIgnoringCaseAndPadding() {
        assertEquals(listOf("Bravo.docx"), names(query = "  bravo "))
        assertEquals(listOf("charlie.hwp", "delta.hwpx"), names(query = ".HWP"))
        assertTrue(names(query = "nothing").isEmpty())
    }

    @Test
    fun recentKeepsRecencyOrderRegardlessOfSort() {
        assertEquals(
            listOf("charlie.hwp", "alpha.pdf"),
            names(filter = HomeFilter.RECENT, sortOrder = SortOrder.NAME, recent = listOf(charlie, alpha)),
        )
    }

    @Test
    fun recentAppliesTheQuery() {
        assertEquals(listOf("alpha.pdf"), names(filter = HomeFilter.RECENT, query = "alp", recent = listOf(charlie, alpha)))
    }

    private fun document(name: String, type: DocumentType, sizeBytes: Long, lastModified: Long) =
        DocumentFile(uri = "content://docs/$name", name = name, type = type, sizeBytes = sizeBytes, lastModified = lastModified)
}
