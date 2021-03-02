package org.wikipedia.alphaupdater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import okhttp3.Request
import okhttp3.Response
import org.wikipedia.R
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.client
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
        val pintent = PendingIntent.getActivity(context, 0, intent, 0)
        val notificationManager = context.getSystemService<NotificationManager>()!!

        // Notification channel ( >= API 26 )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(CHANNEL_ID, "Alpha updates", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(mChannel)
        }
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.alpha_update_notification_title))
                .setContentText(context.getString(R.string.alpha_update_notification_text))
                .setContentIntent(pintent)
                .setAutoCancel(true)
        notificationBuilder.setSmallIcon(R.drawable.ic_w_transparent)
        notificationManager.notify(1, notificationBuilder.build())
    }

    companion object {
        private val RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(1)
        private const val PREFERENCE_KEY_ALPHA_COMMIT = "alpha_last_checked_commit"
        private const val ALPHA_BUILD_APK_URL = "https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/app-alpha-universal-release.apk"
        private const val ALPHA_BUILD_DATA_URL = "https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/rev-hash.txt"
        private const val CHANNEL_ID = "ALPHA_UPDATE_CHECKER_CHANNEL"
    }
}
