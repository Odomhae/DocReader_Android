package com.odom.docreader.viewer.web

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

internal const val BRIDGE_NAME = "DocViewerBridge"

/**
 * Builds the locked-down WebView every web-based viewer runs in. It knows nothing about document
 * formats: the caller names an HTML entry point in assets, and that page pulls the document from
 * [ViewerUrls.DOCUMENT_PATH].
 */
@SuppressLint("SetJavaScriptEnabled")
internal fun createViewerWebView(
    context: Context,
    entryAssetPath: String,
    documentUri: Uri,
    onEvent: (WebViewerEvent) -> Unit,
): WebView {
    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler(ViewerUrls.ASSETS_PATH, WebViewAssetLoader.AssetsPathHandler(context))
        .addPathHandler(ViewerUrls.DOCUMENT_PATH, DocumentPathHandler(context.contentResolver, documentUri))
        .build()
    return WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        webViewClient = object : WebViewClient() {
            // Anything the asset loader does not own would otherwise go to the network.
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse =
                assetLoader.shouldInterceptRequest(request.url) ?: blockedResponse()

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                !ViewerUrls.isInternal(request.url.toString())

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                onEvent(WebViewerEvent.RendererGone)
                // Returning true keeps the app alive; the screen replaces this view with an error.
                return true
            }
        }
        setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (isDoneCounting) {
                val activeMatch = if (numberOfMatches == 0) 0 else activeMatchOrdinal + 1
                onEvent(WebViewerEvent.FindResult(activeMatch, numberOfMatches))
            }
        }
        addJavascriptInterface(ViewerBridge(onEvent), BRIDGE_NAME)
        onEvent(WebViewerEvent.LoadStarted)
        loadUrl(ViewerUrls.entryUrl(entryAssetPath))
    }
}

private fun blockedResponse() = WebResourceResponse(
    "text/plain",
    "utf-8",
    403,
    "Blocked",
    emptyMap(),
    ByteArrayInputStream(ByteArray(0)),
)

/** The whole surface exposed to page JavaScript. Methods run on a WebView thread, not the main thread. */
private class ViewerBridge(private val onEvent: (WebViewerEvent) -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onLoaded(pageCount: Int) = post(WebViewerEvent.Loaded(pageCount))

    @JavascriptInterface
    fun onPageChanged(page: Int) = post(WebViewerEvent.PageChanged(page))

    @JavascriptInterface
    fun onFindResult(activeMatch: Int, matchCount: Int) = post(WebViewerEvent.FindResult(activeMatch, matchCount))

    // The message is deliberately dropped: it can quote document content, which must not be logged.
    @JavascriptInterface
    fun onError(@Suppress("UNUSED_PARAMETER") message: String) = post(WebViewerEvent.RenderFailed)

    private fun post(event: WebViewerEvent) {
        mainHandler.post { onEvent(event) }
    }
}
