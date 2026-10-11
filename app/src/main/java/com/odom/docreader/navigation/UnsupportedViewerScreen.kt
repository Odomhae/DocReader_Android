package com.odom.docreader.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.odom.docreader.R
import com.odom.docreader.core.ui.EmptyState
import com.odom.docreader.core.ui.R as UiR

/** Shown for a file that is not one of the supported formats or could not be read. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnsupportedViewerScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(UiR.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        EmptyState(
            title = stringResource(R.string.unsupported_title),
            message = stringResource(R.string.unsupported_message),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
