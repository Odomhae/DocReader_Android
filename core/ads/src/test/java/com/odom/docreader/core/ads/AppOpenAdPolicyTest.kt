package com.odom.docreader.core.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenAdPolicyTest {
    @Test
    fun showsWhenLaunchedFromLauncherWithALoadedAd() {
        assertTrue(shouldShowAppOpenAd(launchedFromLauncher = true, firstLaunchSession = false, adAvailable = true))
    }

    @Test
    fun skipsWhenNoAdIsLoadedYet() {
        assertFalse(shouldShowAppOpenAd(launchedFromLauncher = true, firstLaunchSession = false, adAvailable = false))
    }

    @Test
    fun skipsWhenAnotherAppOpenedADocument() {
        assertFalse(shouldShowAppOpenAd(launchedFromLauncher = false, firstLaunchSession = false, adAvailable = true))
    }

    @Test
    fun skipsOnTheFirstLaunch() {
        assertFalse(shouldShowAppOpenAd(launchedFromLauncher = true, firstLaunchSession = true, adAvailable = true))
    }
}
