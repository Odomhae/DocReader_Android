package com.odom.docreader

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.odom.docreader.core.ads.AdsController
import com.odom.docreader.core.ui.theme.DocReaderTheme
import com.odom.docreader.navigation.DocReaderNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

// FragmentActivity because the PDF viewer hosts androidx.pdf's PdfViewerFragment.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val appearanceViewModel: AppearanceViewModel by viewModels()

    @Inject
    lateinit var ads: AdsController

    private var homeShown = false

    // The app is light-only. Without this, a device in dark mode would still pick night
    // resources in androidx.pdf's views and light system bar icons in enableEdgeToEdge().
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        applyOverrideConfiguration(Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_NO })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // On re-creation the navigation back stack already holds the document that was opened.
        if (savedInstanceState == null) {
            handleViewIntent(intent)
            ads.gatherConsent(this)
            if (intent.isLauncherLaunch()) {
                lifecycleScope.launch {
                    // A late ad must not cover a document or appear while the app is in the background.
                    ads.showAppOpenAdOnLaunch(this@MainActivity) {
                        homeShown && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    }
                }
            }
        }
        setContent {
            val fontScale by appearanceViewModel.uiState.collectAsStateWithLifecycle()
            DocReaderTheme(fontScaleMultiplier = fontScale.multiplier) {
                val incomingDocument by viewModel.incomingDocument.collectAsStateWithLifecycle()
                DocReaderNavHost(
                    incomingDocument = incomingDocument,
                    onIncomingDocumentHandled = viewModel::onIncomingDocumentHandled,
                    onHomeShownChanged = { homeShown = it },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewIntent(intent)
    }

    private fun Intent.isLauncherLaunch(): Boolean =
        action == Intent.ACTION_MAIN && hasCategory(Intent.CATEGORY_LAUNCHER)

    private fun handleViewIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        viewModel.onViewIntent(
            uri = uri.toString(),
            mimeType = intent.type,
            persistable = intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0,
        )
    }
}
