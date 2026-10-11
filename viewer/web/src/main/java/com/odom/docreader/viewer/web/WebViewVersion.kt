package com.odom.docreader.viewer.web

import android.content.Context
import androidx.webkit.WebViewCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal fun parseWebViewMajorVersion(versionName: String?): Int? =
    versionName?.substringBefore('.')?.trim()?.toIntOrNull()

// An unreadable version is let through: the viewer page still reports a real failure.
internal fun isWebViewSupported(versionName: String?, minMajorVersion: Int): Boolean {
    val major = parseWebViewMajorVersion(versionName) ?: return true
    return major >= minMajorVersion
}

interface WebViewVersionProvider {
    /** Version name of the installed WebView implementation, or null if it cannot be read. */
    fun currentVersionName(): String?
}

class SystemWebViewVersionProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : WebViewVersionProvider {
    override fun currentVersionName(): String? = WebViewCompat.getCurrentWebViewPackage(context)?.versionName
}
