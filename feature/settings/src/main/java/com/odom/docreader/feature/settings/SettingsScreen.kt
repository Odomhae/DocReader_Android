package com.odom.docreader.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.docreader.core.ads.AdsViewModel
import com.odom.docreader.core.ads.BannerAd
import com.odom.docreader.core.model.FontScale
import com.odom.docreader.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenLicenses: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adsState by adsViewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    var showFontScaleDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(UiR.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
        bottomBar = { BannerAd() },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ListItem(
                modifier = Modifier.clickable { showFontScaleDialog = true },
                headlineContent = { Text(stringResource(R.string.settings_font_scale)) },
                supportingContent = { Text(stringResource(uiState.fontScale.labelRes)) },
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenLicenses),
                headlineContent = { Text(stringResource(R.string.settings_licenses)) },
                supportingContent = { Text(stringResource(R.string.settings_licenses_summary)) },
            )
            HorizontalDivider()
            // UMP requires a way to change ad consent only where it asked for consent (EEA and UK).
            if (adsState.privacyOptionsRequired && activity != null) {
                ListItem(
                    modifier = Modifier.clickable { adsViewModel.showPrivacyOptionsForm(activity) },
                    headlineContent = { Text(stringResource(R.string.settings_privacy_options)) },
                    supportingContent = { Text(stringResource(R.string.settings_privacy_options_summary)) },
                )
                HorizontalDivider()
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(versionName) },
            )
        }
    }
    if (showFontScaleDialog) {
        FontScaleDialog(
            selected = uiState.fontScale,
            onSelected = { fontScale ->
                showFontScaleDialog = false
                viewModel.onFontScaleSelected(fontScale)
            },
            onDismiss = { showFontScaleDialog = false },
        )
    }
}

@Composable
private fun FontScaleDialog(selected: FontScale, onSelected: (FontScale) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_font_scale)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                FontScale.entries.forEach { option ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, role = Role.RadioButton) { onSelected(option) },
                        leadingContent = { RadioButton(selected = option == selected, onClick = null) },
                        headlineContent = { Text(stringResource(option.labelRes)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_font_scale_cancel)) }
        },
    )
}

private val FontScale.labelRes: Int
    get() = when (this) {
        FontScale.SMALL -> R.string.settings_font_scale_small
        FontScale.NORMAL -> R.string.settings_font_scale_normal
        FontScale.LARGE -> R.string.settings_font_scale_large
        FontScale.EXTRA_LARGE -> R.string.settings_font_scale_extra_large
    }
