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
    fun rowsWithAnUnknownTypeAreSkipped() = runTest {
        dao.upsert(DocumentEntity("content://docs/x", "x.odt", "ODT", 1, 2, lastOpenedAt = 5))
        clock.now = 100
        repository.markOpened(document("a.pdf"))

        assertEquals(listOf("a.pdf"), repository.recentDocuments.first().map { it.file.name })
    }

    private class MutableClock(var now: Long = 0) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(now)
    }

    private class FakeDocumentDao : DocumentDao {
        private val rows = MutableStateFlow<Map<String, DocumentEntity>>(emptyMap())

        override fun observeRecent(limit: Int): Flow<List<DocumentEntity>> = rows.map { current ->
            current.values.sortedByDescending { it.lastOpenedAt }.take(limit)
        }

        override suspend fun upsert(document: DocumentEntity) {
            rows.value = rows.value + (document.uri to document)
        }
    }
}
