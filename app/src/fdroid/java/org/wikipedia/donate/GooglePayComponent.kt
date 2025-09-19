package org.wikipedia.donate

import android.app.Activity
import android.content.Intent

object GooglePayComponent {
    const val CURRENCY_FALLBACK = "USD"

    suspend fun isGooglePayAvailable(activity: Activity): Boolean {
        return false
    }

    fun getDonateActivityIntent(activity: Activity, campaignId: String? = null, donateUrl: String? = null, filledAmount: Float = 0f): Intent {
        return Intent()
    }
}
