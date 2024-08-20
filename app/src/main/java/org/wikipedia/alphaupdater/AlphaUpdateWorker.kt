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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.IOException
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.client
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.settings.PrefsIoUtil
import java.util.concurrent.TimeUnit

class AlphaUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val hashString = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(ALPHA_BUILD_DATA_URL).build()
            try {
                client.newCall(request).execute().body!!.use { it.string() }
            } catch (e: IOException) {
                null
            }
        }
        if (hashString == null) {
            return Result.failure()
        }
        if (PrefsIoUtil.getString(PREFERENCE_KEY_ALPHA_COMMIT, "") != hashString) {
            showNotification()
        }
        PrefsIoUtil.setString(PREFERENCE_KEY_ALPHA_COMMIT, hashString)
        return Result.success()
    }

    private fun showNotification() {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ALPHA_BUILD_APK_URL))
        val pendingIntent = PendingIntentCompat.getActivity(applicationContext, 0, intent, 0, false)

        val notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        val notificationCategory = NotificationCategory.ALPHA_BUILD_CHECKER

        val notificationBuilder = NotificationCompat.Builder(applicationContext, notificationCategory.id)
            .setContentTitle(applicationContext.getString(notificationCategory.title))
            .setContentText(applicationContext.getString(notificationCategory.description))
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
