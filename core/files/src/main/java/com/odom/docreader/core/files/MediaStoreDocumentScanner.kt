package com.odom.docreader.core.files

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import com.odom.docreader.core.files.di.IoDispatcher
import com.odom.docreader.core.model.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

// Reads the system's file index instead of walking the directory tree: it is one query, and it
// yields content:// Uris, which is what the viewers, recents and sharing all work with.
class MediaStoreDocumentScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DocumentScanner {

    override suspend fun scan(): ScanResult = withContext(ioDispatcher) {
        if (!StorageAccess.isGranted(context)) return@withContext ScanResult.AccessRequired
        try {
            ScanResult.Success(listDocuments())
        } catch (e: SecurityException) {
            // Access can be revoked between the check above and the query.
            ScanResult.AccessRequired
        }
    }

    private suspend fun listDocuments(): List<DocumentFile> {
        val files = MediaStore.Files.getContentUri(EXTERNAL_VOLUME)
        val documents = mutableListOf<DocumentFile>()
        // A null cursor means the media provider is not running right now; the next refresh retries.
        val cursor = context.contentResolver.query(files, PROJECTION, SELECTION, SELECTION_ARGS, null)
            ?: return documents
        cursor.use {
            while (cursor.moveToNext()) {
                coroutineContext.ensureActive()
                val name = cursor.getString(1)?.substringAfterLast('/') ?: continue
                val type = DocumentTypeDetector.detect(name, mimeType = null) ?: continue
                documents += DocumentFile(
                    uri = ContentUris.withAppendedId(files, cursor.getLong(0)).toString(),
                    name = name,
                    type = type,
                    sizeBytes = cursor.getLong(2),
                    lastModified = cursor.getLong(3) * MILLIS_PER_SECOND,
                )
            }
        }
        return documents
    }

    private companion object {
        // MediaStore.VOLUME_EXTERNAL, which is only declared from API 29.
        const val EXTERNAL_VOLUME = "external"
        const val MILLIS_PER_SECOND = 1000L

        // The path is the only column that reliably carries the file name on every supported
        // version: DISPLAY_NAME is not filled in for non-media files below API 29.
        @Suppress("DEPRECATION")
        const val PATH = MediaColumns.DATA

        val PROJECTION = arrayOf(MediaColumns._ID, PATH, MediaColumns.SIZE, MediaColumns.DATE_MODIFIED)

        // LIKE is case-insensitive for ASCII, so this also matches ".PDF".
        val SELECTION = DocumentTypeDetector.extensions.joinToString(" OR ") { "$PATH LIKE ?" }
        val SELECTION_ARGS = DocumentTypeDetector.extensions.map { "%.$it" }.toTypedArray()
    }
}
