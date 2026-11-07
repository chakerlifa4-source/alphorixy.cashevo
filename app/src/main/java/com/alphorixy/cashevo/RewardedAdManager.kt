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
        // TODO: استبدله بمعرّف وحدة الإعلان المكافئ (Rewarded) الخاصة بك من AdMob.
        // المعرّف الحالي تجريبي تابع لجوجل: لن تصل منه استدعاءات SSV إلى السيرفر الخاص بك.
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
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

    /**
     * عرض الإعلان. لا يُمنح أي رصيد من هنا:
     * المكافأة تُضاف من السيرفر بعد تحقق AdMob (SSV) باستخدام userId المُمرَّر.
     * onRewardEarned للواجهة فقط (رسالة شكر مثلاً).
     */
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

        // يجب ضبطه قبل العرض؛ AdMob يرسله للسيرفر كـ user_id
        ad.setServerSideVerificationOptions(
            ServerSideVerificationOptions.Builder().setUserId(userId).build()
        )

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadAd() // تحميل إعلان جديد بعد الإغلاق
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                loadAd()
            }
        }

        // الإعلان الواحد لا يُعرض إلا مرة واحدة: نفرّغ المرجع فوراً لمنع الضغط المزدوج
        rewardedAd = null
        ad.show(activity) { onRewardEarned() }
    }
}