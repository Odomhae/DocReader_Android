package com.odom.docreader.core.ads

/** Shortest gap between two app open ads, so that opening the app several times a day is not punished. */
internal const val APP_OPEN_AD_MIN_INTERVAL_MILLIS = 60 * 60 * 1000L

/**
 * Whether to show an app open ad when the app is started from its icon.
 *
 * Skipped on the very first launch, as AdMob asks. [millisSinceLastAd] comes from the wall clock so
 * that it survives the process; a negative value means the clock was set back, and must not block
 * ads until it catches up.
 */
internal fun shouldShowAppOpenAd(
    firstLaunchSession: Boolean,
    millisSinceLastAd: Long?,
    adAvailable: Boolean,
): Boolean =
    !firstLaunchSession &&
        (millisSinceLastAd == null || millisSinceLastAd !in 0 until APP_OPEN_AD_MIN_INTERVAL_MILLIS) &&
        adAvailable
