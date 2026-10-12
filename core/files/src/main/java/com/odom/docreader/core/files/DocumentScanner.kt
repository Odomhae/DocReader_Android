package com.odom.docreader.core.files

import com.odom.docreader.core.model.DocumentFile

sealed interface ScanResult {
    data class Success(val documents: List<DocumentFile>) : ScanResult

    /** Storage access was never granted or has been revoked; the user has to grant it. */
    data object AccessRequired : ScanResult
}

interface DocumentScanner {
    /** Lists the supported documents on shared storage. */
    suspend fun scan(): ScanResult
}
