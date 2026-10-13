package com.odom.docreader.feature.home

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.odom.docreader.core.files.StorageAccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Below 0x10000, where ActivityResultRegistry starts handing out its own request codes.
private const val ALL_FILES_ACCESS_REQUEST_CODE = 0x5A01
private const val ACCESS_POLL_INTERVAL_MS = 300L

/**
 * Returns the action that asks for storage access. The result is not reported back: the home
 * screen re-scans whenever it resumes, which happens after either flow.
 */
@Composable
internal fun rememberStorageAccessRequest(): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var returnWhenGranted by remember { mutableStateOf<Job?>(null) }
    // Back from settings, by the wait below or by the user's own back press.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { returnWhenGranted?.cancel() }
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
            if (activity == null) {
                context.startActivity(StorageAccess.allFilesAccessIntent(context))
            } else {
                activity.openAllFilesAccessSettings()
                // The settings screen does not close itself when the switch is turned on, so
                // it is closed from here; the coroutine keeps running while the app is stopped.
                returnWhenGranted?.cancel()
                returnWhenGranted = scope.launch {
                    while (!StorageAccess.isGranted(context)) delay(ACCESS_POLL_INTERVAL_MS)
                    activity.finishActivity(ALL_FILES_ACCESS_REQUEST_CODE)
                }
            }
        } else {
            permissionRequest.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

/**
 * Started for a result only so that it can be closed again with [Activity.finishActivity]; the
 * result itself is ignored.
 */
@RequiresApi(Build.VERSION_CODES.R)
@Suppress("DEPRECATION")
private fun Activity.openAllFilesAccessSettings() {
    try {
        startActivityForResult(StorageAccess.allFilesAccessIntent(this), ALL_FILES_ACCESS_REQUEST_CODE)
    } catch (e: ActivityNotFoundException) {
        // Some devices only ship the screen that lists every app.
        startActivityForResult(StorageAccess.allFilesAccessListIntent(), ALL_FILES_ACCESS_REQUEST_CODE)
    }
}
