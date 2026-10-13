package com.odom.docreader.viewer.txt

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.core.model.PaperColor
import com.odom.docreader.viewer.web.WebDocumentViewerScreen
import com.odom.docreader.viewer.web.WebViewerConfig

// The viewer page is an ES module, which Chromium loads from 61.
private const val MIN_WEBVIEW_MAJOR_VERSION = 61

private val TXT_CONFIG = WebViewerConfig(
    entryAssetPath = "txt/viewer.html",
    minWebViewMajorVersion = MIN_WEBVIEW_MAJOR_VERSION,
    documentType = DocumentType.TXT,
    showPageBar = false,
)

@Composable
fun TxtViewerScreen(
    uri: String,
    title: String,
    onBack: () -> Unit,
    paperColor: PaperColor,
    onReadingSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WebDocumentViewerScreen(
        config = TXT_CONFIG,
        uri = uri,
        title = title,
        onBack = onBack,
        paperColor = paperColor,
        onReadingSettings = onReadingSettings,
        modifier = modifier,
    )
}
