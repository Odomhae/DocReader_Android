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
        viewModel.onDocumentLoaded()

        assertNull(viewModel.uiState.value.failure)
    }
}
