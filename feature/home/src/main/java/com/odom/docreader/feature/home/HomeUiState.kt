package com.odom.docreader.feature.home

import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.core.model.SortOrder

/** First chip row. No chip selected means every format. */
enum class FormatFilter(val types: Set<DocumentType>) {
    PDF(setOf(DocumentType.PDF)),
    DOCX(setOf(DocumentType.DOCX)),
    HWP(setOf(DocumentType.HWP, DocumentType.HWPX)),
    TXT(setOf(DocumentType.TXT)),
}

/** Second chip row. No chip selected means every document on the device. */
enum class CollectionFilter {
    RECENT,
    FAVORITES,
}

enum class StorageStatus {
    LOADING,

    /** Storage access was never granted or has been revoked. */
    ACCESS_REQUIRED,
    READY,
}

data class DocumentListItem(
    val file: DocumentFile,
    val isFavorite: Boolean,
)

data class HomeUiState(
    val storageStatus: StorageStatus = StorageStatus.LOADING,
    val formatFilter: FormatFilter? = null,
    val collectionFilter: CollectionFilter? = null,
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.NAME,
    /** Already filtered and ordered for the filters, [query] and [sortOrder]. */
    val documents: List<DocumentListItem> = emptyList(),
)
