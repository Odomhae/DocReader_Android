package com.odom.docreader.viewer.pdf.legacy

import android.graphics.Bitmap
import com.odom.docreader.viewer.pdf.PdfLoadFailure

sealed interface PdfOpenResult {
    data class Opened(val pages: PdfPages) : PdfOpenResult
    data class Failed(val failure: PdfLoadFailure) : PdfOpenResult
}

interface PdfPages {
    val pageCount: Int

    /** Renders one page [widthPx] wide, or null if the page cannot be drawn or the document was closed. */
    suspend fun render(pageIndex: Int, widthPx: Int): Bitmap?

    fun close()
}

interface PdfDocumentSource {
    suspend fun open(uri: String): PdfOpenResult
}
