package com.odom.docreader.core.ads

/** Shortest gap between two app open ads, so that switching apps back and forth is not punished. */
internal const val APP_OPEN_AD_MIN_INTERVAL_MILLIS = 30 * 60 * 1000L

/**
 * Whether to show an app open ad when the app is started from its icon or comes back from the
 * background.
 *
 * Skipped when the user only stepped out through the app itself (permission settings, share,
 * "open with"), since an ad would then look like an error; when another app handed over a document,
 * which the user wants to read right away; and on the very first launch, as AdMob asks.
 */
internal fun shouldShowAppOpenAd(
    firstLaunchSession: Boolean,
    leftThroughTheApp: Boolean,
    documentFromAnotherApp: Boolean,
    millisSinceLastAd: Long?,
    adAvailable: Boolean,
): Boolean =
    !firstLaunchSession &&
        !leftThroughTheApp &&
        !documentFromAnotherApp &&
        (millisSinceLastAd == null || millisSinceLastAd >= APP_OPEN_AD_MIN_INTERVAL_MILLIS) &&
        adAvailable
