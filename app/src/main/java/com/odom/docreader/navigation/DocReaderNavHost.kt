package com.odom.docreader.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.odom.docreader.IncomingDocument
import com.odom.docreader.ReadingSettingsViewModel
import com.odom.docreader.feature.home.HomeScreen
import com.odom.docreader.feature.settings.LicensesScreen
import com.odom.docreader.feature.settings.SettingsScreen

@Composable
fun DocReaderNavHost(
    incomingDocument: IncomingDocument?,
    onIncomingDocumentHandled: () -> Unit,
    // Scoped to the activity, so every viewer opens with the settings already loaded.
    readingSettingsViewModel: ReadingSettingsViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val onBack: () -> Unit = { navController.popBackStack() }

    LaunchedEffect(incomingDocument) {
        when (incomingDocument) {
            null -> return@LaunchedEffect
            is IncomingDocument.Ready -> incomingDocument.file.let { file ->
                navController.navigate(ViewerRoute(file.uri, file.name, file.type.name))
            }

            IncomingDocument.Unsupported -> navController.navigate(UnsupportedRoute)
        }
        onIncomingDocumentHandled()
    }

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenDocument = { document ->
                    navController.navigate(ViewerRoute(document.uri, document.name, document.type.name))
                },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<ViewerRoute> { backStackEntry ->
            ViewerDestination(
                route = backStackEntry.toRoute<ViewerRoute>(),
                readingSettingsViewModel = readingSettingsViewModel,
                onBack = onBack,
            )
        }
        composable<UnsupportedRoute> {
            UnsupportedViewerScreen(onBack = onBack)
        }
        composable<SettingsRoute> {
            SettingsScreen(onOpenLicenses = { navController.navigate(LicensesRoute) }, onBack = onBack)
        }
        composable<LicensesRoute> {
            LicensesScreen(onBack = onBack)
        }
    }
}
