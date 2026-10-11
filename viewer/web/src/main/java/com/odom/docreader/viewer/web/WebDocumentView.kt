package com.odom.docreader.viewer.web

import android.net.Uri
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/**
 * Sends commands to the viewer page. Calls are ignored while no page is attached.
 *
 * With [pageHandlesSearch] the page's own `docViewer.find` is used instead of WebView's
 * find-in-page, for pages whose text is not all in the DOM at once.
 */
class WebViewerController(private val pageHandlesSearch: Boolean) {
    internal var webView: WebView? = null

    /** [page] is 1-based. */
    fun goToPage(page: Int) {
        evaluate("window.docViewer.goToPage($page)")
    }

    fun find(query: String) {
        when {
            query.isEmpty() -> clearFind()
            pageHandlesSearch -> evaluate("window.docViewer.find(${JSONObject.quote(query)})")
            else -> webView?.findAllAsync(query)
        }
    }

    fun findNext(forward: Boolean) {
        if (pageHandlesSearch) evaluate("window.docViewer.findNext($forward)") else webView?.findNext(forward)
    }

    fun clearFind() {
        if (pageHandlesSearch) evaluate("window.docViewer.clearFind()") else webView?.clearMatches()
    }

    private fun evaluate(script: String) {
        webView?.evaluateJavascript("window.docViewer && $script", null)
    }
}

@Composable
internal fun WebDocumentView(
    entryAssetPath: String,
    documentUri: Uri,
    controller: WebViewerController,
    onEvent: (WebViewerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnEvent = rememberUpdatedState(onEvent)
    AndroidView(
        factory = { context ->
            createViewerWebView(context, entryAssetPath, documentUri) { currentOnEvent.value(it) }
                .also { controller.webView = it }
        },
        modifier = modifier,
        onRelease = { webView ->
            controller.webView = null
            webView.destroy()
        },
    )
}
