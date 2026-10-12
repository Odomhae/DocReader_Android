package com.odom.docreader.feature.home

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.odom.docreader.core.files.StorageAccess

/**
 * Returns the action that asks for storage access. The result is not reported back: the home
 * screen re-scans whenever it resumes, which happens after either flow.
 */
@Composable
internal fun rememberStorageAccessRequest(): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Once the user has chosen "don't ask again" the system denies without showing a dialog,
        // which would leave the button looking dead.
        val dialogIsSuppressed = activity != null &&
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!granted && dialogIsSuppressed) {
            context.startActivity(StorageAccess.appDetailsIntent(context))
        }
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.startActivity(StorageAccess.allFilesAccessIntent(context))
            } catch (e: ActivityNotFoundException) {
                // Some devices only ship the screen that lists every app.
                context.startActivity(StorageAccess.allFilesAccessListIntent())
            }
        } else {
            permissionRequest.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
