package com.odom.docreader.viewer.pdf.legacy

import android.graphics.Bitmap
import com.odom.docreader.viewer.pdf.PdfLoadFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class LegacyPdfViewModelTest {
    private val source = FakePdfDocumentSource()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsLoading() {
        assertEquals(LegacyPdfUiState.Loading, LegacyPdfViewModel(source).uiState.value)
    }

    @Test
    fun openedDocumentReportsItsPageCount() = runTest {
        source.results[URI] = PdfOpenResult.Opened(FakePdfPages(pageCount = 120))
        val viewModel = LegacyPdfViewModel(source)

        viewModel.open(URI)

        assertEquals(LegacyPdfUiState.Ready(pageCount = 120), viewModel.uiState.value)
    }

    @Test
    fun passwordProtectedDocumentIsReportedAsFailure() = runTest {
        source.results[URI] = PdfOpenResult.Failed(PdfLoadFailure.PASSWORD_UNSUPPORTED)
        val viewModel = LegacyPdfViewModel(source)

        viewModel.open(URI)

        assertEquals(LegacyPdfUiState.Failed(PdfLoadFailure.PASSWORD_UNSUPPORTED), viewModel.uiState.value)
    }

    @Test
    fun damagedDocumentIsReportedAsFailure() = runTest {
        source.results[URI] = PdfOpenResult.Failed(PdfLoadFailure.UNREADABLE)
        val viewModel = LegacyPdfViewModel(source)

        viewModel.open(URI)

        assertEquals(LegacyPdfUiState.Failed(PdfLoadFailure.UNREADABLE), viewModel.uiState.value)
    }

    @Test
    fun openingTheSameUriAgainDoesNotReopenIt() = runTest {
        source.results[URI] = PdfOpenResult.Opened(FakePdfPages(pageCount = 3))
        val viewModel = LegacyPdfViewModel(source)

        viewModel.open(URI)
        viewModel.open(URI)

        assertEquals(1, source.openCount)
    }

    @Test
    fun renderingBeforeTheDocumentIsOpenReturnsNothing() = runTest {
        assertNull(LegacyPdfViewModel(source).renderPage(pageIndex = 0, widthPx = 100))
    }

    @Test
    fun renderRequestsGoToTheOpenDocument() = runTest {
        val pages = FakePdfPages(pageCount = 3)
        source.results[URI] = PdfOpenResult.Opened(pages)
        val viewModel = LegacyPdfViewModel(source)
        viewModel.open(URI)

        viewModel.renderPage(pageIndex = 2, widthPx = 640)

        assertEquals(listOf(2 to 640), pages.renderRequests)
    }

    @Test
    fun clearingTheViewModelClosesTheDocument() = runTest {
        val pages = FakePdfPages(pageCount = 3)
        source.results[URI] = PdfOpenResult.Opened(pages)
        val viewModel = LegacyPdfViewModel(source)
        viewModel.open(URI)

        // onCleared is protected; ViewModel has no public way to trigger it outside a ViewModelStore.
        val onCleared = LegacyPdfViewModel::class.java.getDeclaredMethod("onCleared")
        onCleared.isAccessible = true
        onCleared.invoke(viewModel)

        assertTrue(pages.closed)
    }

    private class FakePdfDocumentSource : PdfDocumentSource {
        val results = mutableMapOf<String, PdfOpenResult>()
        var openCount = 0

        override suspend fun open(uri: String): PdfOpenResult {
            openCount++
            return results.getValue(uri)
        }
    }

    private class FakePdfPages(override val pageCount: Int) : PdfPages {
        val renderRequests = mutableListOf<Pair<Int, Int>>()
        var closed = false

        // A real Bitmap cannot be created in a JVM test, so the fake only records the request.
        override suspend fun render(pageIndex: Int, widthPx: Int): Bitmap? {
            renderRequests += pageIndex to widthPx
            return null
        }

        override fun close() {
            closed = true
        }
    }

    private companion object {
        const val URI = "content://docs/a.pdf"
    }
}
