package org.wikipedia.recurring

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.alphaupdater.AlphaUpdateChecker
import org.wikipedia.settings.RemoteConfigRefreshTask
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.log.L

class RecurringTasksExecutor() {
    fun run() {
        val app = WikipediaApp.instance
        MainScope().launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            RemoteConfigRefreshTask().runIfNecessary()
            DailyEventTask(app).runIfNecessary()
            TalkOfflineCleanupTask(app).runIfNecessary()
            if (ReleaseUtil.isAlphaRelease) {
                AlphaUpdateChecker(app).runIfNecessary()
            }
        }
    }
}
