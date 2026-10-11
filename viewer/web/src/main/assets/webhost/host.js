// Runtime shared by every viewer page. It is the only code that talks to the native bridge.
//
// Contract with the native host:
//   - the document is fetched from /document/current (never passed in as a string)
//   - DocViewerBridge.onLoaded(pageCount), onPageChanged(page), onError(message)
//   - the host calls window.docViewer.goToPage(page), pages are 1-based
//   - a page that searches its own text adds find(query), findNext(forward) and clearFind() to
//     window.docViewer and answers with DocViewerBridge.onFindResult(activeMatch, matchCount)

const bridge = window.DocViewerBridge;

export async function fetchDocument() {
  const response = await fetch('/document/current');
  if (!response.ok) throw new Error('document fetch failed: HTTP ' + response.status);
  return new Uint8Array(await response.arrayBuffer());
}

// activeMatch is 1-based, 0 when nothing matched.
export function reportFindResult(activeMatch, matchCount) {
  bridge.onFindResult(activeMatch, matchCount);
}

export function reportError(error) {
  bridge.onError(String((error && error.message) || error));
}

// Call once the page elements exist, in document order. Reports the page count and from then on
// the page under the middle of the viewport.
export function reportLoaded(pageElements) {
  const pages = Array.from(pageElements);
  let reportedPage = 1;

  // Bounding rects rather than offsetTop, so a page that scales its content with a CSS transform
  // (DOCX fit-to-width) is still measured where it is actually drawn.
  const pageTop = (page) => page.getBoundingClientRect().top + window.scrollY;

  const pageAtViewportCenter = () => {
    const center = window.scrollY + window.innerHeight / 2;
    let low = 0;
    let high = pages.length - 1;
    while (low < high) {
      const middle = (low + high + 1) >> 1;
      if (pageTop(pages[middle]) <= center) low = middle;
      else high = middle - 1;
    }
    return low + 1;
  };

  let scheduled = false;
  window.addEventListener('scroll', () => {
    if (scheduled) return;
    scheduled = true;
    requestAnimationFrame(() => {
      scheduled = false;
      const page = pageAtViewportCenter();
      if (page !== reportedPage) {
        reportedPage = page;
        bridge.onPageChanged(page);
      }
    });
  }, { passive: true });

  window.docViewer = {
    goToPage(page) {
      const target = pages[Math.min(Math.max(page, 1), pages.length) - 1];
      if (target) window.scrollTo(0, pageTop(target));
    },
  };

  bridge.onLoaded(pages.length);
}

// Classic-script failures (syntax errors in a bundle, unhandled rejections) never reach a
// try/catch in page code, so they are funnelled to the host from here.
window.addEventListener('error', (event) => reportError(event.message || 'script error'));
window.addEventListener('unhandledrejection', (event) => reportError(event.reason));
