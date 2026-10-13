package com.odom.docreader.navigation

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.docreader.ReadingSettingsSheet
import com.odom.docreader.ReadingSettingsViewModel
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.viewer.docx.DocxViewerScreen
import com.odom.docreader.viewer.hwp.HwpViewerScreen
import com.odom.docreader.viewer.pdf.PdfViewerScreen
import com.odom.docreader.viewer.txt.TxtViewerScreen

/** Picks the viewer for the document's format and gives it the shared reading settings. */
@Composable
internal fun ViewerDestination(
    route: ViewerRoute,
    readingSettingsViewModel: ReadingSettingsViewModel,
    onBack: () -> Unit,
) {
    val readingSettings by readingSettingsViewModel.uiState.collectAsStateWithLifecycle()
    var showReadingSettings by rememberSaveable { mutableStateOf(false) }
    val onReadingSettings = { showReadingSettings = true }

    ViewerScreenBrightness(readingSettings.brightness.takeUnless { readingSettings.useSystemBrightness })

    when (val type = DocumentType.entries.firstOrNull { it.name == route.type }) {
        DocumentType.PDF -> PdfViewerScreen(
            uri = route.uri,
            title = route.name,
            onBack = onBack,
            paperColor = readingSettings.paperColor,
            onReadingSettings = onReadingSettings,
        )

        DocumentType.HWP, DocumentType.HWPX -> HwpViewerScreen(
            uri = route.uri,
            title = route.name,
            type = type,
            onBack = onBack,
            paperColor = readingSettings.paperColor,
            onReadingSettings = onReadingSettings,
        )

        DocumentType.DOCX -> DocxViewerScreen(
            uri = route.uri,
            title = route.name,
            onBack = onBack,
            paperColor = readingSettings.paperColor,
            onReadingSettings = onReadingSettings,
        )

        DocumentType.TXT -> TxtViewerScreen(
            uri = route.uri,
            title = route.name,
            onBack = onBack,
            paperColor = readingSettings.paperColor,
            onReadingSettings = onReadingSettings,
        )

        // A route saved by a build that knew a format this build does not.
        null -> UnsupportedViewerScreen(onBack = onBack)
    }

    if (showReadingSettings) {
        ReadingSettingsSheet(
            settings = readingSettings,
            onPaperColorSelected = readingSettingsViewModel::onPaperColorSelected,
            onUseSystemBrightnessChanged = readingSettingsViewModel::onUseSystemBrightnessChanged,
            onBrightnessChanged = readingSettingsViewModel::onBrightnessChanged,
            onBrightnessChangeFinished = readingSettingsViewModel::onBrightnessChangeFinished,
            onDismiss = { showReadingSettings = false },
        )
    }
}

/** Overrides the window's brightness while a viewer is on screen; null follows the system. */
@Composable
private fun ViewerScreenBrightness(brightness: Float?) {
    val window = LocalActivity.current?.window ?: return
    LaunchedEffect(window, brightness) {
        window.setBrightness(brightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
    }
    // Separate from the effect above so that changing the value does not flash back to the system brightness.
    DisposableEffect(window) {
        onDispose { window.setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) }
    }
}

private fun Window.setBrightness(brightness: Float) {
    attributes = attributes.apply { screenBrightness = brightness }
}
