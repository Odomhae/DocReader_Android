package com.odom.docreader.core.model

data class DocumentFile(
    /** `content://` Uri as a string so this module stays free of Android types. */
    val uri: String,
    val name: String,
    val type: DocumentType,
    val sizeBytes: Long,
    val lastModified: Long,
)
