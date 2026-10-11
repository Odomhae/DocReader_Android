package com.odom.docreader.viewer.web

/** Everything the host reports about the page it is showing. */
sealed interface WebViewerEvent {
    /** The host started (re)loading the viewer page, e.g. after the view was recreated on rotation. */
    data object LoadStarted : WebViewerEvent

    data class Loaded(val pageCount: Int) : WebViewerEvent

    /** [page] is 1-based. */
    data class PageChanged(val page: Int) : WebViewerEvent

    /** The viewer page could not parse or render the document. */
    data object RenderFailed : WebViewerEvent

    /** The WebView renderer process crashed or was killed for memory. */
    data object RendererGone : WebViewerEvent

    /** [activeMatch] is 1-based, 0 when there is no match. */
    data class FindResult(val activeMatch: Int, val matchCount: Int) : WebViewerEvent
}
