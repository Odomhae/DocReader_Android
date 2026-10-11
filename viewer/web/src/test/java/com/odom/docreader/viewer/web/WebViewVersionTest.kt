package com.odom.docreader.viewer.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewVersionTest {
    @Test
    fun parsesMajorVersion() {
        assertEquals(69, parseWebViewMajorVersion("69.0.3497.100"))
        assertEquals(133, parseWebViewMajorVersion("133.0.6943.137"))
    }

    @Test
    fun unparsableVersionHasNoMajor() {
        assertNull(parseWebViewMajorVersion(null))
        assertNull(parseWebViewMajorVersion(""))
        assertNull(parseWebViewMajorVersion("beta.1"))
    }

    @Test
    fun versionBelowMinimumIsUnsupported() {
        assertFalse(isWebViewSupported("69.0.3497.100", minMajorVersion = 96))
        assertFalse(isWebViewSupported("95.0.4638.74", minMajorVersion = 96))
    }

    @Test
    fun minimumAndNewerAreSupported() {
        assertTrue(isWebViewSupported("96.0.4664.45", minMajorVersion = 96))
        assertTrue(isWebViewSupported("133.0.6943.137", minMajorVersion = 96))
        assertTrue(isWebViewSupported("69.0.3497.100", minMajorVersion = 69))
    }

    @Test
    fun unknownVersionIsNotBlocked() {
        assertTrue(isWebViewSupported(null, minMajorVersion = 96))
        assertTrue(isWebViewSupported("unknown", minMajorVersion = 96))
    }
}
