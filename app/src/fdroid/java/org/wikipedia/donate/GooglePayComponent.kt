package org.wikipedia.donate

import android.app.Activity

object GooglePayComponent {
    suspend fun isGooglePayAvailable(activity: Activity): Boolean {
        return false
    }

    fun onGooglePayButtonClicked(activity: Activity) {
    }
}
