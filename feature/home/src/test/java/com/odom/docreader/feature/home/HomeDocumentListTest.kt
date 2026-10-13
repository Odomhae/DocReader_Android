package com.odom.docreader.feature.home

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
        tab: HomeTab = HomeTab.ALL,
        query: String = "",
        sortOrder: SortOrder = SortOrder.NAME,
        recent: List<DocumentFile> = emptyList(),
        favorites: List<DocumentFile> = emptyList(),
    ) = buildDocumentList(tab, query, sortOrder, folder, recent, favorites).map { it.file.name }

    @Test
    fun sortsByNameIgnoringCase() {
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
    fun formatTabsFilterByType() {
        assertEquals(listOf("alpha.pdf"), names(tab = HomeTab.PDF))
        assertEquals(listOf("Bravo.docx"), names(tab = HomeTab.DOCX))
    }

    @Test
    fun txtTabShowsOnlyTextFiles() {
        val notes = document("notes.txt", DocumentType.TXT, sizeBytes = 1, lastModified = 5)
        val items = buildDocumentList(HomeTab.TXT, "", SortOrder.NAME, folder + notes, emptyList(), emptyList())
        assertEquals(listOf("notes.txt"), items.map { it.file.name })
    }

    @Test
    fun hwpTabIncludesHwpx() {
        assertEquals(listOf("charlie.hwp", "delta.hwpx"), names(tab = HomeTab.HWP))
    }

    @Test
    fun queryMatchesFileNameIgnoringCaseAndPadding() {
        assertEquals(listOf("Bravo.docx"), names(query = "  bravo "))
        assertEquals(listOf("charlie.hwp", "delta.hwpx"), names(query = ".HWP"))
        assertTrue(names(query = "nothing").isEmpty())
    }

    @Test
    fun recentTabKeepsRecencyOrderRegardlessOfSort() {
        assertEquals(
            listOf("charlie.hwp", "alpha.pdf"),
            names(tab = HomeTab.RECENT, sortOrder = SortOrder.NAME, recent = listOf(charlie, alpha)),
        )
    }

    @Test
    fun favoritesTabUsesTheSortOrderAndTheQuery() {
        assertEquals(
            listOf("alpha.pdf", "delta.hwpx"),
            names(tab = HomeTab.FAVORITES, favorites = listOf(delta, alpha)),
        )
        assertEquals(listOf("delta.hwpx"), names(tab = HomeTab.FAVORITES, query = "del", favorites = listOf(delta, alpha)))
    }

    @Test
    fun favoriteFlagIsSetFromTheFavoritesList() {
        val items = buildDocumentList(HomeTab.ALL, "", SortOrder.NAME, folder, emptyList(), listOf(charlie))

        assertEquals(listOf("charlie.hwp"), items.filter { it.isFavorite }.map { it.file.name })
    }

    private fun document(name: String, type: DocumentType, sizeBytes: Long, lastModified: Long) =
        DocumentFile(uri = "content://docs/$name", name = name, type = type, sizeBytes = sizeBytes, lastModified = lastModified)
}
