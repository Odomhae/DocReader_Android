package com.odom.docreader.feature.settings

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LicensesViewModelTest {
    private val source = FakeLicenseTextSource()
    private val first = OPEN_SOURCE_LIBRARIES[0]
    private val second = OPEN_SOURCE_LIBRARIES[2]

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun listsTheLibrariesWithNothingOpened() {
        val state = LicensesViewModel(source).uiState.value

        assertEquals(OPEN_SOURCE_LIBRARIES, state.libraries)
        assertNull(state.opened)
    }

    @Test
    fun selectingALibraryShowsItsLicenseText() = runTest {
        val viewModel = LicensesViewModel(source)

        viewModel.onLibrarySelected(first)

        assertEquals(OpenedLicense(first, "text of ${first.licenseAsset}"), viewModel.uiState.value.opened)
    }

    @Test
    fun textIsNullWhileLoading() = runTest {
        source.gate = CompletableDeferred()
        val viewModel = LicensesViewModel(source)

        viewModel.onLibrarySelected(first)

        assertEquals(OpenedLicense(first, text = null), viewModel.uiState.value.opened)
    }

    @Test
    fun closingReturnsToTheList() = runTest {
        val viewModel = LicensesViewModel(source)
        viewModel.onLibrarySelected(first)

        viewModel.onLicenseClosed()

        assertNull(viewModel.uiState.value.opened)
    }

    @Test
    fun aTextThatArrivesAfterClosingIsDropped() = runTest {
        val gate = CompletableDeferred<Unit>()
        source.gate = gate
        val viewModel = LicensesViewModel(source)
        viewModel.onLibrarySelected(first)
        viewModel.onLicenseClosed()

        gate.complete(Unit)

        assertNull(viewModel.uiState.value.opened)
    }

    @Test
    fun aTextThatArrivesAfterAnotherLibraryWasOpenedIsDropped() = runTest {
        val gate = CompletableDeferred<Unit>()
        source.gate = gate
        val viewModel = LicensesViewModel(source)
        viewModel.onLibrarySelected(first)
        source.gate = null
        viewModel.onLibrarySelected(second)

        gate.complete(Unit)

        assertEquals(OpenedLicense(second, "text of ${second.licenseAsset}"), viewModel.uiState.value.opened)
    }

    private class FakeLicenseTextSource : LicenseTextSource {
        /** When set, loads wait for it, so a test can observe the loading state. */
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun load(licenseAsset: String): String {
            gate?.await()
            return "text of $licenseAsset"
        }
    }
}
