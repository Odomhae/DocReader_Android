import { reportFindResult } from '../webhost/host.js';

// In-document search over rhwp's text layout. It works on every page whether or not the page is
// currently rendered, which WebView's own find-in-page cannot do with lazily rendered pages.
export function createSearch(doc, pages) {
  // Per page: lower-cased text of all runs, plus where each run starts in that text.
  let index = null;
  let matches = [];
  let active = -1;

  const buildIndex = () => pages.map((_, pageIndex) => {
    const layout = JSON.parse(doc.getPageTextLayout(pageIndex));
    let text = '';
    const runs = [];
    for (const run of layout.runs || []) {
      runs.push({ start: text.length, run });
      text += run.text;
    }
    return { text: text.toLowerCase(), runs };
  });

  // Rectangles, in page units, covering characters [start, end) of a page's text.
  const rectsFor = (pageIndex, start, end) => {
    const rects = [];
    for (const { start: runStart, run } of index[pageIndex].runs) {
      const from = Math.max(start, runStart) - runStart;
      const to = Math.min(end, runStart + run.text.length) - runStart;
      if (from >= to) continue;
      const left = run.x + run.charX[from];
      rects.push({ x: left, y: run.y, width: run.x + run.charX[to] - left, height: run.h });
    }
    return rects;
  };

  const decorate = (page) => {
    page.querySelectorAll('.match').forEach((element) => element.remove());
    if (!page.firstChild) return;
    const pageIndex = Number(page.dataset.index);
    const pageWidth = Number(page.dataset.width);
    const pageHeight = Number(page.dataset.height);
    matches.forEach((match, matchIndex) => {
      if (match.page !== pageIndex) return;
      for (const rect of match.rects) {
        const box = document.createElement('div');
        box.className = matchIndex === active ? 'match active' : 'match';
        box.style.left = (rect.x / pageWidth * 100) + '%';
        box.style.top = (rect.y / pageHeight * 100) + '%';
        box.style.width = (rect.width / pageWidth * 100) + '%';
        box.style.height = (rect.height / pageHeight * 100) + '%';
        page.appendChild(box);
      }
    });
  };

  const showActive = () => {
    pages.forEach(decorate);
    reportFindResult(active + 1, matches.length);
    if (active < 0) return;
    const match = matches[active];
    const page = pages[match.page];
    const top = page.offsetTop + match.rects[0].y / Number(page.dataset.height) * page.offsetHeight;
    window.scrollTo(0, Math.max(0, top - window.innerHeight / 3));
  };

  const find = (query) => {
    if (!index) index = buildIndex();
    const needle = query.toLowerCase();
    matches = [];
    index.forEach((pageText, pageIndex) => {
      let at = pageText.text.indexOf(needle);
      while (at !== -1) {
        matches.push({ page: pageIndex, rects: rectsFor(pageIndex, at, at + needle.length) });
        at = pageText.text.indexOf(needle, at + needle.length);
      }
    });
    // Start from the first match at or after the page being read, not from the top of the document.
    const centre = window.scrollY + window.innerHeight / 2;
    const currentPage = pages.findIndex((page) => page.offsetTop + page.offsetHeight > centre);
    active = matches.findIndex((match) => match.page >= currentPage);
    if (active === -1 && matches.length > 0) active = 0;
    showActive();
  };

  const findNext = (forward) => {
    if (matches.length === 0) return;
    active = (active + (forward ? 1 : matches.length - 1)) % matches.length;
    showActive();
  };

  const clearFind = () => {
    matches = [];
    active = -1;
    pages.forEach(decorate);
  };

  return { decorate, commands: { find, findNext, clearFind } };
}
