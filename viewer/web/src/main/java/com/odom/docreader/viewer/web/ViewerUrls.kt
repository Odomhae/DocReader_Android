package com.odom.docreader.viewer.web

import androidx.webkit.WebViewAssetLoader
import java.net.URI
import java.net.URISyntaxException

/** The only origin a viewer page may load from or navigate to. Everything else is refused. */
internal object ViewerUrls {
    const val HOST = WebViewAssetLoader.DEFAULT_DOMAIN
    const val ASSETS_PATH = "/assets/"

    /** Viewer pages `fetch` the opened document from here; the host streams it from its Uri. */
    const val DOCUMENT_PATH = "/document/"

    fun entryUrl(entryAssetPath: String): String = "https://$HOST$ASSETS_PATH$entryAssetPath"

    fun isInternal(url: String?): Boolean {
        if (url == null) return false
        val uri = try {
            URI(url)
        } catch (e: URISyntaxException) {
            return false
        }
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(HOST, ignoreCase = true) &&
            uri.port == -1 &&
            uri.userInfo == null
    }
}
