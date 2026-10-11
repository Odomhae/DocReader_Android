package com.odom.docreader.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/** [type] is a [com.odom.docreader.core.model.DocumentType] name. */
@Serializable
data class ViewerRoute(val uri: String, val name: String, val type: String)

/** Shown when another app hands over a file this app cannot open. */
@Serializable
data object UnsupportedRoute

@Serializable
data object SettingsRoute

@Serializable
data object LicensesRoute
