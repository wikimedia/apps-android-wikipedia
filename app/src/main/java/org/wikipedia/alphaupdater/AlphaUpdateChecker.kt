package org.wikipedia.alphaupdater

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import okhttp3.Request
import okhttp3.Response
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.client
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.recurring.RecurringTask
import org.wikipedia.settings.PrefsIoUtil
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class AlphaUpdateChecker(private val context: Context) : RecurringTask() {
    override val name = "alpha-update-checker"

    override fun shouldRun(lastRun: Date): Boolean {
        return System.currentTimeMillis() - lastRun.time >= RUN_INTERVAL_MILLI
    }

    override fun run(lastRun: Date) {
        // Check for updates!
        val hashString: String
        var response: Response? = null
        try {
            val request: Request = Request.Builder().url(ALPHA_BUILD_DATA_URL).build()
            response = client.newCall(request).execute()
            hashString = response.body!!.string()
        } catch (e: IOException) {
            // It's ok, we can do nothing.
            return
        } finally {
            response?.close()
        }
        if (PrefsIoUtil.getString(PREFERENCE_KEY_ALPHA_COMMIT, "") != hashString) {
            showNotification()
        }
        PrefsIoUtil.setString(PREFERENCE_KEY_ALPHA_COMMIT, hashString)
    }

    private fun showNotification() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ALPHA_BUILD_APK_URL))
        val pintent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        val notificationCategory = NotificationCategory.ALPHA_BUILD_CHECKER

        val notificationBuilder = NotificationCompat.Builder(context, notificationCategory.id)
                .setContentTitle(context.getString(notificationCategory.title))
                .setContentText(context.getString(notificationCategory.description))
                .setContentIntent(pintent)
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
