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

        mutableListOf(
            RemoteConfigRefreshTask(),
            DailyEventTask(app),
            TalkOfflineCleanupTask(app),
            CategoriesTableCleanupTask(),
            RecommendedReadingListTask()
        ).also {
            if (ReleaseUtil.isAlphaRelease) {
                it.add(AlphaUpdateChecker(app))
            }
        }.forEach { task ->
            MainScope().launch(CoroutineExceptionHandler { _, throwable ->
                L.e(throwable)
            }) {
                task.runIfNecessary()
            }
        }
    }
}
