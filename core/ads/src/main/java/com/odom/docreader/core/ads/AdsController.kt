package com.odom.docreader.core.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
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
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.odom.docreader.core.data.LaunchHistoryRepository
import com.odom.docreader.core.files.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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

    // Null until the stored launch history has been read.
    private val firstLaunchSession = MutableStateFlow<Boolean?>(null)
    private val appOpenAdAvailable = MutableStateFlow(false)
    private val returnFromBackgroundPending = AtomicBoolean(false)
    private val leftThroughTheApp = AtomicBoolean(false)
    private val documentFromAnotherApp = AtomicBoolean(false)

    @Volatile
    private var lastAppOpenAdAt: Long? = null

    init {
        scope.launch {
            try {
                firstLaunchSession.value = launchHistory.recordLaunch()
            } catch (e: IOException) {
                // Without the history the launch counts as the first, so no app open ad is shown.
                Log.w(TAG, "Launch history unavailable", e)
            }
        }
        // The singleton is created while MainActivity is injected, on the main thread that
        // ProcessLifecycleOwner requires.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) returnFromBackgroundPending.set(true)
            },
        )
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

    /** The app is about to open another app's screen: settings, the share sheet, "open with". */
    fun onLeavingThroughTheApp() {
        leftThroughTheApp.set(true)
    }

    /** Another app asked this app to open a document. */
    fun onDocumentFromAnotherApp() {
        documentFromAnotherApp.set(true)
    }

    /**
     * For an activity created from the launcher icon. Waits for an app open ad only as long as the
     * caller keeps the splash screen up; once the home list is visible an ad would cover it.
     */
    suspend fun showAppOpenAdOnLaunch(activity: Activity) {
        // This start is handled here, not as a return from the background.
        returnFromBackgroundPending.set(false)
        if (!consentInformation.canRequestAds()) return
        withTimeoutOrNull(LAUNCH_AD_MAX_WAIT_MILLIS) {
            if (firstLaunchSession.first { it != null } == true) return@withTimeoutOrNull
            appOpenAdAvailable.first { it }
        }
        showAppOpenAdIfEligible(activity)
    }

    /** Call from the activity's onResume. Shows an app open ad when the app came back from the background. */
    fun showAppOpenAdOnReturn(activity: Activity) {
        if (!returnFromBackgroundPending.getAndSet(false)) {
            // Screens that only pause the app, such as the permission dialog, must not suppress the
            // ad on a later, real return.
            leftThroughTheApp.set(false)
            documentFromAnotherApp.set(false)
            return
        }
        showAppOpenAdIfEligible(activity)
    }

    /** Takes the preloaded exit dialog ad, or null if none is ready. The caller destroys it. */
    fun pollExitDialogAd(): BannerAd? =
        if (_state.value.adsReady) BannerAdPreloader.pollAd(exitDialogAdUnitId) else null

    private fun showAppOpenAdIfEligible(activity: Activity) {
        val adAvailable = _state.value.adsReady && AppOpenAdPreloader.isAdAvailable(appOpenAdUnitId)
        val show = shouldShowAppOpenAd(
            firstLaunchSession = firstLaunchSession.value != false,
            leftThroughTheApp = leftThroughTheApp.getAndSet(false),
            documentFromAnotherApp = documentFromAnotherApp.getAndSet(false),
            millisSinceLastAd = lastAppOpenAdAt?.let { SystemClock.elapsedRealtime() - it },
            adAvailable = adAvailable,
        )
        if (!show || activity.isFinishing || activity.isDestroyed) return
        val ad = AppOpenAdPreloader.pollAd(appOpenAdUnitId) ?: return
        appOpenAdAvailable.value = AppOpenAdPreloader.isAdAvailable(appOpenAdUnitId)
        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.w(TAG, "App open ad failed to show: ${fullScreenContentError.code}")
            }
        }
        lastAppOpenAdAt = SystemClock.elapsedRealtime()
        ad.show(activity)
    }

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
                preloadCallback,
            )
            BannerAdPreloader.start(
                exitDialogAdUnitId,
                PreloadConfiguration(
                    BannerAdRequest.Builder(exitDialogAdUnitId, AdSize.MEDIUM_RECTANGLE).build(),
                    PRELOAD_BUFFER_SIZE,
                ),
                preloadCallback,
            )
            _state.update { it.copy(adsReady = true) }
        }
    }

    // The preload ID is the ad unit ID. AdMob forbids polling or restarting from inside these callbacks.
    private val preloadCallback = object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
            if (preloadId == appOpenAdUnitId) appOpenAdAvailable.value = true
        }

        override fun onAdsExhausted(preloadId: String) {
            if (preloadId == appOpenAdUnitId) appOpenAdAvailable.value = false
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
            Log.w(TAG, "Preload failed: ${adError.code}")
        }
    }

    private companion object {
        const val TAG = "Ads"

        // Each placement shows at most one ad at a time.
        const val PRELOAD_BUFFER_SIZE = 1

        // Longest the splash screen stays up on a launcher start waiting for an app open ad.
        const val LAUNCH_AD_MAX_WAIT_MILLIS = 2_000L
    }
}
