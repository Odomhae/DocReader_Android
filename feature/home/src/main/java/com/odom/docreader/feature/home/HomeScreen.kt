package com.odom.docreader.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.SortOrder
import com.odom.docreader.core.ui.EmptyState

@Composable
fun HomeScreen(
    onOpenDocument: (DocumentFile) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.onFolderPicked(uri.toString())
    }
    // Files can be added or removed while the app is in the background.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    HomeContent(
        uiState = uiState,
        onPickFolder = { folderPicker.launch(null) },
        onOpenSettings = onOpenSettings,
        onQueryChanged = viewModel::onQueryChanged,
        onSortOrderSelected = viewModel::onSortOrderSelected,
        onTabSelected = viewModel::onTabSelected,
        onToggleFavorite = viewModel::onToggleFavorite,
        onOpenDocument = { document ->
            viewModel.onDocumentOpened(document)
            onOpenDocument(document)
        },
        modifier = modifier,
    )
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onPickFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSortOrderSelected: (SortOrder) -> Unit,
    onTabSelected: (HomeTab) -> Unit,
    onToggleFavorite: (DocumentListItem) -> Unit,
    onOpenDocument: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(
                sortOrder = uiState.sortOrder,
                onSortOrderSelected = onSortOrderSelected,
                onPickFolder = onPickFolder,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (uiState.folderStatus) {
            FolderStatus.NOT_SELECTED -> EmptyState(
                title = stringResource(R.string.home_no_folder_title),
                message = stringResource(R.string.home_no_folder_message),
                actionLabel = stringResource(R.string.home_pick_folder),
                onAction = onPickFolder,
                modifier = contentModifier,
            )

            FolderStatus.UNAVAILABLE -> EmptyState(
                title = stringResource(R.string.home_folder_unavailable_title),
                message = stringResource(R.string.home_folder_unavailable_message),
                actionLabel = stringResource(R.string.home_reselect_folder),
                onAction = onPickFolder,
                modifier = contentModifier,
            )

            FolderStatus.LOADING -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            FolderStatus.READY -> Column(modifier = contentModifier) {
                HomeSearchField(query = uiState.query, onQueryChanged = onQueryChanged)
                HomeTabRow(selectedTab = uiState.selectedTab, onTabSelected = onTabSelected)
                if (uiState.documents.isEmpty()) {
                    EmptyState(message = emptyMessage(uiState))
                } else {
                    DocumentList(
                        documents = uiState.documents,
                        onToggleFavorite = onToggleFavorite,
                        onOpenDocument = onOpenDocument,
                    )
                }
            }
        }
    }
}

@Composable
private fun emptyMessage(uiState: HomeUiState): String = stringResource(
    when {
        uiState.query.isNotBlank() -> R.string.home_empty_search
        uiState.selectedTab == HomeTab.RECENT -> R.string.home_empty_recent
        uiState.selectedTab == HomeTab.FAVORITES -> R.string.home_empty_favorites
        else -> R.string.home_empty_folder
    },
)
