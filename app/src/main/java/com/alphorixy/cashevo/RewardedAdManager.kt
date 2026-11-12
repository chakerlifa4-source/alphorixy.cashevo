package com.alphorixy.cashevo

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions

class RewardedAdManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null

    companion object {
        private const val TAG = "RewardedAdManager"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-2573504616567477/معرفك_الحقيقي_هنا"
    }

    fun loadAd(onAdLoaded: () -> Unit = {}) {
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        userId: String,
        onNotReady: () -> Unit = {},
        onRewardEarned: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null) {
            loadAd()
            onNotReady()
            return
        }

        ad.setServerSideVerificationOptions(
            ServerSideVerificationOptions.Builder().setUserId(userId).build()
        )

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                loadAd()
            }
        }

        rewardedAd = null
        ad.show(activity) { onRewardEarned() }
    }
}