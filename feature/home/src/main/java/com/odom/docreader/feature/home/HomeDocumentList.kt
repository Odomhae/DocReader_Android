package com.odom.docreader.feature.home

import com.odom.docreader.core.data.RecentDocument
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.SortOrder

internal fun buildDocumentList(
    format: FormatFilter?,
    collection: CollectionFilter?,
    query: String,
    sortOrder: SortOrder,
    storageDocuments: List<DocumentFile>,
    recentDocuments: List<RecentDocument>,
    favoriteDocuments: List<DocumentFile>,
): List<DocumentListItem> {
    val documents = when (collection) {
        null -> storageDocuments.sortedWith(sortOrder.comparator)
        // Recency is the whole point of this filter, so the chosen sort order does not apply.
        CollectionFilter.RECENT -> recentDocuments.map { it.file }
        CollectionFilter.FAVORITES -> favoriteDocuments.sortedWith(sortOrder.comparator)
    }
    // Only the recent list shows when a document was opened; elsewhere the row shows its modification date.
    val openedAt = if (collection == CollectionFilter.RECENT) {
        recentDocuments.associate { it.file.uri to it.openedAt }
    } else {
        emptyMap()
    }
    val trimmedQuery = query.trim()
    val favoriteUris = favoriteDocuments.mapTo(HashSet()) { it.uri }
    return documents
        .filter { format == null || it.type in format.types }
        .filter { trimmedQuery.isEmpty() || it.name.contains(trimmedQuery, ignoreCase = true) }
        .map { DocumentListItem(file = it, isFavorite = it.uri in favoriteUris, openedAt = openedAt[it.uri]) }
}

private val byName = compareBy<DocumentFile, String>(String.CASE_INSENSITIVE_ORDER) { it.name }

private val SortOrder.comparator: Comparator<DocumentFile>
    get() = when (this) {
        SortOrder.NAME -> byName
        SortOrder.DATE -> compareByDescending<DocumentFile> { it.lastModified }.then(byName)
        SortOrder.SIZE -> compareByDescending<DocumentFile> { it.sizeBytes }.then(byName)
    }
