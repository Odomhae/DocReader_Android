package com.odom.docreader.core.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.odom.docreader.core.files.di.IoDispatcher
import com.odom.docreader.core.model.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

data class ExternalDocument(
    val file: DocumentFile,
    /**
     * True if read access was persisted, so the Uri still works after this launch. Only then may
     * the document be listed under recent documents.
     */
    val canRemember: Boolean,
)

interface ExternalDocumentResolver {
    /**
     * Identifies a document another app asked this app to view. Returns null if it is not a
     * supported format or cannot be read.
     *
     * @param persistable whether the sender offered a persistable permission grant
     */
    suspend fun resolve(uri: String, mimeType: String?, persistable: Boolean): ExternalDocument?
}

class ContentExternalDocumentResolver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ExternalDocumentResolver {

    override suspend fun resolve(uri: String, mimeType: String?, persistable: Boolean): ExternalDocument? =
        withContext(ioDispatcher) {
            val documentUri = Uri.parse(uri)
            val metadata = queryMetadata(documentUri)
            val name = metadata?.name ?: documentUri.lastPathSegment.orEmpty()
            val type = DocumentTypeDetector.detect(
                fileName = name,
                mimeType = mimeType ?: context.contentResolver.getType(documentUri),
                header = readHeader(documentUri),
            ) ?: return@withContext null
            ExternalDocument(
                file = DocumentFile(
                    uri = uri,
                    name = name,
                    type = type,
                    sizeBytes = metadata?.sizeBytes ?: 0,
                    // Not every provider reports a modification time; "now" is the closest honest value.
                    lastModified = metadata?.lastModified ?: System.currentTimeMillis(),
                ),
                canRemember = persistable && persistReadPermission(documentUri),
            )
        }

    private class Metadata(val name: String?, val sizeBytes: Long?, val lastModified: Long?)

    // All columns are requested and looked up by name: providers differ in which ones they support,
    // and some reject a projection naming a column they do not know.
    private fun queryMetadata(uri: Uri): Metadata? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            fun column(name: String) = cursor.getColumnIndex(name).takeIf { it >= 0 && !cursor.isNull(it) }
            Metadata(
                name = column(OpenableColumns.DISPLAY_NAME)?.let(cursor::getString),
                sizeBytes = column(OpenableColumns.SIZE)?.let(cursor::getLong),
                lastModified = column(DocumentsContract.Document.COLUMN_LAST_MODIFIED)?.let(cursor::getLong),
            )
        }
    } catch (e: SecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun readHeader(uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val header = ByteArray(DocumentTypeDetector.HEADER_SIZE)
            var filled = 0
            while (filled < header.size) {
                val read = input.read(header, filled, header.size - filled)
                if (read < 0) break
                filled += read
            }
            header.copyOf(filled)
        }
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }

    private fun persistReadPermission(uri: Uri): Boolean = try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        true
    } catch (e: SecurityException) {
        // The sender set the flag but the grant is not actually persistable.
        false
    }
}
