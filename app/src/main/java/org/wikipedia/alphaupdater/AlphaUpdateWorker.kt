package org.wikipedia.alphaupdater

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.settings.PrefsIoUtil
import org.wikipedia.util.log.L

class AlphaUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        // Check for updates!
        val request = Request.Builder().url(ALPHA_BUILD_DATA_URL).build()
        val hashString = try {
            withContext(Dispatchers.IO) {
                OkHttpConnectionFactory.client.newCall(request).execute().use { it.body!!.string() }
            }
        } catch (e: Exception) {
            L.e(e)
            return Result.failure()
        }

        if (PrefsIoUtil.getString(PREFERENCE_KEY_ALPHA_COMMIT, "") != hashString) {
            showNotification()
        }
        PrefsIoUtil.setString(PREFERENCE_KEY_ALPHA_COMMIT, hashString)
        return Result.success()
    }

    private fun showNotification() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ALPHA_BUILD_APK_URL))
        val pendingIntent = PendingIntentCompat.getActivity(applicationContext, 0, intent, 0, false)
        val notificationCategory = NotificationCategory.ALPHA_BUILD_CHECKER

        val notification = NotificationCompat.Builder(applicationContext, notificationCategory.id)
            .setContentTitle(applicationContext.getString(notificationCategory.title))
            .setContentText(applicationContext.getString(notificationCategory.description))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSmallIcon(notificationCategory.iconResId)
            .build()

        applicationContext.getSystemService<NotificationManager>()!!.notify(1, notification)
    }

    companion object {
        private const val PREFERENCE_KEY_ALPHA_COMMIT = "alpha_last_checked_commit"
        private const val ALPHA_BUILD_APK_URL = "https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/app-alpha-universal-release.apk"
        private const val ALPHA_BUILD_DATA_URL = "https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/rev-hash.txt"
    }
}
