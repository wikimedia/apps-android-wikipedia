package org.wikipedia.alphaupdater

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.recurring.RecurringTask
import org.wikipedia.settings.PrefsIoUtil
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit

class AlphaUpdateChecker(private val context: Context) : RecurringTask() {
    override val name = "alpha-update-checker"

    override fun shouldRun(lastRun: Date): Boolean {
        return System.currentTimeMillis() - lastRun.time >= RUN_INTERVAL_MILLI
    }

    override suspend fun run(lastRun: Date) {
        // Check for updates!
        var hashString: String? = null
        withContext(Dispatchers.IO) {
            try {
                val request: Request = Request.Builder().url(ALPHA_BUILD_DATA_URL).build()
                OkHttpConnectionFactory.client.newCall(request).execute().use {
                    hashString = it.body?.string()
                }
            } catch (e: IOException) {
                // It's ok, we can do nothing.
            }
        }
        hashString?.let {
            if (PrefsIoUtil.getString(PREFERENCE_KEY_ALPHA_COMMIT, "") != it) {
                showNotification()
            }
            PrefsIoUtil.setString(PREFERENCE_KEY_ALPHA_COMMIT, it)
        }
    }

    private fun showNotification() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ALPHA_BUILD_APK_URL))
        val pendingIntent = PendingIntentCompat.getActivity(context, 0, intent, 0, false)

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        val notificationCategory = NotificationCategory.ALPHA_BUILD_CHECKER

        val notificationBuilder = NotificationCompat.Builder(context, notificationCategory.id)
                .setContentTitle(context.getString(notificationCategory.title))
                .setContentText(context.getString(notificationCategory.description))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        notificationBuilder.setSmallIcon(notificationCategory.iconResId)
        notificationManagerCompat.notify(1, notificationBuilder.build())
    }

    companion object {
        private val RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(1)
        private const val PREFERENCE_KEY_ALPHA_COMMIT = "alpha_last_checked_commit"
        private const val ALPHA_BUILD_APK_URL = "https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/app-alpha-universal-release.apk"
        private const val ALPHA_BUILD_DATA_URL = "https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/rev-hash.txt"
    }
}
