package com.odom.docreader.viewer.pdf

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PdfViewerUiState(
    val failure: PdfLoadFailure? = null,
)

/** Holds what androidx.pdf's fragment reports, so it survives the fragment being recreated. */
class PdfViewerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    fun onDocumentLoaded() {
        _uiState.update { it.copy(failure = null) }
    }

    fun onDocumentFailed(failure: PdfLoadFailure) {
        _uiState.update { it.copy(failure = failure) }
    }
}
