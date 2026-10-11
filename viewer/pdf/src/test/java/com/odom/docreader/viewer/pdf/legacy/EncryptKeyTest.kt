package com.odom.docreader.viewer.pdf.legacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptKeyTest {
    private fun ascii(text: String) = text.toByteArray(Charsets.US_ASCII)

    @Test
    fun findsTheEncryptEntryOfATrailer() {
        assertTrue(containsEncryptKey(ascii("trailer << /Size 12 /Root 1 0 R /Encrypt 11 0 R >> startxref")))
    }

    @Test
    fun plainTrailerHasNoEncryptEntry() {
        assertFalse(containsEncryptKey(ascii("trailer << /Size 12 /Root 1 0 R >> startxref")))
    }

    @Test
    fun handlesInputShorterThanTheKey() {
        assertFalse(containsEncryptKey(ascii("/Enc")))
        assertFalse(containsEncryptKey(ByteArray(0)))
    }
}
