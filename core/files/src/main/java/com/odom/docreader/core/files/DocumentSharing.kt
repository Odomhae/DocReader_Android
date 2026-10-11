package com.odom.docreader.core.files

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.odom.docreader.core.model.DocumentType

/** Opens the system share sheet for a document, handing the receiver temporary read access. */
fun shareDocument(context: Context, uri: Uri, type: DocumentType) {
    val send = Intent(Intent.ACTION_SEND)
        .setType(DocumentTypeDetector.mimeTypeOf(type))
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // The grant flag only applies to Uris in the intent data or ClipData, not to EXTRA_STREAM.
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, null))
}

/** Lets the user pick another installed app to view a document this app could not show. */
fun openWithOtherApp(context: Context, uri: Uri, type: DocumentType) {
    val view = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, DocumentTypeDetector.mimeTypeOf(type))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // The chooser itself tells the user when nothing can open the file, so there is no failure to handle.
    context.startActivity(Intent.createChooser(view, null))
}
