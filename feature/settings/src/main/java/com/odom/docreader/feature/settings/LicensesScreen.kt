package com.odom.docreader.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.docreader.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val opened = uiState.opened
    // Back returns from a license text to the list before it leaves the screen.
    BackHandler(enabled = opened != null, onBack = viewModel::onLicenseClosed)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = opened?.library?.name ?: stringResource(R.string.settings_licenses),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = if (opened != null) viewModel::onLicenseClosed else onBack) {
                        Icon(
                            painterResource(UiR.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        if (opened == null) {
            LibraryList(uiState.libraries, viewModel::onLibrarySelected, contentModifier)
        } else {
            LicenseText(opened.text, contentModifier)
        }
    }
}

@Composable
private fun LibraryList(
    libraries: List<OpenSourceLibrary>,
    onLibrarySelected: (OpenSourceLibrary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(libraries, key = { it.name }) { library ->
            ListItem(
                modifier = Modifier.clickable { onLibrarySelected(library) },
                headlineContent = { Text(library.name) },
                supportingContent = { Text(library.license + "\n" + library.copyright) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun LicenseText(text: String?, modifier: Modifier = Modifier) {
    if (text == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Text(
            text = text,
            modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}
