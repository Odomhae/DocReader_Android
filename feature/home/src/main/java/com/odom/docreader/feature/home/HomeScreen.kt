package com.odom.docreader.feature.home

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
    val requestStorageAccess = rememberStorageAccessRequest()
    // Files can be added or removed, and access granted or revoked in system settings, while the
    // app is in the background.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    HomeContent(
        uiState = uiState,
        onGrantAccess = requestStorageAccess,
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
    onGrantAccess: () -> Unit,
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
                onOpenSettings = onOpenSettings,
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (uiState.storageStatus) {
            StorageStatus.ACCESS_REQUIRED -> EmptyState(
                title = stringResource(R.string.home_access_required_title),
                message = stringResource(R.string.home_access_required_message),
                actionLabel = stringResource(R.string.home_grant_access),
                onAction = onGrantAccess,
                modifier = contentModifier,
            )

            StorageStatus.LOADING -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            StorageStatus.READY -> Column(modifier = contentModifier) {
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
        else -> R.string.home_empty_documents
    },
)
