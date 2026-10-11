package com.odom.docreader.viewer.pdf

enum class PdfLoadFailure {
    /** The user dismissed the password prompt. */
    PASSWORD_REQUIRED,

    /** Password protected, and this device's renderer cannot take a password (API 26-27). */
    PASSWORD_UNSUPPORTED,

    /** Damaged, not a PDF, or no longer readable. */
    UNREADABLE,
}
