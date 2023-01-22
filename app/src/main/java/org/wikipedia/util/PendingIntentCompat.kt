package org.wikipedia.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

// TODO: Replace with AndroidX's PendingIntentCompat class once it is available.
object PendingIntentCompat {
    private fun addImmutabilityFlag(flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags or PendingIntent.FLAG_IMMUTABLE
        } else {
            flags
        }
    }

    fun getActivity(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent {
        return PendingIntent.getActivity(context, requestCode, intent, addImmutabilityFlag(flags))
    }

    fun getBroadcast(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent {
        return PendingIntent.getBroadcast(context, requestCode, intent, addImmutabilityFlag(flags))
    }
}
