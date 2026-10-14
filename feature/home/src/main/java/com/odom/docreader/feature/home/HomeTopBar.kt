package com.odom.docreader.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.odom.docreader.core.model.SortOrder
import com.odom.docreader.core.ui.R as UiR

/** Search field with the sort and settings actions beside it; the home screen has no title bar. */
@Composable
internal fun HomeSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    sortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    onOpenSettings: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.weight(1f),
            enabled = enabled,
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
        SortMenuButton(sortOrder = sortOrder, onSortOrderSelected = onSortOrderSelected, enabled = enabled)
        IconButton(onClick = onOpenSettings) {
            Icon(painterResource(UiR.drawable.ic_settings), contentDescription = stringResource(R.string.home_settings))
        }
    }
}

@Composable
private fun SortMenuButton(sortOrder: SortOrder, onSortOrderSelected: (SortOrder) -> Unit, enabled: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    // The box anchors the menu to the button.
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(painterResource(UiR.drawable.ic_sort), contentDescription = stringResource(R.string.home_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOrder.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        expanded = false
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
    }
}

@Composable
internal fun HomeFilterChips(filter: HomeFilter?, onFilterToggled: (HomeFilter) -> Unit) {
    ChipRow {
        HomeFilter.entries.forEach { option ->
            FilterChip(
                selected = option == filter,
                onClick = { onFilterToggled(option) },
                label = { Text(stringResource(option.labelRes)) },
                leadingIcon = if (option == HomeFilter.RECENT) {
                    {
                        Icon(
                            painterResource(UiR.drawable.ic_history),
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

// Scrolls rather than wraps, for large font scales.
@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

private val SortOrder.labelRes: Int
    get() = when (this) {
        SortOrder.NAME -> R.string.home_sort_name
        SortOrder.DATE -> R.string.home_sort_date
        SortOrder.SIZE -> R.string.home_sort_size
    }

private val HomeFilter.labelRes: Int
    get() = when (this) {
        HomeFilter.RECENT -> R.string.home_filter_recent
        HomeFilter.PDF -> R.string.home_type_pdf
        HomeFilter.DOCX -> R.string.home_type_docx
        HomeFilter.HWP -> R.string.home_type_hwp
        HomeFilter.TXT -> R.string.home_type_txt
    }
