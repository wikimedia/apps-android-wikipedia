package org.wikipedia.settings

import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.client
import org.wikipedia.recurring.RecurringTask
import org.wikipedia.util.log.L
import java.util.*
import java.util.concurrent.TimeUnit

class RemoteConfigRefreshTask : RecurringTask() {
    override val name = "remote-config-refresher"

    override fun shouldRun(lastRun: Date): Boolean {
        return System.currentTimeMillis() - lastRun.time >= RUN_INTERVAL_MILLI
    }

    override fun run(lastRun: Date) {
        var response: Response? = null
        try {
            val request = Request.Builder().url(REMOTE_CONFIG_URL).build()
            response = client.newCall(request).execute()
            val config = JSONObject(response.body!!.string())
            WikipediaApp.getInstance().remoteConfig.updateConfig(config)
            L.d(config.toString())
        } catch (e: Exception) {
            L.e(e)
        } finally {
            response?.close()
        }
    }

    companion object {
        // Switch over to production when it is available
        private const val REMOTE_CONFIG_URL = "https://meta.wikimedia.org/static/current/extensions/MobileApp/config/android.json"
        private val RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(1)
    }
}
