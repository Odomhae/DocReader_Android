package com.odom.docreader.core.ads

data class AdsState(
    /** Consent allows ad requests and the SDK has been initialized. */
    val adsReady: Boolean = false,
    /** UMP requires a privacy options entry point (EEA and UK users). */
    val privacyOptionsRequired: Boolean = false,
)
