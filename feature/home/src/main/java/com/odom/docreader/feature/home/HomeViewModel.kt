package com.odom.docreader.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.docreader.core.data.LibraryRepository
import com.odom.docreader.core.data.SettingsRepository
import com.odom.docreader.core.files.DocumentScanner
import com.odom.docreader.core.files.ScanResult
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentScanner: DocumentScanner,
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val filters = MutableStateFlow(Filters())
    private val query = MutableStateFlow("")
    private val storageContent = MutableStateFlow<StorageContent>(StorageContent.Loading)
    private val refreshRequests = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = combine(
        storageContent,
        filters,
        query,
        settingsRepository.sortOrder,
        combine(libraryRepository.recentDocuments, libraryRepository.favoriteDocuments, ::Pair),
    ) { content, filters, query, sortOrder, (recentDocuments, favoriteDocuments) ->
        HomeUiState(
            storageStatus = content.status,
            formatFilter = filters.format,
            collectionFilter = filters.collection,
            query = query,
            sortOrder = sortOrder,
            documents = buildDocumentList(
                format = filters.format,
                collection = filters.collection,
                query = query,
                sortOrder = sortOrder,
                storageDocuments = (content as? StorageContent.Loaded)?.documents.orEmpty(),
                recentDocuments = recentDocuments,
                favoriteDocuments = favoriteDocuments,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    init {
        viewModelScope.launch {
            refreshRequests.collectLatest { loadDocuments() }
        }
    }

    /** Re-scans storage, e.g. after returning from system settings or an app that changed files. */
    fun refresh() {
        refreshRequests.update { it + 1 }
    }

    /** Selecting the chip that is already on turns it off again. */
    fun onFormatFilterToggled(format: FormatFilter) {
        filters.update { current -> current.copy(format = if (current.format == format) null else format) }
    }

    fun onCollectionFilterToggled(collection: CollectionFilter) {
        filters.update { current ->
            current.copy(collection = if (current.collection == collection) null else collection)
        }
    }

    fun onQueryChanged(query: String) {
        this.query.value = query
    }

    fun onSortOrderSelected(sortOrder: SortOrder) {
        viewModelScope.launch { settingsRepository.setSortOrder(sortOrder) }
    }

    fun onToggleFavorite(item: DocumentListItem) {
        viewModelScope.launch { libraryRepository.setFavorite(item.file, favorite = !item.isFavorite) }
    }

    fun onDocumentOpened(document: DocumentFile) {
        viewModelScope.launch { libraryRepository.markOpened(document) }
    }

    // The previous content stays in place while scanning, so a refresh does not flash a spinner.
    private suspend fun loadDocuments() {
        storageContent.value = when (val result = documentScanner.scan()) {
            is ScanResult.Success -> StorageContent.Loaded(result.documents)
            ScanResult.AccessRequired -> StorageContent.AccessRequired
        }
    }

    private data class Filters(val format: FormatFilter? = null, val collection: CollectionFilter? = null)

    private sealed interface StorageContent {
        val status: StorageStatus

        data object Loading : StorageContent {
            override val status = StorageStatus.LOADING
        }

        data object AccessRequired : StorageContent {
            override val status = StorageStatus.ACCESS_REQUIRED
        }

        data class Loaded(val documents: List<DocumentFile>) : StorageContent {
            override val status = StorageStatus.READY
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
