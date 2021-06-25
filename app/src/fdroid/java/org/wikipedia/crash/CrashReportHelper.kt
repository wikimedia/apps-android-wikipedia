package org.wikipedia.crash

import android.app.Application
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.util.*

class CrashReportHelper : Thread.UncaughtExceptionHandler {
    private val props = HashMap<String, String>()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    fun putReportProperty(key: String, value: String): CrashReportHelper {
        props[key] = value
        return this
    }

    fun logCrashManually(throwable: Throwable) {
        L.e(throwable)
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        L.e(exception)
        if (!Prefs.crashedBeforeActivityCreated()) {
            Prefs.crashedBeforeActivityCreated(true)
            launchCrashReportActivity()
        } else {
            L.i("Crashed before showing UI. Skipping reboot.")
        }
        Runtime.getRuntime().exit(0)
    }

    private fun launchCrashReportActivity() {
        val intent = Intent(WikipediaApp.getInstance(), CrashReportActivity::class.java)
                .addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
        WikipediaApp.getInstance().startActivity(intent)
    }

    companion object {
        fun register(app: Application, helper: CrashReportHelper) {
        }

        fun setEnabled(enabled: Boolean) {
        }
    }
}
