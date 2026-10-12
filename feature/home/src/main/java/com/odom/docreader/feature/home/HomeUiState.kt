package com.odom.docreader.feature.home

import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.SortOrder

enum class HomeTab {
    ALL,
    PDF,
    DOCX,
    HWP,
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
    val selectedTab: HomeTab = HomeTab.ALL,
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.NAME,
    /** Already filtered and ordered for [selectedTab], [query] and [sortOrder]. */
    val documents: List<DocumentListItem> = emptyList(),
)
