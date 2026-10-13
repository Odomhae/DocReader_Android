package com.odom.docreader.core.model

enum class PaperColor {
    WHITE,
    SEPIA,
}

/** How documents are shown in the viewers. The rest of the app is not affected. */
data class ReadingSettings(
    val paperColor: PaperColor = PaperColor.WHITE,
    /** While true the app leaves the screen brightness, including auto-brightness, alone. */
    val useSystemBrightness: Boolean = true,
    /** Between [MIN_BRIGHTNESS] and 1. Only applied while [useSystemBrightness] is false. */
    val brightness: Float = DEFAULT_BRIGHTNESS,
) {
    companion object {
        // Not 0: some panels turn almost black there, which would leave no way to see the slider.
        const val MIN_BRIGHTNESS = 0.01f
        const val MAX_BRIGHTNESS = 1f
        const val DEFAULT_BRIGHTNESS = 0.5f
    }
}
