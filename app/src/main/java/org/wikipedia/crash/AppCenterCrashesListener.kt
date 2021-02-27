package org.wikipedia.crash

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import com.microsoft.appcenter.crashes.AbstractCrashesListener
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog
import com.microsoft.appcenter.crashes.model.ErrorReport
import org.wikipedia.WikipediaApp
import org.wikipedia.json.GsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.util.*

class AppCenterCrashesListener : AbstractCrashesListener(), Thread.UncaughtExceptionHandler {
    private val props = HashMap<String, String>()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    fun putReportProperty(key: String, value: String): AppCenterCrashesListener {
        props[key] = value
        return this
    }

    fun logCrashManually(throwable: Throwable) {
        Crashes.trackError(throwable, null, getPropsAttachment())
    }

    override fun shouldProcess(report: ErrorReport?): Boolean {
        return Prefs.isCrashReportAutoUploadEnabled
    }

    override fun getErrorAttachments(report: ErrorReport?): Iterable<ErrorAttachmentLog> {
        return getPropsAttachment()
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        L.e(exception)
        if (!Prefs.crashedBeforeActivityCreated) {
            Prefs.crashedBeforeActivityCreated = true
            launchCrashReportActivity()
        } else {
            L.i("Crashed before showing UI. Skipping reboot.")
        }
        Runtime.getRuntime().exit(0)
    }

    private fun getPropsAttachment(): Iterable<ErrorAttachmentLog> {
        val textLog = ErrorAttachmentLog.attachmentWithText(GsonUtil.getDefaultGson().toJson(props), "details.txt")
        return listOf(textLog)
    }

    private fun launchCrashReportActivity() {
        val intent = Intent(WikipediaApp.getInstance(), CrashReportActivity::class.java)
                .addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
        WikipediaApp.getInstance().startActivity(intent)
    }
}
