package com.odom.docreader

import com.odom.docreader.core.data.LibraryRepository
import com.odom.docreader.core.data.RecentDocument
import com.odom.docreader.core.files.ExternalDocument
import com.odom.docreader.core.files.ExternalDocumentResolver
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.model.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class MainViewModelTest {
    private val resolver = FakeResolver()
    private val library = FakeLibraryRepository()
    private val document = DocumentFile("content://other.app/doc/1", "report.hwp", DocumentType.HWP, 10, 20)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun nothingIsPendingInitially() {
        assertNull(MainViewModel(resolver, library).incomingDocument.value)
    }

    @Test
    fun supportedDocumentBecomesReady() = runTest {
        resolver.result = ExternalDocument(document, canRemember = false)
        val viewModel = MainViewModel(resolver, library)

        viewModel.onViewIntent(document.uri, "application/x-hwp", persistable = false)

        assertEquals(IncomingDocument.Ready(document), viewModel.incomingDocument.value)
    }

    @Test
    fun resolverReceivesTheIntentDetails() = runTest {
        val viewModel = MainViewModel(resolver, library)

        viewModel.onViewIntent(document.uri, "application/x-hwp", persistable = true)

        assertEquals(Triple(document.uri, "application/x-hwp", true), resolver.lastRequest)
    }

    @Test
    fun unresolvableDocumentIsReportedAsUnsupported() = runTest {
        resolver.result = null
        val viewModel = MainViewModel(resolver, library)

        viewModel.onViewIntent("content://other.app/doc/2", "image/png", persistable = false)

        assertEquals(IncomingDocument.Unsupported, viewModel.incomingDocument.value)
        assertTrue(library.opened.isEmpty())
    }

    @Test
    fun documentWithPersistedAccessIsAddedToRecents() = runTest {
        resolver.result = ExternalDocument(document, canRemember = true)
        val viewModel = MainViewModel(resolver, library)

        viewModel.onViewIntent(document.uri, null, persistable = true)

        assertEquals(listOf(document), library.opened)
    }

    @Test
    fun documentWithTemporaryAccessIsNotAddedToRecents() = runTest {
        resolver.result = ExternalDocument(document, canRemember = false)
        val viewModel = MainViewModel(resolver, library)

        viewModel.onViewIntent(document.uri, null, persistable = false)

        assertTrue(library.opened.isEmpty())
    }

    @Test
    fun handledDocumentIsCleared() = runTest {
        resolver.result = ExternalDocument(document, canRemember = false)
        val viewModel = MainViewModel(resolver, library)
        viewModel.onViewIntent(document.uri, null, persistable = false)

        viewModel.onIncomingDocumentHandled()

        assertNull(viewModel.incomingDocument.value)
    }

    private class FakeResolver : ExternalDocumentResolver {
        var result: ExternalDocument? = null
        var lastRequest: Triple<String, String?, Boolean>? = null

        override suspend fun resolve(uri: String, mimeType: String?, persistable: Boolean): ExternalDocument? {
            lastRequest = Triple(uri, mimeType, persistable)
            return result
        }
    }

    private class FakeLibraryRepository : LibraryRepository {
        val opened = mutableListOf<DocumentFile>()
        override val recentDocuments: Flow<List<RecentDocument>> = flowOf(emptyList())

        override suspend fun markOpened(document: DocumentFile) {
            opened += document
        }
    }
}
