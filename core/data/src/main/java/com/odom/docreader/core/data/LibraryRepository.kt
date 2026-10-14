package com.odom.docreader.core.data

import com.odom.docreader.core.data.db.DocumentDao
import com.odom.docreader.core.data.db.DocumentEntity
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.DocumentType
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [openedAt] is epoch milliseconds. */
data class RecentDocument(val file: DocumentFile, val openedAt: Long)

interface LibraryRepository {
    /** Most recently opened first. */
    val recentDocuments: Flow<List<RecentDocument>>

    suspend fun markOpened(document: DocumentFile)
}

class RoomLibraryRepository @Inject constructor(
    private val documentDao: DocumentDao,
    private val clock: Clock,
) : LibraryRepository {

    override val recentDocuments: Flow<List<RecentDocument>> =
        documentDao.observeRecent(RECENT_LIMIT).map { entities ->
            entities.mapNotNull { entity -> entity.toDocumentFile()?.let { RecentDocument(it, entity.lastOpenedAt) } }
        }

    override suspend fun markOpened(document: DocumentFile) {
        documentDao.upsert(
            DocumentEntity(
                uri = document.uri,
                name = document.name,
                type = document.type.name,
                sizeBytes = document.sizeBytes,
                lastModified = document.lastModified,
                lastOpenedAt = clock.millis(),
            ),
        )
    }

    // A row written by a build that knew a format this build does not is skipped, not a crash.
    private fun DocumentEntity.toDocumentFile(): DocumentFile? {
        val documentType = DocumentType.entries.firstOrNull { it.name == type } ?: return null
        return DocumentFile(uri = uri, name = name, type = documentType, sizeBytes = sizeBytes, lastModified = lastModified)
    }

    private companion object {
        const val RECENT_LIMIT = 50
    }
}
