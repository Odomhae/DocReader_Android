package com.odom.docreader.viewer.web

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebViewerViewModelTest {
    private val savedStateHandle = SavedStateHandle()

    private fun createViewModel(webViewVersion: String? = "133.0.6943.137") =
        WebViewerViewModel(savedStateHandle, object : WebViewVersionProvider {
            override fun currentVersionName(): String? = webViewVersion
        })

    private fun readyViewModel(pageCount: Int = 10): WebViewerViewModel = createViewModel().apply {
        start(minWebViewMajorVersion = 96)
        onViewerEvent(WebViewerEvent.LoadStarted)
        onViewerEvent(WebViewerEvent.Loaded(pageCount))
    }

    @Test
    fun nothingIsShownUntilTheWebViewIsChecked() {
        assertEquals(WebViewerStatus.CHECKING, createViewModel().uiState.value.status)
    }

    @Test
    fun supportedWebViewStartsLoading() {
        val viewModel = createViewModel()

        viewModel.start(minWebViewMajorVersion = 96)

        assertEquals(WebViewerStatus.LOADING, viewModel.uiState.value.status)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun outdatedWebViewFailsWithBothVersions() {
        val viewModel = createViewModel(webViewVersion = "69.0.3497.100")

        viewModel.start(minWebViewMajorVersion = 96)

        assertEquals(WebViewerStatus.FAILED, viewModel.uiState.value.status)
        assertEquals(WebViewerFailure.WebViewOutdated("69.0.3497.100", 96), viewModel.uiState.value.failure)
    }

    @Test
    fun unreadableWebViewVersionIsNotBlocked() {
        val viewModel = createViewModel(webViewVersion = null)

        viewModel.start(minWebViewMajorVersion = 96)

        assertEquals(WebViewerStatus.LOADING, viewModel.uiState.value.status)
    }

    @Test
    fun loadedDocumentIsReadyWithItsPageCount() {
        val viewModel = readyViewModel(pageCount = 12)

        assertEquals(WebViewerStatus.READY, viewModel.uiState.value.status)
        assertEquals(12, viewModel.uiState.value.pageCount)
        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun pageChangesAreTrackedAndSaved() {
        val viewModel = readyViewModel()

        viewModel.onViewerEvent(WebViewerEvent.PageChanged(7))

        assertEquals(7, viewModel.uiState.value.currentPage)
        assertEquals(7, savedStateHandle.get<Int>("current_page"))
    }

    @Test
    fun savedPageIsRestoredAfterProcessDeath() {
        savedStateHandle["current_page"] = 7

        assertEquals(7, readyViewModel(pageCount = 10).uiState.value.currentPage)
    }

    @Test
    fun savedPageBeyondTheDocumentIsClamped() {
        savedStateHandle["current_page"] = 40

        assertEquals(10, readyViewModel(pageCount = 10).uiState.value.currentPage)
    }

    @Test
    fun reloadAfterRotationGoesBackToLoadingAndKeepsThePage() {
        val viewModel = readyViewModel()
        viewModel.onViewerEvent(WebViewerEvent.PageChanged(4))

        viewModel.onViewerEvent(WebViewerEvent.LoadStarted)

        assertEquals(WebViewerStatus.LOADING, viewModel.uiState.value.status)
        assertEquals(4, viewModel.uiState.value.currentPage)
    }

    @Test
    fun renderFailureIsExposed() {
        val viewModel = createViewModel().apply { start(96) }

        viewModel.onViewerEvent(WebViewerEvent.RenderFailed)

        assertEquals(WebViewerStatus.FAILED, viewModel.uiState.value.status)
        assertEquals(WebViewerFailure.RenderFailed, viewModel.uiState.value.failure)
    }

    @Test
    fun rendererCrashIsExposed() {
        val viewModel = readyViewModel()

        viewModel.onViewerEvent(WebViewerEvent.RendererGone)

        assertEquals(WebViewerFailure.RendererCrashed, viewModel.uiState.value.failure)
    }

    @Test
    fun aFailureIsNotClearedByALaterLoadStart() {
        val viewModel = createViewModel(webViewVersion = "69.0.3497.100").apply { start(96) }

        viewModel.onViewerEvent(WebViewerEvent.LoadStarted)

        assertEquals(WebViewerStatus.FAILED, viewModel.uiState.value.status)
    }

    @Test
    fun searchOpensOnlyOnceTheDocumentIsReady() {
        val loading = createViewModel().apply { start(96) }
        loading.onSearchOpened()
        assertNull(loading.uiState.value.search)

        val ready = readyViewModel()
        ready.onSearchOpened()
        assertEquals(WebViewerSearch(), ready.uiState.value.search)
    }

    @Test
    fun searchTracksQueryAndMatches() {
        val viewModel = readyViewModel().apply { onSearchOpened() }

        viewModel.onSearchQueryChanged("fox")
        viewModel.onViewerEvent(WebViewerEvent.FindResult(activeMatch = 1, matchCount = 3))

        assertEquals(WebViewerSearch("fox", activeMatch = 1, matchCount = 3), viewModel.uiState.value.search)
    }

    @Test
    fun changingTheQueryResetsTheCountsUntilNewResultsArrive() {
        val viewModel = readyViewModel().apply { onSearchOpened() }
        viewModel.onSearchQueryChanged("fox")
        viewModel.onViewerEvent(WebViewerEvent.FindResult(activeMatch = 1, matchCount = 3))

        viewModel.onSearchQueryChanged("foxes")

        assertEquals(WebViewerSearch("foxes", activeMatch = 0, matchCount = 0), viewModel.uiState.value.search)
    }

    @Test
    fun findResultsAfterSearchWasClosedAreIgnored() {
        val viewModel = readyViewModel().apply { onSearchOpened() }
        viewModel.onSearchClosed()

        viewModel.onViewerEvent(WebViewerEvent.FindResult(activeMatch = 1, matchCount = 3))

        assertNull(viewModel.uiState.value.search)
    }
}
