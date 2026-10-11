package com.odom.docreader.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OpenedLicense(
    val library: OpenSourceLibrary,
    /** Null while the text is being read. */
    val text: String?,
)

data class LicensesUiState(
    val libraries: List<OpenSourceLibrary> = emptyList(),
    /** The library whose full license text is on screen, if any. */
    val opened: OpenedLicense? = null,
)

@HiltViewModel
class LicensesViewModel @Inject constructor(
    private val licenseTextSource: LicenseTextSource,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LicensesUiState(libraries = OPEN_SOURCE_LIBRARIES))
    val uiState: StateFlow<LicensesUiState> = _uiState.asStateFlow()

    fun onLibrarySelected(library: OpenSourceLibrary) {
        _uiState.update { it.copy(opened = OpenedLicense(library, text = null)) }
        viewModelScope.launch {
            val text = licenseTextSource.load(library.licenseAsset)
            // The user may have closed this one, or opened another, while it was loading.
            _uiState.update { state ->
                if (state.opened?.library == library) state.copy(opened = OpenedLicense(library, text)) else state
            }
        }
    }

    fun onLicenseClosed() {
        _uiState.update { it.copy(opened = null) }
    }
}
