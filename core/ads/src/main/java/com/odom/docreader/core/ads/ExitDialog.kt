package com.odom.docreader.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize

/**
 * Asks before leaving the app, with the preloaded 300x250 ad on top when one is ready. Without an
 * ad (offline, no fill, no consent) the same question is shown on its own, so back always behaves
 * the same way.
 */
@Composable
fun ExitDialog(onDismiss: () -> Unit, onExit: () -> Unit, viewModel: AdsViewModel = hiltViewModel()) {
    val activity = LocalActivity.current
    val ad = remember { viewModel.pollExitDialogAd() }
    DisposableEffect(ad) {
        onDispose { ad?.destroy() }
    }

    // The platform default width is too narrow for a 300dp ad on small phones.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.width(DialogContentWidth).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (ad != null && activity != null) {
                    AndroidView(
                        factory = { ad.getView(activity) },
                        modifier = Modifier.size(AdSize.MEDIUM_RECTANGLE_WIDTH.dp, AdSize.MEDIUM_RECTANGLE_HEIGHT.dp),
                    )
                    // AdMob does not allow the buttons of an exit confirmation right next to the ad.
                    Spacer(modifier = Modifier.height(32.dp))
                }
                Text(
                    text = stringResource(R.string.ads_exit_message),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.ads_exit_cancel)) }
                    TextButton(onClick = onExit) { Text(stringResource(R.string.ads_exit_confirm)) }
                }
            }
        }
    }
}

// The ad width plus the dialog's own padding.
private val DialogContentWidth = (AdSize.MEDIUM_RECTANGLE_WIDTH + 32).dp
