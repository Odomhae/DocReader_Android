package com.odom.docreader.core.model

/**
 * The 1-based page to show for what the user typed, or null if it is not a page number.
 * Numbers outside the document go to the nearest page, so typing a large number reaches the end.
 */
fun resolvePageInput(input: String, pageCount: Int): Int? {
    val digits = input.trim()
    if (pageCount < 1 || digits.isEmpty() || !digits.all { it in '0'..'9' }) return null
    // Too long to be an Int, so certainly past the last page.
    val page = digits.toIntOrNull() ?: return pageCount
    return page.coerceIn(1, pageCount)
}
