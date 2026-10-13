package com.odom.docreader.viewer.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfViewerViewModelTest {
    private val viewModel = PdfViewerViewModel()

    @Test
    fun startsWithoutFailure() {
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun failureIsExposed() {
        viewModel.onDocumentFailed(PdfLoadFailure.PASSWORD_REQUIRED)

        assertEquals(PdfLoadFailure.PASSWORD_REQUIRED, viewModel.uiState.value.failure)
    }

    @Test
    fun aLaterSuccessfulLoadClearsTheFailure() {
        viewModel.onDocumentFailed(PdfLoadFailure.UNREADABLE)
        viewModel.onDocumentLoaded(pageCount = 3)

        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun loadedDocumentReportsItsPageCount() {
        viewModel.onDocumentLoaded(pageCount = 120)

        assertEquals(120, viewModel.uiState.value.pageCount)
        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun currentPageFollowsTheViewer() {
        viewModel.onDocumentLoaded(pageCount = 120)

        viewModel.onPageChanged(42)

        assertEquals(42, viewModel.uiState.value.currentPage)
    }
}
