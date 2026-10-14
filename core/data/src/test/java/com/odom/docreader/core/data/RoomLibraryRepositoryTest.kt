package com.odom.docreader.core.data

import com.odom.docreader.core.data.db.DocumentDao
import com.odom.docreader.core.data.db.DocumentEntity
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.DocumentType
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomLibraryRepositoryTest {
    private val dao = FakeDocumentDao()
    private val clock = MutableClock()
    private val repository = RoomLibraryRepository(dao, clock)

    private fun document(name: String, type: DocumentType = DocumentType.PDF) =
        DocumentFile(uri = "content://docs/$name", name = name, type = type, sizeBytes = 10, lastModified = 20)

    @Test
    fun openedDocumentsAreListedMostRecentFirst() = runTest {
        clock.now = 100
        repository.markOpened(document("a.pdf"))
        clock.now = 200
        repository.markOpened(document("b.hwp", DocumentType.HWP))

        assertEquals(listOf("b.hwp", "a.pdf"), repository.recentDocuments.first().map { it.file.name })
    }

    @Test
    fun recentDocumentsCarryTheTimeTheyWereOpened() = runTest {
        clock.now = 100
        repository.markOpened(document("a.pdf"))
        clock.now = 200
        repository.markOpened(document("a.pdf"))

        assertEquals(listOf(200L), repository.recentDocuments.first().map { it.openedAt })
    }

    @Test
    fun reopeningMovesADocumentToTheFront() = runTest {
        clock.now = 100
        repository.markOpened(document("a.pdf"))
        clock.now = 200
        repository.markOpened(document("b.pdf"))
        clock.now = 300
        repository.markOpened(document("a.pdf"))

        assertEquals(listOf("a.pdf", "b.pdf"), repository.recentDocuments.first().map { it.file.name })
    }

    @Test
    fun openingKeepsTheFavoriteFlag() = runTest {
        repository.setFavorite(document("a.pdf"), favorite = true)
        repository.markOpened(document("a.pdf"))

        assertEquals(listOf("a.pdf"), repository.favoriteDocuments.first().map { it.name })
    }

    @Test
    fun favoritingKeepsTheOpenedTime() = runTest {
        clock.now = 100
        repository.markOpened(document("a.pdf"))
        repository.setFavorite(document("a.pdf"), favorite = true)
        repository.setFavorite(document("a.pdf"), favorite = false)

        assertEquals(listOf("a.pdf"), repository.recentDocuments.first().map { it.file.name })
        assertTrue(repository.favoriteDocuments.first().isEmpty())
    }

    @Test
    fun unfavoritingANeverOpenedDocumentForgetsIt() = runTest {
        repository.setFavorite(document("a.pdf"), favorite = true)
        repository.setFavorite(document("a.pdf"), favorite = false)

        assertNull(dao.find("content://docs/a.pdf"))
    }

    @Test
    fun rowsWithAnUnknownTypeAreSkipped() = runTest {
        dao.upsert(DocumentEntity("content://docs/x", "x.odt", "ODT", 1, 2, lastOpenedAt = 5, isFavorite = true))
        clock.now = 100
        repository.markOpened(document("a.pdf"))

        assertEquals(listOf("a.pdf"), repository.recentDocuments.first().map { it.file.name })
        assertTrue(repository.favoriteDocuments.first().isEmpty())
    }

    private class MutableClock(var now: Long = 0) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(now)
    }

    private class FakeDocumentDao : DocumentDao {
        private val rows = MutableStateFlow<Map<String, DocumentEntity>>(emptyMap())

        override fun observeRecent(limit: Int): Flow<List<DocumentEntity>> = rows.map { current ->
            current.values.filter { it.lastOpenedAt != null }.sortedByDescending { it.lastOpenedAt }.take(limit)
        }

        override fun observeFavorites(): Flow<List<DocumentEntity>> =
            rows.map { current -> current.values.filter { it.isFavorite } }

        override suspend fun find(uri: String): DocumentEntity? = rows.value[uri]

        override suspend fun upsert(document: DocumentEntity) {
            rows.value = rows.value + (document.uri to document)
        }

        override suspend fun delete(uri: String) {
            rows.value = rows.value - uri
        }
    }
}
