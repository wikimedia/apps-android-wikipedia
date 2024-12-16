package org.wikipedia

import android.util.Log

object EspressoLogger {
    private const val TAG = "EspressoError"

    fun logError(message: String) {
        Log.e(TAG, message)
    }
}
