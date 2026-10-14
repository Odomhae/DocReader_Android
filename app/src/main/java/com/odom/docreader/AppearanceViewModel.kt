package com.odom.docreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.docreader.core.data.SettingsRepository
import com.odom.docreader.core.model.FontScale
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** The app-wide text size, applied around the whole UI. It is changed from the settings screen. */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<FontScale> =
        settingsRepository.fontScale.stateIn(viewModelScope, SharingStarted.Eagerly, FontScale.NORMAL)
}
