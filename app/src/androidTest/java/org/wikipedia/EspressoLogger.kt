package org.wikipedia

import android.util.Log

class EspressoLogger {
    companion object {
        private const val TAG = "EspressoError"

        fun logError(message: String) {
            Log.e(TAG, message)
        }
    }
}
