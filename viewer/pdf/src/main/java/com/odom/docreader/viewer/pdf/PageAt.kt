package com.odom.docreader.viewer.pdf

/** Vertical extent of a visible page, in the viewer's own coordinates. */
internal data class PageSpan(val page: Int, val top: Float, val bottom: Float)

/** The page under [y], or the nearest one when [y] falls between pages. Null if [pages] is empty. */
internal fun pageClosestTo(y: Float, pages: List<PageSpan>): Int? =
    pages.minByOrNull { span ->
        when {
            y < span.top -> span.top - y
            y > span.bottom -> y - span.bottom
            else -> 0f
        }
    }?.page
