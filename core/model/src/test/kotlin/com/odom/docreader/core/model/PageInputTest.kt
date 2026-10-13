package com.odom.docreader.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageInputTest {
    @Test
    fun pageWithinTheDocumentIsUsedAsIs() {
        assertEquals(7, resolvePageInput("7", pageCount = 12))
    }

    @Test
    fun tooLargeANumberGoesToTheLastPage() {
        assertEquals(12, resolvePageInput("99", pageCount = 12))
        assertEquals(12, resolvePageInput("99999999999999999999", pageCount = 12))
    }

    @Test
    fun zeroGoesToTheFirstPage() {
        assertEquals(1, resolvePageInput("0", pageCount = 12))
    }

    @Test
    fun surroundingSpacesAreIgnored() {
        assertEquals(3, resolvePageInput(" 3 ", pageCount = 12))
    }

    @Test
    fun nothingToGoToWhenTheInputIsNotAPageNumber() {
        assertNull(resolvePageInput("", pageCount = 12))
        assertNull(resolvePageInput("  ", pageCount = 12))
        assertNull(resolvePageInput("3a", pageCount = 12))
        assertNull(resolvePageInput("-3", pageCount = 12))
    }

    @Test
    fun nothingToGoToInAnEmptyDocument() {
        assertNull(resolvePageInput("1", pageCount = 0))
    }
}
