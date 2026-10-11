package com.odom.docreader.viewer.pdf.legacy

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.docreader.viewer.pdf.PdfFailureMessage
import com.odom.docreader.viewer.pdf.PdfViewerScaffold
import com.odom.docreader.viewer.pdf.R

private const val MAX_ZOOM = 4f

// Pages are rendered wider than the screen so text stays readable when zoomed, capped to bound memory.
private const val RENDER_SCALE = 1.5f
private const val MAX_RENDER_WIDTH_PX = 1620

// Placeholder shape until a page is rendered: A4 portrait.
private const val DEFAULT_PAGE_ASPECT_RATIO = 1f / 1.414f

/** PdfRenderer-based viewer for API 26-27. It has no text layer, so there is no in-document search. */
@Composable
internal fun LegacyPdfViewer(
    uri: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LegacyPdfViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uri) { viewModel.open(uri) }

    PdfViewerScaffold(uri = uri, title = title, onBack = onBack, onSearch = null, modifier = modifier) { contentModifier ->
        when (val state = uiState) {
            LegacyPdfUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is LegacyPdfUiState.Failed -> PdfFailureMessage(state.failure, contentModifier)

            is LegacyPdfUiState.Ready -> PdfPageList(
                pageCount = state.pageCount,
                renderPage = viewModel::renderPage,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun PdfPageList(
    pageCount: Int,
    renderPage: suspend (pageIndex: Int, widthPx: Int) -> android.graphics.Bitmap?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = modifier.clipToBounds().background(MaterialTheme.colorScheme.surfaceVariant)) {
        val density = LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val renderWidthPx = (viewportWidthPx * RENDER_SCALE).toInt().coerceIn(1, MAX_RENDER_WIDTH_PX)
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            zoom = (zoom * zoomChange).coerceIn(1f, MAX_ZOOM)
            val maxOffsetX = viewportWidthPx * (zoom - 1f) / 2f
            offsetX = (offsetX + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
            // Vertical movement scrolls the list instead of shifting the layer, so every page stays reachable.
            listState.dispatchRawDelta(-panChange.y / zoom)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState, canPan = { zoom > 1f })
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = offsetX
                    transformOrigin = TransformOrigin(0.5f, 0f)
                },
            // The layer grows downwards when zoomed; the extra padding keeps the last page scrollable into view.
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + maxHeight * (1f - 1f / zoom)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(count = pageCount, key = { it }) { pageIndex ->
                PdfPage(pageIndex = pageIndex, renderWidthPx = renderWidthPx, renderPage = renderPage)
            }
        }
    }
}

private sealed interface PageImage {
    data object Rendering : PageImage
    data object Unavailable : PageImage
    data class Rendered(val bitmap: android.graphics.Bitmap) : PageImage
}

@Composable
private fun PdfPage(
    pageIndex: Int,
    renderWidthPx: Int,
    renderPage: suspend (pageIndex: Int, widthPx: Int) -> android.graphics.Bitmap?,
) {
    val image by produceState<PageImage>(PageImage.Rendering, pageIndex, renderWidthPx) {
        value = renderPage(pageIndex, renderWidthPx)?.let { PageImage.Rendered(it) } ?: PageImage.Unavailable
    }
    val pageModifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    when (val current = image) {
        is PageImage.Rendered -> Image(
            bitmap = current.bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.pdf_page_description, pageIndex + 1),
            modifier = pageModifier.aspectRatio(current.bitmap.width.toFloat() / current.bitmap.height),
            contentScale = ContentScale.FillWidth,
        )

        PageImage.Rendering -> Box(
            modifier = pageModifier.aspectRatio(DEFAULT_PAGE_ASPECT_RATIO).background(Color.White),
        )

        PageImage.Unavailable -> Box(
            modifier = pageModifier.aspectRatio(DEFAULT_PAGE_ASPECT_RATIO).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.pdf_page_unavailable, pageIndex + 1),
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
            )
        }
    }
}
