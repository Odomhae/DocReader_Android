package com.odom.docreader.feature.home

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.odom.docreader.core.model.DocumentFile
import com.odom.docreader.core.ui.R as UiR
import java.text.DateFormat
import java.util.Date

@Composable
internal fun DocumentList(
    documents: List<DocumentListItem>,
    onToggleFavorite: (DocumentListItem) -> Unit,
    onOpenDocument: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(documents, key = { it.file.uri }) { item ->
            DocumentRow(
                item = item,
                onToggleFavorite = { onToggleFavorite(item) },
                onClick = { onOpenDocument(item.file) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun DocumentRow(item: DocumentListItem, onToggleFavorite: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val openedAt = item.openedAt
    val time = if (openedAt != null) openedTimeText(openedAt) else dateFormat.format(Date(item.file.lastModified))
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { DocumentTypeBadge(item.file.type) },
        headlineContent = { Text(item.file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(stringResource(R.string.home_document_details, Formatter.formatShortFileSize(context, item.file.sizeBytes), time))
        },
        trailingContent = {
            IconButton(onClick = onToggleFavorite) {
                if (item.isFavorite) {
                    Icon(
                        painterResource(UiR.drawable.ic_star),
                        contentDescription = stringResource(R.string.home_remove_favorite),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        painterResource(UiR.drawable.ic_star_border),
                        contentDescription = stringResource(R.string.home_add_favorite),
                    )
                }
            }
        },
    )
}

/** "3 hours ago" style; DateUtils falls back to a date once it is more than a week old. */
@Composable
private fun openedTimeText(openedAt: Long): String {
    val now = System.currentTimeMillis()
    // Under a minute DateUtils would say "0 minutes ago".
    if (now - openedAt < DateUtils.MINUTE_IN_MILLIS) return stringResource(R.string.home_opened_just_now)
    return DateUtils.getRelativeTimeSpanString(openedAt, now, DateUtils.MINUTE_IN_MILLIS).toString()
}
