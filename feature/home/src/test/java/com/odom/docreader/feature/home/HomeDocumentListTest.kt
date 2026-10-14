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
        format: FormatFilter? = null,
        collection: CollectionFilter? = null,
        query: String = "",
        sortOrder: SortOrder = SortOrder.NAME,
        recent: List<DocumentFile> = emptyList(),
        favorites: List<DocumentFile> = emptyList(),
    ) = buildDocumentList(format, collection, query, sortOrder, folder, recent, favorites).map { it.file.name }

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
        assertEquals(listOf("alpha.pdf"), names(format = FormatFilter.PDF))
        assertEquals(listOf("Bravo.docx"), names(format = FormatFilter.DOCX))
    }

    @Test
    fun txtFilterShowsOnlyTextFiles() {
        val notes = document("notes.txt", DocumentType.TXT, sizeBytes = 1, lastModified = 5)
        val items = buildDocumentList(FormatFilter.TXT, null, "", SortOrder.NAME, folder + notes, emptyList(), emptyList())
        assertEquals(listOf("notes.txt"), items.map { it.file.name })
    }

    @Test
    fun hwpFilterIncludesHwpx() {
        assertEquals(listOf("charlie.hwp", "delta.hwpx"), names(format = FormatFilter.HWP))
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
            names(collection = CollectionFilter.RECENT, sortOrder = SortOrder.NAME, recent = listOf(charlie, alpha)),
        )
    }

    @Test
    fun favoritesUseTheSortOrderAndTheQuery() {
        val favorites = listOf(delta, alpha)
        assertEquals(listOf("alpha.pdf", "delta.hwpx"), names(collection = CollectionFilter.FAVORITES, favorites = favorites))
        assertEquals(
            listOf("delta.hwpx"),
            names(collection = CollectionFilter.FAVORITES, query = "del", favorites = favorites),
        )
    }

    @Test
    fun formatFilterCombinesWithCollections() {
        assertEquals(
            listOf("delta.hwpx"),
            names(format = FormatFilter.HWP, collection = CollectionFilter.FAVORITES, favorites = listOf(delta, alpha)),
        )
        assertEquals(
            listOf("alpha.pdf"),
            names(format = FormatFilter.PDF, collection = CollectionFilter.RECENT, recent = listOf(charlie, alpha)),
        )
    }

    @Test
    fun favoriteFlagIsSetFromTheFavoritesList() {
        val items = buildDocumentList(null, null, "", SortOrder.NAME, folder, emptyList(), listOf(charlie))

        assertEquals(listOf("charlie.hwp"), items.filter { it.isFavorite }.map { it.file.name })
    }

    private fun document(name: String, type: DocumentType, sizeBytes: Long, lastModified: Long) =
        DocumentFile(uri = "content://docs/$name", name = name, type = type, sizeBytes = sizeBytes, lastModified = lastModified)
}
