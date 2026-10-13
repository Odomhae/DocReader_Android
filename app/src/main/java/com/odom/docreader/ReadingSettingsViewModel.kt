package com.odom.docreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.docreader.core.data.ReadingSettingsRepository
import com.odom.docreader.core.model.PaperColor
import com.odom.docreader.core.model.ReadingSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Paper color and brightness shared by every viewer. */
@HiltViewModel
class ReadingSettingsViewModel @Inject constructor(
    private val repository: ReadingSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReadingSettings())
    val uiState: StateFlow<ReadingSettings> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { repository.readingSettings.collect { _uiState.value = it } }
    }

    fun onPaperColorSelected(paperColor: PaperColor) {
        viewModelScope.launch { repository.setPaperColor(paperColor) }
    }

    fun onUseSystemBrightnessChanged(useSystemBrightness: Boolean) {
        viewModelScope.launch { repository.setUseSystemBrightness(useSystemBrightness) }
    }

    /** Called for every slider movement. Saving waits for the drag to end so it is one write, not one per frame. */
    fun onBrightnessChanged(brightness: Float) {
        _uiState.update { it.copy(useSystemBrightness = false, brightness = brightness) }
    }

    fun onBrightnessChangeFinished() {
        viewModelScope.launch { repository.setBrightness(_uiState.value.brightness) }
    }
}
