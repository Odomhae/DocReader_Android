package com.odom.docreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.docreader.core.data.LibraryRepository
import com.odom.docreader.core.files.ExternalDocumentResolver
import com.odom.docreader.core.model.DocumentFile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A document another app asked this app to open, waiting to be shown. */
sealed interface IncomingDocument {
    data class Ready(val file: DocumentFile) : IncomingDocument
    data object Unsupported : IncomingDocument
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val externalDocumentResolver: ExternalDocumentResolver,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _incomingDocument = MutableStateFlow<IncomingDocument?>(null)
    val incomingDocument: StateFlow<IncomingDocument?> = _incomingDocument.asStateFlow()

    fun onViewIntent(uri: String, mimeType: String?, persistable: Boolean) {
        viewModelScope.launch {
            val document = externalDocumentResolver.resolve(uri, mimeType, persistable)
            if (document == null) {
                _incomingDocument.value = IncomingDocument.Unsupported
                return@launch
            }
            // A Uri with only a temporary grant would be a dead entry in the recent list next launch.
            if (document.canRemember) libraryRepository.markOpened(document.file)
            _incomingDocument.value = IncomingDocument.Ready(document.file)
        }
    }

    fun onIncomingDocumentHandled() {
        _incomingDocument.value = null
    }
}
