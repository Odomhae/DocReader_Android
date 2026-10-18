package com.odom.docreader.core.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenAdPolicyTest {
    private fun decide(
        firstLaunchSession: Boolean = false,
        leftThroughTheApp: Boolean = false,
        documentFromAnotherApp: Boolean = false,
        millisSinceLastAd: Long? = null,
        adAvailable: Boolean = true,
    ) = shouldShowAppOpenAd(firstLaunchSession, leftThroughTheApp, documentFromAnotherApp, millisSinceLastAd, adAvailable)

    @Test
    fun showsWhenALoadedAdIsWaiting() {
        assertTrue(decide())
    }

    @Test
    fun skipsWhenNoAdIsLoaded() {
        assertFalse(decide(adAvailable = false))
    }

    @Test
    fun skipsOnTheFirstLaunch() {
        assertFalse(decide(firstLaunchSession = true))
    }

    @Test
    fun skipsWhenComingBackFromSettingsOrShareOpenedByTheApp() {
        assertFalse(decide(leftThroughTheApp = true))
    }

    @Test
    fun skipsWhenAnotherAppOpenedADocument() {
        assertFalse(decide(documentFromAnotherApp = true))
    }

    @Test
    fun waitsTheMinimumIntervalAfterTheLastAd() {
        assertFalse(decide(millisSinceLastAd = APP_OPEN_AD_MIN_INTERVAL_MILLIS - 1))
        assertTrue(decide(millisSinceLastAd = APP_OPEN_AD_MIN_INTERVAL_MILLIS))
    }
}
