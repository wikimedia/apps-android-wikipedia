package org.wikipedia.settings

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.IOException
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.client
import org.wikipedia.util.log.L

class RemoteConfigRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val configStr = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(REMOTE_CONFIG_URL).build()
            try {
                client.newCall(request).execute().body!!.use { it.string() }
            } catch (e: IOException) {
                L.e(e)
                null
            }
        }
        if (configStr == null) {
            return Result.failure()
        }
        RemoteConfig.updateConfig(configStr)
        L.d(configStr)
        return Result.success()
    }

    companion object {
        private const val REMOTE_CONFIG_URL = "https://meta.wikimedia.org/w/extensions/MobileApp/config/android.json"
    }
}
