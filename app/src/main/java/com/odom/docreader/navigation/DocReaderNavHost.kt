package com.odom.docreader.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.odom.docreader.IncomingDocument
import com.odom.docreader.core.model.DocumentType
import com.odom.docreader.feature.home.HomeScreen
import com.odom.docreader.feature.settings.LicensesScreen
import com.odom.docreader.feature.settings.SettingsScreen
import com.odom.docreader.viewer.docx.DocxViewerScreen
import com.odom.docreader.viewer.hwp.HwpViewerScreen
import com.odom.docreader.viewer.pdf.PdfViewerScreen

@Composable
fun DocReaderNavHost(incomingDocument: IncomingDocument?, onIncomingDocumentHandled: () -> Unit) {
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
            val route = backStackEntry.toRoute<ViewerRoute>()
            when (val type = DocumentType.entries.firstOrNull { it.name == route.type }) {
                DocumentType.PDF -> PdfViewerScreen(uri = route.uri, title = route.name, onBack = onBack)
                DocumentType.HWP, DocumentType.HWPX ->
                    HwpViewerScreen(uri = route.uri, title = route.name, type = type, onBack = onBack)
                DocumentType.DOCX -> DocxViewerScreen(uri = route.uri, title = route.name, onBack = onBack)
                // A route saved by a build that knew a format this build does not.
                null -> UnsupportedViewerScreen(onBack = onBack)
            }
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
