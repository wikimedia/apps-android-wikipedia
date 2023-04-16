package org.wikipedia.settings

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.util.log.L

class RemoteConfigRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val request = Request.Builder().url(REMOTE_CONFIG_URL).build()

        return try {
            val configStr = withContext(Dispatchers.IO) {
                OkHttpConnectionFactory.client.newCall(request).execute().use { it.body!!.string() }
            }
            RemoteConfig.updateConfig(configStr)
            L.d(configStr)
            Result.success()
        } catch (e: Exception) {
            L.e(e)
            Result.failure()
        }
    }

    companion object {
        private const val REMOTE_CONFIG_URL = "https://meta.wikimedia.org/w/extensions/MobileApp/config/android.json"
    }
}
