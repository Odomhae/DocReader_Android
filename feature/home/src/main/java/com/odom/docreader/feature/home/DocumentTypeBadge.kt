package com.odom.docreader.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.odom.docreader.core.model.DocumentType

private class BadgeColors(val container: Color, val content: Color)

// Kept clear of the brand teal so that no badge reads as "selected". The label carries the
// format too, so the colors only speed up scanning and are not the sole cue.
private val PdfColors = BadgeColors(Color(0xFFC62828), Color.White)
private val DocxColors = BadgeColors(Color(0xFF283593), Color.White)
// Sky blue is light enough that white text would not stay legible on it.
private val HwpColors = BadgeColors(Color(0xFF4FC3F7), Color(0xFF002F45))
private val TxtColors = BadgeColors(Color(0xFF616161), Color.White)

/** Colored format label at the start of a document row. */
@Composable
internal fun DocumentTypeBadge(type: DocumentType, modifier: Modifier = Modifier) {
    val colors = when (type) {
        DocumentType.PDF -> PdfColors
        DocumentType.DOCX -> DocxColors
        DocumentType.HWP, DocumentType.HWPX -> HwpColors
        DocumentType.TXT -> TxtColors
    }
    Box(
        // Fixed width so that file names line up whatever the format.
        modifier = modifier.width(48.dp).background(colors.container, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(type.labelRes),
            color = colors.content,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

private val DocumentType.labelRes: Int
    get() = when (this) {
        DocumentType.PDF -> R.string.home_type_pdf
        DocumentType.DOCX -> R.string.home_type_docx
        DocumentType.HWP -> R.string.home_type_hwp
        DocumentType.HWPX -> R.string.home_type_hwpx
        DocumentType.TXT -> R.string.home_type_txt
    }
