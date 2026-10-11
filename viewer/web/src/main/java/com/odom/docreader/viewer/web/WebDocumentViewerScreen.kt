package com.odom.docreader.viewer.web

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.docreader.core.files.openWithOtherApp
import com.odom.docreader.core.files.shareDocument
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.core.ui.R as UiR

/** What a format module tells the host: where its viewer page is and what it needs to run. */
data class WebViewerConfig(
    /** Path under assets of the HTML entry point, e.g. `hwp/viewer.html`. */
    val entryAssetPath: String,
    val minWebViewMajorVersion: Int,
    /** Only used to label the document for sharing and "open with". */
    val documentType: DocumentType,
    /** True if the page implements `docViewer.find` itself; otherwise WebView's find-in-page is used. */
    val pageHandlesSearch: Boolean = false,
)

@Composable
fun WebDocumentViewerScreen(
    config: WebViewerConfig,
    uri: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WebViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val documentUri = remember(uri) { Uri.parse(uri) }
    val controller = remember(config) { WebViewerController(config.pageHandlesSearch) }
    val search = uiState.search

    LaunchedEffect(config) { viewModel.start(config.minWebViewMajorVersion) }
    BackHandler(enabled = search != null) {
        controller.clearFind()
        viewModel.onSearchClosed()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (search != null) {
                SearchTopBar(
                    search = search,
                    onQueryChanged = { query ->
                        viewModel.onSearchQueryChanged(query)
                        controller.find(query)
                    },
                    onFindNext = controller::findNext,
                    onClose = {
                        controller.clearFind()
                        viewModel.onSearchClosed()
                    },
                )
            } else {
                ViewerTopBar(
                    title = title,
                    onBack = onBack,
                    onSearch = if (uiState.status == WebViewerStatus.READY) viewModel::onSearchOpened else null,
                    onShare = { shareDocument(context, documentUri, config.documentType) },
                )
            }
        },
        bottomBar = {
            if (uiState.status == WebViewerStatus.READY && uiState.pageCount > 0) {
                PageBar(
                    currentPage = uiState.currentPage,
                    pageCount = uiState.pageCount,
                    onGoToPage = controller::goToPage,
                )
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        val failure = uiState.failure
        when {
            uiState.status == WebViewerStatus.CHECKING -> Unit

            failure != null -> FailureContent(
                failure = failure,
                onOpenWithOtherApp = { openWithOtherApp(context, documentUri, config.documentType) },
                modifier = contentModifier,
            )

            else -> Box(modifier = contentModifier) {
                WebDocumentView(
                    entryAssetPath = config.entryAssetPath,
                    documentUri = documentUri,
                    controller = controller,
                    onEvent = { event ->
                        // Read before the event is applied: this is the page saved from the last session.
                        val pageToRestore = uiState.currentPage
                        viewModel.onViewerEvent(event)
                        if (event is WebViewerEvent.Loaded && pageToRestore > 1) {
                            controller.goToPage(pageToRestore)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                if (uiState.status == WebViewerStatus.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerTopBar(title: String, onBack: () -> Unit, onSearch: (() -> Unit)?, onShare: () -> Unit) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(UiR.drawable.ic_arrow_back), contentDescription = stringResource(R.string.web_back))
            }
        },
        actions = {
            if (onSearch != null) {
                IconButton(onClick = onSearch) {
                    Icon(
                        painterResource(UiR.drawable.ic_search),
                        contentDescription = stringResource(R.string.web_search),
                    )
                }
            }
            IconButton(onClick = onShare) {
                Icon(painterResource(UiR.drawable.ic_share), contentDescription = stringResource(R.string.web_share))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    search: WebViewerSearch,
    onQueryChanged: (String) -> Unit,
    onFindNext: (forward: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    TopAppBar(
        title = {
            TextField(
                value = search.query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.web_search_hint)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painterResource(UiR.drawable.ic_close),
                    contentDescription = stringResource(R.string.web_search_close),
                )
            }
        },
        actions = {
            if (search.query.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.web_match_count, search.activeMatch, search.matchCount),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            IconButton(onClick = { onFindNext(false) }, enabled = search.matchCount > 0) {
                Icon(
                    painterResource(UiR.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.web_search_previous),
                )
            }
            IconButton(onClick = { onFindNext(true) }, enabled = search.matchCount > 0) {
                Icon(
                    painterResource(UiR.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.web_search_next),
                )
            }
        },
    )
}

@Composable
private fun PageBar(currentPage: Int, pageCount: Int, onGoToPage: (Int) -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onGoToPage(currentPage - 1) }, enabled = currentPage > 1) {
                Icon(
                    painterResource(UiR.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.web_previous_page),
                )
            }
            Text(
                text = stringResource(R.string.web_page_indicator, currentPage, pageCount),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            IconButton(onClick = { onGoToPage(currentPage + 1) }, enabled = currentPage < pageCount) {
                Icon(
                    painterResource(UiR.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.web_next_page),
                )
            }
        }
    }
}

@Composable
private fun FailureContent(failure: WebViewerFailure, onOpenWithOtherApp: () -> Unit, modifier: Modifier = Modifier) {
    val message = when (failure) {
        is WebViewerFailure.WebViewOutdated -> stringResource(
            R.string.web_error_webview_outdated,
            failure.currentVersion,
            failure.requiredMajorVersion,
        )

        WebViewerFailure.RenderFailed -> stringResource(R.string.web_error_render)
        WebViewerFailure.RendererCrashed -> stringResource(R.string.web_error_renderer_crashed)
    }
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.web_error_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onOpenWithOtherApp) { Text(stringResource(R.string.web_open_with_other_app)) }
    }
}
