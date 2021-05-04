package org.wikipedia.analytics

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.util.StringUtil
import java.util.concurrent.TimeUnit

class DailyStatsFunnel(app: WikipediaApp?) : Funnel(app!!, SCHEMA_NAME, SCHEMA_REVISION, SAMPLE_LOG_ALL) {

    private val absoluteTime = System.currentTimeMillis()

    fun log(context: Context) {
        log("appInstallAgeDays", getInstallAgeDays(context),
                "languages", StringUtil.listToJsonArrayString(app.language().appLanguageCodes),
                "is_anon", !AccountUtil.isLoggedIn)
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    private fun getInstallAgeDays(context: Context): Long {
        return TimeUnit.MILLISECONDS.toDays(getInstallAge(context))
    }

    private fun getInstallAge(context: Context): Long {
        return absoluteTime - getInstallTime(context)
    }

    private fun getInstallTime(context: Context): Long {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppDailyStats"
        private const val SCHEMA_REVISION = 18115101
    }
}
