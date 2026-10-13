package com.odom.docreader.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color

private val SepiaPaper = Color(0xFFF4ECD8)

/**
 * Tints whatever is drawn underneath, including hosted Android views. Multiplying keeps black
 * text black and only warms the light areas, which an alpha overlay would not.
 */
fun Modifier.sepiaPaper(enabled: Boolean): Modifier =
    if (!enabled) {
        this
    } else {
        drawWithContent {
            drawContent()
            drawRect(color = SepiaPaper, blendMode = BlendMode.Multiply)
        }
    }
