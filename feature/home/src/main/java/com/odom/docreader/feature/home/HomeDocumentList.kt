package com.odom.docreader.feature.home

import com.odom.docreader.core.data.RecentDocument
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.SortOrder

internal fun buildDocumentList(
    filter: HomeFilter?,
    query: String,
    sortOrder: SortOrder,
    storageDocuments: List<DocumentFile>,
    recentDocuments: List<RecentDocument>,
): List<DocumentListItem> {
    val trimmedQuery = query.trim()
    val items = if (filter == HomeFilter.RECENT) {
        // Recency is the whole point of this filter, so the chosen sort order does not apply, and
        // the row shows when the document was opened instead of its modification date.
        recentDocuments.map { DocumentListItem(file = it.file, openedAt = it.openedAt) }
    } else {
        storageDocuments
            .filter { filter == null || it.type in filter.types }
            .sortedWith(sortOrder.comparator)
            .map { DocumentListItem(file = it) }
    }
    return items.filter { trimmedQuery.isEmpty() || it.file.name.contains(trimmedQuery, ignoreCase = true) }
}

private val byName = compareBy<DocumentFile, String>(String.CASE_INSENSITIVE_ORDER) { it.name }

private val SortOrder.comparator: Comparator<DocumentFile>
    get() = when (this) {
        SortOrder.NAME -> byName
        SortOrder.DATE -> compareByDescending<DocumentFile> { it.lastModified }.then(byName)
        SortOrder.SIZE -> compareByDescending<DocumentFile> { it.sizeBytes }.then(byName)
    }
