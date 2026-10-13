package com.odom.docreader.feature.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.odom.docreader.core.model.SortOrder
import com.odom.docreader.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopBar(
    sortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.home_title)) },
        actions = {
            IconButton(onClick = { sortMenuExpanded = true }) {
                Icon(painterResource(UiR.drawable.ic_sort), contentDescription = stringResource(R.string.home_sort))
            }
            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                SortOrder.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            sortMenuExpanded = false
                            onSortOrderSelected(option)
                        },
                        trailingIcon = {
                            if (option == sortOrder) {
                                Icon(painterResource(UiR.drawable.ic_check), contentDescription = null)
                            }
                        },
                    )
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    painterResource(UiR.drawable.ic_settings),
                    contentDescription = stringResource(R.string.home_settings),
                )
            }
        },
    )
}

@Composable
internal fun HomeSearchField(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.home_search_hint)) },
        leadingIcon = { Icon(painterResource(UiR.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        painterResource(UiR.drawable.ic_close),
                        contentDescription = stringResource(R.string.home_clear_search),
                    )
                }
            }
        },
    )
}

@Composable
internal fun HomeTabRow(selectedTab: HomeTab, onTabSelected: (HomeTab) -> Unit) {
    PrimaryScrollableTabRow(selectedTabIndex = selectedTab.ordinal, edgePadding = 8.dp) {
        HomeTab.entries.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}

private val SortOrder.labelRes: Int
    get() = when (this) {
        SortOrder.NAME -> R.string.home_sort_name
        SortOrder.DATE -> R.string.home_sort_date
        SortOrder.SIZE -> R.string.home_sort_size
    }

private val HomeTab.labelRes: Int
    get() = when (this) {
        HomeTab.ALL -> R.string.home_tab_all
        HomeTab.PDF -> R.string.home_tab_pdf
        HomeTab.DOCX -> R.string.home_tab_docx
        HomeTab.HWP -> R.string.home_tab_hwp
        HomeTab.TXT -> R.string.home_tab_txt
        HomeTab.RECENT -> R.string.home_tab_recent
        HomeTab.FAVORITES -> R.string.home_tab_favorites
    }
