package org.wikipedia.analytics.metricsplatform

import android.content.Context
import android.content.pm.PackageManager
import org.wikipedia.WikipediaApp
import java.util.concurrent.TimeUnit

class DailyStatsEvent : MetricsEvent() {
    fun log(app: WikipediaApp) {
        submitEvent(
            "daily_stats",
            mapOf(
                "install_age_days" to getInstallAgeDays(app)
            )
        )
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
