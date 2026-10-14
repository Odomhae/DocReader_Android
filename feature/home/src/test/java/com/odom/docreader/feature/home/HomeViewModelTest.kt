package com.odom.docreader.feature.home

import com.odom.docreader.core.data.LibraryRepository
import com.odom.docreader.core.data.RecentDocument
import com.odom.docreader.core.data.SettingsRepository
import com.odom.docreader.core.files.DocumentScanner
import com.odom.docreader.core.files.ScanResult
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.DocumentType
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val scanner = FakeDocumentScanner()
    private val settings = FakeSettingsRepository()
    private val library = FakeLibraryRepository()

    private val pdf = document("a.pdf", DocumentType.PDF)
    private val hwp = document("b.hwp", DocumentType.HWP)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // uiState only runs while it has a subscriber.
    private fun TestScope.createViewModel(): HomeViewModel {
        val viewModel = HomeViewModel(scanner, settings, library)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun missingAccessShowsTheAccessRequiredState() = runTest {
        scanner.result = ScanResult.AccessRequired

        val viewModel = createViewModel()

        assertEquals(StorageStatus.ACCESS_REQUIRED, viewModel.uiState.value.storageStatus)
        assertTrue(viewModel.uiState.value.documents.isEmpty())
    }

    @Test
    fun storageIsScannedOnStart() = runTest {
        scanner.result = ScanResult.Success(listOf(hwp, pdf))

        val viewModel = createViewModel()

        assertEquals(StorageStatus.READY, viewModel.uiState.value.storageStatus)
        assertEquals(listOf("a.pdf", "b.hwp"), viewModel.uiState.value.documents.map { it.file.name })
    }

    @Test
    fun refreshAfterAccessIsGrantedShowsTheDocuments() = runTest {
        scanner.result = ScanResult.AccessRequired
        val viewModel = createViewModel()

        scanner.result = ScanResult.Success(listOf(pdf))
        viewModel.refresh()

        assertEquals(StorageStatus.READY, viewModel.uiState.value.storageStatus)
        assertEquals(listOf("a.pdf"), viewModel.uiState.value.documents.map { it.file.name })
    }

    @Test
    fun refreshAfterAccessIsRevokedShowsTheAccessRequiredState() = runTest {
        scanner.result = ScanResult.Success(listOf(pdf))
        val viewModel = createViewModel()

        scanner.result = ScanResult.AccessRequired
        viewModel.refresh()

        assertEquals(StorageStatus.ACCESS_REQUIRED, viewModel.uiState.value.storageStatus)
        assertTrue(viewModel.uiState.value.documents.isEmpty())
    }

    @Test
    fun refreshPicksUpNewFiles() = runTest {
        scanner.result = ScanResult.Success(listOf(pdf))
        val viewModel = createViewModel()

        scanner.result = ScanResult.Success(listOf(pdf, hwp))
        viewModel.refresh()

        assertEquals(listOf("a.pdf", "b.hwp"), viewModel.uiState.value.documents.map { it.file.name })
    }

    @Test
    fun filterQueryAndSortAreReflectedInTheState() = runTest {
        scanner.result = ScanResult.Success(listOf(pdf, hwp))
        val viewModel = createViewModel()

        viewModel.onFilterToggled(HomeFilter.HWP)
        viewModel.onQueryChanged("b")
        viewModel.onSortOrderSelected(SortOrder.SIZE)

        val state = viewModel.uiState.value
        assertEquals(HomeFilter.HWP, state.filter)
        assertEquals("b", state.query)
        assertEquals(SortOrder.SIZE, state.sortOrder)
    }

    @Test
    fun selectingAnotherChipReplacesTheFirst() = runTest {
        val viewModel = createViewModel()

        viewModel.onFilterToggled(HomeFilter.PDF)
        viewModel.onFilterToggled(HomeFilter.RECENT)

        assertEquals(HomeFilter.RECENT, viewModel.uiState.value.filter)
    }

    @Test
    fun selectingTheActiveChipAgainTurnsItOff() = runTest {
        scanner.result = ScanResult.Success(listOf(pdf, hwp))
        val viewModel = createViewModel()

        viewModel.onFilterToggled(HomeFilter.HWP)
        assertEquals(listOf("b.hwp"), viewModel.uiState.value.documents.map { it.file.name })
        viewModel.onFilterToggled(HomeFilter.HWP)

        assertNull(viewModel.uiState.value.filter)
        assertEquals(listOf("a.pdf", "b.hwp"), viewModel.uiState.value.documents.map { it.file.name })
    }

    @Test
    fun openedDocumentsAppearUnderRecentNewestFirst() = runTest {
        scanner.result = ScanResult.Success(listOf(pdf, hwp))
        val viewModel = createViewModel()

        viewModel.onDocumentOpened(pdf)
        viewModel.onDocumentOpened(hwp)
        viewModel.onFilterToggled(HomeFilter.RECENT)

        assertEquals(listOf("b.hwp", "a.pdf"), viewModel.uiState.value.documents.map { it.file.name })
    }

    private fun document(name: String, type: DocumentType) =
        DocumentFile(uri = "content://docs/$name", name = name, type = type, sizeBytes = name.length.toLong(), lastModified = 0)

    private class FakeDocumentScanner : DocumentScanner {
        var result: ScanResult = ScanResult.Success(emptyList())

        override suspend fun scan(): ScanResult = result
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

    private class FakeLibraryRepository : LibraryRepository {
        override val recentDocuments = MutableStateFlow<List<RecentDocument>>(emptyList())
        private var now = 0L

        override suspend fun markOpened(document: DocumentFile) {
            now += 1
            recentDocuments.value =
                listOf(RecentDocument(document, openedAt = now)) + recentDocuments.value.filter { it.file != document }
        }
    }
}
