package com.odom.docreader.feature.home

import com.odom.docreader.core.data.LibraryRepository
import com.odom.docreader.core.data.SettingsRepository
import com.odom.docreader.core.files.DocumentScanner
import com.odom.docreader.core.files.FolderAccess
import com.odom.docreader.core.files.ScanResult
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.DocumentType
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val scanner = FakeDocumentScanner()
    private val folderAccess = FakeFolderAccess()
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
        val viewModel = HomeViewModel(scanner, folderAccess, settings, library)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun noSavedFolderShowsTheNotSelectedState() = runTest {
        val viewModel = createViewModel()

        assertEquals(FolderStatus.NOT_SELECTED, viewModel.uiState.value.folderStatus)
    }

    @Test
    fun savedFolderIsScannedOnStart() = runTest {
        settings.folderUri.value = FOLDER
        scanner.results[FOLDER] = ScanResult.Success(listOf(hwp, pdf))

        val viewModel = createViewModel()

        assertEquals(FolderStatus.READY, viewModel.uiState.value.folderStatus)
        assertEquals(listOf("a.pdf", "b.hwp"), viewModel.uiState.value.documents.map { it.file.name })
    }

    @Test
    fun lostPermissionShowsTheUnavailableState() = runTest {
        settings.folderUri.value = FOLDER
        scanner.results[FOLDER] = ScanResult.FolderUnavailable

        val viewModel = createViewModel()

        assertEquals(FolderStatus.UNAVAILABLE, viewModel.uiState.value.folderStatus)
        assertTrue(viewModel.uiState.value.documents.isEmpty())
    }

    @Test
    fun pickingAFolderPersistsAccessSavesItAndReleasesTheOldOne() = runTest {
        settings.folderUri.value = FOLDER
        scanner.results[FOLDER] = ScanResult.Success(listOf(pdf))
        scanner.results[OTHER_FOLDER] = ScanResult.Success(listOf(hwp))
        val viewModel = createViewModel()

        viewModel.onFolderPicked(OTHER_FOLDER)

        assertEquals(listOf(OTHER_FOLDER), folderAccess.persisted)
        assertEquals(listOf(FOLDER), folderAccess.released)
        assertEquals(OTHER_FOLDER, settings.folderUri.value)
        assertEquals(listOf("b.hwp"), viewModel.uiState.value.documents.map { it.file.name })
    }

    @Test
    fun pickingTheSameFolderAgainDoesNotReleaseIt() = runTest {
        settings.folderUri.value = FOLDER
        scanner.results[FOLDER] = ScanResult.Success(listOf(pdf))
        val viewModel = createViewModel()

        viewModel.onFolderPicked(FOLDER)

        assertTrue(folderAccess.released.isEmpty())
        assertEquals(FolderStatus.READY, viewModel.uiState.value.folderStatus)
    }

    @Test
    fun refreshPicksUpNewFiles() = runTest {
        settings.folderUri.value = FOLDER
        scanner.results[FOLDER] = ScanResult.Success(listOf(pdf))
        val viewModel = createViewModel()

        scanner.results[FOLDER] = ScanResult.Success(listOf(pdf, hwp))
        viewModel.refresh()

        assertEquals(listOf("a.pdf", "b.hwp"), viewModel.uiState.value.documents.map { it.file.name })
    }

    @Test
    fun tabQueryAndSortAreReflectedInTheState() = runTest {
        settings.folderUri.value = FOLDER
        scanner.results[FOLDER] = ScanResult.Success(listOf(pdf, hwp))
        val viewModel = createViewModel()

        viewModel.onTabSelected(HomeTab.HWP)
        viewModel.onQueryChanged("b")
        viewModel.onSortOrderSelected(SortOrder.SIZE)

        val state = viewModel.uiState.value
        assertEquals(HomeTab.HWP, state.selectedTab)
        assertEquals("b", state.query)
        assertEquals(SortOrder.SIZE, state.sortOrder)
        assertEquals(listOf("b.hwp"), state.documents.map { it.file.name })
    }

    @Test
    fun togglingFavoriteUpdatesTheListAndTheFavoritesTab() = runTest {
        settings.folderUri.value = FOLDER
        scanner.results[FOLDER] = ScanResult.Success(listOf(pdf, hwp))
        val viewModel = createViewModel()

        viewModel.onToggleFavorite(viewModel.uiState.value.documents.first { it.file == hwp })

        assertEquals(listOf("b.hwp"), viewModel.uiState.value.documents.filter { it.isFavorite }.map { it.file.name })
        viewModel.onTabSelected(HomeTab.FAVORITES)
        assertEquals(listOf("b.hwp"), viewModel.uiState.value.documents.map { it.file.name })

        viewModel.onToggleFavorite(viewModel.uiState.value.documents.single())
        assertTrue(viewModel.uiState.value.documents.isEmpty())
    }

    @Test
    fun openedDocumentsAppearInTheRecentTabNewestFirst() = runTest {
        settings.folderUri.value = FOLDER
        scanner.results[FOLDER] = ScanResult.Success(listOf(pdf, hwp))
        val viewModel = createViewModel()

        viewModel.onDocumentOpened(pdf)
        viewModel.onDocumentOpened(hwp)
        viewModel.onTabSelected(HomeTab.RECENT)

        assertEquals(listOf("b.hwp", "a.pdf"), viewModel.uiState.value.documents.map { it.file.name })
    }

    private fun document(name: String, type: DocumentType) =
        DocumentFile(uri = "content://docs/$name", name = name, type = type, sizeBytes = name.length.toLong(), lastModified = 0)

    private class FakeDocumentScanner : DocumentScanner {
        val results = mutableMapOf<String, ScanResult>()

        override suspend fun scan(treeUri: String): ScanResult = results[treeUri] ?: ScanResult.FolderUnavailable
    }

    private class FakeFolderAccess : FolderAccess {
        val persisted = mutableListOf<String>()
        val released = mutableListOf<String>()

        override fun persist(treeUri: String) {
            persisted += treeUri
        }

        override fun release(treeUri: String) {
            released += treeUri
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val folderUri = MutableStateFlow<String?>(null)
        override val sortOrder = MutableStateFlow(SortOrder.NAME)

        override suspend fun setFolderUri(uri: String) {
            folderUri.value = uri
        }

        override suspend fun setSortOrder(sortOrder: SortOrder) {
            this.sortOrder.value = sortOrder
        }
    }

    private class FakeLibraryRepository : LibraryRepository {
        override val recentDocuments = MutableStateFlow<List<DocumentFile>>(emptyList())
        override val favoriteDocuments = MutableStateFlow<List<DocumentFile>>(emptyList())

        override suspend fun markOpened(document: DocumentFile) {
            recentDocuments.value = listOf(document) + (recentDocuments.value - document)
        }

        override suspend fun setFavorite(document: DocumentFile, favorite: Boolean) {
            favoriteDocuments.value =
                if (favorite) favoriteDocuments.value + document else favoriteDocuments.value - document
        }
    }

    private companion object {
        const val FOLDER = "content://tree/docs"
        const val OTHER_FOLDER = "content://tree/other"
    }
}
