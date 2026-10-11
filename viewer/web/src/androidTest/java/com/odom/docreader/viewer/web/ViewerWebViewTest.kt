package com.odom.docreader.viewer.web

import android.net.Uri
import android.os.SystemClock
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewerWebViewTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private var webView: WebView? = null
    private val events = mutableListOf<WebViewerEvent>()

    @After
    fun tearDown() {
        instrumentation.runOnMainSync { webView?.destroy() }
    }

    @Test
    fun externalNetworkRequestsAreBlockedAndAssetsStillLoad() {
        // Without real connectivity the probes would fail for the wrong reason.
        assumeTrue("device has no network", plainWebViewCanLoadExternalImage())
        load("webhost-test/network.html", documentUri = Uri.EMPTY)

        val results = JSONArray(awaitResult())
        val probes = List(results.length()) { results.getString(it) }

        assertEquals(listOf("image:failed", "script:failed", "asset:200"), probes)
    }

    /** Control: the same image loads in a WebView that does not have the viewer's client installed. */
    private fun plainWebViewCanLoadExternalImage(): Boolean {
        instrumentation.runOnMainSync {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                loadDataWithBaseURL(
                    "https://docreader.invalid/",
                    "<script>window.result = null; var i = new Image();" +
                        "i.onload = function () { window.result = 'loaded'; };" +
                        "i.onerror = function () { window.result = 'failed'; };" +
                        "i.src = 'https://www.google.com/favicon.ico';</script>",
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        }
        val loaded = awaitResult() == "\"loaded\""
        instrumentation.runOnMainSync { webView?.destroy() }
        return loaded
    }

    @Test
    fun documentLargerThan20MbIsStreamedIntact() {
        val sizeBytes = 25 * 1024 * 1024
        val file = File(context.cacheDir, "large-document.bin")
        file.outputStream().buffered().use { output ->
            val block = ByteArray(1024 * 1024) { (it % 251).toByte() }
            repeat(25) { output.write(block) }
        }
        try {
            load("webhost-test/document.html", documentUri = Uri.fromFile(file))

            val result = JSONObject(awaitResult(timeoutMillis = 120_000))

            assertFalse("page reported ${result.optString("error")}", result.has("error"))
            assertEquals(sizeBytes, result.getInt("length"))
            val expected = listOf(0, 1, 255, 256, 1048576, sizeBytes - 1).map { (it % (1024 * 1024)) % 251 }
            val samples = result.getJSONArray("samples")
            assertEquals(expected, List(samples.length()) { samples.getInt(it) })
            assertFalse("renderer crashed", events.contains(WebViewerEvent.RendererGone))
        } finally {
            file.delete()
        }
    }

    @Test
    fun missingDocumentFailsTheFetchInsteadOfCrashing() {
        load("webhost-test/document.html", documentUri = Uri.fromFile(File(context.cacheDir, "does-not-exist.bin")))

        val result = JSONObject(awaitResult())

        assertEquals("HTTP 404", result.getString("error"))
    }

    @Test
    fun navigatingToAnExternalLinkIsRefused() {
        load("webhost-test/navigate.html", documentUri = Uri.EMPTY)
        awaitResult()
        val entryUrl = ViewerUrls.entryUrl("webhost-test/navigate.html")

        evaluate("document.getElementById('external').click()")
        SystemClock.sleep(3_000)

        var currentUrl: String? = null
        instrumentation.runOnMainSync { currentUrl = webView?.url }
        assertEquals(entryUrl, currentUrl)
    }

    private fun load(entryAssetPath: String, documentUri: Uri) {
        instrumentation.runOnMainSync {
            webView = createViewerWebView(context, entryAssetPath, documentUri) { events += it }
        }
    }

    /** Polls `window.result` until the test page has set it, and returns it as JSON text. */
    private fun awaitResult(timeoutMillis: Long = 30_000): String {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            // evaluateJavascript hands back the JSON encoding of the expression's value.
            val value = evaluate("window.result === undefined ? null : window.result")
            if (value != "null") return value
            SystemClock.sleep(250)
        }
        throw AssertionError("test page did not produce a result in ${timeoutMillis}ms")
    }

    private fun evaluate(script: String): String {
        val latch = CountDownLatch(1)
        var value = "null"
        instrumentation.runOnMainSync {
            webView!!.evaluateJavascript(script) {
                value = it ?: "null"
                latch.countDown()
            }
        }
        assertTrue("evaluateJavascript timed out", latch.await(30, TimeUnit.SECONDS))
        return value
    }
}
