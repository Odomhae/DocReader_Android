package com.odom.docreader.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdPreloader
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.odom.docreader.core.data.LaunchHistoryRepository
import com.odom.docreader.core.files.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns consent, SDK initialization and the preloaded ads for the whole process.
 *
 * Ad failures are logged by error code only and never shown to the user: an ad that does not load
 * simply leaves no space behind.
 */
@Singleton
class AdsController @Inject constructor(
    @ApplicationContext private val context: Context,
    launchHistory: LaunchHistoryRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val initializationStarted = AtomicBoolean(false)

    private val _state = MutableStateFlow(AdsState())
    val state: StateFlow<AdsState> = _state.asStateFlow()

    val bannerAdUnitId: String = context.getString(R.string.admob_banner_unit)
    private val appOpenAdUnitId = context.getString(R.string.admob_app_open_unit)
    private val exitDialogAdUnitId = context.getString(R.string.admob_exit_dialog_unit)

    // Assume the first launch until the stored history says otherwise.
    @Volatile
    private var firstLaunchSession = true

    init {
        scope.launch { firstLaunchSession = launchHistory.recordLaunch() }
    }

    /** Call on every launch. Shows the consent form only where and when UMP requires it. */
    fun gatherConsent(activity: Activity) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form failed: ${error.errorCode}")
                    onConsentUpdated()
                }
            },
            { error ->
                Log.w(TAG, "Consent update failed: ${error.errorCode}")
                onConsentUpdated()
            },
        )
        // Consent given in an earlier session already allows requests; no need to wait for the update.
        onConsentUpdated()
    }

    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Privacy options form failed: ${error.errorCode}")
            onConsentUpdated()
        }
    }

    /** Shows an app open ad if the user started the app from the launcher and one is ready. */
    fun showAppOpenAdIfEligible(activity: Activity, launchedFromLauncher: Boolean) {
        val adAvailable = _state.value.adsReady && AppOpenAdPreloader.isAdAvailable(appOpenAdUnitId)
        if (!shouldShowAppOpenAd(launchedFromLauncher, firstLaunchSession, adAvailable)) return
        val ad = AppOpenAdPreloader.pollAd(appOpenAdUnitId) ?: return
        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.w(TAG, "App open ad failed to show: ${fullScreenContentError.code}")
            }
        }
        ad.show(activity)
    }

    /** Takes the preloaded exit dialog ad, or null if none is ready. The caller destroys it. */
    fun pollExitDialogAd(): BannerAd? =
        if (_state.value.adsReady) BannerAdPreloader.pollAd(exitDialogAdUnitId) else null

    private fun onConsentUpdated() {
        val privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        _state.update { it.copy(privacyOptionsRequired = privacyOptionsRequired) }
        if (consentInformation.canRequestAds() && initializationStarted.compareAndSet(false, true)) {
            // Initializing on the main thread can cause an ANR.
            scope.launch { initialize() }
        }
    }

    private fun initialize() {
        val appId = context.getString(R.string.admob_app_id)
        MobileAds.initialize(context, InitializationConfig.Builder(appId).build()) {
            // The preloaders retry failed loads and replace expired ads on their own.
            AppOpenAdPreloader.start(
                appOpenAdUnitId,
                PreloadConfiguration(AdRequest.Builder(appOpenAdUnitId).build(), PRELOAD_BUFFER_SIZE),
                LoggingPreloadCallback,
            )
            BannerAdPreloader.start(
                exitDialogAdUnitId,
                PreloadConfiguration(
                    BannerAdRequest.Builder(exitDialogAdUnitId, AdSize.MEDIUM_RECTANGLE).build(),
                    PRELOAD_BUFFER_SIZE,
                ),
                LoggingPreloadCallback,
            )
            _state.update { it.copy(adsReady = true) }
        }
    }

    private object LoggingPreloadCallback : PreloadCallback {
        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
            Log.w(TAG, "Preload failed: ${adError.code}")
        }
    }

    private companion object {
        const val TAG = "Ads"

        // Each placement shows at most one ad at a time.
        const val PRELOAD_BUFFER_SIZE = 1
    }
}
