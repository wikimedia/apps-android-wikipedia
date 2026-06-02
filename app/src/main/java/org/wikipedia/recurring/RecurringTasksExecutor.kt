package org.wikipedia.recurring

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.alphaupdater.AlphaUpdateChecker
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.RemoteConfigRefreshTask
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.ThrowableUtil
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
            MainScope().launch(ThrowableUtil.MwCoroutineExceptionHandler { _, throwable, isNotLoggedIn ->
                L.e(throwable)
                if (isNotLoggedIn) {
                    AccountUtil.bailWithLogout(false)
                }
            }) {
                task.runIfNecessary()
            }
        }
    }
}
