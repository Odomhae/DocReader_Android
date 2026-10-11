package com.odom.docreader.viewer.web

import android.content.ContentResolver
import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

/**
 * Serves the opened document as a stream so the page can `fetch` it. The bytes go from the
 * provider to the renderer without being copied into a Java or JS string, which is what makes
 * large files safe.
 */
internal class DocumentPathHandler(
    private val contentResolver: ContentResolver,
    private val documentUri: Uri,
) : WebViewAssetLoader.PathHandler {

    override fun handle(path: String): WebResourceResponse {
        val stream = try {
            contentResolver.openInputStream(documentUri)
        } catch (e: FileNotFoundException) {
            null
        } catch (e: SecurityException) {
            null
        } ?: return notFound()
        return WebResourceResponse("application/octet-stream", null, 200, "OK", NO_STORE, stream)
    }

    // The page turns a failed fetch into a render failure, so the user sees the error screen.
    private fun notFound() = WebResourceResponse(
        "text/plain",
        "utf-8",
        404,
        "Not Found",
        NO_STORE,
        ByteArrayInputStream(ByteArray(0)),
    )

    private companion object {
        val NO_STORE = mapOf("Cache-Control" to "no-store")
    }
}
