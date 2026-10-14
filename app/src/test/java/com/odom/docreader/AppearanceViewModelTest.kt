package com.odom.docreader

import com.odom.docreader.core.data.SettingsRepository
import com.odom.docreader.core.model.FontScale
import com.odom.docreader.core.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    private val repository = FakeSettingsRepository()

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
        repository.fontScale.value = FontScale.LARGE

        assertEquals(FontScale.LARGE, AppearanceViewModel(repository).uiState.value)
    }

    @Test
    fun followsChangesMadeElsewhere() = runTest {
        val viewModel = AppearanceViewModel(repository)

        repository.setFontScale(FontScale.EXTRA_LARGE)

        assertEquals(FontScale.EXTRA_LARGE, viewModel.uiState.value)
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
