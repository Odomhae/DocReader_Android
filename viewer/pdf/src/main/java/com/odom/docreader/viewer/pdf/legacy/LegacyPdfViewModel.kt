package com.odom.docreader.viewer.pdf.legacy

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.docreader.viewer.pdf.PdfLoadFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LegacyPdfUiState {
    data object Loading : LegacyPdfUiState
    data class Ready(val pageCount: Int) : LegacyPdfUiState
    data class Failed(val failure: PdfLoadFailure) : LegacyPdfUiState
}

@HiltViewModel
class LegacyPdfViewModel @Inject constructor(
    private val documentSource: PdfDocumentSource,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LegacyPdfUiState>(LegacyPdfUiState.Loading)
    val uiState: StateFlow<LegacyPdfUiState> = _uiState.asStateFlow()

    private var openedUri: String? = null
    private var pages: PdfPages? = null

    fun open(uri: String) {
        // Called again on every recomposition host change (rotation); the document is already open.
        if (uri == openedUri) return
        openedUri = uri
        viewModelScope.launch {
            _uiState.value = when (val result = documentSource.open(uri)) {
                is PdfOpenResult.Opened -> {
                    pages = result.pages
                    LegacyPdfUiState.Ready(result.pages.pageCount)
                }

                is PdfOpenResult.Failed -> LegacyPdfUiState.Failed(result.failure)
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap? = pages?.render(pageIndex, widthPx)

    override fun onCleared() {
        pages?.close()
    }
}
