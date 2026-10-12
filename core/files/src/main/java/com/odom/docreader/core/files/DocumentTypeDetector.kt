package com.odom.docreader.core.files

import com.odom.docreader.core.model.DocumentType

object DocumentTypeDetector {
    /** How many leading bytes [detect] looks at when it is given a header. */
    const val HEADER_SIZE = 64

    private val typesByExtension = mapOf(
        "pdf" to DocumentType.PDF,
        "docx" to DocumentType.DOCX,
        "hwp" to DocumentType.HWP,
        "hwpx" to DocumentType.HWPX,
    )

    val extensions: Set<String> get() = typesByExtension.keys

    // HWP has no single registered MIME type; providers report any of these.
    private val typesByMimeType = mapOf(
        "application/pdf" to DocumentType.PDF,
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to DocumentType.DOCX,
        "application/x-hwp" to DocumentType.HWP,
        "application/haansofthwp" to DocumentType.HWP,
        "application/vnd.hancom.hwp" to DocumentType.HWP,
        "application/hwp+zip" to DocumentType.HWPX,
        "application/haansofthwpx" to DocumentType.HWPX,
        "application/vnd.hancom.hwpx" to DocumentType.HWPX,
    )

    private val genericMimeTypes = setOf("application/octet-stream", "*/*")

    private val pdfMagic = "%PDF-".toByteArray(Charsets.US_ASCII)
    private val cfbMagic = byteArrayOf(
        0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte(),
    )
    private val zipMagic = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    private val hwpxMimetypeEntry = "mimetypeapplication/hwp+zip".toByteArray(Charsets.US_ASCII)

    /**
     * Extension wins, then a specific MIME type. The file signature is only consulted for a file
     * with no extension and no useful MIME type, so a `.doc` (also a CFB container) is never
     * mistaken for HWP.
     */
    fun detect(fileName: String?, mimeType: String?, header: ByteArray? = null): DocumentType? {
        val extension = fileName?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase().orEmpty()
        typesByExtension[extension]?.let { return it }

        val normalizedMimeType = mimeType?.substringBefore(';')?.trim()?.lowercase()
        typesByMimeType[normalizedMimeType]?.let { return it }

        val mimeTypeIsUseless = normalizedMimeType == null || normalizedMimeType in genericMimeTypes
        if (extension.isEmpty() && mimeTypeIsUseless && header != null) {
            return detectBySignature(header)
        }
        return null
    }

    fun mimeTypeOf(type: DocumentType): String = when (type) {
        DocumentType.PDF -> "application/pdf"
        DocumentType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        DocumentType.HWP -> "application/x-hwp"
        DocumentType.HWPX -> "application/hwp+zip"
    }

    private fun detectBySignature(header: ByteArray): DocumentType? = when {
        header.startsWith(pdfMagic) -> DocumentType.PDF
        // HWP 5 is the only CFB format this app opens; the viewer reports anything else as an error.
        header.startsWith(cfbMagic) -> DocumentType.HWP
        // HWPX stores an uncompressed "mimetype" entry first, like EPUB. DOCX has no such marker
        // near the start, so a bare ZIP stays unknown.
        header.startsWith(zipMagic) && header.contains(hwpxMimetypeEntry) -> DocumentType.HWPX
        else -> null
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private fun ByteArray.contains(needle: ByteArray): Boolean =
        (0..size - needle.size).any { start -> needle.indices.all { this[start + it] == needle[it] } }
}
