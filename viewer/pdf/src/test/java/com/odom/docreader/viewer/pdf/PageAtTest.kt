package com.odom.docreader.viewer.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageAtTest {
    @Test
    fun pageUnderThePointIsChosen() {
        val pages = listOf(PageSpan(0, top = -900f, bottom = 300f), PageSpan(1, top = 320f, bottom = 1520f))

        assertEquals(1, pageClosestTo(y = 900f, pages = pages))
    }

    // What scrollToPage leaves on screen: the target centered, the end of the page before it still visible.
    @Test
    fun centeredPageWinsOverAnEarlierPageThatIsStillVisible() {
        val pages = listOf(
            PageSpan(4, top = -1000f, bottom = 200f),
            PageSpan(5, top = 220f, bottom = 1420f),
            PageSpan(6, top = 1440f, bottom = 2640f),
        )

        assertEquals(5, pageClosestTo(y = 820f, pages = pages))
    }

    @Test
    fun pointInTheGapBetweenPagesGoesToTheNearerOne() {
        val pages = listOf(PageSpan(0, top = 0f, bottom = 300f), PageSpan(1, top = 320f, bottom = 900f))

        assertEquals(0, pageClosestTo(y = 305f, pages = pages))
        assertEquals(1, pageClosestTo(y = 316f, pages = pages))
    }

    @Test
    fun noPageWhenNothingIsVisible() {
        assertNull(pageClosestTo(y = 100f, pages = emptyList()))
    }
}
