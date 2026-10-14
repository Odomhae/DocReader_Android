package com.odom.docreader.feature.settings

import com.odom.docreader.core.data.SettingsRepository
import com.odom.docreader.core.model.FontScale
import com.odom.docreader.core.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val repository = FakeSettingsRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // uiState only runs while it has a subscriber.
    private fun TestScope.createViewModel(): SettingsViewModel {
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun showsTheSavedFontScale() = runTest {
        repository.fontScale.value = FontScale.SMALL

        assertEquals(FontScale.SMALL, createViewModel().uiState.value.fontScale)
    }

    @Test
    fun selectedFontScaleIsSaved() = runTest {
        val viewModel = createViewModel()

        viewModel.onFontScaleSelected(FontScale.EXTRA_LARGE)

        assertEquals(FontScale.EXTRA_LARGE, repository.fontScale.value)
        assertEquals(FontScale.EXTRA_LARGE, viewModel.uiState.value.fontScale)
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val sortOrder = MutableStateFlow(SortOrder.NAME)
        override val fontScale = MutableStateFlow(FontScale.NORMAL)

        override suspend fun setSortOrder(sortOrder: SortOrder) {
            this.sortOrder.value = sortOrder
        }

        override suspend fun setFontScale(fontScale: FontScale) {
            this.fontScale.value = fontScale
        }
    }
}
