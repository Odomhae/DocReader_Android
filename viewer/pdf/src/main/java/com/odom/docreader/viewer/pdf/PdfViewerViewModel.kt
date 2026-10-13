package com.odom.docreader.viewer.pdf

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PdfViewerUiState(
    val failure: PdfLoadFailure? = null,
    /** 0 until the document has loaded. */
    val pageCount: Int = 0,
    /** 1-based. */
    val currentPage: Int = 1,
)

/** Holds what androidx.pdf's fragment reports, so it survives the fragment being recreated. */
class PdfViewerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    fun onDocumentLoaded(pageCount: Int) {
        _uiState.update { it.copy(failure = null, pageCount = pageCount) }
    }

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun onDocumentFailed(failure: PdfLoadFailure) {
        _uiState.update { it.copy(failure = failure) }
    }
}
