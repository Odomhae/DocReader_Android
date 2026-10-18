package com.odom.docreader.core.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Gives screens in other modules access to the process-wide [AdsController]. */
@HiltViewModel
class AdsViewModel @Inject constructor(
    private val controller: AdsController,
) : ViewModel() {
    val uiState: StateFlow<AdsState> = controller.state

    internal val bannerAdUnitId: String get() = controller.bannerAdUnitId

    internal fun pollExitDialogAd(): BannerAd? = controller.pollExitDialogAd()

    fun showPrivacyOptionsForm(activity: Activity) = controller.showPrivacyOptionsForm(activity)
}
