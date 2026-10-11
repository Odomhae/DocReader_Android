package com.odom.docreader.viewer.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerUrlsTest {
    @Test
    fun entryUrlPointsIntoAssetsOnTheViewerOrigin() {
        assertEquals(
            "https://appassets.androidplatform.net/assets/hwp/viewer.html",
            ViewerUrls.entryUrl("hwp/viewer.html"),
        )
        assertTrue(ViewerUrls.isInternal(ViewerUrls.entryUrl("hwp/viewer.html")))
    }

    @Test
    fun assetsAndTheDocumentAreInternal() {
        assertTrue(ViewerUrls.isInternal("https://appassets.androidplatform.net/assets/webhost/host.js"))
        assertTrue(ViewerUrls.isInternal("https://appassets.androidplatform.net/document/current"))
        assertTrue(ViewerUrls.isInternal("HTTPS://AppAssets.AndroidPlatform.net/assets/a.js?x=1#y"))
    }

    @Test
    fun otherHostsAreExternal() {
        assertFalse(ViewerUrls.isInternal("https://example.com/"))
        assertFalse(ViewerUrls.isInternal("https://fonts.googleapis.com/css2?family=Noto+Sans+KR"))
        assertFalse(ViewerUrls.isInternal("https://appassets.androidplatform.net.evil.example/assets/a.js"))
        assertFalse(ViewerUrls.isInternal("https://evil.example/appassets.androidplatform.net/assets/a.js"))
        assertFalse(ViewerUrls.isInternal("https://user@appassets.androidplatform.net/assets/a.js"))
        assertFalse(ViewerUrls.isInternal("https://appassets.androidplatform.net:8443/assets/a.js"))
    }

    @Test
    fun otherSchemesAreExternal() {
        assertFalse(ViewerUrls.isInternal("http://appassets.androidplatform.net/assets/a.js"))
        assertFalse(ViewerUrls.isInternal("file:///android_asset/hwp/viewer.html"))
        assertFalse(ViewerUrls.isInternal("content://com.example/doc"))
        assertFalse(ViewerUrls.isInternal("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(ViewerUrls.isInternal("javascript:alert(1)"))
        assertFalse(ViewerUrls.isInternal("data:text/html,hi"))
    }

    @Test
    fun malformedOrMissingUrlsAreExternal() {
        assertFalse(ViewerUrls.isInternal(null))
        assertFalse(ViewerUrls.isInternal(""))
        assertFalse(ViewerUrls.isInternal("not a url"))
        assertFalse(ViewerUrls.isInternal("https://"))
    }
}
