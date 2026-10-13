package com.odom.docreader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.odom.docreader.core.model.PaperColor
import com.odom.docreader.core.model.ReadingSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    settings: ReadingSettings,
    onPaperColorSelected: (PaperColor) -> Unit,
    onUseSystemBrightnessChanged: (Boolean) -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onBrightnessChangeFinished: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.reading_paper_color), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaperColor.entries.forEach { paperColor ->
                    FilterChip(
                        selected = settings.paperColor == paperColor,
                        onClick = { onPaperColorSelected(paperColor) },
                        label = { Text(stringResource(paperColor.labelRes)) },
                    )
                }
            }

            val brightnessLabel = stringResource(R.string.reading_brightness)
            Text(
                brightnessLabel,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.useSystemBrightness,
                        role = Role.Switch,
                        onValueChange = onUseSystemBrightnessChanged,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.reading_use_system_brightness), modifier = Modifier.weight(1f))
                Switch(checked = settings.useSystemBrightness, onCheckedChange = null)
            }
            Slider(
                value = settings.brightness,
                onValueChange = onBrightnessChanged,
                onValueChangeFinished = onBrightnessChangeFinished,
                valueRange = ReadingSettings.MIN_BRIGHTNESS..ReadingSettings.MAX_BRIGHTNESS,
                modifier = Modifier.semantics { contentDescription = brightnessLabel },
            )
        }
    }
}

private val PaperColor.labelRes: Int
    get() = when (this) {
        PaperColor.WHITE -> R.string.reading_paper_white
        PaperColor.SEPIA -> R.string.reading_paper_sepia
    }
