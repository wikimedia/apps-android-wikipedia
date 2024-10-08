package org.wikipedia.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.client
import org.wikipedia.recurring.RecurringTask
import org.wikipedia.util.log.L
import java.util.Date
import java.util.concurrent.TimeUnit

class RemoteConfigRefreshTask : RecurringTask() {
    override val name = "remote-config-refresher"

    override fun shouldRun(lastRun: Date): Boolean {
        return millisSinceLastRun(lastRun) >= TimeUnit.DAYS.toMillis(RUN_INTERVAL_DAYS)
    }

    override suspend fun run(lastRun: Date) {
        withContext(Dispatchers.IO) {
            var response: Response? = null
            try {
                val request = Request.Builder().url(REMOTE_CONFIG_URL).build()
                response = client.newCall(request).execute()
                val configStr = response.body!!.string()
                RemoteConfig.updateConfig(configStr)
                L.d(configStr)
            } catch (e: Exception) {
                L.e(e)
            } finally {
                response?.closeQuietly()
            }

            val userInfo = ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserInfo()
            // This clumsy comparison is necessary because the field is an integer value when enabled, but an empty string when disabled.
            // Since we want the default to lean towards opt-in, we check very specifically for an empty string, to make sure the user has opted out.
            val fundraisingOptOut = userInfo.query?.userInfo?.options?.fundraisingOptIn?.toString()?.replace("\"", "")?.isEmpty()
            Prefs.donationBannerOptIn = fundraisingOptOut != true
        }
    }

    companion object {
        private const val REMOTE_CONFIG_URL = "https://meta.wikimedia.org/w/extensions/MobileApp/config/android.json"
        private const val RUN_INTERVAL_DAYS = 1L
    }
}
