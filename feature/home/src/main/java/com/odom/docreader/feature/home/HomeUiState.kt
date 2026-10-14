package com.odom.docreader.feature.home

import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.core.model.SortOrder

/** The chip row; at most one is on. No chip selected means every document on the device. */
enum class HomeFilter(val types: Set<DocumentType>) {
    /** Documents the user opened, any format. */
    RECENT(DocumentType.entries.toSet()),
    PDF(setOf(DocumentType.PDF)),
    DOCX(setOf(DocumentType.DOCX)),
    HWP(setOf(DocumentType.HWP, DocumentType.HWPX)),
    TXT(setOf(DocumentType.TXT)),
}

enum class StorageStatus {
    LOADING,

    /** Storage access was never granted or has been revoked. */
    ACCESS_REQUIRED,
    READY,
}

data class DocumentListItem(
    val file: DocumentFile,
    /** When the document was last opened, set only while the recent list is shown. Epoch milliseconds. */
    val openedAt: Long? = null,
)

data class HomeUiState(
    val storageStatus: StorageStatus = StorageStatus.LOADING,
    val filter: HomeFilter? = null,
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.NAME,
    /** Already filtered and ordered for [filter], [query] and [sortOrder]. */
    val documents: List<DocumentListItem> = emptyList(),
)
