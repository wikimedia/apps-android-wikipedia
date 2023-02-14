package org.wikipedia.recurring

import android.content.Context
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class TalkOfflineCleanupTask(context: Context) : RecurringTask() {
    override val name = context.getString(R.string.preference_key_talk_offline_cleanup_task_name)

    override fun shouldRun(lastRun: Date): Boolean {
        return timeSinceLastRun(lastRun) > CLEANUP_MAX_AGE_MILLIS
    }

    override fun run(lastRun: Date) {
        AppDatabase.instance.offlineObjectDao()
            .searchForOfflineObjects(CLEANUP_URL_SEARCH_KEY)
            .filter {
                (absoluteTime - File(it.path + ".0").lastModified()) > CLEANUP_MAX_AGE_MILLIS
            }.forEach {
                AppDatabase.instance.offlineObjectDao().deleteOfflineObject(it)
                AppDatabase.instance.offlineObjectDao().deleteFilesForObject(it)
            }
    }

    companion object {
        private val CLEANUP_URL_SEARCH_KEY = "action=discussiontoolspageinfo"
        private val CLEANUP_MAX_AGE_MILLIS = TimeUnit.DAYS.toMillis(7)
    }
}
