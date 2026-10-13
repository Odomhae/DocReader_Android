package com.odom.docreader.viewer.pdf

import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import android.util.SparseArray
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.OperationCanceledException
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.odom.docreader.core.model.PaperColor
import com.odom.docreader.core.ui.sepiaPaper
import kotlinx.coroutines.CancellationException

private const val TAG = "PdfViewer"

@RequiresApi(Build.VERSION_CODES.P)
@Composable
internal fun AndroidxPdfViewer(
    uri: String,
    title: String,
    onBack: () -> Unit,
    paperColor: PaperColor,
    onReadingSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfViewerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fragment by remember { mutableStateOf<DocPdfViewerFragment?>(null) }
    val failure = uiState.failure

    PdfViewerScaffold(
        uri = uri,
        title = title,
        onBack = onBack,
        onSearch = if (failure == null && isPdfSearchSupported()) {
            { fragment?.startSearch() }
        } else {
            null
        },
        onReadingSettings = onReadingSettings,
        pageCount = if (failure == null) uiState.pageCount else 0,
        currentPage = uiState.currentPage,
        onGoToPage = { page -> fragment?.goToPage(page) },
        modifier = modifier,
    ) { contentModifier ->
        if (failure != null) {
            PdfFailureMessage(failure, contentModifier)
        } else {
            AndroidFragment<DocPdfViewerFragment>(
                modifier = contentModifier.sepiaPaper(paperColor == PaperColor.SEPIA),
            ) { pdfFragment ->
                pdfFragment.listener = object : DocPdfViewerFragment.Listener {
                    override fun onLoaded(pageCount: Int) = viewModel.onDocumentLoaded(pageCount)
                    override fun onPageChanged(page: Int) = viewModel.onPageChanged(page)
                    override fun onFailed(failure: PdfLoadFailure) = viewModel.onDocumentFailed(failure)
                }
                // A fragment restored after process death is already loading this Uri from its saved
                // state. Setting it again cancels that load, which the fragment reports as an error.
                val documentUri = Uri.parse(uri)
                if (pdfFragment.documentUri != documentUri) {
                    pdfFragment.documentUri = documentUri
                }
                fragment = pdfFragment
            }
        }
    }
}

// Same condition androidx.pdf uses to pick a renderer with a text layer; older devices fall back
// to the platform's original PdfRenderer, where the fragment's search silently does nothing.
private fun isPdfSearchSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13)

/** Public with a no-arg constructor because FragmentManager re-creates it after process death. */
@RequiresApi(Build.VERSION_CODES.P)
class DocPdfViewerFragment : PdfViewerFragment() {
    interface Listener {
        fun onLoaded(pageCount: Int)

        /** [page] is 1-based. */
        fun onPageChanged(page: Int)
        fun onFailed(failure: PdfLoadFailure)
    }

    var listener: Listener? = null

    private var attachedPdfView: PdfView? = null

    /** [page] is 1-based. */
    fun goToPage(page: Int) {
        attachedPdfView?.scrollToPage(page - 1)
    }

    // The only way the fragment hands out its PdfView in 1.0.0-beta01, and still marked experimental.
    @OptIn(ExperimentalPdfApi::class)
    override fun onPdfViewCreated(pdfView: PdfView) {
        super.onPdfViewCreated(pdfView)
        attachedPdfView = pdfView
        pdfView.addOnViewportChangedListener(
            object : PdfView.OnViewportChangedListener {
                override fun onViewportChanged(
                    firstVisiblePage: Int,
                    visiblePagesCount: Int,
                    pageLocations: SparseArray<RectF>,
                    zoomLevel: Float,
                ) {
                    // Not firstVisiblePage: scrollToPage centers its target, which leaves the end of the
                    // page before it on screen, and the bar would then never advance past that page.
                    val visiblePages = List(pageLocations.size()) { index ->
                        val location = pageLocations.valueAt(index)
                        PageSpan(pageLocations.keyAt(index), location.top, location.bottom)
                    }
                    val centeredPage = pageClosestTo(pdfView.height / 2f, visiblePages) ?: return
                    listener?.onPageChanged(centeredPage + 1)
                }
            },
        )
    }

    override fun onDestroyView() {
        attachedPdfView = null
        super.onDestroyView()
    }

    fun startSearch() {
        // The setter touches views, which do not exist before onCreateView.
        if (view != null) isTextSearchActive = true
    }

    override fun onLoadDocumentSuccess(document: PdfDocument) {
        listener?.onLoaded(document.pageCount)
    }

    override fun onLoadDocumentError(error: Throwable) {
        // A load superseded by another one is not a problem with the document.
        if (generateSequence(error) { it.cause }.any { it is CancellationException }) return
        // The fragment reports a dismissed password dialog as a cancelled operation.
        val failure = if (error is OperationCanceledException) {
            PdfLoadFailure.PASSWORD_REQUIRED
        } else {
            PdfLoadFailure.UNREADABLE
        }
        // Class names only: exception messages can contain the file name.
        Log.w(TAG, "PDF load failed: " + generateSequence(error) { it.cause }.joinToString(" <- ") { it.javaClass.name })
        listener?.onFailed(failure)
    }

    // The default shows a toolbox whose only action launches an external annotation editor,
    // and editing is out of scope for this app.
    override fun onRequestImmersiveMode(enterImmersive: Boolean) = Unit
}
