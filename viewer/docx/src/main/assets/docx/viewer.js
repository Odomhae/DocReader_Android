import { fetchDocument, reportError, reportLoaded } from '../webhost/host.js';

// Pages are laid out at their real size (an A4 page is about 794 CSS px wide), which is far wider
// than a phone. The whole document is scaled down to the viewport width so nothing scrolls
// sideways; pinch zoom then magnifies from there.
const WRAPPER_PADDING_PX = 8;

function fitToWidth(container, pages, viewportWidth) {
  // renderAsync also puts its <style> elements in the container, so the wrapper is not the first child.
  const wrapper = container.querySelector('.docx-wrapper');
  // Measured from the pages: the wrapper itself is only as wide as the viewport it overflows.
  const contentWidth = Math.max(...pages.map((page) => page.offsetWidth)) + 2 * WRAPPER_PADDING_PX;
  const scale = Math.min(1, viewportWidth / contentWidth);
  wrapper.style.width = (contentWidth - 2 * WRAPPER_PADDING_PX) + 'px';
  wrapper.style.transformOrigin = '0 0';
  wrapper.style.transform = 'scale(' + scale + ')';
  // A transform does not change layout size, so the scrollable height is set to what is drawn.
  container.style.height = (wrapper.offsetHeight * scale) + 'px';
  container.style.overflow = 'hidden';
}

async function main() {
  // Taken before rendering: once content overflows sideways, WebView reports the content width
  // as the viewport width, which would make the scale come out as 1.
  const viewportWidth = document.documentElement.clientWidth;
  const bytes = await fetchDocument();
  const container = document.getElementById('viewer');
  await window.docx.renderAsync(bytes, container, null, {
    className: 'docx',
    inWrapper: true,
    breakPages: true,
    renderHeaders: true,
    renderFooters: true,
    renderFootnotes: true,
    renderEndnotes: true,
  });

  const pages = Array.from(container.querySelectorAll('section.docx'));
  if (pages.length === 0) throw new Error('document has no pages');

  fitToWidth(container, pages, viewportWidth);
  reportLoaded(pages);
}

main().catch(reportError);
