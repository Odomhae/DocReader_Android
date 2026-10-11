package com.odom.docreader.core.data

import com.odom.docreader.core.data.db.DocumentDao
import com.odom.docreader.core.data.db.DocumentEntity
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.DocumentType
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface LibraryRepository {
    /** Most recently opened first. */
    val recentDocuments: Flow<List<DocumentFile>>

    val favoriteDocuments: Flow<List<DocumentFile>>

    suspend fun markOpened(document: DocumentFile)

    suspend fun setFavorite(document: DocumentFile, favorite: Boolean)
}

class RoomLibraryRepository @Inject constructor(
    private val documentDao: DocumentDao,
    private val clock: Clock,
) : LibraryRepository {

    override val recentDocuments: Flow<List<DocumentFile>> =
        documentDao.observeRecent(RECENT_LIMIT).map { entities -> entities.mapNotNull { it.toDocumentFile() } }

    override val favoriteDocuments: Flow<List<DocumentFile>> =
        documentDao.observeFavorites().map { entities -> entities.mapNotNull { it.toDocumentFile() } }

    override suspend fun markOpened(document: DocumentFile) {
        val isFavorite = documentDao.find(document.uri)?.isFavorite ?: false
        documentDao.upsert(document.toEntity(lastOpenedAt = clock.millis(), isFavorite = isFavorite))
    }

    override suspend fun setFavorite(document: DocumentFile, favorite: Boolean) {
        val lastOpenedAt = documentDao.find(document.uri)?.lastOpenedAt
        if (!favorite && lastOpenedAt == null) {
            // Neither recent nor favourite any more, so nothing is left to remember.
            documentDao.delete(document.uri)
        } else {
            documentDao.upsert(document.toEntity(lastOpenedAt = lastOpenedAt, isFavorite = favorite))
        }
    }

    private fun DocumentFile.toEntity(lastOpenedAt: Long?, isFavorite: Boolean) = DocumentEntity(
        uri = uri,
        name = name,
        type = type.name,
        sizeBytes = sizeBytes,
        lastModified = lastModified,
        lastOpenedAt = lastOpenedAt,
        isFavorite = isFavorite,
    )

    // A row written by a build that knew a format this build does not is skipped, not a crash.
    private fun DocumentEntity.toDocumentFile(): DocumentFile? {
        val documentType = DocumentType.entries.firstOrNull { it.name == type } ?: return null
        return DocumentFile(uri = uri, name = name, type = documentType, sizeBytes = sizeBytes, lastModified = lastModified)
    }

    private companion object {
        const val RECENT_LIMIT = 50
    }
}
