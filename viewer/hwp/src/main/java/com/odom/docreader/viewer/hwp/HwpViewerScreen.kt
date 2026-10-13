package com.odom.docreader.viewer.hwp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.core.model.PaperColor
import com.odom.docreader.viewer.web.WebDocumentViewerScreen
import com.odom.docreader.viewer.web.WebViewerConfig

// rhwp_bg.wasm needs WASM reference-types, which Chromium ships from 96 (see docs/hwp-spike.md).
private const val MIN_WEBVIEW_MAJOR_VERSION = 96

/** Viewer for HWP and HWPX; [type] only distinguishes the two when sharing. */
@Composable
fun HwpViewerScreen(
    uri: String,
    title: String,
    type: DocumentType,
    onBack: () -> Unit,
    paperColor: PaperColor,
    onReadingSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = remember(type) {
        WebViewerConfig(
            entryAssetPath = "hwp/viewer.html",
            minWebViewMajorVersion = MIN_WEBVIEW_MAJOR_VERSION,
            documentType = type,
            // Pages are rendered lazily, so WebView's find-in-page would only see what is on screen.
            pageHandlesSearch = true,
        )
    }
    WebDocumentViewerScreen(
        config = config,
        uri = uri,
        title = title,
        onBack = onBack,
        paperColor = paperColor,
        onReadingSettings = onReadingSettings,
        modifier = modifier,
    )
}
