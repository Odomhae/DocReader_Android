package com.odom.docreader.core.files

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import com.odom.docreader.core.files.di.IoDispatcher
import com.odom.docreader.core.model.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class SafDocumentScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DocumentScanner {

    override suspend fun scan(treeUri: String): ScanResult = withContext(ioDispatcher) {
        val tree = Uri.parse(treeUri)
        if (!hasPersistedReadPermission(tree)) return@withContext ScanResult.FolderUnavailable
        try {
            ScanResult.Success(listDocuments(tree))
        } catch (e: SecurityException) {
            ScanResult.FolderUnavailable
        } catch (e: FileNotFoundException) {
            ScanResult.FolderUnavailable
        } catch (e: IllegalArgumentException) {
            // DocumentsProvider reports a deleted or unmounted tree this way.
            ScanResult.FolderUnavailable
        }
    }

    private fun hasPersistedReadPermission(tree: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri == tree && it.isReadPermission }

    // Queries DocumentsContract directly: DocumentFile.listFiles() issues one query per file for
    // each attribute, which is far too slow for a large folder.
    private suspend fun listDocuments(tree: Uri): List<DocumentFile> {
        val documents = mutableListOf<DocumentFile>()
        val pendingFolderIds = ArrayDeque(listOf(DocumentsContract.getTreeDocumentId(tree)))
        while (pendingFolderIds.isNotEmpty()) {
            coroutineContext.ensureActive()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, pendingFolderIds.removeFirst())
            // The system provider answers a missing folder with a null cursor instead of throwing.
            val cursor = context.contentResolver.query(children, PROJECTION, null, null, null)
                ?: throw FileNotFoundException("No cursor for the folder")
            cursor.use {
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(0)
                    val name = cursor.getString(1).orEmpty()
                    val mimeType = cursor.getString(2)
                    if (mimeType == Document.MIME_TYPE_DIR) {
                        pendingFolderIds.addLast(documentId)
                        continue
                    }
                    val type = DocumentTypeDetector.detect(name, mimeType) ?: continue
                    documents += DocumentFile(
                        uri = DocumentsContract.buildDocumentUriUsingTree(tree, documentId).toString(),
                        name = name,
                        type = type,
                        sizeBytes = cursor.getLong(3),
                        lastModified = cursor.getLong(4),
                    )
                }
            }
        }
        return documents
    }

    private companion object {
        val PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED,
        )
    }
}
