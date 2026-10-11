package com.odom.docreader.viewer.web

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class WebViewerStatus {
    /** Before the WebView version has been checked; nothing is shown yet. */
    CHECKING,
    LOADING,
    READY,
    FAILED,
}

sealed interface WebViewerFailure {
    data class WebViewOutdated(val currentVersion: String, val requiredMajorVersion: Int) : WebViewerFailure
    data object RenderFailed : WebViewerFailure
    data object RendererCrashed : WebViewerFailure
}

data class WebViewerSearch(
    val query: String = "",
    val activeMatch: Int = 0,
    val matchCount: Int = 0,
)

data class WebViewerUiState(
    val status: WebViewerStatus = WebViewerStatus.CHECKING,
    val failure: WebViewerFailure? = null,
    val pageCount: Int = 0,
    /** 1-based. Kept across process death so the reader returns to where they were. */
    val currentPage: Int = 1,
    /** Non-null while the search bar is open. */
    val search: WebViewerSearch? = null,
)

@HiltViewModel
class WebViewerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val webViewVersionProvider: WebViewVersionProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebViewerUiState(currentPage = savedStateHandle[KEY_CURRENT_PAGE] ?: 1))
    val uiState: StateFlow<WebViewerUiState> = _uiState.asStateFlow()

    /** Decides whether this device's WebView can run the viewer page at all. */
    fun start(minWebViewMajorVersion: Int) {
        if (_uiState.value.status != WebViewerStatus.CHECKING) return
        val versionName = webViewVersionProvider.currentVersionName()
        _uiState.update {
            if (isWebViewSupported(versionName, minWebViewMajorVersion)) {
                it.copy(status = WebViewerStatus.LOADING)
            } else {
                it.copy(
                    status = WebViewerStatus.FAILED,
                    failure = WebViewerFailure.WebViewOutdated(versionName.orEmpty(), minWebViewMajorVersion),
                )
            }
        }
    }

    fun onViewerEvent(event: WebViewerEvent) {
        when (event) {
            WebViewerEvent.LoadStarted -> _uiState.update {
                if (it.status == WebViewerStatus.FAILED) it else it.copy(status = WebViewerStatus.LOADING, search = null)
            }

            is WebViewerEvent.Loaded -> _uiState.update {
                it.copy(
                    status = WebViewerStatus.READY,
                    pageCount = event.pageCount,
                    currentPage = it.currentPage.coerceIn(1, event.pageCount.coerceAtLeast(1)),
                )
            }

            is WebViewerEvent.PageChanged -> {
                savedStateHandle[KEY_CURRENT_PAGE] = event.page
                _uiState.update { it.copy(currentPage = event.page) }
            }

            WebViewerEvent.RenderFailed -> fail(WebViewerFailure.RenderFailed)
            WebViewerEvent.RendererGone -> fail(WebViewerFailure.RendererCrashed)

            is WebViewerEvent.FindResult -> _uiState.update { state ->
                state.copy(
                    search = state.search?.copy(activeMatch = event.activeMatch, matchCount = event.matchCount),
                )
            }
        }
    }

    fun onSearchOpened() {
        _uiState.update { if (it.status == WebViewerStatus.READY) it.copy(search = WebViewerSearch()) else it }
    }

    fun onSearchClosed() {
        _uiState.update { it.copy(search = null) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(search = state.search?.copy(query = query, activeMatch = 0, matchCount = 0))
        }
    }

    private fun fail(failure: WebViewerFailure) {
        _uiState.update { it.copy(status = WebViewerStatus.FAILED, failure = failure, search = null) }
    }

    private companion object {
        const val KEY_CURRENT_PAGE = "current_page"
    }
}
