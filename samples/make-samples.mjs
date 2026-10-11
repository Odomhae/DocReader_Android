// Generates the self-authored HWP/HWPX samples in this folder with @rhwp/core.
// usage: node make-samples.mjs <@rhwp/core package dir> <output dir>
import { readFileSync, writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { deflateSync } from 'node:zlib';
import path from 'node:path';

const [pkgDir, outDir] = process.argv.slice(2);

// Node has no canvas; a rough width estimate is enough to author the samples.
globalThis.measureTextWidth = (font, text) => {
  const size = parseFloat((font.match(/([\d.]+)px/) || [0, 16])[1]);
  let width = 0;
  for (const ch of text) width += ch.charCodeAt(0) > 0x2e80 ? size : size * 0.5;
  return width;
};
const rhwp = await import(pathToFileURL(path.join(pkgDir, 'rhwp.js')).href);
rhwp.initSync({ module: readFileSync(path.join(pkgDir, 'rhwp_bg.wasm')) });
console.log('rhwp', rhwp.version());

function ok(json, what) {
  const result = JSON.parse(json);
  if (result.ok === false) throw new Error(`${what} failed: ${json}`);
  return result;
}

// A small builder over rhwp's editing API that only ever appends at the end of the body.
class Builder {
  constructor() {
    this.doc = rhwp.HwpDocument.createEmpty();
    this.doc.createBlankDocument();
    this.para = 0;
    this.length = 0;
  }

  text(text) {
    ok(this.doc.insertText(0, this.para, this.length, text), 'insertText');
    this.length += text.length;
    return this;
  }

  newParagraph() {
    this.para = ok(this.doc.splitParagraph(0, this.para, this.length), 'splitParagraph').paraIdx;
    this.length = 0;
    return this;
  }

  paragraph(text) {
    return this.text(text).newParagraph();
  }

  pageBreak() {
    ok(this.doc.insertPageBreak(0, this.para, this.length), 'insertPageBreak');
    this.para = this.doc.getParagraphCount(0) - 1;
    this.length = 0;
    return this;
  }

  table(rows) {
    const columns = rows[0].length;
    const created = ok(this.doc.createTable(0, this.para, this.length, rows.length, columns), 'createTable');
    rows.flat().forEach((cellText, cellIndex) => {
      ok(this.doc.insertTextInCell(0, created.paraIdx, created.controlIdx, cellIndex, 0, 0, cellText), 'insertTextInCell');
    });
    return this.toEnd();
  }

  picture(png, widthPx, heightPx) {
    // 7200 HWPUNIT per inch; the picture is placed at 96 dpi.
    const options = {
      sectionIdx: 0, paraIdx: this.para, charOffset: this.length,
      width: widthPx * 75, height: heightPx * 75, naturalWidthPx: widthPx, naturalHeightPx: heightPx,
      extension: 'png', description: 'sample picture',
    };
    const inserted = ok(this.doc.insertPictureEx(JSON.stringify(options), png), 'insertPicture');
    // Inserted pictures float at the paper origin by default; samples want them in the text flow.
    ok(this.doc.setPictureProperties(0, inserted.paraIdx, inserted.controlIdx, JSON.stringify({ treatAsChar: true })), 'setPictureProperties');
    return this.toEnd();
  }

  equation(script) {
    ok(this.doc.insertEquation(0, this.para, this.length, script, 1200, 0), 'insertEquation');
    return this.toEnd();
  }

  header(text) {
    ok(this.doc.createHeaderFooter(0, true, 0), 'createHeader');
    ok(this.doc.insertTextInHeaderFooter(0, true, 0, 0, 0, text), 'header text');
    return this;
  }

  footer(text) {
    ok(this.doc.createHeaderFooter(0, false, 0), 'createFooter');
    ok(this.doc.insertTextInHeaderFooter(0, false, 0, 0, 0, text), 'footer text');
    return this;
  }

  // Controls shift paragraph indexes, so continue in a fresh paragraph after the last one.
  toEnd() {
    this.para = this.doc.getParagraphCount(0) - 1;
    this.length = this.doc.getParagraphLength(0, this.para);
    return this.newParagraph();
  }

  save(name, format) {
    const bytes = format === 'hwpx' ? this.doc.exportHwpx() : this.doc.exportHwp();
    writeFileSync(path.join(outDir, name), bytes);
    const reloaded = new rhwp.HwpDocument(bytes);
    const pages = reloaded.pageCount();
    for (let i = 0; i < pages; i++) reloaded.renderPageSvg(i);
    const svg = reloaded.renderPageSvg(0);
    console.log(
      `${name}: ${bytes.length} bytes, ${pages} page(s)`,
      `table=${svg.includes('<line') || svg.includes('<path')}`,
      `image=${svg.includes('<image')}`,
    );
    return pages;
  }
}

function crc32(bytes) {
  let crc = ~0;
  for (const byte of bytes) {
    crc ^= byte;
    for (let k = 0; k < 8; k++) crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
  }
  return ~crc >>> 0;
}

// A hand-built gradient PNG, so the sample contains no third-party image.
function gradientPng(width, height) {
  const raw = Buffer.alloc((width * 3 + 1) * height);
  for (let y = 0; y < height; y++) {
    const row = y * (width * 3 + 1);
    for (let x = 0; x < width; x++) {
      raw[row + 1 + x * 3] = Math.round(255 * x / width);
      raw[row + 2 + x * 3] = Math.round(255 * y / height);
      raw[row + 3 + x * 3] = 160;
    }
  }
  const chunk = (type, data) => {
    const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
    const out = Buffer.alloc(body.length + 8);
    out.writeUInt32BE(data.length, 0);
    body.copy(out, 4);
    out.writeUInt32BE(crc32(body), body.length + 4);
    return out;
  };
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width, 0);
  header.writeUInt32BE(height, 4);
  header.set([8, 2, 0, 0, 0], 8);
  return new Uint8Array(Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', header),
    chunk('IDAT', deflateSync(raw)),
    chunk('IEND', Buffer.alloc(0)),
  ]));
}

const png = gradientPng(240, 160);
const tableRows = [
  ['구분', '1분기', '2분기'],
  ['매출', '1,200', '1,450'],
  ['비용', '800', '910'],
];
const quadratic = 'x = {-b +- sqrt {b^2 - 4ac}} over {2a}';
const pangram = '다람쥐 헌 쳇바퀴에 타고파. The quick brown fox jumps over the lazy dog. ';

new Builder()
  .text('DocReader HWP 렌더링 검증용 샘플 문서 이 문서는 @rhwp/core로 직접 생성했으며 저작권 문제가 없습니다. ')
  .text('가나다라마바사아자차카타파하 ABCDEFG abcdefg 0123456789 ' + pangram)
  .save('sample.hwp');

new Builder()
  .text('DocReader HWPX 샘플 문서. ' + pangram)
  .save('sample.hwpx', 'hwpx');

new Builder().paragraph('표가 들어 있는 샘플 문서').table(tableRows).paragraph('표 아래 문단입니다.')
  .save('sample-table.hwp');

new Builder().paragraph('그림이 들어 있는 샘플 문서').picture(png, 240, 160).paragraph('그림 아래 문단입니다.')
  .save('sample-image.hwp');

new Builder().paragraph('수식이 들어 있는 샘플 문서').equation(quadratic).paragraph('근의 공식입니다.')
  .save('sample-equation.hwp');

const multipage = new Builder();
for (let page = 1; page <= 6; page++) {
  multipage.paragraph(`${page}쪽 제목 chapter${page}`);
  for (let line = 0; line < 8; line++) multipage.paragraph(pangram.repeat(2));
  if (page < 6) multipage.pageBreak();
}
multipage.save('sample-multipage.hwp');

new Builder()
  .header('DocReader 샘플 머리말')
  .footer('DocReader 샘플 꼬리말')
  .paragraph('표, 그림, 수식이 모두 들어 있는 HWPX 샘플 문서')
  .table(tableRows)
  .picture(png, 240, 160)
  .equation(quadratic)
  .paragraph(pangram)
  .save('sample-mixed.hwpx', 'hwpx');

new Builder()
  .paragraph('표, 그림, 수식이 모두 들어 있는 HWP 샘플 문서')
  .table(tableRows)
  .picture(png, 240, 160)
  .equation(quadratic)
  .paragraph(pangram)
  .save('sample-mixed.hwp');
