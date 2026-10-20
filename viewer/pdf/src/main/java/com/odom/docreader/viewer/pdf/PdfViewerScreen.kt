package com.odom.docreader.viewer.pdf

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.odom.docreader.core.files.shareDocument
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.core.model.PaperColor
import com.odom.docreader.core.ui.EmptyState
import com.odom.docreader.core.ui.PageBar
import com.odom.docreader.core.ui.R as UiR
import com.odom.docreader.viewer.pdf.legacy.LegacyPdfViewer

@Composable
fun PdfViewerScreen(
    uri: String,
    title: String,
    onBack: () -> Unit,
    paperColor: PaperColor,
    onReadingSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // androidx.pdf's minSdk is 28; older devices get the PdfRenderer-based viewer.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        AndroidxPdfViewer(
            uri = uri,
            title = title,
            onBack = onBack,
            paperColor = paperColor,
            onReadingSettings = onReadingSettings,
            modifier = modifier,
        )
    } else {
        LegacyPdfViewer(
            uri = uri,
            title = title,
            onBack = onBack,
            paperColor = paperColor,
            onReadingSettings = onReadingSettings,
            modifier = modifier,
        )
    }
}

/**
 * Top and bottom bars shared by both PDF viewers. [onSearch] is null when in-document search is
 * unavailable; the page bar is hidden while [pageCount] is 0.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfViewerScaffold(
    uri: String,
    title: String,
    onBack: () -> Unit,
    onSearch: (() -> Unit)?,
    onReadingSettings: () -> Unit,
    pageCount: Int,
    currentPage: Int,
    onGoToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(UiR.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.pdf_back),
                        )
                    }
                },
                actions = {
                    if (onSearch != null) {
                        IconButton(onClick = onSearch) {
                            Icon(
                                painterResource(UiR.drawable.ic_search),
                                contentDescription = stringResource(R.string.pdf_search),
                            )
                        }
                    }
                    IconButton(onClick = onReadingSettings) {
                        Icon(
                            painterResource(UiR.drawable.ic_reading_settings),
                            contentDescription = stringResource(R.string.pdf_reading_settings),
                        )
                    }
                    IconButton(onClick = { shareDocument(context, Uri.parse(uri), DocumentType.PDF) }) {
                        Icon(
                            painterResource(UiR.drawable.ic_share),
                            contentDescription = stringResource(R.string.pdf_share),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (pageCount > 0) {
                PageBar(currentPage = currentPage, pageCount = pageCount, onGoToPage = onGoToPage)
            }
        },
    ) { innerPadding ->
        // androidx.pdf's search bar sits at the bottom of the fragment and its own keyboard handling
        // never sees the insets Compose consumes, so the content area itself shrinks above the keyboard.
        content(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding).imePadding())
    }
}

@Composable
internal fun PdfFailureMessage(failure: PdfLoadFailure, modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.pdf_error_title),
        message = stringResource(
            when (failure) {
                PdfLoadFailure.PASSWORD_REQUIRED -> R.string.pdf_error_password_required
                PdfLoadFailure.PASSWORD_UNSUPPORTED -> R.string.pdf_error_password_unsupported
                PdfLoadFailure.UNREADABLE -> R.string.pdf_error_unreadable
            },
        ),
        modifier = modifier,
    )
}
