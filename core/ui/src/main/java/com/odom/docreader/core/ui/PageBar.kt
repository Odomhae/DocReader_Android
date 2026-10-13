package com.odom.docreader.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.odom.docreader.core.model.resolvePageInput

// More digits than any document has pages; keeps the field from growing across the bar.
private const val MAX_PAGE_DIGITS = 6

/**
 * Bottom bar of every viewer: previous / next and the current page, which can be edited to jump
 * to a page. [currentPage] is 1-based.
 */
@Composable
fun PageBar(currentPage: Int, pageCount: Int, onGoToPage: (Int) -> Unit, modifier: Modifier = Modifier) {
    // Non-null while the page number is being edited.
    var draft by remember { mutableStateOf<TextFieldValue?>(null) }
    val editing = draft != null
    val focusManager = LocalFocusManager.current
    ClearFocusWhenKeyboardCloses(editing = editing, onClearFocus = { focusManager.clearFocus() })

    // Only while editing: the keyboard of a viewer's search field must not lift this bar.
    val insets = if (editing) WindowInsets.navigationBars.union(WindowInsets.ime) else WindowInsets.navigationBars
    Surface(modifier = modifier, tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(insets),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onGoToPage(currentPage - 1) }, enabled = currentPage > 1) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.page_previous),
                )
            }
            PageNumberField(
                value = draft ?: TextFieldValue(currentPage.toString()),
                onValueChange = { value ->
                    draft = value.copy(text = value.text.filter { it in '0'..'9' }.take(MAX_PAGE_DIGITS))
                },
                onFocusChanged = { focused ->
                    val text = currentPage.toString()
                    // Selected, so that typing replaces the current page instead of appending to it.
                    draft = if (focused) TextFieldValue(text, selection = TextRange(0, text.length)) else null
                },
                onDone = {
                    resolvePageInput(draft?.text.orEmpty(), pageCount)?.let(onGoToPage)
                    focusManager.clearFocus()
                },
            )
            Text(
                text = stringResource(R.string.page_count, pageCount),
                modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            IconButton(onClick = { onGoToPage(currentPage + 1) }, enabled = currentPage < pageCount) {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.page_next),
                )
            }
        }
    }
}

@Composable
private fun PageNumberField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    val description = stringResource(R.string.page_number_field)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .padding(start = 16.dp)
            .widthIn(min = 48.dp)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .semantics { contentDescription = description },
        textStyle = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onDone() }),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            // The filled box is what tells the reader the number can be tapped.
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                innerTextField()
            }
        },
    )
}

/** Closing the keyboard with Back leaves the field focused; treat that as cancelling the edit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClearFocusWhenKeyboardCloses(editing: Boolean, onClearFocus: () -> Unit) {
    val keyboardVisible = WindowInsets.isImeVisible
    var keyboardWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(editing, keyboardVisible) {
        when {
            !editing -> keyboardWasVisible = false
            keyboardVisible -> keyboardWasVisible = true
            // The keyboard is not up yet right after the field gains focus.
            keyboardWasVisible -> onClearFocus()
        }
    }
}
