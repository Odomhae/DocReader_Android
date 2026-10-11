package com.odom.docreader.core.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface FolderAccess {
    /** Keeps read access to a picked tree Uri across restarts. */
    fun persist(treeUri: String)

    fun release(treeUri: String)
}

class SafFolderAccess @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : FolderAccess {

    override fun persist(treeUri: String) {
        context.contentResolver.takePersistableUriPermission(
            Uri.parse(treeUri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    override fun release(treeUri: String) {
        val uri = Uri.parse(treeUri)
        val isHeld = context.contentResolver.persistedUriPermissions.any { it.uri == uri }
        // Releasing a grant that is not held throws.
        if (isHeld) {
            context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
