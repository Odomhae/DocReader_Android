package com.odom.docreader.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenAdPolicyTest {
    private fun decide(
        firstLaunchSession: Boolean = false,
        millisSinceLastAd: Long? = null,
        adAvailable: Boolean = true,
    ) = shouldShowAppOpenAd(firstLaunchSession, millisSinceLastAd, adAvailable)

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
    fun theMinimumIntervalIsOneHour() {
        assertEquals(60 * 60 * 1000L, APP_OPEN_AD_MIN_INTERVAL_MILLIS)
    }

    @Test
    fun waitsTheMinimumIntervalAfterTheLastAd() {
        assertFalse(decide(millisSinceLastAd = 0))
        assertFalse(decide(millisSinceLastAd = APP_OPEN_AD_MIN_INTERVAL_MILLIS - 1))
        assertTrue(decide(millisSinceLastAd = APP_OPEN_AD_MIN_INTERVAL_MILLIS))
    }

    @Test
    fun aClockSetBackDoesNotBlockAds() {
        assertTrue(decide(millisSinceLastAd = -1))
    }
}
