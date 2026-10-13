package com.odom.docreader

import com.odom.docreader.core.data.ReadingSettingsRepository
import com.odom.docreader.core.model.PaperColor
import com.odom.docreader.core.model.ReadingSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingSettingsViewModelTest {
    private val repository = FakeReadingSettingsRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsWhatIsSaved() = runTest {
        repository.readingSettings.value = ReadingSettings(paperColor = PaperColor.SEPIA)

        assertEquals(PaperColor.SEPIA, ReadingSettingsViewModel(repository).uiState.value.paperColor)
    }

    @Test
    fun paperColorIsSaved() = runTest {
        val viewModel = ReadingSettingsViewModel(repository)

        viewModel.onPaperColorSelected(PaperColor.SEPIA)

        assertEquals(PaperColor.SEPIA, repository.readingSettings.value.paperColor)
        assertEquals(PaperColor.SEPIA, viewModel.uiState.value.paperColor)
    }

    @Test
    fun draggingTheSliderAppliesBrightnessWithoutSavingIt() = runTest {
        val viewModel = ReadingSettingsViewModel(repository)

        viewModel.onBrightnessChanged(0.3f)

        assertEquals(ReadingSettings(useSystemBrightness = false, brightness = 0.3f), viewModel.uiState.value)
        assertEquals(ReadingSettings(), repository.readingSettings.value)
    }

    @Test
    fun brightnessIsSavedWhenTheDragEnds() = runTest {
        val viewModel = ReadingSettingsViewModel(repository)
        viewModel.onBrightnessChanged(0.3f)

        viewModel.onBrightnessChangeFinished()

        assertEquals(ReadingSettings(useSystemBrightness = false, brightness = 0.3f), repository.readingSettings.value)
    }

    @Test
    fun followingTheSystemAgainIsSaved() = runTest {
        repository.readingSettings.value = ReadingSettings(useSystemBrightness = false, brightness = 0.3f)
        val viewModel = ReadingSettingsViewModel(repository)

        viewModel.onUseSystemBrightnessChanged(true)

        assertEquals(ReadingSettings(useSystemBrightness = true, brightness = 0.3f), viewModel.uiState.value)
    }

    private class FakeReadingSettingsRepository : ReadingSettingsRepository {
        override val readingSettings = MutableStateFlow(ReadingSettings())

        override suspend fun setPaperColor(paperColor: PaperColor) {
            readingSettings.update { it.copy(paperColor = paperColor) }
        }

        override suspend fun setUseSystemBrightness(useSystemBrightness: Boolean) {
            readingSettings.update { it.copy(useSystemBrightness = useSystemBrightness) }
        }

        override suspend fun setBrightness(brightness: Float) {
            readingSettings.update { it.copy(useSystemBrightness = false, brightness = brightness) }
        }
    }
}
