import { fetchDocument, reportError, reportLoaded } from '../webhost/host.js';
import { decodeText } from './decode.js';

async function main() {
  const bytes = await fetchDocument();
  const viewer = document.getElementById('viewer');
  // textContent, never innerHTML: the file is shown as written, markup included.
  viewer.textContent = decodeText(bytes);
  reportLoaded([viewer]);
}

main().catch(reportError);
