package com.odom.docreader.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.docreader.core.data.LibraryRepository
import com.odom.docreader.core.data.SettingsRepository
import com.odom.docreader.core.files.DocumentScanner
import com.odom.docreader.core.files.FolderAccess
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentScanner: DocumentScanner,
    private val folderAccess: FolderAccess,
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(HomeTab.ALL)
    private val query = MutableStateFlow("")
    private val folderContent = MutableStateFlow<FolderContent>(FolderContent.Loading)
    private val refreshRequests = MutableStateFlow(0)
    private var loadedFolderUri: String? = null

    val uiState: StateFlow<HomeUiState> = combine(
        folderContent,
        selectedTab,
        query,
        settingsRepository.sortOrder,
        combine(libraryRepository.recentDocuments, libraryRepository.favoriteDocuments, ::Pair),
    ) { content, tab, query, sortOrder, (recentDocuments, favoriteDocuments) ->
        HomeUiState(
            folderStatus = content.status,
            selectedTab = tab,
            query = query,
            sortOrder = sortOrder,
            documents = buildDocumentList(
                tab = tab,
                query = query,
                sortOrder = sortOrder,
                folderDocuments = (content as? FolderContent.Loaded)?.documents.orEmpty(),
                recentDocuments = recentDocuments,
                favoriteDocuments = favoriteDocuments,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    init {
        viewModelScope.launch {
            combine(settingsRepository.folderUri, refreshRequests) { folderUri, _ -> folderUri }
                .collectLatest { loadFolder(it) }
        }
    }

    /** Re-scans the current folder, e.g. after returning from another app that changed it. */
    fun refresh() {
        refreshRequests.update { it + 1 }
    }

    fun onFolderPicked(treeUri: String) {
        viewModelScope.launch {
            val previousUri = settingsRepository.folderUri.first()
            folderAccess.persist(treeUri)
            settingsRepository.setFolderUri(treeUri)
            if (previousUri != null && previousUri != treeUri) {
                folderAccess.release(previousUri)
            }
            // Picking the same folder again does not change the saved Uri, so ask explicitly.
            refresh()
        }
    }

    fun onTabSelected(tab: HomeTab) {
        selectedTab.value = tab
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

    private suspend fun loadFolder(folderUri: String?) {
        if (folderUri == null) {
            loadedFolderUri = null
            folderContent.value = FolderContent.NotSelected
            return
        }
        // A refresh of the same folder keeps the current list on screen instead of flashing a spinner.
        if (folderUri != loadedFolderUri) {
            folderContent.value = FolderContent.Loading
        }
        folderContent.value = when (val result = documentScanner.scan(folderUri)) {
            is ScanResult.Success -> FolderContent.Loaded(result.documents)
            ScanResult.FolderUnavailable -> FolderContent.Unavailable
        }
        loadedFolderUri = folderUri
    }

    private sealed interface FolderContent {
        val status: FolderStatus

        data object NotSelected : FolderContent {
            override val status = FolderStatus.NOT_SELECTED
        }

        data object Loading : FolderContent {
            override val status = FolderStatus.LOADING
        }

        data object Unavailable : FolderContent {
            override val status = FolderStatus.UNAVAILABLE
        }

        data class Loaded(val documents: List<DocumentFile>) : FolderContent {
            override val status = FolderStatus.READY
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
