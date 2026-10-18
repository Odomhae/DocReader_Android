package com.odom.docreader.core.ads

/**
 * An app open ad is shown only when the user starts the app from its launcher icon and an ad is
 * already loaded: there is no loading screen to wait on, and an ad that pops up once the home list
 * is visible would cover content the user is already reading. AdMob also asks not to show one on
 * the very first launch.
 */
internal fun shouldShowAppOpenAd(
    launchedFromLauncher: Boolean,
    firstLaunchSession: Boolean,
    adAvailable: Boolean,
): Boolean = launchedFromLauncher && !firstLaunchSession && adAvailable
