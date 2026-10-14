package com.odom.docreader.core.model

/** Size of the app's own text, multiplied with the system font size. Document content is not affected. */
enum class FontScale(val multiplier: Float) {
    SMALL(0.85f),
    NORMAL(1f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f),
}
