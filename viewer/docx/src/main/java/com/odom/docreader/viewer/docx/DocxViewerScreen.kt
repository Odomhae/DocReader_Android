package com.odom.docreader.viewer.docx

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.viewer.web.WebDocumentViewerScreen
import com.odom.docreader.viewer.web.WebViewerConfig

// docx-preview 0.4.1 is shipped as ES2020 (optional chaining, ??), which Chromium parses from 80.
private const val MIN_WEBVIEW_MAJOR_VERSION = 80

private val DOCX_CONFIG = WebViewerConfig(
    entryAssetPath = "docx/viewer.html",
    minWebViewMajorVersion = MIN_WEBVIEW_MAJOR_VERSION,
    documentType = DocumentType.DOCX,
)

@Composable
fun DocxViewerScreen(uri: String, title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    WebDocumentViewerScreen(config = DOCX_CONFIG, uri = uri, title = title, onBack = onBack, modifier = modifier)
}
