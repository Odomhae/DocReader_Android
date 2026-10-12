package com.odom.docreader.feature.home

import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.core.model.SortOrder

internal fun buildDocumentList(
    tab: HomeTab,
    query: String,
    sortOrder: SortOrder,
    storageDocuments: List<DocumentFile>,
    recentDocuments: List<DocumentFile>,
    favoriteDocuments: List<DocumentFile>,
): List<DocumentListItem> {
    val documents = when (tab) {
        HomeTab.ALL -> storageDocuments.sortedWith(sortOrder.comparator)
        HomeTab.PDF -> storageDocuments.filter { it.type == DocumentType.PDF }.sortedWith(sortOrder.comparator)
        HomeTab.DOCX -> storageDocuments.filter { it.type == DocumentType.DOCX }.sortedWith(sortOrder.comparator)
        HomeTab.HWP -> storageDocuments
            .filter { it.type == DocumentType.HWP || it.type == DocumentType.HWPX }
            .sortedWith(sortOrder.comparator)
        // Recency is the whole point of this tab, so the chosen sort order does not apply.
        HomeTab.RECENT -> recentDocuments
        HomeTab.FAVORITES -> favoriteDocuments.sortedWith(sortOrder.comparator)
    }
    val trimmedQuery = query.trim()
    val favoriteUris = favoriteDocuments.mapTo(HashSet()) { it.uri }
    return documents
        .filter { trimmedQuery.isEmpty() || it.name.contains(trimmedQuery, ignoreCase = true) }
        .map { DocumentListItem(file = it, isFavorite = it.uri in favoriteUris) }
}

private val byName = compareBy<DocumentFile, String>(String.CASE_INSENSITIVE_ORDER) { it.name }

private val SortOrder.comparator: Comparator<DocumentFile>
    get() = when (this) {
        SortOrder.NAME -> byName
        SortOrder.DATE -> compareByDescending<DocumentFile> { it.lastModified }.then(byName)
        SortOrder.SIZE -> compareByDescending<DocumentFile> { it.sizeBytes }.then(byName)
    }
