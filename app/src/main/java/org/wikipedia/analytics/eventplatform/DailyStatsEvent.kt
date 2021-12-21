package org.wikipedia.analytics.eventplatform

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import java.util.concurrent.TimeUnit

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_daily_stats/1.0.0")
class DailyStatsEvent(private val app_install_age_in_days: Long,
                      private val languages: List<String>,
                      private val is_anon: Boolean) : AppsEvent(STREAM_NAME) {

    companion object {
        private const val STREAM_NAME = "android.daily_stats"

        fun log(app: WikipediaApp) {
            EventPlatformClient.submit(DailyStatsEvent(getInstallAgeDays(app), app.language().appLanguageCodes, !AccountUtil.isLoggedIn))
        }

        private fun getInstallAgeDays(context: Context): Long {
            return TimeUnit.MILLISECONDS.toDays(getInstallAge(context))
        }

        private fun getInstallAge(context: Context): Long {
            return System.currentTimeMillis() - getInstallTime(context)
        }

        private fun getInstallTime(context: Context): Long {
            return try {
                context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException(e)
            }
        }
    }
}
