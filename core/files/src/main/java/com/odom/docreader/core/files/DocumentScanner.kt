package com.odom.docreader.core.files

import com.odom.docreader.core.model.DocumentFile

sealed interface ScanResult {
    data class Success(val documents: List<DocumentFile>) : ScanResult

    /** The permission was revoked or the folder no longer exists; the user has to pick again. */
    data object FolderUnavailable : ScanResult
}

interface DocumentScanner {
    /** Lists the supported documents under a SAF tree Uri, including sub-folders. */
    suspend fun scan(treeUri: String): ScanResult
}
