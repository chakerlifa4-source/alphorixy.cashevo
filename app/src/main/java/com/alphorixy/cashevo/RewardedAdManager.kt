package com.alphorixy.cashevo

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null

    // تحميل الإعلان في الخلفية (نستخدم Test Ad Unit ID الخاص بجوجل)
    fun loadAd(onAdLoaded: () -> Unit = {}) {
        val adRequest = AdRequest.Builder().build()
        // هذا معرف إعلان تجريبي حقيقي للفيديو بمكافأة من جوجل
        RewardedAd.load(
            context,
            "ca-app-pub-3940256099942544/5224354917",
            adRequest,
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

    // عرض الإعلان للمستخدم وإرجاع النتيجة
    fun showAd(activity: Activity, onRewardEarned: (Double) -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) { rewardItem ->
                // عند مشاهدة الإعلان كاملاً يحصل المستخدم على المكافأة (0.25$)
                onRewardEarned(0.25)
                // إعادة تحميل إعلان آخر جديد للمرة القادمة
                loadAd()
            }
        } else {
            // إعادة التحميل إذا لم يكن جاهزاً بعد
            loadAd()
        }
    }
}