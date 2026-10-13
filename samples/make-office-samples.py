"""Generates the self-authored PDF and DOCX samples in this folder, with no third-party content.

usage: python make-office-samples.py <output dir>
Needs pypdf (BSD-3-Clause) only for the password-protected PDF.
"""
import io
import struct
import sys
import zipfile
import zlib
from pathlib import Path

from pypdf import PdfReader, PdfWriter

PAGE_COUNT = 120
PASSWORD = "docreader"

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
REL_BASE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
XML_HEADER = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
PANGRAM = "다람쥐 헌 쳇바퀴에 타고파. The quick brown fox jumps over the lazy dog. "


def build_pdf(page_count: int) -> bytes:
    """Writes a minimal text-only PDF by hand."""
    objects: list[bytes] = []

    def add(body: bytes) -> int:
        objects.append(body)
        return len(objects)

    catalog_id = add(b"")
    pages_id = add(b"")
    font_id = add(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
    page_ids = []
    for number in range(1, page_count + 1):
        lines = [f"DocReader sample PDF - page {number} of {page_count}"]
        lines += [f"Line {i}: the quick brown fox jumps over the lazy dog." for i in range(1, 31)]
        text = ["BT", "/F1 14 Tf", "72 770 Td", "18 TL"]
        for line in lines:
            text.append(f"({line}) Tj T*")
        text.append("ET")
        stream = "\n".join(text).encode("ascii")
        content_id = add(b"<< /Length %d >>\nstream\n" % len(stream) + stream + b"\nendstream")
        page_ids.append(add(
            b"<< /Type /Page /Parent %d 0 R /MediaBox [0 0 595 842] /Contents %d 0 R "
            b"/Resources << /Font << /F1 %d 0 R >> >> >>" % (pages_id, content_id, font_id)
        ))
    objects[catalog_id - 1] = b"<< /Type /Catalog /Pages %d 0 R >>" % pages_id
    kids = b" ".join(b"%d 0 R" % page_id for page_id in page_ids)
    objects[pages_id - 1] = b"<< /Type /Pages /Count %d /Kids [%s] >>" % (len(page_ids), kids)

    out = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = []
    for index, body in enumerate(objects, start=1):
        offsets.append(len(out))
        out += b"%d 0 obj\n" % index + body + b"\nendobj\n"
    xref_offset = len(out)
    out += b"xref\n0 %d\n" % (len(objects) + 1)
    out += b"0000000000 65535 f \n"
    for offset in offsets:
        out += b"%010d 00000 n \n" % offset
    out += b"trailer\n<< /Size %d /Root %d 0 R >>\nstartxref\n%d\n%%%%EOF\n" % (
        len(objects) + 1, catalog_id, xref_offset)
    return bytes(out)


def gradient_png(width: int, height: int) -> bytes:
    """A hand-built gradient image, so the samples contain no third-party picture."""
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw += bytes((255 * x // width, 255 * y // height, 160))

    def chunk(kind: bytes, data: bytes) -> bytes:
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

    header = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", zlib.compress(bytes(raw)))
        + chunk(b"IEND", b"")
    )


def paragraph(text: str) -> str:
    return f'<w:p><w:r><w:t xml:space="preserve">{text}</w:t></w:r></w:p>'


def page_break() -> str:
    return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'


def table(rows: list[list[str]]) -> str:
    borders = "".join(
        f'<w:{side} w:val="single" w:sz="4" w:space="0" w:color="000000"/>'
        for side in ("top", "left", "bottom", "right", "insideH", "insideV")
    )
    grid = "".join('<w:gridCol w:w="3000"/>' for _ in rows[0])
    body = "".join(
        "<w:tr>" + "".join(
            f'<w:tc><w:tcPr><w:tcW w:w="3000" w:type="dxa"/></w:tcPr>{paragraph(cell)}</w:tc>' for cell in row
        ) + "</w:tr>"
        for row in rows
    )
    return (
        f"<w:tbl><w:tblPr><w:tblBorders>{borders}</w:tblBorders></w:tblPr>"
        f"<w:tblGrid>{grid}</w:tblGrid>{body}</w:tbl>"
    )


def picture(relationship_id: str, width_px: int, height_px: int) -> str:
    # 914400 EMU per inch; the picture is placed at 96 dpi.
    cx, cy = width_px * 9525, height_px * 9525
    return (
        "<w:p><w:r><w:drawing>"
        f'<wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="{cx}" cy="{cy}"/>'
        '<wp:docPr id="1" name="Picture 1"/>'
        '<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">'
        '<a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
        '<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">'
        '<pic:nvPicPr><pic:cNvPr id="1" name="image1.png"/><pic:cNvPicPr/></pic:nvPicPr>'
        f'<pic:blipFill><a:blip r:embed="{relationship_id}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
        f'<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>'
        '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>'
        "</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>"
    )


def build_docx(body: str, image: bytes | None = None, header: str | None = None, footer: str | None = None) -> bytes:
    """Packs WordprocessingML [body] into a .docx. The body refers to [image] as rId2."""
    overrides = [("/word/document.xml", "wordprocessingml.document.main")]
    relationships = []
    section_refs = ""
    if image is not None:
        relationships.append(("rId2", "image", "media/image1.png"))
    if header is not None:
        overrides.append(("/word/header1.xml", "wordprocessingml.header"))
        relationships.append(("rId3", "header", "header1.xml"))
        section_refs += '<w:headerReference w:type="default" r:id="rId3"/>'
    if footer is not None:
        overrides.append(("/word/footer1.xml", "wordprocessingml.footer"))
        relationships.append(("rId4", "footer", "footer1.xml"))
        section_refs += '<w:footerReference w:type="default" r:id="rId4"/>'

    content_types = (
        f'{XML_HEADER}<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml" ContentType="application/xml"/>'
        '<Default Extension="png" ContentType="image/png"/>'
        + "".join(
            f'<Override PartName="{part}" ContentType="application/vnd.openxmlformats-officedocument.{kind}+xml"/>'
            for part, kind in overrides
        )
        + "</Types>"
    )
    package_rels = (
        f'{XML_HEADER}<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        f'<Relationship Id="rId1" Type="{REL_BASE}/officeDocument" Target="word/document.xml"/></Relationships>'
    )
    document_rels = (
        f'{XML_HEADER}<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        + "".join(
            f'<Relationship Id="{rid}" Type="{REL_BASE}/{kind}" Target="{target}"/>'
            for rid, kind, target in relationships
        )
        + "</Relationships>"
    )
    namespaces = (
        f'xmlns:w="{W_NS}" xmlns:r="{R_NS}" '
        'xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"'
    )
    section = (
        f'<w:sectPr>{section_refs}<w:pgSz w:w="11906" w:h="16838"/>'
        '<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" '
        'w:header="708" w:footer="708" w:gutter="0"/></w:sectPr>'
    )
    document = f"{XML_HEADER}<w:document {namespaces}><w:body>{body}{section}</w:body></w:document>"

    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("[Content_Types].xml", content_types)
        archive.writestr("_rels/.rels", package_rels)
        archive.writestr("word/document.xml", document)
        archive.writestr("word/_rels/document.xml.rels", document_rels)
        if image is not None:
            archive.writestr("word/media/image1.png", image)
        if header is not None:
            archive.writestr("word/header1.xml", f"{XML_HEADER}<w:hdr {namespaces}>{paragraph(header)}</w:hdr>")
        if footer is not None:
            archive.writestr("word/footer1.xml", f"{XML_HEADER}<w:ftr {namespaces}>{paragraph(footer)}</w:ftr>")
    return buffer.getvalue()


def docx_samples() -> dict[str, bytes]:
    png = gradient_png(240, 160)
    rows = [["구분", "1분기", "2분기"], ["매출", "1,200", "1,450"], ["비용", "800", "910"]]
    multipage = "".join(
        paragraph(f"{page}쪽 제목 chapter{page}")
        + "".join(paragraph(PANGRAM * 2) for _ in range(8))
        + (page_break() if page < 6 else "")
        for page in range(1, 7)
    )
    return {
        "sample.docx": build_docx(
            paragraph("DocReader DOCX 샘플 문서")
            + paragraph("이 문서는 직접 생성했으며 저작권 문제가 없습니다.")
            + paragraph(PANGRAM)
        ),
        "sample-table.docx": build_docx(
            paragraph("표가 들어 있는 샘플 문서") + table(rows) + paragraph("표 아래 문단입니다.")
        ),
        "sample-image.docx": build_docx(
            paragraph("그림이 들어 있는 샘플 문서") + picture("rId2", 240, 160) + paragraph("그림 아래 문단입니다."),
            image=png,
        ),
        "sample-header-footer.docx": build_docx(
            paragraph("머리글과 바닥글이 들어 있는 샘플 문서") + paragraph(PANGRAM),
            header="DocReader 샘플 머리글",
            footer="DocReader 샘플 바닥글",
        ),
        "sample-multipage.docx": build_docx(multipage, header="여러 쪽 샘플 머리글", footer="여러 쪽 샘플 바닥글"),
        "sample-mixed.docx": build_docx(
            paragraph("표, 그림, 머리글, 바닥글이 모두 들어 있는 샘플 문서")
            + table(rows)
            + picture("rId2", 240, 160)
            + paragraph(PANGRAM),
            image=png,
            header="DocReader 샘플 머리글",
            footer="DocReader 샘플 바닥글",
        ),
    }


def main() -> None:
    out_dir = Path(sys.argv[1])
    plain = build_pdf(PAGE_COUNT)
    plain_path = out_dir / "sample-120pages.pdf"
    plain_path.write_bytes(plain)
    assert len(PdfReader(plain_path).pages) == PAGE_COUNT

    writer = PdfWriter()
    for page in PdfReader(plain_path).pages[:3]:
        writer.add_page(page)
    writer.encrypt(PASSWORD, algorithm="RC4-128")
    with open(out_dir / "sample-password.pdf", "wb") as f:
        writer.write(f)
    assert PdfReader(out_dir / "sample-password.pdf").is_encrypted

    # A valid header followed by a truncated body: opens as "PDF" by name but cannot be parsed.
    (out_dir / "sample-corrupt.pdf").write_bytes(plain[:700])

    samples = docx_samples()
    for name, data in samples.items():
        (out_dir / name).write_bytes(data)
    # Starts like a ZIP but has no central directory.
    (out_dir / "sample-corrupt.docx").write_bytes(samples["sample.docx"][:300])

    # The same text in both encodings the viewer has to tell apart.
    text = "텍스트 샘플\n\n한글과 English, 숫자 123.\n탭\t과   여러 칸 공백은 그대로 보인다.\n<b>태그</b>도 글자 그대로 보인다.\n"
    (out_dir / "sample-utf8.txt").write_bytes(text.encode("utf-8"))
    (out_dir / "sample-cp949.txt").write_bytes(text.encode("cp949"))
    print("written to", out_dir)


if __name__ == "__main__":
    main()
