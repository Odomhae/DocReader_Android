import init, { HwpDocument } from './rhwp/rhwp.js';
import { fetchDocument, reportError, reportLoaded } from '../webhost/host.js';
import { createSearch } from './search.js';

// rhwp lays out text inside WASM and has no font access there, so it calls back into this
// global for glyph widths. It must exist before init().
let measureContext = null;
let lastFont = '';
window.measureTextWidth = (font, text) => {
  if (!measureContext) measureContext = document.createElement('canvas').getContext('2d');
  if (font !== lastFont) {
    measureContext.font = font;
    lastFont = font;
  }
  return measureContext.measureText(text).width;
};

// Pages this far outside the viewport are still kept rendered, so scrolling does not show blanks.
const RENDER_MARGIN = '150% 0px';

// Line breaks are computed from measured glyph widths, so the bundled faces have to be loaded
// before the document is laid out, or the layout is done with the wrong metrics.
async function loadBundledFonts() {
  const faces = ['400 16px "Nanum Myeongjo"', '700 16px "Nanum Myeongjo"'];
  await Promise.all(faces.map((face) => document.fonts.load(face, '가')));
}

async function main() {
  const [bytes] = await Promise.all([
    fetchDocument(),
    init({ module_or_path: './rhwp/rhwp_bg.wasm' }),
    loadBundledFonts(),
  ]);
  const doc = new HwpDocument(bytes);
  const pageCount = doc.pageCount();
  if (pageCount < 1) throw new Error('document has no pages');

  const viewer = document.getElementById('viewer');
  const pages = [];
  for (let index = 0; index < pageCount; index++) {
    const info = JSON.parse(doc.getPageInfo(index));
    const page = document.createElement('div');
    page.className = 'page';
    page.dataset.index = String(index);
    page.dataset.width = String(info.width);
    page.dataset.height = String(info.height);
    // Reserves the page's space before it is rendered, so the scroll height is right from the start.
    page.style.aspectRatio = info.width + ' / ' + info.height;
    viewer.appendChild(page);
    pages.push(page);
  }

  const search = createSearch(doc, pages);

  // A long document as SVG is far too much DOM to keep at once: only pages near the viewport
  // hold their SVG, the rest are empty boxes of the right size.
  const observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      const page = entry.target;
      if (entry.isIntersecting) {
        if (!page.firstChild) {
          page.innerHTML = doc.renderPageSvg(Number(page.dataset.index));
          search.decorate(page);
        }
      } else {
        page.textContent = '';
      }
    }
  }, { rootMargin: RENDER_MARGIN });
  pages.forEach((page) => observer.observe(page));

  reportLoaded(pages);
  Object.assign(window.docViewer, search.commands);
}

main().catch(reportError);
