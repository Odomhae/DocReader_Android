package com.odom.docreader.core.files

import com.odom.docreader.core.model.DocumentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentTypeDetectorTest {
    private val cfbHeader = byteArrayOf(
        0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte(), 0, 0,
    )
    private val zipPrefix = byteArrayOf(0x50, 0x4B, 0x03, 0x04) + ByteArray(26)
    private val hwpxHeader = zipPrefix + "mimetypeapplication/hwp+zip".toByteArray(Charsets.US_ASCII)
    private val docxHeader = zipPrefix + "[Content_Types].xml".toByteArray(Charsets.US_ASCII)

    @Test
    fun detectsByExtensionIgnoringCase() {
        assertEquals(DocumentType.PDF, DocumentTypeDetector.detect("report.PDF", null))
        assertEquals(DocumentType.DOCX, DocumentTypeDetector.detect("letter.docx", null))
        assertEquals(DocumentType.HWP, DocumentTypeDetector.detect("공문.hwp", null))
        assertEquals(DocumentType.HWPX, DocumentTypeDetector.detect("공문.HwpX", null))
    }

    @Test
    fun extensionWinsOverMimeType() {
        assertEquals(DocumentType.HWP, DocumentTypeDetector.detect("a.hwp", "application/pdf"))
        assertEquals(DocumentType.HWP, DocumentTypeDetector.detect("a.hwp", "application/octet-stream"))
    }

    @Test
    fun detectsByMimeTypeWhenExtensionIsUnknown() {
        assertEquals(DocumentType.PDF, DocumentTypeDetector.detect("download", "application/pdf"))
        assertEquals(DocumentType.PDF, DocumentTypeDetector.detect("file.bin", "Application/PDF; charset=binary"))
        assertEquals(DocumentType.HWP, DocumentTypeDetector.detect(null, "application/x-hwp"))
        assertEquals(DocumentType.HWP, DocumentTypeDetector.detect(null, "application/haansofthwp"))
        assertEquals(DocumentType.HWPX, DocumentTypeDetector.detect(null, "application/hwp+zip"))
        assertEquals(
            DocumentType.DOCX,
            DocumentTypeDetector.detect(null, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        )
    }

    @Test
    fun unsupportedFormatsAreRejected() {
        assertNull(DocumentTypeDetector.detect("old.doc", "application/msword"))
        assertNull(DocumentTypeDetector.detect("sheet.xlsx", null))
        assertNull(DocumentTypeDetector.detect("slides.pptx", null))
        assertNull(DocumentTypeDetector.detect("notes.txt", "text/plain"))
        assertNull(DocumentTypeDetector.detect(null, null))
    }

    @Test
    fun signatureIsUsedOnlyWithoutExtensionAndMimeType() {
        val pdfHeader = "%PDF-1.7".toByteArray(Charsets.US_ASCII)
        assertEquals(DocumentType.PDF, DocumentTypeDetector.detect("download", null, pdfHeader))
        assertEquals(DocumentType.PDF, DocumentTypeDetector.detect(null, "application/octet-stream", pdfHeader))
        assertEquals(DocumentType.HWP, DocumentTypeDetector.detect("download", "application/octet-stream", cfbHeader))
        assertEquals(DocumentType.HWPX, DocumentTypeDetector.detect("download", null, hwpxHeader))
    }

    @Test
    fun cfbContainerWithAnotherExtensionIsNotHwp() {
        assertNull(DocumentTypeDetector.detect("old.doc", "application/octet-stream", cfbHeader))
        assertNull(DocumentTypeDetector.detect("old.doc", null, cfbHeader))
    }

    @Test
    fun bareZipAndUnknownSignaturesStayUnknown() {
        assertNull(DocumentTypeDetector.detect("download", null, docxHeader))
        assertNull(DocumentTypeDetector.detect("download", null, byteArrayOf(1, 2, 3)))
        assertNull(DocumentTypeDetector.detect("download", null, ByteArray(0)))
    }

    @Test
    fun mimeTypeOfRoundTripsThroughDetect() {
        DocumentType.entries.forEach { type ->
            assertEquals(type, DocumentTypeDetector.detect(null, DocumentTypeDetector.mimeTypeOf(type)))
        }
    }
}
